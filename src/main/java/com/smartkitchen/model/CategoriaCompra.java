package com.smartkitchen.model;

/** Secciones del supermercado para agrupar la lista de la compra. */
public enum CategoriaCompra {
    FRUTA_VERDURA("Fruta y verdura", "🥦"),
    CARNE("Carne", "🥩"),
    PESCADO("Pescado", "🐟"),
    LACTEOS("Lácteos", "🥛"),
    PANADERIA("Panadería", "🍞"),
    CONSERVAS("Conservas", "🥫"),
    CONGELADOS("Congelados", "❄️"),
    BEBIDAS("Bebidas", "🧃"),
    LIMPIEZA("Limpieza", "🧽"),
    OTROS("Otros", "🛒");

    private final String etiqueta;
    private final String emoji;

    CategoriaCompra(String etiqueta, String emoji) {
        this.etiqueta = etiqueta;
        this.emoji = emoji;
    }

    public String getEtiqueta() { return etiqueta; }
    public String getEmoji() { return emoji; }
}
