package com.smartkitchen.repository;

import com.smartkitchen.model.ItemCompra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ItemCompraRepository extends JpaRepository<ItemCompra, Long> {
    List<ItemCompra> findByCompradoFalseOrderByNombreAsc();
    List<ItemCompra> findByCompradoTrueOrderByFechaCompradoDescNombreAsc();
    List<ItemCompra> findByCompradoTrueAndFechaCompradoGreaterThanEqualOrderByFechaCompradoDesc(LocalDate desde);
    boolean existsByNombreIgnoreCaseAndCompradoFalse(String nombre);
    long countByCompradoFalse();
}
