package com.mycompany.app.service;

import com.mycompany.app.entity.Gasto;
import com.mycompany.app.repository.GastoRepository;

import org.springframework.beans.factory.annotation.Autowired;
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

        return gastoRepository.save(gasto);
    }

    public List<Gasto> listarPorGrupo(Long grupoId) {
        return gastoRepository.findByGrupoId(grupoId);
    }
}