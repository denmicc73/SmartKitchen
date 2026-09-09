package com.smartkitchen.controller;

import com.smartkitchen.model.CategoriaReceta;
import com.smartkitchen.model.Dificultad;
import com.smartkitchen.model.Etiqueta;
import com.smartkitchen.model.Receta;
import com.smartkitchen.service.AlmacenImagenesService;
import com.smartkitchen.service.AsistenteIaService;
import com.smartkitchen.service.RecetaService;
import com.smartkitchen.web.RecetaForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/recetas")
public class RecetaController {

    private final RecetaService recetaService;
    private final AsistenteIaService asistenteIaService;
    private final AlmacenImagenesService almacenImagenes;

    public RecetaController(RecetaService recetaService,
                            AsistenteIaService asistenteIaService,
                            AlmacenImagenesService almacenImagenes) {
        this.recetaService = recetaService;
        this.asistenteIaService = asistenteIaService;
        this.almacenImagenes = almacenImagenes;
    }

    @ModelAttribute("categorias")
    public CategoriaReceta[] categorias() { return CategoriaReceta.values(); }

    @ModelAttribute("dificultades")
    public Dificultad[] dificultades() { return Dificultad.values(); }

    @ModelAttribute("todasEtiquetas")
    public Etiqueta[] etiquetas() { return Etiqueta.values(); }

    @GetMapping
    public String listar(@RequestParam(required = false) String q,
                         @RequestParam(required = false) CategoriaReceta categoria,
                         @RequestParam(required = false) Dificultad dificultad,
                         @RequestParam(required = false) Integer tiempoMax,
                         @RequestParam(required = false) Etiqueta etiqueta,
                         @RequestParam(defaultValue = "false") boolean favoritas,
                         Model model) {
        model.addAttribute("recetas",
                recetaService.filtrar(q, categoria, dificultad, tiempoMax, etiqueta, favoritas));
        model.addAttribute("q", q);
        model.addAttribute("categoriaSel", categoria);
        model.addAttribute("dificultadSel", dificultad);
        model.addAttribute("tiempoMax", tiempoMax);
        model.addAttribute("etiquetaSel", etiqueta);
        model.addAttribute("soloFavoritas", favoritas);
        model.addAttribute("total", recetaService.total());
        return "recetas";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        model.addAttribute("receta", recetaService.buscarPorId(id));
        return "receta-detalle";
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("form", new RecetaForm());
        model.addAttribute("editar", false);
        return "receta-form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("form", RecetaForm.desde(recetaService.buscarPorId(id)));
        model.addAttribute("editar", true);
        return "receta-form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("form") RecetaForm form,
                          BindingResult binding,
                          @RequestParam(required = false) MultipartFile imagen,
                          Model model,
                          RedirectAttributes ra) {
        if (binding.hasErrors()) {
            model.addAttribute("editar", form.getId() != null);
            return "receta-form";
        }
        try {
            String subida = almacenImagenes.guardar(imagen);
            if (subida != null) {
                form.setImagenUrl(subida);
            }
        } catch (RuntimeException e) {
            binding.rejectValue("imagenUrl", "imagen.invalida", e.getMessage());
            model.addAttribute("editar", form.getId() != null);
            return "receta-form";
        }
        Receta guardada = recetaService.guardarDesdeForm(form);
        ra.addFlashAttribute("ok", "Receta guardada.");
        return "redirect:/recetas/" + guardada.getId();
    }

    @PostMapping("/{id}/favorita")
    public String favorita(@PathVariable Long id,
                           @RequestParam(defaultValue = "/recetas") String volver) {
        recetaService.alternarFavorita(id);
        return "redirect:" + volver;
    }

    @PostMapping("/{id}/valorar")
    public String valorar(@PathVariable Long id, @RequestParam Integer estrellas) {
        recetaService.valorar(id, estrellas);
        return "redirect:/recetas/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        recetaService.eliminar(id);
        ra.addFlashAttribute("ok", "Receta eliminada.");
        return "redirect:/recetas";
    }

    // Ayudante IA (antigua pantalla de sugerencias). Opcional.
    @GetMapping("/asistente")
    public String asistente(@RequestParam(defaultValue = "3") int dias, Model model) {
        model.addAttribute("sugerencia", asistenteIaService.sugerirRecetas(dias));
        model.addAttribute("iaDisponible", asistenteIaService.iaDisponible());
        model.addAttribute("dias", dias);
        return "recetas-asistente";
    }
}
