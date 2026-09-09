package com.smartkitchen.model;

/** Momento del día o tipo de plato al que pertenece una receta. */
public enum CategoriaReceta {
    DESAYUNO("Desayuno", "🥐"),
    COMIDA("Comida", "🍽️"),
    CENA("Cena", "🌙"),
    POSTRE("Postre", "🍰"),
    SNACK("Snack", "🍿"),
    GUARNICION("Guarnición", "🥗"),
    BEBIDA("Bebida", "🥤");

    private final String etiqueta;
    private final String emoji;

    CategoriaReceta(String etiqueta, String emoji) {
        this.etiqueta = etiqueta;
        this.emoji = emoji;
    }

    public String getEtiqueta() { return etiqueta; }
    public String getEmoji() { return emoji; }
}
