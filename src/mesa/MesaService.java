package mesa;

import juego.JuegoService;
import usuario.Usuario;

import java.math.BigDecimal;
import java.util.List;

public class MesaService {
    private final MesaRepository repository;
    private final JuegoService juegoService;

    public MesaService(MesaRepository repository, JuegoService juegoService) {
        this.repository = repository;
        this.juegoService = juegoService;
    }

    public Mesa crear(Usuario actor, Mesa mesa) {
        requireAdmin(actor);
        validar(mesa);
        juegoService.obtenerPorId(mesa.getIdJuego());
        if (mesa.getEstado() == null || mesa.getEstado().isBlank()) {
            mesa.setEstado(Mesa.ESTADO_ABIERTA);
        }
        return repository.save(mesa);
    }

    public Mesa actualizar(Usuario actor, Long id, Mesa cambios) {
        requireAdmin(actor);
        Mesa existente = obtenerPorId(id);
        if (cambios.getIdJuego() != null) {
            juegoService.obtenerPorId(cambios.getIdJuego());
            existente.setIdJuego(cambios.getIdJuego());
        }
        if (cambios.getApuestaMinima() != null) {
            existente.setApuestaMinima(cambios.getApuestaMinima());
        }
        if (cambios.getApuestaMaxima() != null) {
            existente.setApuestaMaxima(cambios.getApuestaMaxima());
        }
        if (cambios.getEstado() != null) {
            existente.setEstado(cambios.getEstado());
        }
        validar(existente);
        return repository.save(existente);
    }

    public void eliminar(Usuario actor, Long id) {
        requireAdmin(actor);
        if (!repository.deleteById(id)) {
            throw new IllegalArgumentException("Mesa no encontrada");
        }
    }

    public Mesa obtener(Long id) {
        return obtenerPorId(id);
    }

    public List<Mesa> listar() {
        return repository.findAll();
    }

    public List<Mesa> listarPorJuego(Long juegoId) {
        return repository.findByJuegoId(juegoId);
    }

    public Mesa obtenerPorId(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Mesa no encontrada"));
    }

    private void validar(Mesa mesa) {
        if (mesa.getIdJuego() == null) {
            throw new IllegalArgumentException("La mesa debe estar asociada a un juego");
        }
        if (mesa.getApuestaMinima() == null || mesa.getApuestaMinima().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La apuesta minima debe ser mayor a cero");
        }
        if (mesa.getApuestaMaxima() == null || mesa.getApuestaMaxima().compareTo(mesa.getApuestaMinima()) < 0) {
            throw new IllegalArgumentException("La apuesta maxima debe ser mayor o igual a la minima");
        }
    }

    private void requireAdmin(Usuario actor) {
        if (actor == null || !actor.esAdmin()) {
            throw new SecurityException("Operacion solo permitida para ADMIN");
        }
    }
}
