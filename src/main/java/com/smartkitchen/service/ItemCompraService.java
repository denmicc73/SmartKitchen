package com.smartkitchen.service;

import com.smartkitchen.model.CategoriaCompra;
import com.smartkitchen.model.ItemCompra;
import com.smartkitchen.model.Prioridad;
import com.smartkitchen.repository.ItemCompraRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ItemCompraService {

    private final ItemCompraRepository itemCompraRepository;

    public ItemCompraService(ItemCompraRepository itemCompraRepository) {
        this.itemCompraRepository = itemCompraRepository;
    }

    public List<ItemCompra> pendientes() {
        return itemCompraRepository.findByCompradoFalseOrderByNombreAsc();
    }

    public long numPendientes() {
        return itemCompraRepository.countByCompradoFalse();
    }

    public List<ItemCompra> prioritarios() {
        return pendientes().stream()
                .filter(i -> i.getPrioridad() == Prioridad.ALTA)
                .toList();
    }

    /**
     * Pendientes agrupados por sección del supermercado, respetando el orden
     * del enum. Dentro de cada grupo, primero la prioridad más alta.
     */
    public Map<CategoriaCompra, List<ItemCompra>> pendientesAgrupados() {
        Comparator<ItemCompra> orden = Comparator
                .comparingInt((ItemCompra i) -> i.getPrioridad().ordinal())
                .thenComparing(ItemCompra::getNombre, String.CASE_INSENSITIVE_ORDER);
        List<ItemCompra> pendientes = pendientes();
        Map<CategoriaCompra, List<ItemCompra>> mapa = new LinkedHashMap<>();
        for (CategoriaCompra categoria : CategoriaCompra.values()) {
            List<ItemCompra> items = pendientes.stream()
                    .filter(i -> i.getCategoria() == categoria)
                    .sorted(orden)
                    .toList();
            if (!items.isEmpty()) {
                mapa.put(categoria, items);
            }
        }
        return mapa;
    }

    public List<ItemCompra> comprados() {
        return itemCompraRepository.findByCompradoTrueOrderByFechaCompradoDescNombreAsc();
    }

    /** Historial de compra de los últimos {@code dias} días. */
    public List<ItemCompra> historial(int dias) {
        return itemCompraRepository
                .findByCompradoTrueAndFechaCompradoGreaterThanEqualOrderByFechaCompradoDesc(LocalDate.now().minusDays(dias));
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
        item.setFechaComprado(comprado ? LocalDate.now() : null);
        itemCompraRepository.save(item);
    }

    public void eliminar(Long id) {
        itemCompraRepository.deleteById(id);
    }

    /** Vacía del historial todos los productos ya comprados. */
    public void vaciarHistorial() {
        itemCompraRepository.deleteAll(comprados());
    }
}
