package com.mycompany.app.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "gasto_cuotas")
public class GastoCuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "gasto_id", nullable = false)
    private Gasto gasto;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Monto exacto que le corresponde a este usuario en este gasto */
    @Column(nullable = false)
    private Double monto;

    public GastoCuota() {}

    public GastoCuota(Gasto gasto, Usuario usuario, Double monto) {
        this.gasto = gasto;
        this.usuario = usuario;
        this.monto = monto;
    }

    public Long getId() { return id; }
    public Gasto getGasto() { return gasto; }
    public void setGasto(Gasto gasto) { this.gasto = gasto; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }
}