package com.mycompany.app.entity;

public class Gasto {
    private String descripcion;
    private double gasto;
    private String fecha;
    private Usuario pagadoPor;
    private Grupo grupo;

    public Gasto(String descripcion, double gasto, String fecha, Usuario pagadoPor, Grupo grupo) {
        this.descripcion = descripcion;
        this.gasto = gasto;
        this.fecha = fecha;
        this.pagadoPor = pagadoPor;
        this.grupo = grupo;
    }

    // Getters and setters
    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public double getGasto() {
        return gasto;
    }

    public void setGasto(double gasto) {
        this.gasto = gasto;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public Usuario getPagadoPor() {
        return pagadoPor;
    }

    public void setPagadoPor(Usuario pagadoPor) {
        this.pagadoPor = pagadoPor;
    }

    public Grupo getGrupo() {
        return grupo;
    }

    public void setGrupo(Grupo grupo) {
        this.grupo = grupo;
    }
}