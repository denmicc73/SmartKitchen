package com.smartkitchen.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartkitchen.model.CategoriaCompra;
import com.smartkitchen.model.Zona;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reconoce un producto a partir de una foto usando la IA de Anthropic (vision).
 *
 * <p>Se envia solo la imagen y una instruccion; nunca datos de usuarios ni del
 * inventario. Si no hay {@code ANTHROPIC_API_KEY} configurada, lanza
 * {@link IllegalStateException} para que la interfaz avise de que hay que
 * configurar la clave.
 */
@Service
public class EscanerProductoService {

    private static final Logger log = LoggerFactory.getLogger(EscanerProductoService.class);

    private static final Set<String> TIPOS_IMAGEN = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");
    private static final long MAX_BYTES = 6L * 1024 * 1024;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;

    @Value("${smartkitchen.ia.api-key:}")
    private String apiKey;

    @Value("${smartkitchen.ia.modelo:claude-sonnet-4-6}")
    private String modelo;

    public EscanerProductoService(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(60))
                .build();
    }

    public boolean iaDisponible() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Resultado del reconocimiento.
     *
     * @param fechaCaducidad  fecha de caducidad ya resuelta: la impresa en la
     *                        etiqueta si la IA la ha leido, o una estimacion
     *                        (hoy + vida util tipica) si no. Puede ser null.
     * @param caducidadEstimada  true si {@code fechaCaducidad} es una estimacion,
     *                           false si se ha leido de la etiqueta.
     */
    public record ProductoDetectado(
            String nombre,
            CategoriaCompra categoria,
            Zona zonaSugerida,
            String unidad,
            Double cantidad,
            LocalDate fechaCaducidad,
            boolean caducidadEstimada,
            String marca,
            int confianza,
            String nota) {
    }

    public ProductoDetectado detectar(byte[] imagen, String tipoContenido) {
        if (!iaDisponible()) {
            throw new IllegalStateException(
                    "El escaner necesita una clave de IA. Configura ANTHROPIC_API_KEY (ver ia.properties.example).");
        }
        if (imagen == null || imagen.length == 0) {
            throw new IllegalArgumentException("No se ha recibido ninguna foto.");
        }
        if (imagen.length > MAX_BYTES) {
            throw new IllegalArgumentException("La foto es demasiado grande (maximo 6 MB).");
        }
        String tipo = tipoContenido == null ? "" : tipoContenido.toLowerCase(Locale.ROOT);
        if (!TIPOS_IMAGEN.contains(tipo)) {
            throw new IllegalArgumentException("Formato de imagen no admitido. Usa JPG, PNG, WEBP o GIF.");
        }

        try {
            String respuesta = llamarIa(Base64.getEncoder().encodeToString(imagen), tipo);
            return parsear(respuesta);
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Fallo al reconocer el producto con la IA: {}", e.getMessage());
            throw new RuntimeException("No se ha podido reconocer el producto ahora mismo. Intentalo de nuevo.");
        }
    }

    // --------------------------------------------------------------- interno

    private String llamarIa(String imagenBase64, String mediaType) throws Exception {
        String categorias = Arrays.stream(CategoriaCompra.values())
                .map(c -> c.name() + " (" + c.getEtiqueta() + ")")
                .collect(Collectors.joining(", "));
        String zonas = Arrays.stream(Zona.values())
                .map(z -> z.name() + " (" + z.getEtiqueta() + ")")
                .collect(Collectors.joining(", "));

        String hoy = LocalDate.now().toString();
        String instruccion = """
                Identifica el producto de alimentacion o del hogar que aparece en la foto \
                para el inventario de una cocina domestica. Hoy es %s.

                Responde SOLO con un objeto JSON valido, sin texto alrededor, con estas claves:
                - "nombre": nombre corto del producto en espanol (ej. "Leche entera").
                - "marca": marca si se ve, o null.
                - "categoria": una de [%s]. Usa el codigo en MAYUSCULAS.
                - "zona": donde se guarda en casa, una de [%s]. Usa el codigo en MAYUSCULAS.
                - "unidad": unidad tipica ("unidad", "l", "ml", "g", "kg", "paquete"...).
                - "cantidad": numero estimado de esa unidad segun el envase, o null.
                - "fecha_caducidad": la fecha impresa en el envase, en formato AAAA-MM-DD. \
                Busca textos como "consumir preferentemente antes de", "fecha de caducidad", \
                "consumir antes de", "CAD", "EXP", "F. Cons.", "best before". Si solo se ve \
                mes y ano, usa el ultimo dia de ese mes. Si NO se lee ninguna fecha, null.
                - "fecha_leida": true si "fecha_caducidad" la has leido del envase; false si es \
                estimada o null.
                - "dias_caducidad": SOLO si "fecha_caducidad" es null: vida util tipica del \
                producto sin abrir, en dias desde hoy. Si no, null.
                - "confianza": entero 0-100 de lo seguro que estas del producto.

                Si no reconoces bien el producto, pon tu mejor estimacion y una confianza baja.
                """.formatted(hoy, categorias, zonas);

        var contenido = List.of(
                objectMapper.createObjectNode()
                        .put("type", "image")
                        .set("source", objectMapper.createObjectNode()
                                .put("type", "base64")
                                .put("media_type", mediaType)
                                .put("data", imagenBase64)),
                objectMapper.createObjectNode()
                        .put("type", "text")
                        .put("text", instruccion));

        var mensaje = objectMapper.createObjectNode();
        mensaje.put("role", "user");
        mensaje.set("content", objectMapper.valueToTree(contenido));

        var body = objectMapper.createObjectNode();
        body.put("model", modelo);
        body.put("max_tokens", 512);
        body.set("messages", objectMapper.createArrayNode().add(mensaje));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        String respuesta = restTemplate.postForObject(
                "https://api.anthropic.com/v1/messages",
                new HttpEntity<>(objectMapper.writeValueAsString(body), headers),
                String.class);

        JsonNode root = objectMapper.readTree(respuesta);
        JsonNode content = root.path("content");
        if (content.isArray() && !content.isEmpty()) {
            return content.get(0).path("text").asText("");
        }
        throw new IllegalStateException("Respuesta inesperada de la IA.");
    }

    private ProductoDetectado parsear(String texto) throws Exception {
        int ini = texto.indexOf('{');
        int fin = texto.lastIndexOf('}');
        if (ini < 0 || fin <= ini) {
            throw new IllegalStateException("La IA no ha devuelto un producto reconocible.");
        }
        DeteccionDto d = objectMapper.readValue(texto.substring(ini, fin + 1), DeteccionDto.class);

        String nombre = (d.nombre == null || d.nombre.isBlank()) ? "Producto" : d.nombre.trim();
        CategoriaCompra categoria = enumSeguro(CategoriaCompra.class, d.categoria, CategoriaCompra.OTROS);
        Zona zona = enumSeguro(Zona.class, d.zona, Zona.DESPENSA);
        String unidad = (d.unidad == null || d.unidad.isBlank()) ? "unidad" : d.unidad.trim();
        int confianza = Math.max(0, Math.min(100, d.confianza));

        // Fecha de la etiqueta si es legible y razonable; si no, estimacion por vida util.
        LocalDate fechaEtiqueta = fechaRazonable(parsearFecha(d.fecha_caducidad));
        LocalDate fechaCaducidad;
        boolean estimada;
        if (fechaEtiqueta != null) {
            fechaCaducidad = fechaEtiqueta;
            estimada = false;
        } else if (d.dias_caducidad != null && d.dias_caducidad > 0) {
            fechaCaducidad = LocalDate.now().plusDays(d.dias_caducidad);
            estimada = true;
        } else {
            fechaCaducidad = null;
            estimada = false;
        }

        return new ProductoDetectado(nombre, categoria, zona, unidad, d.cantidad,
                fechaCaducidad, estimada, blancoANull(d.marca), confianza, null);
    }

    /** Acepta AAAA-MM-DD y AAAA-MM (ultimo dia de mes). Devuelve null si no encaja. */
    private static LocalDate parsearFecha(String v) {
        if (v == null || v.isBlank() || "null".equalsIgnoreCase(v.trim())) return null;
        String s = v.trim();
        try {
            return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception ignored) {
            // sigue
        }
        try {
            if (s.matches("\\d{4}-\\d{1,2}")) {
                return YearMonth.parse(s.length() == 6 ? s.replace("-", "-0") : s).atEndOfMonth();
            }
        } catch (Exception ignored) {
            // sigue
        }
        return null;
    }

    /** Descarta fechas absurdas (probablemente mal leidas). */
    private static LocalDate fechaRazonable(LocalDate f) {
        if (f == null) return null;
        LocalDate hoy = LocalDate.now();
        if (f.isBefore(hoy.minusYears(5)) || f.isAfter(hoy.plusYears(15))) return null;
        return f;
    }

    private static <E extends Enum<E>> E enumSeguro(Class<E> tipo, String valor, E porDefecto) {
        if (valor == null || valor.isBlank()) return porDefecto;
        try {
            return Enum.valueOf(tipo, valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return porDefecto;
        }
    }

    private static String blancoANull(String s) {
        return (s == null || s.isBlank() || "null".equalsIgnoreCase(s.trim())) ? null : s.trim();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class DeteccionDto {
        public String nombre;
        public String marca;
        public String categoria;
        public String zona;
        public String unidad;
        public Double cantidad;
        public String fecha_caducidad;
        public Boolean fecha_leida;
        public Integer dias_caducidad;
        public int confianza;
    }
}
