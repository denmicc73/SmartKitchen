package com.smartkitchen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartkitchen.model.Alimento;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Ayudante opcional basado en IA. Genera ideas de recetas a partir de los
 * alimentos que caducan pronto.
 *
 * Solo se envían a la IA los nombres/cantidades de los alimentos (lo mínimo
 * necesario). Nunca se envían datos de usuarios.
 *
 * Si no hay ANTHROPIC_API_KEY configurada funciona en modo "offline",
 * devolviendo una sugerencia simple basada en reglas, para que la app
 * sea usable sin depender de la IA.
 *
 */
@Service
public class AsistenteIaService {

    private final AlimentoService alimentoService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${smartkitchen.ia.api-key}")
    private String apiKey;

    @Value("${smartkitchen.ia.modelo}")
    private String modelo;

    public AsistenteIaService(AlimentoService alimentoService) {
        this.alimentoService = alimentoService;
    }

    public boolean iaDisponible() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String sugerirRecetas(int diasHorizonte) {
        List<Alimento> proximosACaducar = alimentoService.proximosACaducar(diasHorizonte);

        if (proximosACaducar.isEmpty()) {
            return "No tienes alimentos próximos a caducar en los próximos " + diasHorizonte + " días. ¡Todo en orden!";
        }

        String listaIngredientes = proximosACaducar.stream()
                .map(a -> a.getNombre() + " (" + a.getCantidad() + " " + a.getUnidad() + ")")
                .collect(Collectors.joining(", "));

        if (!iaDisponible()) {
            return "Modo sin IA: estos ingredientes caducan pronto y deberías usarlos ya: "
                    + listaIngredientes
                    + ". Configura ANTHROPIC_API_KEY para recibir recetas generadas automáticamente.";
        }

        return llamarIA(listaIngredientes);
    }

    private String llamarIA(String ingredientes) {
        try {
            String prompt = "Tengo estos ingredientes que caducan pronto en mi casa: " + ingredientes +
                    ". Sugiéreme 2 recetas sencillas y rápidas para aprovecharlos, con pasos breves.";

            String body = objectMapper.writeValueAsString(new AnthropicRequest(modelo, prompt));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            String response = restTemplate.postForObject(
                    "https://api.anthropic.com/v1/messages", request, String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode content = root.path("content");
            if (content.isArray() && content.size() > 0) {
                return content.get(0).path("text").asText("No se pudo generar la receta.");
            }
            return "No se pudo generar la receta (respuesta inesperada de la IA).";
        } catch (Exception e) {
            return "No se pudo contactar con la IA en este momento (" + e.getMessage() + "). "
                    + "Ingredientes a usar pronto: " + ingredientes;
        }
    }

    // DTO mínimo para el body de la API de Anthropic
    private record AnthropicRequest(String model, int max_tokens, java.util.List<Message> messages) {
        AnthropicRequest(String model, String prompt) {
            this(model, 1000, java.util.List.of(new Message("user", prompt)));
        }
    }

    private record Message(String role, String content) {}
}
