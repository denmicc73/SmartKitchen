package com.smartkitchen.controller;

import com.smartkitchen.model.Alimento;
import com.smartkitchen.model.ItemCompra;
import com.smartkitchen.model.Zona;
import com.smartkitchen.service.AlimentoService;
import com.smartkitchen.service.ItemCompraService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequestMapping("/lista-compra")
public class ListaCompraController {

    private final ItemCompraService itemCompraService;
    private final AlimentoService alimentoService;

    public ListaCompraController(ItemCompraService itemCompraService, AlimentoService alimentoService) {
        this.itemCompraService = itemCompraService;
        this.alimentoService = alimentoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("pendientes", itemCompraService.pendientes());
        model.addAttribute("comprados", itemCompraService.comprados());
        model.addAttribute("nuevoItem", new ItemCompra());
        return "lista-compra";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("nuevoItem") ItemCompra item) {
        itemCompraService.guardar(item);
        return "redirect:/lista-compra";
    }

    @PostMapping("/{id}/marcar")
    public String marcar(@PathVariable Long id, @RequestParam boolean comprado) {
        if (comprado) {
            return "redirect:/lista-compra/" + id + "/caducidad";
        }

        itemCompraService.marcarComprado(id, comprado);
        return "redirect:/lista-compra";
    }

    @GetMapping("/{id}/caducidad")
    public String pedirCaducidad(@PathVariable Long id, Model model) {
        model.addAttribute("item", itemCompraService.buscarPorId(id));
        return "confirmar-compra";
    }

    @PostMapping("/{id}/confirmar-compra")
    public String confirmarCompra(@PathVariable Long id,
                                  @RequestParam(required = false)
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                  LocalDate fechaCaducidad) {
        ItemCompra item = itemCompraService.buscarPorId(id);

        Alimento alimento = new Alimento();
        alimento.setNombre(item.getNombre());
        alimento.setCantidad(item.getCantidad());
        alimento.setUnidad(item.getUnidad());
        alimento.setZona(item.getZona() != null ? item.getZona() : Zona.DESPENSA);
        alimento.setFechaCaducidad(fechaCaducidad);
        alimentoService.guardar(alimento);

        itemCompraService.marcarComprado(id, true);
        return "redirect:/lista-compra";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id) {
        itemCompraService.eliminar(id);
        return "redirect:/lista-compra";
    }

    // Se llama desde el aviso "¿Añadir a la lista de la compra?" que aparece
    // junto a los alimentos próximos a caducar (dashboard e inventario).
    @PostMapping("/desde-alimento/{alimentoId}")
    public String anadirDesdeAlimento(@PathVariable Long alimentoId,
                                      @RequestParam(defaultValue = "/") String volver) {
        Alimento alimento = alimentoService.buscarPorId(alimentoId);

        ItemCompra item = new ItemCompra();
        item.setNombre(alimento.getNombre());
        item.setCantidad(alimento.getCantidad());
        item.setUnidad(alimento.getUnidad());
        item.setZona(alimento.getZona());
        item.setGeneradoAutomaticamente(true);
        itemCompraService.guardar(item);

        alimentoService.eliminar(alimentoId);

        return "redirect:" + volver;
    }
}
