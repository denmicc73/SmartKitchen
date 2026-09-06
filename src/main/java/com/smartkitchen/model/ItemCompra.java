package com.smartkitchen.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

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

    @Column(nullable = false)
    private boolean comprado = false;

    // Si el item se generó automáticamente porque un alimento se agotó/caducó
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

    public boolean isComprado() { return comprado; }
    public void setComprado(boolean comprado) { this.comprado = comprado; }

    public boolean isGeneradoAutomaticamente() { return generadoAutomaticamente; }
    public void setGeneradoAutomaticamente(boolean generadoAutomaticamente) { this.generadoAutomaticamente = generadoAutomaticamente; }
}
