package com.smartkitchen.controller;

import com.smartkitchen.service.CuentaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Alta de cuenta ({@code /registro}), confirmacion de correo ({@code /confirmar})
 * y recuperacion de contrasena ({@code /recuperar}, {@code /reset}).
 * Todas las rutas son publicas (ver {@code SecurityConfig}).
 */
@Controller
public class CuentaController {

    private final CuentaService cuenta;

    public CuentaController(CuentaService cuenta) {
        this.cuenta = cuenta;
    }

    // ----------------------------------------------------------------- registro

    @GetMapping("/registro")
    public String formRegistro() {
        return "cuenta/registro";
    }

    @PostMapping("/registro")
    public String registrar(@RequestParam String username,
                            @RequestParam String email,
                            @RequestParam String password,
                            Model model,
                            RedirectAttributes ra) {
        try {
            cuenta.registrar(username, email, password);
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            return "cuenta/registro";
        }
        ra.addFlashAttribute("email", email.trim());
        return "redirect:/registro/hecho";
    }

    @GetMapping("/registro/hecho")
    public String registroHecho(Model model) {
        if (!model.containsAttribute("email")) {
            return "redirect:/registro";
        }
        return "cuenta/registro-hecho";
    }

    @PostMapping("/registro/reenviar")
    public String reenviar(@RequestParam String email, RedirectAttributes ra) {
        cuenta.reenviarConfirmacion(email);
        ra.addFlashAttribute("info",
                "Si la cuenta existe y aun no esta confirmada, te hemos enviado un nuevo enlace.");
        return "redirect:/login";
    }

    @GetMapping("/confirmar")
    public String confirmar(@RequestParam String token, RedirectAttributes ra) {
        try {
            cuenta.confirmar(token);
            ra.addFlashAttribute("info", "Correo confirmado. Ya puedes iniciar sesion.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/login";
    }

    // -------------------------------------------------------- olvide mi clave

    @GetMapping("/recuperar")
    public String formRecuperar() {
        return "cuenta/recuperar";
    }

    @PostMapping("/recuperar")
    public String solicitarReset(@RequestParam String email, RedirectAttributes ra) {
        cuenta.solicitarReset(email);
        ra.addFlashAttribute("info",
                "Si hay una cuenta con ese correo, recibiras un enlace para restablecer la contrasena.");
        return "redirect:/login";
    }

    @GetMapping("/reset")
    public String formReset(@RequestParam String token, Model model, RedirectAttributes ra) {
        try {
            cuenta.validarTokenReset(token);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/recuperar";
        }
        model.addAttribute("token", token);
        return "cuenta/reset";
    }

    @PostMapping("/reset")
    public String reset(@RequestParam String token,
                        @RequestParam String password,
                        @RequestParam String password2,
                        Model model,
                        RedirectAttributes ra) {
        if (!password.equals(password2)) {
            model.addAttribute("error", "Las contrasenas no coinciden.");
            model.addAttribute("token", token);
            return "cuenta/reset";
        }
        try {
            cuenta.resetear(token, password);
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("token", token);
            return "cuenta/reset";
        }
        ra.addFlashAttribute("info", "Contrasena actualizada. Inicia sesion con la nueva.");
        return "redirect:/login";
    }
}
