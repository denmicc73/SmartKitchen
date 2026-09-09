package com.smartkitchen.service;

import com.smartkitchen.model.CategoriaReceta;
import com.smartkitchen.model.Dificultad;
import com.smartkitchen.model.Etiqueta;
import com.smartkitchen.model.IngredienteReceta;
import com.smartkitchen.model.Receta;
import com.smartkitchen.repository.RecetaRepository;
import com.smartkitchen.web.RecetaForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

/** CRUD y consultas del recetario de casa. */
@Service
public class RecetaService {

    private final RecetaRepository recetaRepository;

    public RecetaService(RecetaRepository recetaRepository) {
        this.recetaRepository = recetaRepository;
    }

    public List<Receta> listarTodas() {
        return recetaRepository.findAllByOrderByNombreAsc();
    }

    public Receta buscarPorId(Long id) {
        return recetaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Receta no encontrada: " + id));
    }

    public long total() {
        return recetaRepository.count();
    }

    public List<Receta> favoritas() {
        return recetaRepository.findByFavoritaTrueOrderByNombreAsc();
    }

    public List<Receta> mejorValoradas(int limite) {
        return listarTodas().stream()
                .filter(r -> r.getValoracion() != null)
                .sorted(Comparator.comparing(Receta::getValoracion).reversed())
                .limit(limite)
                .toList();
    }

    /** Filtro combinable para la pantalla de recetas. */
    public List<Receta> filtrar(String texto, CategoriaReceta categoria, Dificultad dificultad,
                                Integer tiempoMax, Etiqueta etiqueta, boolean soloFavoritas) {
        String q = texto == null ? "" : texto.trim().toLowerCase();
        return listarTodas().stream()
                .filter(r -> q.isEmpty()
                        || r.getNombre().toLowerCase().contains(q)
                        || (r.getDescripcion() != null && r.getDescripcion().toLowerCase().contains(q))
                        || r.getIngredientes().stream().anyMatch(i -> i.getNombre().toLowerCase().contains(q)))
                .filter(r -> categoria == null || r.getCategoria() == categoria)
                .filter(r -> dificultad == null || r.getDificultad() == dificultad)
                .filter(r -> tiempoMax == null || r.getTiempoMinutos() <= tiempoMax)
                .filter(r -> etiqueta == null || r.getEtiquetas().contains(etiqueta))
                .filter(r -> !soloFavoritas || r.isFavorita())
                .toList();
    }

    /**
     * Sugerencia del día: determinista por fecha para que no cambie en cada
     * recarga. Prefiere favoritas; si no hay, cualquier receta.
     */
    public Receta sugerenciaDelDia() {
        List<Receta> candidatas = favoritas();
        if (candidatas.isEmpty()) candidatas = listarTodas();
        if (candidatas.isEmpty()) return null;
        int indice = Math.floorMod((int) LocalDate.now().toEpochDay(), candidatas.size());
        return candidatas.get(indice);
    }

    @Transactional
    public Receta guardarDesdeForm(RecetaForm form) {
        Receta r = (form.getId() != null) ? buscarPorId(form.getId()) : new Receta();
        r.setNombre(form.getNombre());
        r.setDescripcion(vacioANull(form.getDescripcion()));
        r.setImagenUrl(vacioANull(form.getImagenUrl()));
        r.setPersonas(Math.max(1, form.getPersonas()));
        r.setTiempoMinutos(Math.max(1, form.getTiempoMinutos()));
        r.setDificultad(form.getDificultad());
        r.setCategoria(form.getCategoria());
        r.setFavorita(form.isFavorita());
        r.setValoracion(normalizarValoracion(form.getValoracion()));

        r.getPasos().clear();
        r.getPasos().addAll(form.pasosComoLista());

        r.setEtiquetas(new LinkedHashSet<>(form.getEtiquetas()));

        r.limpiarIngredientes();
        for (RecetaForm.Fila fila : form.ingredientesValidos()) {
            String unidad = (fila.getUnidad() == null || fila.getUnidad().isBlank()) ? "unidad" : fila.getUnidad().trim();
            r.addIngrediente(new IngredienteReceta(fila.getNombre().trim(), fila.getCantidad(), unidad));
        }
        return recetaRepository.save(r);
    }

    @Transactional
    public boolean alternarFavorita(Long id) {
        Receta r = buscarPorId(id);
        r.setFavorita(!r.isFavorita());
        recetaRepository.save(r);
        return r.isFavorita();
    }

    @Transactional
    public void valorar(Long id, Integer estrellas) {
        Receta r = buscarPorId(id);
        r.setValoracion(normalizarValoracion(estrellas));
        recetaRepository.save(r);
    }

    @Transactional
    public void marcarCocinadaHoy(Long id) {
        Receta r = buscarPorId(id);
        r.setUltimaVez(LocalDate.now());
        recetaRepository.save(r);
    }

    public void eliminar(Long id) {
        recetaRepository.deleteById(id);
    }

    private static String vacioANull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static Integer normalizarValoracion(Integer v) {
        if (v == null || v < 1 || v > 5) return null;
        return v;
    }
}
