package com.smartkitchen.controller;

import com.smartkitchen.model.Alimento;
import com.smartkitchen.model.Zona;
import com.smartkitchen.service.AlimentoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/inventario")
public class InventarioController {

    private final AlimentoService alimentoService;

    public InventarioController(AlimentoService alimentoService) {
        this.alimentoService = alimentoService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) Zona zona, Model model) {
        model.addAttribute("alimentos", zona != null
                ? alimentoService.listarPorZona(zona)
                : alimentoService.listarTodos());
        model.addAttribute("zonas", Zona.values());
        model.addAttribute("zonaSeleccionada", zona);
        model.addAttribute("nuevoAlimento", new Alimento());
        model.addAttribute("avisos", alimentoService.avisosPendientes(3));
        return "inventario";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("nuevoAlimento") Alimento alimento) {
        alimentoService.guardar(alimento);
        return "redirect:/inventario";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id) {
        alimentoService.eliminar(id);
        return "redirect:/inventario";
    }

    // Endpoint pensado para ser llamado por un lector NFC (p.ej. una app puente
    // en el móvil o una automatización) al escanear un tarro/recipiente.
    @GetMapping("/nfc/{codigo}")
    @ResponseBody
    public Alimento porCodigoNfc(@PathVariable String codigo) {
        return alimentoService.buscarPorCodigoNfc(codigo);
    }
}