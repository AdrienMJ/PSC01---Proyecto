package com.mycompany.app.repository;

import com.mycompany.app.entity.Gasto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GastoRepository extends JpaRepository<Gasto, Long> {

    // Obtener todos los gastos de un grupo
    List<Gasto> findByGrupoId(Long grupoId);

    // Obtener todos los gastos de un usuario (pagador)
    List<Gasto> findByPagadorId(Long usuarioId);
}