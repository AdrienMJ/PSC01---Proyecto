package entity;

public class Usuario extends Grupo {
    String nombreUsuario;
    String correo;
    String contraseña;
    Grupo idGrupo; 

    public Usuario(String nombreUsuario, String correo, String contraseña, Grupo idGrupo) {
        this.nombreUsuario = nombreUsuario;
        this.correo = correo;
        this.contraseña = contraseña;
        this.idGrupo = idGrupo;
    }

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

    public Grupo getIdGrupo() {
        return idGrupo;
    }

    public void setIdGrupo(Grupo idGrupo) {
        this.idGrupo = idGrupo;
    }
    

}
