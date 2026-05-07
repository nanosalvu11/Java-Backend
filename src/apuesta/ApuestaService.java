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
import java.util.Collections;
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

        BigDecimal resultadoMonto = juegoService.calcularResultadoAleatorio(juego, monto);
        aplicarResultado(usuarioId, actor, resultadoMonto);
        Apuesta apuesta = new Apuesta(null, usuarioId, mesaId, monto, resultadoMonto, LocalDateTime.now());
        return repository.save(apuesta);
    }

    public synchronized Apuesta jugarBlackjack(Usuario actor, Long usuarioId, Long mesaId, BigDecimal monto, List<String> decisiones) {
        return jugarBlackjackDetalle(actor, usuarioId, mesaId, monto, decisiones).getApuesta();
    }

    public synchronized BlackjackResultado jugarBlackjackDetalle(Usuario actor, Long usuarioId, Long mesaId, BigDecimal monto, List<String> decisiones) {
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

        List<String> playerCards = drawInitialHand(2);
        List<String> dealerCards = new ArrayList<>();
        dealerCards.add(drawCard());
        int playerScore = calculateScore(playerCards);
        int dealerScore = calculateScore(dealerCards);

        for (String decision : decisiones == null ? List.<String>of() : decisiones) {
            if ("PEDIR".equalsIgnoreCase(decision)) {
                playerCards.add(drawCard());
                playerScore = calculateScore(playerCards);
                if (playerScore > 21) {
                    break;
                }
            }
            if ("PLANTARSE".equalsIgnoreCase(decision)) {
                break;
            }
        }

        while (playerScore <= 21 && dealerScore < 17) {
            dealerCards.add(drawCard());
            dealerScore = calculateScore(dealerCards);
        }

        BigDecimal resultadoMonto;
        String gameResult;
        if (playerScore > 21) {
            resultadoMonto = monto.negate();
            gameResult = "PERDISTE - ¡Te pasaste de 21!";
        } else if (dealerScore > 21) {
            resultadoMonto = monto;
            gameResult = "¡GANASTE! - Dealer se pasó";
        } else if (playerScore > dealerScore) {
            resultadoMonto = monto;
            gameResult = "¡GANASTE! - Tienes mejor mano";
        } else if (playerScore == dealerScore) {
            resultadoMonto = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            gameResult = "EMPATE";
        } else {
            resultadoMonto = monto.negate();
            gameResult = "PERDISTE - Dealer tiene mejor mano";
        }

        aplicarResultado(usuarioId, actor, resultadoMonto);
        Apuesta apuesta = repository.save(new Apuesta(null, usuarioId, mesaId, monto, resultadoMonto, LocalDateTime.now()));
        return new BlackjackResultado(apuesta, playerCards, dealerCards, playerScore, dealerScore, gameResult);
    }

    public synchronized RuletaResultado jugarRuletaDetalle(Usuario actor, Long usuarioId, Long mesaId, BigDecimal monto, Integer selectedNumber, String selectedColor) {
        validarActorApuesta(actor, usuarioId);
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto apostado debe ser mayor a cero");
        }

        Usuario usuario = usuarioService.obtenerPorId(usuarioId);
        Mesa mesa = mesaService.obtenerPorId(mesaId);
        Juego juego = juegoService.obtenerPorId(mesa.getIdJuego());
        if (!juego.getNombre().equalsIgnoreCase("ruleta") && !juego.getNombre().equalsIgnoreCase("roulette")) {
            throw new IllegalArgumentException("La mesa indicada no es de Ruleta");
        }

        validarMesaYSaldo(usuario, mesa, monto);
        if (selectedNumber == null && (selectedColor == null || selectedColor.isBlank())) {
            throw new IllegalArgumentException("Debe seleccionar un número o un color");
        }

        int winningNumber = random.nextInt(37);
        String winningColor = colorRuleta(winningNumber);

        boolean won = false;
        BigDecimal winAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (selectedNumber != null && selectedNumber == winningNumber) {
            won = true;
            winAmount = monto.multiply(new BigDecimal("36")).setScale(2, RoundingMode.HALF_UP);
        } else if (selectedColor != null && selectedColor.equalsIgnoreCase(winningColor)) {
            won = true;
            winAmount = monto.multiply(new BigDecimal("2")).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal resultadoMonto = won ? winAmount : monto.negate();
        aplicarResultado(usuarioId, actor, resultadoMonto);
        Apuesta apuesta = repository.save(new Apuesta(null, usuarioId, mesaId, monto, resultadoMonto, LocalDateTime.now()));
        return new RuletaResultado(apuesta, winningNumber, winningColor, won, winAmount,
                selectedNumber, selectedColor);
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

    // La lógica de cálculo de resultados se delega ahora a JuegoService

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

    private List<String> drawInitialHand(int count) {
        List<String> hand = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            hand.add(drawCard());
        }
        return hand;
    }

    private String drawCard() {
        String[] suits = {"♠", "♥", "♦", "♣"};
        String[] ranks = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
        return ranks[random.nextInt(ranks.length)] + suits[random.nextInt(suits.length)];
    }

    private int calculateScore(List<String> cards) {
        int score = 0;
        int aces = 0;
        for (String card : cards) {
            String rank = card.replaceAll("[♠♥♦♣]", "");
            if ("A".equals(rank)) {
                score += 11;
                aces++;
            } else if ("J".equals(rank) || "Q".equals(rank) || "K".equals(rank)) {
                score += 10;
            } else {
                score += Integer.parseInt(rank);
            }
        }
        while (score > 21 && aces > 0) {
            score -= 10;
            aces--;
        }
        return score;
    }

    private String colorRuleta(int number) {
        if (number == 0) {
            return "green";
        }
        int[] red = {1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36};
        for (int n : red) {
            if (n == number) {
                return "red";
            }
        }
        return "black";
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

    public static class BlackjackResultado {
        private final Apuesta apuesta;
        private final List<String> playerCards;
        private final List<String> dealerCards;
        private final int playerScore;
        private final int dealerScore;
        private final String gameResult;

        public BlackjackResultado(Apuesta apuesta, List<String> playerCards, List<String> dealerCards, int playerScore, int dealerScore, String gameResult) {
            this.apuesta = apuesta;
            this.playerCards = playerCards;
            this.dealerCards = dealerCards;
            this.playerScore = playerScore;
            this.dealerScore = dealerScore;
            this.gameResult = gameResult;
        }

        public Apuesta getApuesta() { return apuesta; }
        public List<String> getPlayerCards() { return Collections.unmodifiableList(playerCards); }
        public List<String> getDealerCards() { return Collections.unmodifiableList(dealerCards); }
        public int getPlayerScore() { return playerScore; }
        public int getDealerScore() { return dealerScore; }
        public String getGameResult() { return gameResult; }
        public BigDecimal getWinAmount() { return apuesta.getResultadoMonto().compareTo(BigDecimal.ZERO) > 0 ? apuesta.getResultadoMonto() : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP); }
    }

    public static class RuletaResultado {
        private final Apuesta apuesta;
        private final int winningNumber;
        private final String winningColor;
        private final boolean won;
        private final BigDecimal winAmount;
        private final Integer selectedNumber;
        private final String selectedColor;

        public RuletaResultado(Apuesta apuesta, int winningNumber, String winningColor, boolean won, BigDecimal winAmount, Integer selectedNumber, String selectedColor) {
            this.apuesta = apuesta;
            this.winningNumber = winningNumber;
            this.winningColor = winningColor;
            this.won = won;
            this.winAmount = winAmount;
            this.selectedNumber = selectedNumber;
            this.selectedColor = selectedColor;
        }

        public Apuesta getApuesta() { return apuesta; }
        public int getWinningNumber() { return winningNumber; }
        public String getWinningColor() { return winningColor; }
        public boolean isWon() { return won; }
        public BigDecimal getWinAmount() { return winAmount; }
        public Integer getSelectedNumber() { return selectedNumber; }
        public String getSelectedColor() { return selectedColor; }
    }
}
