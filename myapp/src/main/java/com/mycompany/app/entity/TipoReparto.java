package com.mycompany.app.entity;

public enum TipoReparto {
    /** Todos los participantes pagan lo mismo: monto / n */
    IGUAL,
    /** Cada participante tiene un porcentaje distinto (suman 100%) */
    PORCENTAJE,
    /** Cada participante tiene una cuota fija en € (suman el monto total) */
    CUOTA_FIJA
}