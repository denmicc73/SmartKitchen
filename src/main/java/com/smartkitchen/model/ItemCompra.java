package com.smartkitchen.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

@Entity
@Table(name = "items_compra")
public class ItemCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private double cantidad = 1;

    @Column(nullable = false)
    private String unidad = "unidad";

    @Enumerated(EnumType.STRING)
    private Zona zona = Zona.DESPENSA;

    // Sección del supermercado, para agrupar la lista automáticamente.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaCompra categoria = CategoriaCompra.OTROS;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Prioridad prioridad = Prioridad.MEDIA;

    @Column(nullable = false)
    private boolean comprado = false;

    // Fecha en la que se marcó como comprado (para el historial).
    private LocalDate fechaComprado;

    // Si el item se generó automáticamente porque un alimento se agotó/caducó
    // o al generar la compra a partir del menú semanal.
    private boolean generadoAutomaticamente = false;

    public ItemCompra() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public double getCantidad() { return cantidad; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }

    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }

    public Zona getZona() { return zona; }
    public void setZona(Zona zona) { this.zona = zona; }

    public CategoriaCompra getCategoria() { return categoria; }
    public void setCategoria(CategoriaCompra categoria) { this.categoria = categoria; }

    public Prioridad getPrioridad() { return prioridad; }
    public void setPrioridad(Prioridad prioridad) { this.prioridad = prioridad; }

    public boolean isComprado() { return comprado; }
    public void setComprado(boolean comprado) { this.comprado = comprado; }

    public LocalDate getFechaComprado() { return fechaComprado; }
    public void setFechaComprado(LocalDate fechaComprado) { this.fechaComprado = fechaComprado; }

    public boolean isGeneradoAutomaticamente() { return generadoAutomaticamente; }
    public void setGeneradoAutomaticamente(boolean generadoAutomaticamente) { this.generadoAutomaticamente = generadoAutomaticamente; }
}
