package com.smartkitchen.repository;

import com.smartkitchen.model.Alimento;
import com.smartkitchen.model.Zona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface AlimentoRepository extends JpaRepository<Alimento, Long> {
    List<Alimento> findByZonaOrderByFechaCaducidadAsc(Zona zona);
    List<Alimento> findAllByOrderByFechaCaducidadAsc();
    List<Alimento> findByFechaCaducidadLessThanEqualOrderByFechaCaducidadAsc(LocalDate fecha);

    @Query("select a from Alimento a where a.stockMinimo > 0 and a.cantidad <= a.stockMinimo order by a.nombre asc")
    List<Alimento> findBajoStock();
}
