package com.smartkitchen.web;

import com.smartkitchen.model.Usuario;
import com.smartkitchen.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Expone el usuario autenticado a todas las plantillas (para el avatar y el
 * nombre de la barra superior) sin tener que repetirlo en cada controlador.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final UsuarioService usuarioService;

    public GlobalModelAdvice(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @ModelAttribute("usuarioActual")
    public Usuario usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return usuarioService.buscarPorUsername(auth.getName()).orElse(null);
    }
}
