package com.smartkitchen.web;

import com.smartkitchen.model.CategoriaReceta;
import com.smartkitchen.model.Dificultad;
import com.smartkitchen.model.Etiqueta;
import com.smartkitchen.model.IngredienteReceta;
import com.smartkitchen.model.Receta;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

/**
 * Objeto de formulario para crear/editar recetas. Separa la UI de la entidad:
 * los pasos se editan como texto (una línea por paso) y los ingredientes como
 * filas dinámicas.
 */
public class RecetaForm {

    private Long id;

    @NotBlank
    private String nombre;

    private String descripcion;
    private String imagenUrl;
    private int personas = 2;
    private int tiempoMinutos = 30;
    private Dificultad dificultad = Dificultad.FACIL;
    private CategoriaReceta categoria = CategoriaReceta.COMIDA;
    private boolean favorita;
    private Integer valoracion;
    private String pasosTexto = "";
    private List<Etiqueta> etiquetas = new ArrayList<>();
    private List<Fila> ingredientes = new ArrayList<>();

    public RecetaForm() {
        // Filas vacías iniciales para que el formulario de alta no salga vacío.
        for (int i = 0; i < 4; i++) {
            ingredientes.add(new Fila());
        }
    }

    /** Construye el formulario a partir de una receta existente (edición). */
    public static RecetaForm desde(Receta r) {
        RecetaForm f = new RecetaForm();
        f.id = r.getId();
        f.nombre = r.getNombre();
        f.descripcion = r.getDescripcion();
        f.imagenUrl = r.getImagenUrl();
        f.personas = r.getPersonas();
        f.tiempoMinutos = r.getTiempoMinutos();
        f.dificultad = r.getDificultad();
        f.categoria = r.getCategoria();
        f.favorita = r.isFavorita();
        f.valoracion = r.getValoracion();
        f.pasosTexto = String.join("\n", r.getPasos());
        f.etiquetas = new ArrayList<>(r.getEtiquetas());
        f.ingredientes = new ArrayList<>();
        for (IngredienteReceta ing : r.getIngredientes()) {
            Fila fila = new Fila();
            fila.nombre = ing.getNombre();
            fila.cantidad = ing.getCantidad();
            fila.unidad = ing.getUnidad();
            f.ingredientes.add(fila);
        }
        // Un par de filas vacías extra para poder añadir más.
        f.ingredientes.add(new Fila());
        f.ingredientes.add(new Fila());
        return f;
    }

    public List<String> pasosComoLista() {
        List<String> out = new ArrayList<>();
        if (pasosTexto == null) return out;
        for (String linea : pasosTexto.split("\\r?\\n")) {
            String limpio = linea.trim();
            if (!limpio.isEmpty()) out.add(limpio);
        }
        return out;
    }

    public List<Fila> ingredientesValidos() {
        List<Fila> out = new ArrayList<>();
        for (Fila fila : ingredientes) {
            if (fila.nombre != null && !fila.nombre.isBlank()) {
                out.add(fila);
            }
        }
        return out;
    }

    public static class Fila {
        private String nombre;
        private Double cantidad;
        private String unidad = "unidad";

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public Double getCantidad() { return cantidad; }
        public void setCantidad(Double cantidad) { this.cantidad = cantidad; }
        public String getUnidad() { return unidad; }
        public void setUnidad(String unidad) { this.unidad = unidad; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
    public int getPersonas() { return personas; }
    public void setPersonas(int personas) { this.personas = personas; }
    public int getTiempoMinutos() { return tiempoMinutos; }
    public void setTiempoMinutos(int tiempoMinutos) { this.tiempoMinutos = tiempoMinutos; }
    public Dificultad getDificultad() { return dificultad; }
    public void setDificultad(Dificultad dificultad) { this.dificultad = dificultad; }
    public CategoriaReceta getCategoria() { return categoria; }
    public void setCategoria(CategoriaReceta categoria) { this.categoria = categoria; }
    public boolean isFavorita() { return favorita; }
    public void setFavorita(boolean favorita) { this.favorita = favorita; }
    public Integer getValoracion() { return valoracion; }
    public void setValoracion(Integer valoracion) { this.valoracion = valoracion; }
    public String getPasosTexto() { return pasosTexto; }
    public void setPasosTexto(String pasosTexto) { this.pasosTexto = pasosTexto; }
    public List<Etiqueta> getEtiquetas() { return etiquetas; }
    public void setEtiquetas(List<Etiqueta> etiquetas) { this.etiquetas = etiquetas; }
    public List<Fila> getIngredientes() { return ingredientes; }
    public void setIngredientes(List<Fila> ingredientes) { this.ingredientes = ingredientes; }
}
