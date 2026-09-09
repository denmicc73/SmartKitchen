package com.smartkitchen.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "recetas")
public class Receta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String nombre;

    @Column(length = 2000)
    private String descripcion;

    // URL externa o ruta local servida en /uploads/**
    private String imagenUrl;

    @Positive
    @Column(nullable = false)
    private int personas = 2;

    @Positive
    @Column(nullable = false)
    private int tiempoMinutos = 30;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Dificultad dificultad = Dificultad.FACIL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaReceta categoria = CategoriaReceta.COMIDA;

    @Column(nullable = false)
    private boolean favorita = false;

    // 1..5, null si aún no se ha valorado.
    private Integer valoracion;

    // Última vez que se cocinó (para no repetir siempre los mismos platos).
    private LocalDate ultimaVez;

    @Column(nullable = false)
    private LocalDate fechaCreacion = LocalDate.now();

    @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderColumn(name = "orden")
    private List<IngredienteReceta> ingredientes = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "receta_pasos", joinColumns = @JoinColumn(name = "receta_id"))
    @OrderColumn(name = "orden")
    @Column(name = "paso", length = 1000)
    private List<String> pasos = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "receta_etiquetas", joinColumns = @JoinColumn(name = "receta_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "etiqueta")
    private Set<Etiqueta> etiquetas = new LinkedHashSet<>();

    public Receta() {}

    public void addIngrediente(IngredienteReceta ing) {
        ing.setReceta(this);
        ingredientes.add(ing);
    }

    public void limpiarIngredientes() {
        ingredientes.clear();
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

    public LocalDate getUltimaVez() { return ultimaVez; }
    public void setUltimaVez(LocalDate ultimaVez) { this.ultimaVez = ultimaVez; }

    public LocalDate getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDate fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public List<IngredienteReceta> getIngredientes() { return ingredientes; }
    public void setIngredientes(List<IngredienteReceta> ingredientes) { this.ingredientes = ingredientes; }

    public List<String> getPasos() { return pasos; }
    public void setPasos(List<String> pasos) { this.pasos = pasos; }

    public Set<Etiqueta> getEtiquetas() { return etiquetas; }
    public void setEtiquetas(Set<Etiqueta> etiquetas) { this.etiquetas = etiquetas; }

    @Transient
    public boolean isRapida() {
        return tiempoMinutos <= 20;
    }

    @Transient
    public String getTiempoTexto() {
        if (tiempoMinutos < 60) return tiempoMinutos + " min";
        int h = tiempoMinutos / 60;
        int m = tiempoMinutos % 60;
        return m == 0 ? h + " h" : h + " h " + m + " min";
    }
}
