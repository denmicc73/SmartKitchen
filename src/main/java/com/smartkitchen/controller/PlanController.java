package com.smartkitchen.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Planificador semanal. De momento es solo un marcador de posicion para que la
 * navegacion (pestana "Plan") sea coherente; la vista de calendario se
 * implementara en la fase de planificacion.
 */
@Controller
public class PlanController {

    @GetMapping("/plan")
    public String plan() {
        return "plan";
    }
}
