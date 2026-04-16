package juego;

import usuario.Usuario;

import java.util.List;

public class JuegoService {
    private final JuegoRepository repository;

    public JuegoService(JuegoRepository repository) {
        this.repository = repository;
    }

    public Juego crear(Usuario actor, Juego juego) {
        requireAdmin(actor);
        validar(juego);
        return repository.save(juego);
    }

    public Juego actualizar(Usuario actor, Long id, Juego cambios) {
        requireAdmin(actor);
        Juego existente = obtenerPorId(id);
        if (cambios.getNombre() != null) {
            existente.setNombre(cambios.getNombre());
        }
        if (cambios.getReglas() != null) {
            existente.setReglas(cambios.getReglas());
        }
        return repository.save(existente);
    }

    public void eliminar(Usuario actor, Long id) {
        requireAdmin(actor);
        if (!repository.deleteById(id)) {
            throw new IllegalArgumentException("Juego no encontrado");
        }
    }

    public Juego obtener(Long id) {
        return obtenerPorId(id);
    }

    public List<Juego> listar() {
        return repository.findAll();
    }

    public Juego obtenerPorId(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Juego no encontrado"));
    }

    private void validar(Juego juego) {
        if (juego.getNombre() == null || juego.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del juego es obligatorio");
        }
    }

    private void requireAdmin(Usuario actor) {
        if (actor == null || !actor.esAdmin()) {
            throw new SecurityException("Operacion solo permitida para ADMIN");
        }
    }
}
