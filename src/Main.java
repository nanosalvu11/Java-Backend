import app.CasinoApp;
import apuesta.Apuesta;
import apuesta.ApuestaService;
import http.ApiServer;
import juego.Juego;
import mesa.Mesa;
import usuario.Usuario;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && "server".equalsIgnoreCase(args[0])) {
            runServer();
            return;
        }
        runDemo();
    }

    private static void runDemo() {
        CasinoApp app = CasinoApp.createDefault();
        Usuario admin = app.getAdmin();
        Usuario jugador = app.getJugador();
        Juego ruleta = app.getRuleta();
        Juego blackjack = app.getBlackjack();
        Mesa mesaRuleta = app.getMesaRuleta();
        Mesa mesaBlackjack = app.getMesaBlackjack();

        Apuesta apuesta1 = app.getApuestaService().realizarApuesta(jugador, jugador.getId(), mesaRuleta.getId(), new BigDecimal("100"));
        Apuesta apuesta2 = app.getApuestaService().jugarBlackjack(jugador, jugador.getId(), mesaBlackjack.getId(), new BigDecimal("120"), List.of("PEDIR", "PLANTARSE"));

        System.out.println("Casino backend listo");
        System.out.println("Usuario admin: " + admin.getEmail());
        System.out.println("Usuario jugador: " + jugador.getEmail());
        System.out.println("Juegos: " + ruleta.getNombre() + ", " + blackjack.getNombre());
        System.out.println("Apuestas registradas: " + apuesta1.getId() + ", " + apuesta2.getId());

        List<ApuestaService.HistorialPartida> historial = app.getApuestaService().obtenerHistorial(jugador, jugador.getId());
        System.out.println("Historial del jugador: " + historial.size() + " partidas");

        List<ApuestaService.ReporteRecaudacion> reporte = app.getApuestaService().reporteRecaudacion(
                admin,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                null
        );

        for (ApuestaService.ReporteRecaudacion item : reporte) {
            System.out.println("Juego=" + item.getJuego() + ", apuestas=" + item.getCantidadApuestas() + ", recaudacionCasino=" + item.getResultadoCasino());
        }

        System.out.println("Saldo final jugador: " + jugador.getSaldo());
    }

    private static void runServer() throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        ApiServer server = new ApiServer(CasinoApp.createDefault(), port);
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));

        System.out.println("API REST lista en http://localhost:" + server.getPort() + "/api");
        System.out.println("Login admin: admin@casino.com / admin123");
        System.out.println("Login jugador: j1@casino.com / 1234");

        new CountDownLatch(1).await();
    }
}
