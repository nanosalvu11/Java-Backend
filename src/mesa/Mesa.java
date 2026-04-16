package mesa;

import java.math.BigDecimal;

public class Mesa {
    public static final String ESTADO_ABIERTA = "ABIERTA";
    public static final String ESTADO_CERRADA = "CERRADA";

    private Long id;
    private Long idJuego;
    private BigDecimal apuestaMinima;
    private BigDecimal apuestaMaxima;
    private String estado;

    public Mesa() {
    }

    public Mesa(Long id, Long idJuego, BigDecimal apuestaMinima, BigDecimal apuestaMaxima, String estado) {
        this.id = id;
        this.idJuego = idJuego;
        this.apuestaMinima = apuestaMinima;
        this.apuestaMaxima = apuestaMaxima;
        this.estado = estado;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdJuego() {
        return idJuego;
    }

    public void setIdJuego(Long idJuego) {
        this.idJuego = idJuego;
    }

    public BigDecimal getApuestaMinima() {
        return apuestaMinima;
    }

    public void setApuestaMinima(BigDecimal apuestaMinima) {
        this.apuestaMinima = apuestaMinima;
    }

    public BigDecimal getApuestaMaxima() {
        return apuestaMaxima;
    }

    public void setApuestaMaxima(BigDecimal apuestaMaxima) {
        this.apuestaMaxima = apuestaMaxima;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
