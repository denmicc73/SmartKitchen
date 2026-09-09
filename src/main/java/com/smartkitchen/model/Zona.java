package com.smartkitchen.model;

/** Dónde se guarda un alimento en casa. */
public enum Zona {
    NEVERA("Nevera", "🧊"),
    CONGELADOR("Congelador", "❄️"),
    DESPENSA("Despensa", "📦");

    private final String etiqueta;
    private final String emoji;

    Zona(String etiqueta, String emoji) {
        this.etiqueta = etiqueta;
        this.emoji = emoji;
    }

    public String getEtiqueta() { return etiqueta; }
    public String getEmoji() { return emoji; }
}
