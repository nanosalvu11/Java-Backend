package usuario;

import java.math.BigDecimal;

public class Usuario {
    public static final String ROL_ADMIN = "ADMIN";
    public static final String ROL_JUGADOR = "JUGADOR";

    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String password;
    private BigDecimal saldo;
    private String rol;

    public Usuario() {
    }

    public Usuario(Long id, String nombre, String apellido, String email, String password, BigDecimal saldo, String rol) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.password = password;
        this.saldo = saldo;
        this.rol = rol;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public boolean esAdmin() {
        return ROL_ADMIN.equalsIgnoreCase(rol);
    }

    public boolean esJugador() {
        return ROL_JUGADOR.equalsIgnoreCase(rol);
    }
}
