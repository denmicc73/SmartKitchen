package com.smartkitchen.repository;

import com.smartkitchen.model.TipoToken;
import com.smartkitchen.model.TokenCuenta;
import com.smartkitchen.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface TokenCuentaRepository extends JpaRepository<TokenCuenta, Long> {

    Optional<TokenCuenta> findByValor(String valor);

    Optional<TokenCuenta> findByValorAndTipo(String valor, TipoToken tipo);

    @Modifying
    @Query("delete from TokenCuenta t where t.usuario = ?1 and t.tipo = ?2")
    void borrarPorUsuarioYTipo(Usuario usuario, TipoToken tipo);

    @Modifying
    @Query("delete from TokenCuenta t where t.expiraEn < ?1")
    int borrarCaducados(Instant limite);
}
