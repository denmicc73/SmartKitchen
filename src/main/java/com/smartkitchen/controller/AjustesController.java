package com.smartkitchen.controller;

import com.smartkitchen.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/ajustes")
public class AjustesController {

    private final UsuarioService usuarioService;

    public AjustesController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String ajustes(Principal principal, Model model) {
        usuarioService.buscarPorUsername(principal.getName())
                .ifPresent(u -> model.addAttribute("usuario", u));
        model.addAttribute("usuarios", usuarioService.listarUsuarios());
        return "ajustes";
    }

    @PostMapping("/perfil")
    public String actualizarPerfil(Principal principal,
                                   @RequestParam(required = false) String nombreVisible,
                                   @RequestParam(required = false) String avatarEmoji,
                                   RedirectAttributes ra) {
        usuarioService.actualizarPerfil(principal.getName(), nombreVisible, avatarEmoji);
        ra.addFlashAttribute("ok", "Perfil actualizado.");
        return "redirect:/ajustes";
    }

    @PostMapping("/password")
    public String cambiarPassword(Principal principal,
                                  @RequestParam String actual,
                                  @RequestParam String nueva,
                                  RedirectAttributes ra) {
        try {
            usuarioService.cambiarPassword(principal.getName(), actual, nueva);
            ra.addFlashAttribute("ok", "Contraseña cambiada.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ajustes";
    }

    @PostMapping("/usuarios")
    public String crearUsuario(@RequestParam String username,
                               @RequestParam String password,
                               @RequestParam(defaultValue = "MIEMBRO") String rol,
                               RedirectAttributes ra) {
        try {
            usuarioService.crearUsuario(username, password, rol);
            ra.addFlashAttribute("ok", "Usuario creado.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ajustes";
    }

    @PostMapping("/usuarios/{id}/activo")
    public String alternarActivo(@PathVariable Long id, RedirectAttributes ra) {
        try {
            usuarioService.alternarActivo(id);
            ra.addFlashAttribute("ok", "Usuario actualizado.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ajustes";
    }
}
