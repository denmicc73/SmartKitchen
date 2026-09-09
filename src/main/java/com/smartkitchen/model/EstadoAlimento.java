package com.smartkitchen.model;

/**
 * Estado visual de un alimento del inventario. No se persiste: se calcula
 * a partir de la cantidad, el stock mínimo y la fecha de caducidad.
 */
public enum EstadoAlimento {
    DISPONIBLE("Disponible", "🟢"),
    CADUCA_PRONTO("Caduca pronto", "🟠"),
    POCO_STOCK("Poco stock", "🟠"),
    AGOTADO("Agotado", "🔴"),
    CADUCADO("Caducado", "🔴");

    private final String etiqueta;
    private final String emoji;

    EstadoAlimento(String etiqueta, String emoji) {
        this.etiqueta = etiqueta;
        this.emoji = emoji;
    }

    public String getEtiqueta() { return etiqueta; }
    public String getEmoji() { return emoji; }

    public boolean isAlerta() {
        return this != DISPONIBLE;
    }
}
