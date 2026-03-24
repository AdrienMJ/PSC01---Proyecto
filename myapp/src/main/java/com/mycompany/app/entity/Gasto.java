package com.mycompany.app.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "gastos")
public class Gasto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String concepto;
    private Double monto;
    private LocalDateTime fecha = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario pagador;

    @ManyToOne
    @JoinColumn(name = "grupo_id")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Grupo grupo;

    public Gasto() {}

    public Gasto(String concepto, Double monto, Usuario pagador, Grupo grupo) {
        this.concepto = concepto;
        this.monto = monto;
        this.pagador = pagador;
        this.grupo = grupo;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }
    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }
    public LocalDateTime getFecha() { return fecha; }
    public Usuario getPagador() { return pagador; }
    public void setPagador(Usuario pagador) { this.pagador = pagador; }
    public Grupo getGrupo() { return grupo; }
    public void setGrupo(Grupo grupo) { this.grupo = grupo; }
}