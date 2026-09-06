package com.smartkitchen.repository;

import com.smartkitchen.model.Alimento;
import com.smartkitchen.model.Zona;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AlimentoRepository extends JpaRepository<Alimento, Long> {
    List<Alimento> findByZonaOrderByFechaCaducidadAsc(Zona zona);
    List<Alimento> findAllByOrderByFechaCaducidadAsc();
    List<Alimento> findByFechaCaducidadLessThanEqualOrderByFechaCaducidadAsc(LocalDate fecha);
}
