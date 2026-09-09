package com.smartkitchen.service;

import com.smartkitchen.model.Alimento;
import com.smartkitchen.model.EstadoAlimento;
import com.smartkitchen.model.Zona;
import com.smartkitchen.repository.AlimentoRepository;
import com.smartkitchen.repository.ItemCompraRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AlimentoService {

    private final AlimentoRepository alimentoRepository;
    private final ItemCompraRepository itemCompraRepository;

    public AlimentoService(AlimentoRepository alimentoRepository, ItemCompraRepository itemCompraRepository) {
        this.alimentoRepository = alimentoRepository;
        this.itemCompraRepository = itemCompraRepository;
    }

    public List<Alimento> listarTodos() {
        return alimentoRepository.findAllByOrderByFechaCaducidadAsc();
    }

    public List<Alimento> listarPorZona(Zona zona) {
        return alimentoRepository.findByZonaOrderByFechaCaducidadAsc(zona);
    }

    /**
     * Alimentos agrupados por zona, en el orden del enum {@link Zona} y con las
     * zonas vacías omitidas. Dentro de cada zona se mantiene el orden por fecha
     * de caducidad de {@link #listarTodos()}.
     */
    public Map<Zona, List<Alimento>> agrupadosPorZona() {
        List<Alimento> todos = listarTodos();
        Map<Zona, List<Alimento>> mapa = new java.util.LinkedHashMap<>();
        for (Zona zona : Zona.values()) {
            List<Alimento> deLaZona = todos.stream().filter(a -> a.getZona() == zona).toList();
            if (!deLaZona.isEmpty()) {
                mapa.put(zona, deLaZona);
            }
        }
        return mapa;
    }

    public List<Alimento> proximosACaducar(int dias) {
        return alimentoRepository.findByFechaCaducidadLessThanEqualOrderByFechaCaducidadAsc(LocalDate.now().plusDays(dias));
    }

    public List<Alimento> caducados() {
        return proximosACaducar(0).stream()
                .filter(a -> a.getEstado() == EstadoAlimento.CADUCADO)
                .toList();
    }

    public List<Alimento> bajoStock() {
        return alimentoRepository.findBajoStock();
    }

    /** Recuento por estado visual, para el dashboard. */
    public Map<EstadoAlimento, Long> resumenPorEstado() {
        return listarTodos().stream()
                .collect(Collectors.groupingBy(Alimento::getEstado, Collectors.counting()));
    }

    public long total() {
        return alimentoRepository.count();
    }

    // Igual que proximosACaducar, pero sin los que el usuario ya marcó
    // como "Sí" en el aviso de añadir a la lista de la compra. Se usa
    // para no volver a mostrar la pregunta una vez respondida.
    public List<Alimento> avisosPendientes(int dias) {
        return proximosACaducar(dias).stream()
                .filter(a -> !a.isEnListaCompra())
                .filter(a -> !itemCompraRepository.existsByNombreIgnoreCaseAndCompradoFalse(a.getNombre()))
                .toList();
    }

    public Alimento guardar(Alimento alimento) {
        return alimentoRepository.save(alimento);
    }

    public Alimento buscarPorId(Long id) {
        return alimentoRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Alimento no encontrado: " + id));
    }

    public Alimento buscarPorCodigoNfc(String codigo) {
        return alimentoRepository.findAll().stream()
                .filter(a -> codigo.equals(a.getCodigoNfc()))
                .findFirst()
                .orElse(null);
    }

    public void eliminar(Long id) {
        alimentoRepository.deleteById(id);
    }
}
