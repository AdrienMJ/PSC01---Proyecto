package com.myapp.entity;

public class Gasto {
    String descripcion;
    double gasto;
    String fecha;

    public Gasto(String descripcion, double gasto, String fecha) {
        this.descripcion = descripcion;
        this.gasto = gasto;
        this.fecha = fecha;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public double getgasto() {
        return gasto;
    }

    public void setgasto(double gasto) {
        this.gasto = gasto;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }
}
