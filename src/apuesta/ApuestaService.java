package apuesta;

import juego.Juego;
import juego.JuegoService;
import mesa.Mesa;
import mesa.MesaService;
import usuario.Usuario;
import usuario.UsuarioService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ApuestaService {
    private final ApuestaRepository repository;
    private final UsuarioService usuarioService;
    private final MesaService mesaService;
    private final JuegoService juegoService;
    private final Random random = new Random();

    public ApuestaService(ApuestaRepository repository, UsuarioService usuarioService, MesaService mesaService, JuegoService juegoService) {
        this.repository = repository;
        this.usuarioService = usuarioService;
        this.mesaService = mesaService;
        this.juegoService = juegoService;
    }

    public synchronized Apuesta realizarApuesta(Usuario actor, Long usuarioId, Long mesaId, BigDecimal monto) {
        validarActorApuesta(actor, usuarioId);
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto apostado debe ser mayor a cero");
        }

        Usuario usuario = usuarioService.obtenerPorId(usuarioId);
        Mesa mesa = mesaService.obtenerPorId(mesaId);
        Juego juego = juegoService.obtenerPorId(mesa.getIdJuego());

        validarMesaYSaldo(usuario, mesa, monto);

        BigDecimal resultadoMonto = resolverResultadoAleatorio(juego, monto);
        aplicarResultado(usuarioId, actor, resultadoMonto);
        Apuesta apuesta = new Apuesta(null, usuarioId, mesaId, monto, resultadoMonto, LocalDateTime.now());
        return repository.save(apuesta);
    }

    public synchronized Apuesta jugarBlackjack(Usuario actor, Long usuarioId, Long mesaId, BigDecimal monto, List<String> decisiones) {
        validarActorApuesta(actor, usuarioId);
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto apostado debe ser mayor a cero");
        }

        Usuario usuario = usuarioService.obtenerPorId(usuarioId);
        Mesa mesa = mesaService.obtenerPorId(mesaId);
        Juego juego = juegoService.obtenerPorId(mesa.getIdJuego());
        if (!juego.getNombre().equalsIgnoreCase("blackjack")) {
            throw new IllegalArgumentException("La mesa indicada no es de Blackjack");
        }

        validarMesaYSaldo(usuario, mesa, monto);

        int totalJugador = 12 + random.nextInt(5);
        int totalCrupier = 15 + random.nextInt(5);

        for (String decision : decisiones) {
            if ("PEDIR".equalsIgnoreCase(decision)) {
                totalJugador += 1 + random.nextInt(10);
                if (totalJugador > 21) {
                    break;
                }
            }
            if ("PLANTARSE".equalsIgnoreCase(decision)) {
                break;
            }
        }

        while (totalJugador <= 21 && totalCrupier < 17) {
            totalCrupier += 1 + random.nextInt(10);
        }

        BigDecimal resultadoMonto;
        if (totalJugador > 21) {
            resultadoMonto = monto.negate();
        } else if (totalCrupier > 21 || totalJugador > totalCrupier) {
            resultadoMonto = monto;
        } else if (totalJugador == totalCrupier) {
            resultadoMonto = BigDecimal.ZERO;
        } else {
            resultadoMonto = monto.negate();
        }

        aplicarResultado(usuarioId, actor, resultadoMonto);
        Apuesta apuesta = new Apuesta(null, usuarioId, mesaId, monto, resultadoMonto, LocalDateTime.now());
        return repository.save(apuesta);
    }

    public List<HistorialPartida> obtenerHistorial(Usuario actor, Long usuarioId) {
        if (actor == null) {
            throw new SecurityException("No autorizado para ver este historial");
        }
        if (actor.esJugador() && !actor.getId().equals(usuarioId)) {
            throw new SecurityException("Un jugador solo puede ver su propio historial");
        }

        List<Apuesta> apuestas = repository.findByUsuarioId(usuarioId);
        List<HistorialPartida> historial = new ArrayList<>();
        for (Apuesta apuesta : apuestas) {
            Mesa mesa = mesaService.obtenerPorId(apuesta.getIdMesa());
            Juego juego = juegoService.obtenerPorId(mesa.getIdJuego());
            historial.add(new HistorialPartida(
                    apuesta.getId(),
                    apuesta.getFecha(),
                    juego.getNombre(),
                    apuesta.getMontoApostado(),
                    apuesta.getResultadoMonto()
            ));
        }
        return historial;
    }

    public List<ReporteRecaudacion> reporteRecaudacion(Usuario actor, LocalDateTime desde, LocalDateTime hasta, Long juegoId) {
        requireAdmin(actor);
        List<Apuesta> apuestas = repository.findByFecha(desde, hasta);

        if (juegoId != null) {
                apuestas = apuestas.stream()
                    .filter(a -> mesaService.obtenerPorId(a.getIdMesa()).getIdJuego().equals(juegoId))
                    .toList();
        }

        List<ReporteRecaudacion> resultado = new ArrayList<>();
        List<Juego> juegos = juegoId == null ? juegoService.listar() : List.of(juegoService.obtenerPorId(juegoId));

        for (Juego juego : juegos) {
            BigDecimal totalApostado = BigDecimal.ZERO;
            BigDecimal totalResultadoCasino = BigDecimal.ZERO;
            long cantidad = 0;

            for (Apuesta apuesta : apuestas) {
                Mesa mesa = mesaService.obtenerPorId(apuesta.getIdMesa());
                if (!mesa.getIdJuego().equals(juego.getId())) {
                    continue;
                }
                cantidad++;
                totalApostado = totalApostado.add(apuesta.getMontoApostado());
                totalResultadoCasino = totalResultadoCasino.subtract(apuesta.getResultadoMonto());
            }

            resultado.add(new ReporteRecaudacion(
                    juego.getNombre(),
                    cantidad,
                    totalApostado.setScale(2, RoundingMode.HALF_UP),
                    totalResultadoCasino.setScale(2, RoundingMode.HALF_UP)
            ));
        }

        return resultado;
    }

    private void validarMesaYSaldo(Usuario usuario, Mesa mesa, BigDecimal monto) {
        if (!Mesa.ESTADO_ABIERTA.equalsIgnoreCase(mesa.getEstado())) {
            throw new IllegalStateException("La mesa esta cerrada");
        }
        if (monto.compareTo(mesa.getApuestaMinima()) < 0 || monto.compareTo(mesa.getApuestaMaxima()) > 0) {
            throw new IllegalArgumentException("El monto no respeta los limites de la mesa");
        }
        if (usuario.getSaldo().compareTo(monto) < 0) {
            throw new IllegalArgumentException("Saldo insuficiente");
        }
    }

    private BigDecimal resolverResultadoAleatorio(Juego juego, BigDecimal monto) {
        boolean gana = random.nextBoolean();
        if (!gana) {
            return monto.negate();
        }
        if ("blackjack".equalsIgnoreCase(juego.getNombre())) {
            return monto.multiply(new BigDecimal("1.5"));
        }
        return monto;
    }

    private void aplicarResultado(Long usuarioId, Usuario actor, BigDecimal resultadoMonto) {
        if (resultadoMonto.compareTo(BigDecimal.ZERO) > 0) {
            usuarioService.depositar(actor, usuarioId, resultadoMonto);
            return;
        }
        if (resultadoMonto.compareTo(BigDecimal.ZERO) < 0) {
            usuarioService.retirar(actor, usuarioId, resultadoMonto.abs());
        }
    }

    private void validarActorApuesta(Usuario actor, Long usuarioId) {
        if (actor == null) {
            throw new SecurityException("No autorizado para apostar por este usuario");
        }
        if (actor.esAdmin()) {
            return;
        }
        if (actor.esJugador() && actor.getId().equals(usuarioId)) {
            return;
        }
        throw new SecurityException("No autorizado para apostar por este usuario");
    }

    private void requireAdmin(Usuario actor) {
        if (actor == null || !actor.esAdmin()) {
            throw new SecurityException("Operacion solo permitida para ADMIN");
        }
    }

    public static class HistorialPartida {
        private final Long idApuesta;
        private final LocalDateTime fecha;
        private final String juego;
        private final BigDecimal montoApostado;
        private final BigDecimal resultadoMonto;

        public HistorialPartida(Long idApuesta, LocalDateTime fecha, String juego, BigDecimal montoApostado, BigDecimal resultadoMonto) {
            this.idApuesta = idApuesta;
            this.fecha = fecha;
            this.juego = juego;
            this.montoApostado = montoApostado;
            this.resultadoMonto = resultadoMonto;
        }

        public Long getIdApuesta() {
            return idApuesta;
        }

        public LocalDateTime getFecha() {
            return fecha;
        }

        public String getJuego() {
            return juego;
        }

        public BigDecimal getMontoApostado() {
            return montoApostado;
        }

        public BigDecimal getResultadoMonto() {
            return resultadoMonto;
        }
    }

    public static class ReporteRecaudacion {
        private final String juego;
        private final long cantidadApuestas;
        private final BigDecimal totalApostado;
        private final BigDecimal resultadoCasino;

        public ReporteRecaudacion(String juego, long cantidadApuestas, BigDecimal totalApostado, BigDecimal resultadoCasino) {
            this.juego = juego;
            this.cantidadApuestas = cantidadApuestas;
            this.totalApostado = totalApostado;
            this.resultadoCasino = resultadoCasino;
        }

        public String getJuego() {
            return juego;
        }

        public long getCantidadApuestas() {
            return cantidadApuestas;
        }

        public BigDecimal getTotalApostado() {
            return totalApostado;
        }

        public BigDecimal getResultadoCasino() {
            return resultadoCasino;
        }
    }
}
