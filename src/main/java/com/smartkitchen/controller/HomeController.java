package com.smartkitchen.controller;

import com.smartkitchen.model.EstadoAlimento;
import com.smartkitchen.service.AlimentoService;
import com.smartkitchen.service.ItemCompraService;
import com.smartkitchen.service.RecetaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
public class HomeController {

    private static final int DIAS_AVISO = 3;

    private final AlimentoService alimentoService;
    private final ItemCompraService itemCompraService;
    private final RecetaService recetaService;

    public HomeController(AlimentoService alimentoService,
                          ItemCompraService itemCompraService,
                          RecetaService recetaService) {
        this.alimentoService = alimentoService;
        this.itemCompraService = itemCompraService;
        this.recetaService = recetaService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        // 🍽️ Hoy
        model.addAttribute("recetaHoy", recetaService.sugerenciaDelDia());

        // 🛒 Compra
        model.addAttribute("pendientesCompra", itemCompraService.numPendientes());
        model.addAttribute("prioritariosCompra", itemCompraService.prioritarios());

        // 🧊 Inventario
        Map<EstadoAlimento, Long> resumen = alimentoService.resumenPorEstado();
        model.addAttribute("totalAlimentos", alimentoService.total());
        model.addAttribute("bajoStock", alimentoService.bajoStock());
        model.addAttribute("proximosACaducar", alimentoService.proximosACaducar(DIAS_AVISO));
        model.addAttribute("caducados", alimentoService.caducados());
        model.addAttribute("numDisponibles",
                resumen.getOrDefault(EstadoAlimento.DISPONIBLE, 0L));

        // ⚠️ Alertas (avisos que aún no se han resuelto)
        model.addAttribute("avisos", alimentoService.avisosPendientes(DIAS_AVISO));

        // 📊 Recetario
        model.addAttribute("totalRecetas", recetaService.total());

        return "dashboard";
    }
}
