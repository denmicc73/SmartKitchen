package com.smartkitchen.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Migracion de esquema minima y defensiva.
 *
 * <p>{@code spring.jpa.hibernate.ddl-auto=update} NO puede anadir una columna
 * {@code NOT NULL} a una tabla que ya tiene filas: lo intenta, falla y deja la
 * columna sin crear, y a partir de ese momento cualquier consulta que la use
 * revienta. Este runner anade a mano, de forma idempotente, las columnas que se
 * han ido incorporando a las entidades despues de que hubiera datos.
 *
 * <p>Se ejecuta antes que el resto de {@code ApplicationRunner}/{@code
 * CommandLineRunner} (incluido el que crea el usuario admin). Cada sentencia va
 * en su propio try/catch para que un fallo no impida arrancar.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MigracionEsquema implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracionEsquema.class);

    private final JdbcTemplate jdbc;

    public MigracionEsquema(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        // usuarios: correo y verificacion (funcionalidad de registro/recuperacion)
        addColumn("usuarios", "email", "VARCHAR(255)");
        if (addColumn("usuarios", "email_verificado", "BOOLEAN DEFAULT FALSE NOT NULL")) {
            // Solo al crear la columna: las cuentas que ya existian se dan por
            // verificadas para no bloquearlas. No debe repetirse en cada arranque
            // o auto-verificaria altas pendientes de confirmar.
            run("UPDATE usuarios SET email_verificado = TRUE",
                    "marcar cuentas existentes como verificadas");
        }

        // usuarios: perfil (avatar y nombre visible)
        addColumn("usuarios", "nombre_visible", "VARCHAR(255)");
        addColumn("usuarios", "avatar_emoji", "VARCHAR(32) DEFAULT '🧑‍🍳' NOT NULL");

        // alimentos: categoria de supermercado y stock minimo
        addColumn("alimentos", "categoria", "VARCHAR(40) DEFAULT 'OTROS' NOT NULL");
        addColumn("alimentos", "stock_minimo", "DOUBLE PRECISION DEFAULT 0 NOT NULL");

        // items_compra: categoria y prioridad
        addColumn("items_compra", "categoria", "VARCHAR(40) DEFAULT 'OTROS' NOT NULL");
        addColumn("items_compra", "prioridad", "VARCHAR(20) DEFAULT 'MEDIA' NOT NULL");
    }

    /** @return true si la columna se acaba de crear; false si ya existia o no se pudo. */
    private boolean addColumn(String tabla, String columna, String definicion) {
        if (!tableExists(tabla) || columnExists(tabla, columna)) {
            return false;
        }
        run("ALTER TABLE " + tabla + " ADD COLUMN " + columna + " " + definicion,
                "anadir columna " + tabla + "." + columna);
        return columnExists(tabla, columna);
    }

    private boolean tableExists(String tabla) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME) = UPPER(?)",
                Integer.class, tabla);
        return n != null && n > 0;
    }

    private boolean columnExists(String tabla, String columna) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE UPPER(TABLE_NAME) = UPPER(?) AND UPPER(COLUMN_NAME) = UPPER(?)",
                Integer.class, tabla, columna);
        return n != null && n > 0;
    }

    private void run(String sql, String descripcion) {
        try {
            jdbc.execute(sql);
            log.info("Migracion: {}", descripcion);
        } catch (RuntimeException e) {
            log.warn("Migracion omitida ({}): {}", descripcion, e.getMessage());
        }
    }
}
