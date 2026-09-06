package com.smartkitchen.repository;

import com.smartkitchen.model.ItemCompra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemCompraRepository extends JpaRepository<ItemCompra, Long> {
    List<ItemCompra> findByCompradoFalseOrderByNombreAsc();
    List<ItemCompra> findByCompradoTrueOrderByNombreAsc();
    boolean existsByNombreIgnoreCaseAndCompradoFalse(String nombre);
}
