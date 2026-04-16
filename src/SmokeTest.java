import apuesta.ApuestaRepository;
import apuesta.ApuestaService;
import juego.Juego;
import juego.JuegoRepository;
import juego.JuegoService;
import mesa.Mesa;
import mesa.MesaRepository;
import mesa.MesaService;
import usuario.Usuario;
import usuario.UsuarioRepository;
import usuario.UsuarioService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class SmokeTest {
    public static void main(String[] args) {
        UsuarioRepository usuarioRepository = new UsuarioRepository();
        JuegoRepository juegoRepository = new JuegoRepository();
        MesaRepository mesaRepository = new MesaRepository();
        ApuestaRepository apuestaRepository = new ApuestaRepository();

        UsuarioService usuarioService = new UsuarioService(usuarioRepository);
        JuegoService juegoService = new JuegoService(juegoRepository);
        MesaService mesaService = new MesaService(mesaRepository, juegoService);
        ApuestaService apuestaService = new ApuestaService(apuestaRepository, usuarioService, mesaService, juegoService);

        Usuario admin = usuarioRepository.save(new Usuario(null, "Admin", "Root", "admin@casino.com", "admin", BigDecimal.ZERO, Usuario.ROL_ADMIN));
        Usuario jugador = usuarioService.crear(admin, new Usuario(null, "Jugador", "Uno", "j1@casino.com", "1234", new BigDecimal("500"), Usuario.ROL_JUGADOR));

        Juego ruleta = juegoService.crear(admin, new Juego(null, "Ruleta", "Reglas"));
        Juego blackjack = juegoService.crear(admin, new Juego(null, "Blackjack", "Reglas"));
        Mesa mesaR = mesaService.crear(admin, new Mesa(null, ruleta.getId(), new BigDecimal("10"), new BigDecimal("100"), Mesa.ESTADO_ABIERTA));
        Mesa mesaB = mesaService.crear(admin, new Mesa(null, blackjack.getId(), new BigDecimal("20"), new BigDecimal("200"), Mesa.ESTADO_ABIERTA));

        apuestaService.realizarApuesta(jugador, jugador.getId(), mesaR.getId(), new BigDecimal("50"));
        apuestaService.jugarBlackjack(jugador, jugador.getId(), mesaB.getId(), new BigDecimal("40"), List.of("PEDIR", "PLANTARSE"));

        if (apuestaService.obtenerHistorial(jugador, jugador.getId()).size() != 2) {
            throw new IllegalStateException("El historial deberia tener 2 apuestas");
        }

        List<ApuestaService.ReporteRecaudacion> reporte = apuestaService.reporteRecaudacion(
                admin,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                null
        );

        if (reporte.size() < 2) {
            throw new IllegalStateException("El reporte deberia incluir los juegos cargados");
        }

        System.out.println("SmokeTest OK");
    }
}

