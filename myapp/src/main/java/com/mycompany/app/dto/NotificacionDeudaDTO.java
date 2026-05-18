package com.mycompany.app.dto;

public class NotificacionDeudaDTO {
    private Long grupoId;
    private String grupoNombre;
    private String moneda;
    private Long acreedorId;
    private String acreedorUsername;
    private Double monto;

    public NotificacionDeudaDTO() {
    }

    public NotificacionDeudaDTO(Long grupoId, String grupoNombre, String moneda,
            Long acreedorId, String acreedorUsername, Double monto) {
        this.grupoId = grupoId;
        this.grupoNombre = grupoNombre;
        this.moneda = moneda;
        this.acreedorId = acreedorId;
        this.acreedorUsername = acreedorUsername;
        this.monto = monto;
    }

    public Long getGrupoId() {
        return grupoId;
    }

    public void setGrupoId(Long grupoId) {
        this.grupoId = grupoId;
    }

    public String getGrupoNombre() {
        return grupoNombre;
    }

    public void setGrupoNombre(String grupoNombre) {
        this.grupoNombre = grupoNombre;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public Long getAcreedorId() {
        return acreedorId;
    }

    public void setAcreedorId(Long acreedorId) {
        this.acreedorId = acreedorId;
    }

    public String getAcreedorUsername() {
        return acreedorUsername;
    }

    public void setAcreedorUsername(String acreedorUsername) {
        this.acreedorUsername = acreedorUsername;
    }

    public Double getMonto() {
        return monto;
    }

    public void setMonto(Double monto) {
        this.monto = monto;
    }
}