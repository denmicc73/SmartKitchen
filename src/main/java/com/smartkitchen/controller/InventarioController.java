package com.smartkitchen.controller;

import com.smartkitchen.model.Alimento;
import com.smartkitchen.model.CategoriaCompra;
import com.smartkitchen.model.EstadoAlimento;
import com.smartkitchen.model.Zona;
import com.smartkitchen.service.AlimentoService;
import com.smartkitchen.service.EscanerProductoService;
import com.smartkitchen.service.EscanerProductoService.ProductoDetectado;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Map;

@Controller
@RequestMapping("/inventario")
public class InventarioController {

    private final AlimentoService alimentoService;
    private final EscanerProductoService escanerService;

    public InventarioController(AlimentoService alimentoService,
                               EscanerProductoService escanerService) {
        this.alimentoService = alimentoService;
        this.escanerService = escanerService;
    }

    @ModelAttribute("zonas")
    public Zona[] zonas() { return Zona.values(); }

    @ModelAttribute("categorias")
    public CategoriaCompra[] categorias() { return CategoriaCompra.values(); }

    @GetMapping
    public String listar(@RequestParam(required = false) Zona zona, Model model) {
        model.addAttribute("alimentos", zona != null
                ? alimentoService.listarPorZona(zona)
                : alimentoService.listarTodos());
        model.addAttribute("zonaSeleccionada", zona);
        model.addAttribute("nuevoAlimento", new Alimento());
        poblarResumen(model);
        return "inventario";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("nuevoAlimento") Alimento alimento,
                        BindingResult binding, Model model, RedirectAttributes ra) {
        if (binding.hasErrors()) {
            model.addAttribute("alimentos", alimentoService.listarTodos());
            model.addAttribute("abrirFormulario", true);
            poblarResumen(model);
            return "inventario";
        }
        alimentoService.guardar(alimento);
        ra.addFlashAttribute("ok", "Producto añadido al inventario.");
        return "redirect:/inventario";
    }

    // ===== Escaneo de producto con la cámara + IA ==========================

    @GetMapping("/escanear")
    public String escanear(Model model) {
        model.addAttribute("iaDisponible", escanerService.iaDisponible());
        return "escaner";
    }

    @PostMapping("/escanear")
    public String reconocer(@RequestParam("foto") MultipartFile foto,
                            Model model, RedirectAttributes ra) {
        ProductoDetectado d;
        try {
            d = escanerService.detectar(foto.getBytes(), foto.getContentType());
        } catch (IOException e) {
            ra.addFlashAttribute("error", "No se ha podido leer la foto.");
            return "redirect:/inventario/escanear";
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/inventario/escanear";
        }

        Alimento prellenado = new Alimento();
        prellenado.setNombre(d.nombre());
        prellenado.setCategoria(d.categoria());
        prellenado.setZona(d.zonaSugerida());
        prellenado.setUnidad(d.unidad());
        prellenado.setCantidad(d.cantidad() != null && d.cantidad() > 0 ? d.cantidad() : 1);
        prellenado.setFechaCaducidad(d.fechaCaducidad());
        if (d.marca() != null) {
            prellenado.setNotas(d.marca());
        }

        model.addAttribute("nuevoAlimento", prellenado);
        model.addAttribute("deteccion", d);
        model.addAttribute("abrirFormulario", true);
        model.addAttribute("alimentos", alimentoService.listarTodos());
        model.addAttribute("zonaSeleccionada", null);
        poblarResumen(model);
        return "inventario";
    }

    /** Datos comunes de la pantalla: agrupación por zona, avisos y contadores. */
    private void poblarResumen(Model model) {
        model.addAttribute("porZona", alimentoService.agrupadosPorZona());
        model.addAttribute("avisos", alimentoService.avisosPendientes(3));
        model.addAttribute("bajoStock", alimentoService.bajoStock());
        model.addAttribute("total", alimentoService.total());

        Map<EstadoAlimento, Long> resumen = alimentoService.resumenPorEstado();
        model.addAttribute("resumen", resumen);
        model.addAttribute("nDisponibles", resumen.getOrDefault(EstadoAlimento.DISPONIBLE, 0L));
        model.addAttribute("nPocoStock", resumen.getOrDefault(EstadoAlimento.POCO_STOCK, 0L));
        model.addAttribute("nCaducaPronto", resumen.getOrDefault(EstadoAlimento.CADUCA_PRONTO, 0L));
        model.addAttribute("nCaducado",
                resumen.getOrDefault(EstadoAlimento.CADUCADO, 0L)
                        + resumen.getOrDefault(EstadoAlimento.AGOTADO, 0L));
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("alimento", alimentoService.buscarPorId(id));
        return "inventario-editar";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("alimento") Alimento datos,
                             BindingResult binding,
                             RedirectAttributes ra) {
        if (binding.hasErrors()) {
            return "inventario-editar";
        }
        Alimento actual = alimentoService.buscarPorId(id);
        actual.setNombre(datos.getNombre());
        actual.setZona(datos.getZona());
        actual.setCategoria(datos.getCategoria());
        actual.setCantidad(datos.getCantidad());
        actual.setUnidad(datos.getUnidad());
        actual.setStockMinimo(datos.getStockMinimo());
        actual.setFechaCaducidad(datos.getFechaCaducidad());
        actual.setCodigoNfc(datos.getCodigoNfc());
        actual.setNotas(datos.getNotas());
        alimentoService.guardar(actual);
        ra.addFlashAttribute("ok", "Producto actualizado.");
        return "redirect:/inventario";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        alimentoService.eliminar(id);
        ra.addFlashAttribute("ok", "Producto eliminado.");
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
