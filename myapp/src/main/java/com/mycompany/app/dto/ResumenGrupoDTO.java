package com.mycompany.app.dto;

import java.util.List;

public class ResumenGrupoDTO {
    private Double totalGastado; // <-- Nuevo campo para la HU #11
    private List<BalancePersonaDTO> balances;
    private List<TransferenciaDTO> soluciones;

    public ResumenGrupoDTO() {}

    public ResumenGrupoDTO(Double totalGastado, List<BalancePersonaDTO> balances, List<TransferenciaDTO> soluciones) {
        this.totalGastado = totalGastado;
        this.balances = balances;
        this.soluciones = soluciones;
    }

    public void setTotalGastado(Double totalGastado) { this.totalGastado = totalGastado; }
    public void setBalances(List<BalancePersonaDTO> balances) { this.balances = balances; }
    public void setSoluciones(List<TransferenciaDTO> soluciones) { this.soluciones = soluciones; }

    public Double getTotalGastado() { return totalGastado; }
    public List<BalancePersonaDTO> getBalances() { return balances; }
    public List<TransferenciaDTO> getSoluciones() { return soluciones; }
}