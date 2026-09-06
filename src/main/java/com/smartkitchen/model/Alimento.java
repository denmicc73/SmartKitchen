package com.smartkitchen.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

@Entity
@Table(name = "alimentos")
public class Alimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String nombre;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Zona zona;

    @Positive
    @Column(nullable = false)
    private double cantidad = 1;

    @Column(nullable = false)
    private String unidad = "unidad"; // unidad, g, kg, ml, l...

    private LocalDate fechaCaducidad;

    private LocalDate fechaEntrada = LocalDate.now();

    // Código NFC/QR asociado al recipiente, si lo tiene
    private String codigoNfc;

    private String notas;

    // Se marca en true cuando el usuario responde "Sí" al aviso de
    // "¿Añadir a la lista de la compra?", para no volver a preguntar por
    // el mismo alimento en cada recarga de página.
    @Column(nullable = false)
    private boolean enListaCompra = false;

    public Alimento() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Zona getZona() { return zona; }
    public void setZona(Zona zona) { this.zona = zona; }

    public double getCantidad() { return cantidad; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }

    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }

    public LocalDate getFechaCaducidad() { return fechaCaducidad; }
    public void setFechaCaducidad(LocalDate fechaCaducidad) { this.fechaCaducidad = fechaCaducidad; }

    public LocalDate getFechaEntrada() { return fechaEntrada; }
    public void setFechaEntrada(LocalDate fechaEntrada) { this.fechaEntrada = fechaEntrada; }

    public String getCodigoNfc() { return codigoNfc; }
    public void setCodigoNfc(String codigoNfc) { this.codigoNfc = codigoNfc; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    public boolean isEnListaCompra() { return enListaCompra; }
    public void setEnListaCompra(boolean enListaCompra) { this.enListaCompra = enListaCompra; }

    // Días restantes hasta caducar (negativo si ya caducó). Null si no tiene fecha.
    @Transient
    public Long getDiasParaCaducar() {
        if (fechaCaducidad == null) return null;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), fechaCaducidad);
    }
}