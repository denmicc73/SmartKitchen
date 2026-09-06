package com.smartkitchen.controller;

import com.smartkitchen.service.AlimentoService;
import com.smartkitchen.service.ItemCompraService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final AlimentoService alimentoService;
    private final ItemCompraService itemCompraService;

    public HomeController(AlimentoService alimentoService, ItemCompraService itemCompraService) {
        this.alimentoService = alimentoService;
        this.itemCompraService = itemCompraService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("proximosACaducar", alimentoService.proximosACaducar(3));
        model.addAttribute("avisos", alimentoService.avisosPendientes(3));
        model.addAttribute("totalAlimentos", alimentoService.listarTodos().size());
        model.addAttribute("pendientesCompra", itemCompraService.pendientes().size());
        return "dashboard";
    }
}