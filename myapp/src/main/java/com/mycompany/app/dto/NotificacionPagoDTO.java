package com.mycompany.app.dto;

import java.time.LocalDateTime;

/**
 * Represents a payment that is pending confirmation by the receptor.
 * Returned to the receptor so they can confirm or reject it.
 */
public class NotificacionPagoDTO {

    private Long pagoId;
    private Long pagadorId;
    private String pagadorUsername;
    private Long grupoId;
    private String grupoNombre;
    private String moneda;
    private Double monto;
    private LocalDateTime fecha;

    public NotificacionPagoDTO() {}

    public NotificacionPagoDTO(Long pagoId, Long pagadorId, String pagadorUsername,
                                Long grupoId, String grupoNombre, String moneda,
                                Double monto, LocalDateTime fecha) {
        this.pagoId = pagoId;
        this.pagadorId = pagadorId;
        this.pagadorUsername = pagadorUsername;
        this.grupoId = grupoId;
        this.grupoNombre = grupoNombre;
        this.moneda = moneda;
        this.monto = monto;
        this.fecha = fecha;
    }

    public Long getPagoId() { return pagoId; }
    public void setPagoId(Long pagoId) { this.pagoId = pagoId; }
    public Long getPagadorId() { return pagadorId; }
    public void setPagadorId(Long pagadorId) { this.pagadorId = pagadorId; }
    public String getPagadorUsername() { return pagadorUsername; }
    public void setPagadorUsername(String pagadorUsername) { this.pagadorUsername = pagadorUsername; }
    public Long getGrupoId() { return grupoId; }
    public void setGrupoId(Long grupoId) { this.grupoId = grupoId; }
    public String getGrupoNombre() { return grupoNombre; }
    public void setGrupoNombre(String grupoNombre) { this.grupoNombre = grupoNombre; }
    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
