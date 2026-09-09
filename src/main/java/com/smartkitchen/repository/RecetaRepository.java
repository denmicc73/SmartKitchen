package com.smartkitchen.repository;

import com.smartkitchen.model.Receta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecetaRepository extends JpaRepository<Receta, Long> {
    List<Receta> findAllByOrderByNombreAsc();
    List<Receta> findByFavoritaTrueOrderByNombreAsc();
    long countByFavoritaTrue();
}
