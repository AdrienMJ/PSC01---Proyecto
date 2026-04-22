package com.mycompany.app.dto;

public class BalancePersonaDTO {
    private Long usuarioId;
    private String username;
    private double balance;
    private String estado;

    public BalancePersonaDTO(Long usuarioId, String username, double balance, String estado) {
        this.usuarioId = usuarioId;
        this.username = username;
        this.balance = balance;
        this.estado = estado;
    }

    public Long getUsuarioId() { return usuarioId; }
    public String getUsername() { return username; }
    public double getBalance() { return balance; }
    public String getEstado() { return estado; }
}