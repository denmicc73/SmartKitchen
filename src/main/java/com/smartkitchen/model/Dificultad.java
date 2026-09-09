package com.smartkitchen.model;

/** Nivel de dificultad de una receta. */
public enum Dificultad {
    FACIL("Fácil", "🟢"),
    MEDIA("Media", "🟡"),
    DIFICIL("Difícil", "🔴");

    private final String etiqueta;
    private final String emoji;

    Dificultad(String etiqueta, String emoji) {
        this.etiqueta = etiqueta;
        this.emoji = emoji;
    }

    public String getEtiqueta() { return etiqueta; }
    public String getEmoji() { return emoji; }
}
