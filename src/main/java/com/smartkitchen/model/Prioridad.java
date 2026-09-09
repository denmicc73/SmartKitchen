package com.smartkitchen.model;

/** Prioridad de un producto en la lista de la compra. */
public enum Prioridad {
    ALTA("Alta", "🔴"),
    MEDIA("Media", "🟠"),
    BAJA("Baja", "⚪");

    private final String etiqueta;
    private final String emoji;

    Prioridad(String etiqueta, String emoji) {
        this.etiqueta = etiqueta;
        this.emoji = emoji;
    }

    public String getEtiqueta() { return etiqueta; }
    public String getEmoji() { return emoji; }
}
