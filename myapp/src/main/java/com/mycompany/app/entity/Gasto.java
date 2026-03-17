package com.mycompany.app.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "gastos")
public class Gasto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String concepto;
    private Double monto;
    private LocalDateTime fecha;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario pagador;

    @ManyToOne
    @JoinColumn(name = "grupo_id")
    private Grupo grupo;

    public Gasto() {}

    // Constructor que coincide con tu App.java (ajustado)
    public Gasto(String concepto, Double monto, Usuario pagador, Grupo grupo) {
        this.concepto = concepto;
        this.monto = monto;
        this.pagador = pagador;
        this.grupo = grupo;
        this.fecha = LocalDateTime.now();
    }

    // Getters y Setters
    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }
    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }
}