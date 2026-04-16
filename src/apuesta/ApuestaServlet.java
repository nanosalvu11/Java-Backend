package apuesta;

import usuario.Usuario;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ApuestaServlet {
    public static final String BASE_PATH = "/api/apuestas/*";

    private final ApuestaService apuestaService;

    public ApuestaServlet(ApuestaService apuestaService) {
        this.apuestaService = apuestaService;
    }

    public Apuesta postApostar(Usuario actor, Long usuarioId, Long mesaId, BigDecimal monto) {
        return apuestaService.realizarApuesta(actor, usuarioId, mesaId, monto);
    }

    public Apuesta postBlackjack(Usuario actor, Long usuarioId, Long mesaId, BigDecimal monto, List<String> decisiones) {
        return apuestaService.jugarBlackjack(actor, usuarioId, mesaId, monto, decisiones);
    }

    public List<ApuestaService.HistorialPartida> getHistorial(Usuario actor, Long usuarioId) {
        return apuestaService.obtenerHistorial(actor, usuarioId);
    }

    public List<ApuestaService.ReporteRecaudacion> getReporteRecaudacion(Usuario actor, LocalDateTime desde, LocalDateTime hasta, Long juegoId) {
        return apuestaService.reporteRecaudacion(actor, desde, hasta, juegoId);
    }
}
