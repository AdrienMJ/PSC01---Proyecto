package com.mycompany.app.repository;

import com.mycompany.app.entity.Gasto;
import com.mycompany.app.entity.CategoriaGasto;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GastoRepository extends JpaRepository<Gasto, Long> {

    // Obtener todos los gastos de un grupo
    List<Gasto> findByGrupoId(Long grupoId);
    List<Gasto> findByGrupoId(Long grupoId, Sort sort);
    List<Gasto> findByGrupoIdAndCategoria(Long grupoId, CategoriaGasto categoria, Sort sort);

    // Obtener todos los gastos de un usuario (pagador)
    List<Gasto> findByPagadorId(Long usuarioId);
    
    // Obtener gasto con grupo y miembros cargados
    @Query("SELECT g FROM Gasto g LEFT JOIN FETCH g.grupo WHERE g.id = :id")
    Optional<Gasto> findByIdWithGrupo(@Param("id") Long id);
    
    // Obtener grupo por ID con miembros
    @Query("SELECT g FROM Grupo g LEFT JOIN FETCH g.miembros WHERE g.id = :id")
    Optional<com.mycompany.app.entity.Grupo> findGrupoWithMembers(@Param("id") Long id);
}