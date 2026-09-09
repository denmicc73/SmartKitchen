package com.smartkitchen.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Envio de correo de la aplicacion (confirmacion de cuenta y reseteo de clave).
 *
 * <p>Elige el canal de envio en este orden:
 * <ol>
 *   <li><b>API HTTP de Brevo</b> si hay {@code smartkitchen.mail.brevo.api-key}.
 *       Es la unica opcion que funciona en hosts que bloquean el SMTP saliente,
 *       como el plan gratuito de Render.</li>
 *   <li><b>SMTP</b> si hay {@code spring.mail.host/username/password} (util en
 *       local o en servidores propios).</li>
 *   <li><b>Log</b> si no hay nada configurado: escribe el asunto y el cuerpo en
 *       la consola para poder copiar el enlace a mano (modo desarrollo).</li>
 * </ol>
 *
 * <p>Ningun metodo lanza excepcion: si el envio falla se registra y se devuelve
 * {@code false}. Los correos se maquetan en HTML + texto plano en {@link #enviarAccion}.
 */
@Service
public class CorreoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoService.class);
    private static final String BREVO_ENDPOINT = "https://api.brevo.com/v3/smtp/email";

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String remitente;
    private final String host;
    private final String usuario;
    private final boolean smtpConfigurado;

    private final String apiKey;
    private final boolean apiConfigurada;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper json = new ObjectMapper();

    public CorreoService(ObjectProvider<JavaMailSender> mailSender,
                         @Value("${smartkitchen.mail.from:}") String remitente,
                         @Value("${spring.mail.host:}") String host,
                         @Value("${spring.mail.username:}") String usuario,
                         @Value("${spring.mail.password:}") String password,
                         @Value("${smartkitchen.mail.brevo.api-key:}") String apiKey) {
        this.mailSender = mailSender;
        this.host = host == null ? "" : host.trim();
        this.usuario = usuario == null ? "" : usuario.trim();
        this.remitente = (remitente == null || remitente.isBlank()) ? this.usuario : remitente.trim();
        this.smtpConfigurado = !this.host.isBlank() && !this.usuario.isBlank()
                && password != null && !password.isBlank();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.apiConfigurada = !this.apiKey.isBlank();
    }

    @PostConstruct
    void avisarEstado() {
        if (apiConfigurada) {
            log.info("Envio de correo ACTIVADO — API HTTP (Brevo), remitente {}", remitente);
        } else if (smtpConfigurado) {
            log.info("Envio de correo ACTIVADO — SMTP {}, remitente {}", host, remitente);
        } else {
            log.info("Envio de correo DESACTIVADO (sin BREVO_API_KEY ni SMTP_USER/SMTP_PASS). "
                    + "Los enlaces de confirmacion y reseteo se escribiran en el log.");
        }
    }

    /**
     * Envia un correo "de accion": saludo, un parrafo, un boton grande que lleva
     * a {@code url} y una nota al pie. Se manda en HTML + texto plano.
     *
     * @return {@code true} si el correo se envio de verdad. Nunca lanza excepcion.
     */
    public boolean enviarAccion(String destino, String asunto, String nombre, String titulo,
                                String intro, String textoBoton, String url, String notaPie) {
        String html = plantillaHtml(titulo, "Hola " + nombre + ":", intro, textoBoton, url, notaPie);
        String texto = plantillaTexto(titulo, "Hola " + nombre + ":", intro, textoBoton, url, notaPie);
        return enviar(destino, asunto, texto, html);
    }

    /**
     * Envio de bajo nivel. {@code html} puede ser {@code null} (solo texto).
     * Nunca lanza excepcion: si no hay SMTP o el envio falla, lo registra en el
     * log y devuelve {@code false}.
     */
    public boolean enviar(String destino, String asunto, String textoPlano, String html) {
        if (apiConfigurada) {
            return enviarPorApi(destino, asunto, textoPlano, html);
        }
        JavaMailSender sender = smtpConfigurado ? mailSender.getIfAvailable() : null;
        if (sender == null) {
            log.info("""
                    Correo NO enviado (sin BREVO_API_KEY ni SMTP, modo desarrollo).
                    ------------------------------------------------------------
                    Para   : {}
                    Asunto : {}

                    {}
                    ------------------------------------------------------------""",
                    destino, asunto, textoPlano);
            return false;
        }
        try {
            MimeMessage mensaje = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mensaje, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");
            if (remitente.contains("<")) {
                helper.setFrom(remitente);
            } else {
                helper.setFrom(remitente, "Smart Kitchen");
            }
            helper.setTo(destino);
            helper.setSubject(asunto);
            if (html != null && !html.isBlank()) {
                helper.setText(textoPlano, html);
            } else {
                helper.setText(textoPlano, false);
            }
            sender.send(mensaje);
            log.info("Correo enviado a {} · {}", destino, asunto);
            return true;
        } catch (Exception e) {
            log.error("No se pudo enviar el correo a {}: {}", destino, e.getMessage());
            return false;
        }
    }

    /** Compatibilidad: envio en texto plano sin HTML. */
    public boolean enviar(String destino, String asunto, String cuerpo) {
        return enviar(destino, asunto, cuerpo, null);
    }

    // ------------------------------------------------------------ API HTTP (Brevo)

    /**
     * Envia el correo por la API transaccional de Brevo (HTTP). Se usa cuando el
     * host bloquea el SMTP saliente (p. ej. el plan gratis de Render). El
     * remitente debe ser una direccion verificada en la cuenta de Brevo; se toma
     * de {@code smartkitchen.mail.from} (MAIL_FROM).
     */
    private boolean enviarPorApi(String destino, String asunto, String textoPlano, String html) {
        try {
            String emailRem = emailDe(remitente);
            String nombreRem = remitente.contains("<")
                    ? remitente.substring(0, remitente.indexOf('<')).trim()
                    : "Smart Kitchen";
            if (nombreRem.isBlank()) {
                nombreRem = "Smart Kitchen";
            }

            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("sender", Map.of("name", nombreRem, "email", emailRem));
            cuerpo.put("to", List.of(Map.of("email", destino)));
            cuerpo.put("subject", asunto);
            cuerpo.put("textContent", textoPlano);
            if (html != null && !html.isBlank()) {
                cuerpo.put("htmlContent", html);
            }

            HttpRequest peticion = HttpRequest.newBuilder(URI.create(BREVO_ENDPOINT))
                    .header("api-key", apiKey)
                    .header("content-type", "application/json")
                    .header("accept", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(
                            json.writeValueAsString(cuerpo), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> respuesta = httpClient.send(
                    peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (respuesta.statusCode() / 100 == 2) {
                log.info("Correo enviado (Brevo) a {} · {}", destino, asunto);
                return true;
            }
            log.error("Brevo rechazo el correo a {} (HTTP {}): {}",
                    destino, respuesta.statusCode(), respuesta.body());
            return false;
        } catch (Exception e) {
            log.error("No se pudo enviar el correo (Brevo) a {}: {}", destino, e.getMessage());
            return false;
        }
    }

    /** Extrae la direccion de un remitente en formato "Nombre <correo>" o "correo". */
    private static String emailDe(String remitente) {
        int abre = remitente.indexOf('<');
        int cierra = remitente.indexOf('>');
        if (abre >= 0 && cierra > abre) {
            return remitente.substring(abre + 1, cierra).trim();
        }
        return remitente.trim();
    }

    // ------------------------------------------------------------ plantillas

    private static String plantillaTexto(String titulo, String saludo, String intro,
                                         String textoBoton, String url, String notaPie) {
        return """
                SMART KITCHEN

                %s

                %s

                %s

                %s:
                %s

                %s

                — Smart Kitchen · el centro de la cocina de casa
                """.formatted(titulo, saludo, intro, textoBoton, url, notaPie);
    }

    private static String plantillaHtml(String titulo, String saludo, String intro,
                                        String textoBoton, String url, String notaPie) {
        String t = esc(titulo);
        String s = esc(saludo);
        String i = esc(intro);
        String b = esc(textoBoton);
        String u = esc(url);
        String n = esc(notaPie);
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;padding:0;background:#f6f3ee;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#f6f3ee;">
                  <tr><td align="center" style="padding:28px 12px;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="max-width:460px;width:100%%;background:#ffffff;border:1px solid #ece5db;border-radius:16px;">
                      <tr><td style="padding:26px 30px 4px;font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;">
                        <span style="font-size:18px;font-weight:700;color:#3d7358;">&#127869;&#65039; Smart Kitchen</span>
                      </td></tr>
                      <tr><td style="padding:10px 30px 0;font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;">
                        <h1 style="margin:0 0 4px;font-size:20px;line-height:1.3;color:#2c2825;font-weight:700;">%s</h1>
                      </td></tr>
                      <tr><td style="padding:10px 30px 0;font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;font-size:15px;line-height:1.6;color:#2c2825;">
                        <p style="margin:0 0 12px;">%s</p>
                        <p style="margin:0 0 20px;">%s</p>
                      </td></tr>
                      <tr><td style="padding:2px 30px 4px;font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;">
                        <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                          <td style="border-radius:12px;background:#4f8a6b;">
                            <a href="%s" style="display:inline-block;padding:13px 28px;font-size:15px;font-weight:600;color:#ffffff;text-decoration:none;">%s</a>
                          </td>
                        </tr></table>
                      </td></tr>
                      <tr><td style="padding:18px 30px 0;font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;font-size:13px;line-height:1.6;color:#8a8078;">
                        Si el bot&oacute;n no funciona, copia esta direcci&oacute;n en tu navegador:<br>
                        <span style="color:#3d7358;word-break:break-all;">%s</span>
                      </td></tr>
                      <tr><td style="padding:22px 30px 26px;font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;">
                        <div style="border-top:1px solid #ece5db;margin:0 0 14px;"></div>
                        <p style="margin:0 0 6px;font-size:12px;line-height:1.6;color:#8a8078;">%s</p>
                        <p style="margin:0;font-size:12px;color:#b7afa2;">Smart Kitchen &middot; el centro de la cocina de casa</p>
                      </td></tr>
                    </table>
                  </td></tr>
                </table>
                </body>
                </html>
                """.formatted(t, s, i, u, b, u, n);
    }

    private static String esc(String v) {
        if (v == null) return "";
        return v.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
