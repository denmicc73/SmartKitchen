package com.smartkitchen.service;

import com.smartkitchen.model.TipoToken;
import com.smartkitchen.model.TokenCuenta;
import com.smartkitchen.model.Usuario;
import com.smartkitchen.repository.TokenCuentaRepository;
import com.smartkitchen.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;

/**
 * Alta de usuarios por registro publico, confirmacion de correo y recuperacion
 * de contrasena. Todo el envio de correo pasa por {@link CorreoService}, que en
 * desarrollo simplemente escribe el enlace en el log.
 */
@Service
public class CuentaService {

    private static final Logger log = LoggerFactory.getLogger(CuentaService.class);

    private static final Duration VIDA_CONFIRMACION = Duration.ofHours(24);
    private static final Duration VIDA_RESET = Duration.ofHours(1);
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UsuarioRepository usuarios;
    private final TokenCuentaRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final CorreoService correo;
    private final String baseUrl;

    public CuentaService(UsuarioRepository usuarios,
                         TokenCuentaRepository tokens,
                         PasswordEncoder passwordEncoder,
                         CorreoService correo,
                         @Value("${smartkitchen.base-url}") String baseUrl) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.correo = correo;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    // ---------------------------------------------------------------- registro

    @Transactional
    public void registrar(String username, String email, String password) {
        String u = username == null ? "" : username.trim();
        String e = email == null ? "" : email.trim().toLowerCase();

        if (u.length() < 3) {
            throw new IllegalArgumentException("El nombre de usuario debe tener al menos 3 caracteres.");
        }
        if (!EMAIL.matcher(e).matches()) {
            throw new IllegalArgumentException("El correo no tiene un formato valido.");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("La contrasena debe tener al menos 8 caracteres.");
        }
        if (usuarios.existsByUsernameIgnoreCase(u)) {
            throw new IllegalArgumentException("Ya existe un usuario con ese nombre.");
        }
        if (usuarios.existsByEmailIgnoreCase(e)) {
            throw new IllegalArgumentException("Ya hay una cuenta registrada con ese correo.");
        }

        Usuario nuevo = new Usuario(u, passwordEncoder.encode(password), "MIEMBRO");
        nuevo.setEmail(e);
        nuevo.setActivo(false);
        nuevo.setEmailVerificado(false);
        usuarios.save(nuevo);
        log.info("Nueva alta pendiente de confirmar: {} <{}>", u, e);

        enviarConfirmacion(nuevo);
    }

    /** Reenvia el correo de confirmacion si la cuenta existe y aun no esta verificada. */
    @Transactional
    public void reenviarConfirmacion(String email) {
        String e = email == null ? "" : email.trim().toLowerCase();
        usuarios.findByEmailIgnoreCase(e)
                .filter(usr -> !usr.isEmailVerificado())
                .ifPresent(this::enviarConfirmacion);
    }

    private void enviarConfirmacion(Usuario usr) {
        tokens.borrarPorUsuarioYTipo(usr, TipoToken.CONFIRMACION);
        TokenCuenta token = tokens.save(
                new TokenCuenta(usr, TipoToken.CONFIRMACION, Instant.now().plus(VIDA_CONFIRMACION)));
        String enlace = baseUrl + "/confirmar?token=" + token.getValor();
        correo.enviarAccion(usr.getEmail(),
                "Confirma tu cuenta de Smart Kitchen",
                usr.getUsername(),
                "Confirma tu cuenta",
                "Ya casi está. Pulsa el botón para activar tu cuenta y empezar a usar Smart Kitchen. "
                        + "El enlace caduca en 24 horas.",
                "Confirmar mi cuenta",
                enlace,
                "Si no has creado ninguna cuenta en Smart Kitchen, puedes ignorar este mensaje.");
    }

    @Transactional
    public void confirmar(String valorToken) {
        TokenCuenta token = tokens.findByValorAndTipo(valorToken, TipoToken.CONFIRMACION)
                .orElseThrow(() -> new IllegalArgumentException("El enlace de confirmacion no es valido."));
        if (!token.esValido()) {
            throw new IllegalArgumentException("El enlace de confirmacion ha caducado. Pide uno nuevo desde la pantalla de acceso.");
        }
        Usuario usr = token.getUsuario();
        usr.setEmailVerificado(true);
        usr.setActivo(true);
        token.setUsado(true);
        log.info("Cuenta confirmada: {}", usr.getUsername());
    }

    // ------------------------------------------------------- reseteo de clave

    /** Envia un enlace de reseteo. No revela si el correo existe o no. */
    @Transactional
    public void solicitarReset(String email) {
        String e = email == null ? "" : email.trim().toLowerCase();
        usuarios.findByEmailIgnoreCase(e).ifPresentOrElse(usr -> {
            tokens.borrarPorUsuarioYTipo(usr, TipoToken.RESET);
            TokenCuenta token = tokens.save(
                    new TokenCuenta(usr, TipoToken.RESET, Instant.now().plus(VIDA_RESET)));
            String enlace = baseUrl + "/reset?token=" + token.getValor();
            correo.enviarAccion(usr.getEmail(),
                    "Restablece tu contraseña de Smart Kitchen",
                    usr.getUsername(),
                    "Restablece tu contraseña",
                    "Has pedido una nueva contraseña para tu cuenta. Pulsa el botón para elegir una. "
                            + "El enlace caduca en 1 hora.",
                    "Crear nueva contraseña",
                    enlace,
                    "Si no has sido tú, ignora este mensaje: tu contraseña no cambiará.");
        }, () -> log.info("Solicitud de reseteo para un correo sin cuenta: {}", e));
    }

    @Transactional(readOnly = true)
    public void validarTokenReset(String valorToken) {
        TokenCuenta token = tokens.findByValorAndTipo(valorToken, TipoToken.RESET)
                .orElseThrow(() -> new IllegalArgumentException("El enlace no es valido."));
        if (!token.esValido()) {
            throw new IllegalArgumentException("El enlace ha caducado. Solicita uno nuevo.");
        }
    }

    @Transactional
    public void resetear(String valorToken, String nuevaPassword) {
        if (nuevaPassword == null || nuevaPassword.length() < 8) {
            throw new IllegalArgumentException("La contrasena debe tener al menos 8 caracteres.");
        }
        TokenCuenta token = tokens.findByValorAndTipo(valorToken, TipoToken.RESET)
                .orElseThrow(() -> new IllegalArgumentException("El enlace no es valido."));
        if (!token.esValido()) {
            throw new IllegalArgumentException("El enlace ha caducado. Solicita uno nuevo.");
        }
        Usuario usr = token.getUsuario();
        usr.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usr.setActivo(true);
        usr.setEmailVerificado(true);
        token.setUsado(true);
        log.info("Contrasena restablecida: {}", usr.getUsername());
    }

    // ----------------------------------------------------------------- limpieza

    /** Borra tokens caducados una vez al dia. */
    @Scheduled(fixedDelay = 24 * 60 * 60 * 1000L, initialDelay = 60 * 1000L)
    @Transactional
    public void limpiarTokensCaducados() {
        int borrados = tokens.borrarCaducados(Instant.now());
        if (borrados > 0) {
            log.info("Limpieza: {} tokens de cuenta caducados eliminados", borrados);
        }
    }
}
