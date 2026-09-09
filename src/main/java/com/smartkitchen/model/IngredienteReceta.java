package com.smartkitchen.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "ingredientes_receta")
public class IngredienteReceta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "receta_id", nullable = false)
    private Receta receta;

    @NotBlank
    @Column(nullable = false)
    private String nombre;

    // Puede ser null cuando la cantidad es "al gusto".
    private Double cantidad;

    @Column(nullable = false)
    private String unidad = "unidad";

    public IngredienteReceta() {}

    public IngredienteReceta(String nombre, Double cantidad, String unidad) {
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.unidad = unidad;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Receta getReceta() { return receta; }
    public void setReceta(Receta receta) { this.receta = receta; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Double getCantidad() { return cantidad; }
    public void setCantidad(Double cantidad) { this.cantidad = cantidad; }

    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }

    @Transient
    public String getCantidadTexto() {
        if (cantidad == null) return "al gusto";
        String num = (cantidad == Math.floor(cantidad))
                ? String.valueOf(cantidad.longValue())
                : String.valueOf(cantidad);
        return num + " " + unidad;
    }
}
