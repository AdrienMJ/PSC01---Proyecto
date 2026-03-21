package com.mycompany.app.repository;

import com.mycompany.app.entity.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GrupoRepository extends JpaRepository<Grupo, Long> {
    
    // Spring Data JPA creará la consulta automáticamente analizando el nombre del método
    //Solo mostrara grupos donde el usuario con id 'usuarioId' es miembro (ningun otro grupo)
    List<Grupo> findByMiembros_Id(Long usuarioId);

    boolean existsByIdAndMiembros_Id(Long grupoId, Long usuarioId);
}