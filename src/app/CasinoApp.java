package app;

import apuesta.ApuestaRepository;
import apuesta.ApuestaService;
import juego.JuegoRepository;
import juego.JuegoService;
import mesa.MesaRepository;
import mesa.MesaService;
import usuario.UsuarioRepository;
import usuario.UsuarioService;

public final class CasinoApp {
    public static final String CONTEXT_ATTRIBUTE = "casinoApp";

    private final UsuarioRepository usuarioRepository;
    private final JuegoRepository juegoRepository;
    private final MesaRepository mesaRepository;
    private final ApuestaRepository apuestaRepository;
    private final UsuarioService usuarioService;
    private final JuegoService juegoService;
    private final MesaService mesaService;
    private final ApuestaService apuestaService;

    private CasinoApp(UsuarioRepository usuarioRepository,
                      JuegoRepository juegoRepository,
                      MesaRepository mesaRepository,
                      ApuestaRepository apuestaRepository,
                      UsuarioService usuarioService,
                      JuegoService juegoService,
                      MesaService mesaService,
                      ApuestaService apuestaService) {
        this.usuarioRepository = usuarioRepository;
        this.juegoRepository = juegoRepository;
        this.mesaRepository = mesaRepository;
        this.apuestaRepository = apuestaRepository;
        this.usuarioService = usuarioService;
        this.juegoService = juegoService;
        this.mesaService = mesaService;
        this.apuestaService = apuestaService;
    }

    public static CasinoApp createDefault() {
        return create();
    }

    public static CasinoApp create() {
        UsuarioRepository usuarioRepository = new UsuarioRepository();
        JuegoRepository juegoRepository = new JuegoRepository();
        MesaRepository mesaRepository = new MesaRepository();
        ApuestaRepository apuestaRepository = new ApuestaRepository();

        UsuarioService usuarioService = new UsuarioService(usuarioRepository);
        JuegoService juegoService = new JuegoService(juegoRepository);
        MesaService mesaService = new MesaService(mesaRepository, juegoService);
        ApuestaService apuestaService = new ApuestaService(apuestaRepository, usuarioService, mesaService, juegoService);

        return new CasinoApp(
                usuarioRepository,
                juegoRepository,
                mesaRepository,
                apuestaRepository,
                usuarioService,
                juegoService,
                mesaService,
                apuestaService
        );
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
}
