package com.mycompany.app.dto;

import java.util.List;

public class ResumenGrupoDTO {
    private List<BalancePersonaDTO> balances;
    private List<TransferenciaDTO> soluciones;

    public ResumenGrupoDTO(List<BalancePersonaDTO> balances, List<TransferenciaDTO> soluciones) {
        this.balances = balances;
        this.soluciones = soluciones;
    }

    public List<BalancePersonaDTO> getBalances() { return balances; }
    public List<TransferenciaDTO> getSoluciones() { return soluciones; }
}