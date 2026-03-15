package com.mycompany.app.entity;

public class Usuario {
    private String nombreUsuario;
    private String correo;
    private String contraseña;
    private Grupo grupo;

    public Usuario(String nombreUsuario, String correo, String contraseña, Grupo grupo) {
        this.nombreUsuario = nombreUsuario;
        this.correo = correo;
        this.contraseña = contraseña;
        this.grupo = grupo;
    }

    // Getters and setters
    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContraseña() {
        return contraseña;
    }

    public void setContraseña(String contraseña) {
        this.contraseña = contraseña;
    }

    public Grupo getGrupo() {
        return grupo;
    }

    public void setGrupo(Grupo grupo) {
        this.grupo = grupo;
    }
}