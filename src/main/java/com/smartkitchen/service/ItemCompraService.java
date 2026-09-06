package com.smartkitchen.service;

import com.smartkitchen.model.ItemCompra;
import com.smartkitchen.repository.ItemCompraRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemCompraService {

    private final ItemCompraRepository itemCompraRepository;

    public ItemCompraService(ItemCompraRepository itemCompraRepository) {
        this.itemCompraRepository = itemCompraRepository;
    }

    public List<ItemCompra> pendientes() {
        return itemCompraRepository.findByCompradoFalseOrderByNombreAsc();
    }

    public List<ItemCompra> comprados() {
        return itemCompraRepository.findByCompradoTrueOrderByNombreAsc();
    }

    public ItemCompra guardar(ItemCompra item) {
        return itemCompraRepository.save(item);
    }

    public ItemCompra buscarPorId(Long id) {
        return itemCompraRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item no encontrado: " + id));
    }

    public void marcarComprado(Long id, boolean comprado) {
        ItemCompra item = buscarPorId(id);
        item.setComprado(comprado);
        itemCompraRepository.save(item);
    }

    public void eliminar(Long id) {
        itemCompraRepository.deleteById(id);
    }
}
