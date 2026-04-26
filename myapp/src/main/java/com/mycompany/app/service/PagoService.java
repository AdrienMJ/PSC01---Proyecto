package com.mycompany.app.service;

import com.mycompany.app.entity.Grupo;
import com.mycompany.app.entity.Pago;
import com.mycompany.app.entity.Usuario;
import com.mycompany.app.repository.GrupoRepository;
import com.mycompany.app.repository.PagoRepository;
import com.mycompany.app.repository.UsuarioRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PagoService {

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private GrupoRepository grupoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public Pago registrarPago(Pago pago) throws Exception {
        if (pago.getMonto() == null || pago.getMonto() <= 0) {
            throw new Exception("El monto debe ser mayor que 0");
        }

        if (pago.getPagador() == null || pago.getPagador().getId() == null) {
            throw new Exception("El pagador es obligatorio");
        }

        if (pago.getReceptor() == null || pago.getReceptor().getId() == null) {
            throw new Exception("El receptor es obligatorio");
        }

        if (pago.getGrupo() == null || pago.getGrupo().getId() == null) {
            throw new Exception("El grupo es obligatorio");
        }

        if (pago.getPagador().getId().equals(pago.getReceptor().getId())) {
            throw new Exception("El pagador y el receptor no pueden ser la misma persona");
        }

        Grupo grupo = grupoRepository.findById(pago.getGrupo().getId())
                .orElseThrow(() -> new Exception("Grupo no encontrado"));

        Usuario pagador = usuarioRepository.findById(pago.getPagador().getId())
                .orElseThrow(() -> new Exception("Pagador no encontrado"));

        Usuario receptor = usuarioRepository.findById(pago.getReceptor().getId())
                .orElseThrow(() -> new Exception("Receptor no encontrado"));

        Set<Long> idsMiembros = grupo.getMiembros().stream()
                .map(Usuario::getId)
                .collect(Collectors.toSet());

        if (!idsMiembros.contains(pagador.getId())) {
            throw new Exception("El pagador no pertenece al grupo");
        }

        if (!idsMiembros.contains(receptor.getId())) {
            throw new Exception("El receptor no pertenece al grupo");
        }

        pago.setGrupo(grupo);
        pago.setPagador(pagador);
        pago.setReceptor(receptor);

        return pagoRepository.save(pago);
    }

    public List<Pago> obtenerHistorialPorGrupo(Long grupoId) throws Exception {
        grupoRepository.findById(grupoId)
                .orElseThrow(() -> new Exception("Grupo no encontrado"));
        return pagoRepository.findByGrupoIdOrderByFechaDesc(grupoId);
    }

    public List<Pago> obtenerPagosPorUsuario(Long usuarioId) {
        List<Pago> enviados = pagoRepository.findByPagadorId(usuarioId);
        List<Pago> recibidos = pagoRepository.findByReceptorId(usuarioId);
        enviados.addAll(recibidos);
        return enviados;
    }
}