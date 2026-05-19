package com.mycompany.app.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "gastos")
public class Gasto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String concepto;
    private Double monto;
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Moneda moneda;
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CategoriaGasto categoria = CategoriaGasto.OTROS;
    @Column(length = 8)
    private String emote;
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean repartoGeneral = true;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean pagado = false;
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(name = "ticket_url", length = 500)
    private String ticketUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_reparto", length = 20, nullable = false, columnDefinition = "varchar(20) default 'IGUAL'")
    private TipoReparto tipoReparto = TipoReparto.IGUAL;

    /** Cuotas individuales; solo se usan cuando tipoReparto != IGUAL */
    @OneToMany(mappedBy = "gasto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<GastoCuota> cuotas = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario pagador;

    @ManyToOne
    @JoinColumn(name = "grupo_id")
    private Grupo grupo;

        @ManyToMany(fetch = FetchType.EAGER)
        @JoinTable(
            name = "gasto_participantes",
            joinColumns = @JoinColumn(name = "gasto_id"),
            inverseJoinColumns = @JoinColumn(name = "usuario_id")
        )
        private List<Usuario> participantes = new ArrayList<>();

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
    public CategoriaGasto getCategoria() { return categoria; }
    public void setCategoria(CategoriaGasto categoria) { this.categoria = categoria; }
    public Moneda getMoneda() { return moneda; }
    public void setMoneda(Moneda moneda) { this.moneda = moneda; }
    public String getEmote() { return emote; }
    public void setEmote(String emote) { this.emote = emote; }
    public boolean isRepartoGeneral() { return repartoGeneral; }
    public void setRepartoGeneral(Boolean repartoGeneral) { this.repartoGeneral = repartoGeneral; }
    public boolean isPagado() { return pagado; }
    public void setPagado(boolean pagado) { this.pagado = pagado; }
    public LocalDateTime getFecha() { return fecha; }
    public Usuario getPagador() { return pagador; }
    public void setPagador(Usuario pagador) { this.pagador = pagador; }
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Grupo getGrupo() { return grupo; }
    public void setGrupo(Grupo grupo) { this.grupo = grupo; }
    public List<Usuario> getParticipantes() { return participantes; }
    public void setParticipantes(List<Usuario> participantes) { this.participantes = participantes; }
    public String getTicketUrl() { return ticketUrl; }
    public void setTicketUrl(String ticketUrl) { this.ticketUrl = ticketUrl; }
    public TipoReparto getTipoReparto() { return tipoReparto; }
    public void setTipoReparto(TipoReparto tipoReparto) { this.tipoReparto = tipoReparto; }
    @JsonIgnore
    public List<GastoCuota> getCuotas() { return cuotas; }
    public void setCuotas(List<GastoCuota> cuotas) { this.cuotas = cuotas; }

    /**
     * Campo transitorio (no se persiste) para recibir las cuotas desde el frontend.
     * Clave: userId, Valor: monto o porcentaje según tipoReparto.
     */
    @Transient
    private Map<Long, Double> cuotasMap;
    public Map<Long, Double> getCuotasMap() { return cuotasMap; }
    public void setCuotasMap(Map<Long, Double> cuotasMap) { this.cuotasMap = cuotasMap; }
}