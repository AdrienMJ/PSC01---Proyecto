package com.mycompany.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TransferenciaDTO {
    private Long deUsuarioId;
    private String deUsername;
    private Long aUsuarioId;
    private String aUsername;
    private double monto;

    public TransferenciaDTO(Long deUsuarioId, String deUsername, Long aUsuarioId, String aUsername, double monto) {
        this.deUsuarioId = deUsuarioId;
        this.deUsername = deUsername;
        this.aUsuarioId = aUsuarioId;
        this.aUsername = aUsername;
        this.monto = monto;
    }

    public Long getDeUsuarioId() { return deUsuarioId; }
    public String getDeUsername() { return deUsername; }
    public Long getAUsuarioId() { return aUsuarioId; }
    @JsonProperty("aUsername")
    public String getAUsername() { return aUsername; }
    public double getMonto() { return monto; }
}