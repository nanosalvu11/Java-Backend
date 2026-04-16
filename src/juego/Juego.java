package juego;

public class Juego {
    private Long id;
    private String nombre;
    private String reglas;

    public Juego() {
    }

    public Juego(Long id, String nombre, String reglas) {
        this.id = id;
        this.nombre = nombre;
        this.reglas = reglas;
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

    public String getReglas() {
        return reglas;
    }

    public void setReglas(String reglas) {
        this.reglas = reglas;
    }
}
