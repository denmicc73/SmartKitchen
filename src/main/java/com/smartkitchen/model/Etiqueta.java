package com.smartkitchen.model;

/** Etiquetas libres para clasificar y filtrar recetas. */
public enum Etiqueta {
    CARNE("Carne", "🥩"),
    PESCADO("Pescado", "🐟"),
    PASTA("Pasta", "🍝"),
    ARROZ("Arroz", "🍚"),
    VERDURA("Verdura", "🥦"),
    SALUDABLE("Saludable", "🥗"),
    RAPIDA("Rápida", "⚡"),
    ECONOMICA("Económica", "💰"),
    VEGETARIANA("Vegetariana", "🌱"),
    VEGANA("Vegana", "🌿"),
    SIN_GLUTEN("Sin gluten", "🌾"),
    PICANTE("Picante", "🌶️"),
    HORNO("Al horno", "🔥"),
    FAVORITA("Favorita", "❤️");

    private final String etiqueta;
    private final String emoji;

    Etiqueta(String etiqueta, String emoji) {
        this.etiqueta = etiqueta;
        this.emoji = emoji;
    }

    public String getEtiqueta() { return etiqueta; }
    public String getEmoji() { return emoji; }
}
