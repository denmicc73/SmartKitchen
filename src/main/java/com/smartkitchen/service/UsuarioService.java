package com.smartkitchen.service;

import com.smartkitchen.model.Usuario;
import com.smartkitchen.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService implements UserDetailsService, CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Value("${smartkitchen.admin.username}")
    private String adminUsername;

    @Value("${smartkitchen.admin.password}")
    private String adminPassword;

    @Value("${smartkitchen.admin.email:}")
    private String adminEmail;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, Environment environment) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        if (!usuario.isActivo()) {
            throw new UsernameNotFoundException("Usuario desactivado: " + username);
        }

        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol())))
                .build();
    }

    public Usuario crearUsuario(String username, String passwordPlano, String rol) {
        String limpio = username == null ? "" : username.trim();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        }
        if (usuarioRepository.findByUsername(limpio).isPresent()) {
            throw new IllegalArgumentException("Ya existe un usuario con ese nombre.");
        }
        if (passwordPlano == null || passwordPlano.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        }
        String rolFinal = "ADMIN".equals(rol) ? "ADMIN" : "MIEMBRO";
        Usuario u = new Usuario(limpio, passwordEncoder.encode(passwordPlano), rolFinal);
        // Los usuarios creados a mano por el admin son de confianza: no pasan por
        // confirmacion de correo.
        u.setEmailVerificado(true);
        u.setActivo(true);
        return usuarioRepository.save(u);
    }

    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + id));
    }

    /** Activa/desactiva un usuario. No permite desactivar al último ADMIN activo. */
    public void alternarActivo(Long id) {
        Usuario u = buscarPorId(id);
        if (u.isActivo() && u.isAdmin() && contarAdminsActivos() <= 1) {
            throw new IllegalStateException("Debe quedar al menos un administrador activo.");
        }
        u.setActivo(!u.isActivo());
        usuarioRepository.save(u);
    }

    /** Actualiza el perfil (nombre visible y avatar) del propio usuario. */
    public void actualizarPerfil(String username, String nombreVisible, String avatarEmoji) {
        Usuario u = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
        u.setNombreVisible(nombreVisible == null || nombreVisible.isBlank() ? null : nombreVisible.trim());
        if (avatarEmoji != null && !avatarEmoji.isBlank()) {
            u.setAvatarEmoji(avatarEmoji.trim());
        }
        usuarioRepository.save(u);
    }

    public void cambiarPassword(String username, String actual, String nueva) {
        Usuario u = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
        if (!passwordEncoder.matches(actual, u.getPasswordHash())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        }
        if (nueva == null || nueva.length() < 8) {
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos 8 caracteres.");
        }
        u.setPasswordHash(passwordEncoder.encode(nueva));
        usuarioRepository.save(u);
    }

    private long contarAdminsActivos() {
        return usuarioRepository.findAll().stream()
                .filter(Usuario::isActivo)
                .filter(Usuario::isAdmin)
                .count();
    }

    @Override
    public void run(String... args) {
        validarCredencialesProduccion();

        if (usuarioRepository.count() == 0) {
            Usuario admin = crearUsuario(adminUsername, adminPassword, "ADMIN");
            if (adminEmail != null && !adminEmail.isBlank()) {
                admin.setEmail(adminEmail.trim().toLowerCase());
                usuarioRepository.save(admin);
            }
            System.out.println("Usuario admin inicial creado: " + adminUsername);
        } else {
            // Si el admin ya existe pero no tiene correo y ahora hay uno configurado,
            // lo guardamos para que pueda usar "He olvidado mi contrasena".
            if (adminEmail != null && !adminEmail.isBlank()) {
                usuarioRepository.findByUsername(adminUsername).ifPresent(admin -> {
                    if (admin.getEmail() == null || admin.getEmail().isBlank()) {
                        admin.setEmail(adminEmail.trim().toLowerCase());
                        admin.setEmailVerificado(true);
                        usuarioRepository.save(admin);
                        System.out.println("Correo del admin actualizado: " + admin.getEmail());
                    }
                });
            }
        }
    }

    private void validarCredencialesProduccion() {
        for (String perfil : environment.getActiveProfiles()) {
            if ("prod".equals(perfil)) {
                if (adminPassword == null || adminPassword.length() < 16 || "changeme123".equals(adminPassword)) {
                    throw new IllegalStateException("ADMIN_PASSWORD debe tener al menos 16 caracteres en produccion.");
                }
                return;
            }
        }
    }
}
