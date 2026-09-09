package com.smartkitchen.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Token de un solo uso enviado por correo para confirmar la cuenta o para
 * restablecer la contrasena. Caduca a las {@code expiraEn} y se marca como
 * {@code usado} en cuanto se consume.
 */
@Entity
@Table(name = "tokens_cuenta", indexes = @Index(name = "idx_token_valor", columnList = "valor", unique = true))
public class TokenCuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String valor;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoToken tipo;

    @Column(nullable = false)
    private Instant creadoEn = Instant.now();

    @Column(nullable = false)
    private Instant expiraEn;

    @Column(nullable = false)
    private boolean usado = false;

    public TokenCuenta() {}

    public TokenCuenta(Usuario usuario, TipoToken tipo, Instant expiraEn) {
        this.usuario = usuario;
        this.tipo = tipo;
        this.expiraEn = expiraEn;
        this.valor = UUID.randomUUID().toString().replace("-", "")
                + Long.toHexString(System.nanoTime());
    }

    public boolean esValido() {
        return !usado && Instant.now().isBefore(expiraEn);
    }

    public Long getId() { return id; }

    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public TipoToken getTipo() { return tipo; }
    public void setTipo(TipoToken tipo) { this.tipo = tipo; }

    public Instant getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Instant creadoEn) { this.creadoEn = creadoEn; }

    public Instant getExpiraEn() { return expiraEn; }
    public void setExpiraEn(Instant expiraEn) { this.expiraEn = expiraEn; }

    public boolean isUsado() { return usado; }
    public void setUsado(boolean usado) { this.usado = usado; }
}
