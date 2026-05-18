package com.mycompany.app.repository;

import com.mycompany.app.entity.Comentario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComentarioRepository extends JpaRepository<Comentario, Long> {
    List<Comentario> findByGastoIdOrderByFechaAsc(Long gastoId);
}