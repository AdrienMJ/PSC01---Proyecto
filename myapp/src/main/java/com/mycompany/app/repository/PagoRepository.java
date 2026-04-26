package com.mycompany.app.repository;

import com.mycompany.app.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findByGrupoId(Long grupoId);
    List<Pago> findByPagadorId(Long usuarioId);
    List<Pago> findByReceptorId(Long usuarioId);
    List<Pago> findByGrupoIdOrderByFechaDesc(Long grupoId);
}