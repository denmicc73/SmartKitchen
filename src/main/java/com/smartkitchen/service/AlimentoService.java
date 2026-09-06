package com.smartkitchen.service;

import com.smartkitchen.model.Alimento;
import com.smartkitchen.model.Zona;
import com.smartkitchen.repository.AlimentoRepository;
import com.smartkitchen.repository.ItemCompraRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

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

    public List<Alimento> proximosACaducar(int dias) {
        return alimentoRepository.findByFechaCaducidadLessThanEqualOrderByFechaCaducidadAsc(LocalDate.now().plusDays(dias));
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
