package com.mycompany.app.repository;

import com.mycompany.app.entity.GastoCuota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GastoCuotaRepository extends JpaRepository<GastoCuota, Long> {
    List<GastoCuota> findByGastoId(Long gastoId);
    void deleteByGastoId(Long gastoId);
}