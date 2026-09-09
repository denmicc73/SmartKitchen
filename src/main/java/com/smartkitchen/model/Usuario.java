package com.smartkitchen.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String username;

    // Correo de contacto. Se usa para confirmar la cuenta y para recuperar la
    // contrasena. Puede ser null en cuentas antiguas creadas antes de esta
    // funcionalidad; para cuentas nuevas siempre se rellena.
    @Column(unique = true)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String rol = "MIEMBRO"; // ADMIN o MIEMBRO

    // true cuando el usuario puede iniciar sesion. Las altas por registro
    // publico nacen desactivadas hasta que se confirma el correo.
    @Column(nullable = false)
    private boolean activo = true;

    // true cuando el usuario ha pulsado el enlace de confirmacion de su correo.
    // columnDefinition con DEFAULT para que ddl-auto=update pueda anadir la
    // columna aunque la tabla ya tenga filas.
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean emailVerificado = false;

    // Nombre para mostrar en la app (si es null se usa el username).
    private String nombreVisible;

    // Emoji que hace de avatar en la barra superior y ajustes.
    @Column(nullable = false)
    private String avatarEmoji = "🧑‍🍳";

    public Usuario() {}

    public Usuario(String username, String passwordHash, String rol) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.rol = rol;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public boolean isEmailVerificado() { return emailVerificado; }
    public void setEmailVerificado(boolean emailVerificado) { this.emailVerificado = emailVerificado; }

    public String getNombreVisible() { return nombreVisible; }
    public void setNombreVisible(String nombreVisible) { this.nombreVisible = nombreVisible; }

    public String getAvatarEmoji() { return avatarEmoji; }
    public void setAvatarEmoji(String avatarEmoji) { this.avatarEmoji = avatarEmoji; }

    @Transient
    public String getNombreParaMostrar() {
        return (nombreVisible != null && !nombreVisible.isBlank()) ? nombreVisible : username;
    }

    @Transient
    public boolean isAdmin() {
        return "ADMIN".equals(rol);
    }
}
