package apuesta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Apuesta {
    private Long id;
    private Long idUsuario;
    private Long idMesa;
    private BigDecimal montoApostado;
    private BigDecimal resultadoMonto;
    private LocalDateTime fecha;

    public Apuesta() {
    }

    public Apuesta(Long id, Long idUsuario, Long idMesa, BigDecimal montoApostado, BigDecimal resultadoMonto, LocalDateTime fecha) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.idMesa = idMesa;
        this.montoApostado = montoApostado;
        this.resultadoMonto = resultadoMonto;
        this.fecha = fecha;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Long getIdMesa() {
        return idMesa;
    }

    public void setIdMesa(Long idMesa) {
        this.idMesa = idMesa;
    }

    public BigDecimal getMontoApostado() {
        return montoApostado;
    }

    public void setMontoApostado(BigDecimal montoApostado) {
        this.montoApostado = montoApostado;
    }

    public BigDecimal getResultadoMonto() {
        return resultadoMonto;
    }

    public void setResultadoMonto(BigDecimal resultadoMonto) {
        this.resultadoMonto = resultadoMonto;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}
