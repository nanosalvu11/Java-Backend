package app;

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

public final class CasinoApp {
    private final UsuarioRepository usuarioRepository;
    private final JuegoRepository juegoRepository;
    private final MesaRepository mesaRepository;
    private final ApuestaRepository apuestaRepository;
    private final UsuarioService usuarioService;
    private final JuegoService juegoService;
    private final MesaService mesaService;
    private final ApuestaService apuestaService;
    private final Usuario admin;
    private final Usuario jugador;
    private final Juego ruleta;
    private final Juego blackjack;
    private final Mesa mesaRuleta;
    private final Mesa mesaBlackjack;

    private CasinoApp(UsuarioRepository usuarioRepository,
                      JuegoRepository juegoRepository,
                      MesaRepository mesaRepository,
                      ApuestaRepository apuestaRepository,
                      UsuarioService usuarioService,
                      JuegoService juegoService,
                      MesaService mesaService,
                      ApuestaService apuestaService,
                      Usuario admin,
                      Usuario jugador,
                      Juego ruleta,
                      Juego blackjack,
                      Mesa mesaRuleta,
                      Mesa mesaBlackjack) {
        this.usuarioRepository = usuarioRepository;
        this.juegoRepository = juegoRepository;
        this.mesaRepository = mesaRepository;
        this.apuestaRepository = apuestaRepository;
        this.usuarioService = usuarioService;
        this.juegoService = juegoService;
        this.mesaService = mesaService;
        this.apuestaService = apuestaService;
        this.admin = admin;
        this.jugador = jugador;
        this.ruleta = ruleta;
        this.blackjack = blackjack;
        this.mesaRuleta = mesaRuleta;
        this.mesaBlackjack = mesaBlackjack;
    }

    public static CasinoApp createDefault() {
        UsuarioRepository usuarioRepository = new UsuarioRepository();
        JuegoRepository juegoRepository = new JuegoRepository();
        MesaRepository mesaRepository = new MesaRepository();
        ApuestaRepository apuestaRepository = new ApuestaRepository();

        UsuarioService usuarioService = new UsuarioService(usuarioRepository);
        JuegoService juegoService = new JuegoService(juegoRepository);
        MesaService mesaService = new MesaService(mesaRepository, juegoService);
        ApuestaService apuestaService = new ApuestaService(apuestaRepository, usuarioService, mesaService, juegoService);

        Usuario admin = asegurarUsuario(usuarioRepository, new Usuario(null, "Admin", "Casino", "elchiqui@casino.com", "admin123", BigDecimal.ZERO, Usuario.ROL_ADMIN));
        Usuario jugador = usuarioRepository.findByEmail("j1@casino.com")
                .map(existente -> {
                    existente.setNombre("Jugador");
                    existente.setApellido("Uno");
                    existente.setPassword("1234");
                    existente.setSaldo(new BigDecimal("1000.00"));
                    existente.setRol(Usuario.ROL_JUGADOR);
                    return usuarioRepository.save(existente);
                })
                .orElseGet(() -> usuarioService.crear(admin, new Usuario(null, "Jugador", "Uno", "j1@casino.com", "1234", new BigDecimal("1000.00"), Usuario.ROL_JUGADOR)));

        Juego ruleta = asegurarJuego(juegoRepository, juegoService, admin, new Juego(null, "Ruleta", "Apuesta simple par/impar"));
        Juego blackjack = asegurarJuego(juegoRepository, juegoService, admin, new Juego(null, "Blackjack", "Objetivo: acercarse a 21 sin pasarse"));

        Mesa mesaRuleta = asegurarMesa(mesaRepository, mesaService, admin, new Mesa(null, ruleta.getId(), new BigDecimal("50"), new BigDecimal("500"), Mesa.ESTADO_ABIERTA));
        Mesa mesaBlackjack = asegurarMesa(mesaRepository, mesaService, admin, new Mesa(null, blackjack.getId(), new BigDecimal("100"), new BigDecimal("300"), Mesa.ESTADO_ABIERTA));

        return new CasinoApp(
                usuarioRepository,
                juegoRepository,
                mesaRepository,
                apuestaRepository,
                usuarioService,
                juegoService,
                mesaService,
                apuestaService,
                admin,
                jugador,
                ruleta,
                blackjack,
                mesaRuleta,
                mesaBlackjack
        );
    }

    private static Usuario asegurarUsuario(UsuarioRepository repository, Usuario seed) {
        return repository.findByEmail(seed.getEmail())
                .map(existente -> {
                    existente.setNombre(seed.getNombre());
                    existente.setApellido(seed.getApellido());
                    existente.setPassword(seed.getPassword());
                    existente.setSaldo(seed.getSaldo());
                    existente.setRol(seed.getRol());
                    return repository.save(existente);
                })
                .orElseGet(() -> repository.save(seed));
    }

    private static Juego asegurarJuego(JuegoRepository repository, JuegoService service, Usuario actor, Juego seed) {
        return repository.findByNombre(seed.getNombre())
                .map(existente -> {
                    existente.setReglas(seed.getReglas());
                    return repository.save(existente);
                })
                .orElseGet(() -> service.crear(actor, seed));
    }

    private static Mesa asegurarMesa(MesaRepository repository, MesaService service, Usuario actor, Mesa seed) {
        return repository.findByJuegoId(seed.getIdJuego()).stream()
                .findFirst()
                .map(existente -> {
                    existente.setApuestaMinima(seed.getApuestaMinima());
                    existente.setApuestaMaxima(seed.getApuestaMaxima());
                    existente.setEstado(seed.getEstado());
                    return repository.save(existente);
                })
                .orElseGet(() -> service.crear(actor, seed));
    }

    public UsuarioRepository getUsuarioRepository() {
        return usuarioRepository;
    }

    public JuegoRepository getJuegoRepository() {
        return juegoRepository;
    }

    public MesaRepository getMesaRepository() {
        return mesaRepository;
    }

    public ApuestaRepository getApuestaRepository() {
        return apuestaRepository;
    }

    public UsuarioService getUsuarioService() {
        return usuarioService;
    }

    public JuegoService getJuegoService() {
        return juegoService;
    }

    public MesaService getMesaService() {
        return mesaService;
    }

    public ApuestaService getApuestaService() {
        return apuestaService;
    }

    public Usuario getAdmin() {
        return admin;
    }

    public Usuario getJugador() {
        return jugador;
    }

    public Juego getRuleta() {
        return ruleta;
    }

    public Juego getBlackjack() {
        return blackjack;
    }

    public Mesa getMesaRuleta() {
        return mesaRuleta;
    }

    public Mesa getMesaBlackjack() {
        return mesaBlackjack;
    }
}
