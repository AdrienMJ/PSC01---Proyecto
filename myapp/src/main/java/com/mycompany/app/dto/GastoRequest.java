package com.mycompany.app.dto;

import com.mycompany.app.entity.Moneda; // Asegúrate de importar tu Enum

public class GastoRequest {
    public String concepto;
    public Double monto;
    public Long idPagador;
    public Long idGrupo;
    public Moneda moneda; // Cambiado de String a Moneda
    public boolean repartoGeneral;
}