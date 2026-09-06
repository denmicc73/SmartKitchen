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

@Service
public class UsuarioService implements UserDetailsService, CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Value("${smartkitchen.admin.username}")
    private String adminUsername;

    @Value("${smartkitchen.admin.password}")
    private String adminPassword;

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
        Usuario u = new Usuario(username, passwordEncoder.encode(passwordPlano), rol);
        return usuarioRepository.save(u);
    }

    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    @Override
    public void run(String... args) {
        validarCredencialesProduccion();

        if (usuarioRepository.count() == 0) {
            crearUsuario(adminUsername, adminPassword, "ADMIN");
            System.out.println("Usuario admin inicial creado: " + adminUsername);
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
