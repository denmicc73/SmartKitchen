package com.smartkitchen.controller;

import com.smartkitchen.service.RecetaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/recetas")
public class RecetaController {

    private final RecetaService recetaService;

    public RecetaController(RecetaService recetaService) {
        this.recetaService = recetaService;
    }

    @GetMapping
    public String recetas(@RequestParam(defaultValue = "3") int dias, Model model) {
        model.addAttribute("sugerencia", recetaService.sugerirRecetas(dias));
        model.addAttribute("iaDisponible", recetaService.iaDisponible());
        model.addAttribute("dias", dias);
        return "recetas";
    }
}
