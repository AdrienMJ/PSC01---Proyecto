package com.mycompany.app.service;

import com.mycompany.app.entity.Gasto;
import com.mycompany.app.entity.CategoriaGasto;
import com.mycompany.app.repository.GastoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GastoService {

    @Autowired
    private GastoRepository gastoRepository;
    

    public Gasto crear(Gasto gasto) throws Exception {
        // Validación básica
        if (gasto.getMonto() == null || gasto.getMonto() <= 0) {
            throw new Exception("El monto debe ser mayor que 0");
        }

        if (gasto.getCategoria() == null) {
            gasto.setCategoria(CategoriaGasto.OTROS);
        }

        if (gasto.getEmote() == null || gasto.getEmote().isBlank()) {
            gasto.setEmote("🧾");
        }

        return gastoRepository.save(gasto);
    }

    public List<Gasto> listarPorGrupo(Long grupoId, String ordenar, String direccion) {
        String propiedad = "fecha";
        if ("monto".equalsIgnoreCase(ordenar)) {
            propiedad = "monto";
        }

        Sort.Direction sentido = "asc".equalsIgnoreCase(direccion) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return gastoRepository.findByGrupoId(grupoId, Sort.by(sentido, propiedad));
    }

    public Gasto obtenerPorId(Long id) throws Exception {
        return gastoRepository.findById(id)
            .orElseThrow(() -> new Exception("Gasto no encontrado"));
    }

    public Gasto marcarComoPagado(Long gastoId, Long usuarioId) throws Exception {
        Gasto gasto = gastoRepository.findById(gastoId)
            .orElseThrow(() -> new Exception("Gasto no encontrado"));

        if (gasto.isPagado()) {
            throw new Exception("El gasto ya está marcado como pagado");
        }

        if (gasto.getGrupo() == null || gasto.getGrupo().getMiembros().stream().noneMatch(u -> u.getId().equals(usuarioId))) {
            throw new Exception("Usuario no pertenece al grupo");
        }

        if (gasto.getPagador() != null && gasto.getPagador().getId().equals(usuarioId)) {
            throw new Exception("El pagador no puede marcar su propio gasto como pagado");
        }

        gasto.setPagado(true);
        return gastoRepository.save(gasto);
    }
}
