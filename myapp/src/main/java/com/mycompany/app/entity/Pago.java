package com.mycompany.app.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
public class Pago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double monto;
    private LocalDateTime fecha = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "pagador_id")
    private Usuario pagador; // quien paga la deuda

    @ManyToOne
    @JoinColumn(name = "receptor_id")
    private Usuario receptor; // quien recibe el pago

    @ManyToOne
    @JoinColumn(name = "grupo_id")
    private Grupo grupo;

    public Pago() {}

    public Pago(Double monto, Usuario pagador, Usuario receptor, Grupo grupo) {
        this.monto = monto;
        this.pagador = pagador;
        this.receptor = receptor;
        this.grupo = grupo;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }
    public LocalDateTime getFecha() { return fecha; }
    public Usuario getPagador() { return pagador; }
    public void setPagador(Usuario pagador) { this.pagador = pagador; }
    public Usuario getReceptor() { return receptor; }
    public void setReceptor(Usuario receptor) { this.receptor = receptor; }
    public Grupo getGrupo() { return grupo; }
    public void setGrupo(Grupo grupo) { this.grupo = grupo; }
}