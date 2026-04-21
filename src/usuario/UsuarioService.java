package usuario;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;

public class UsuarioService {
    private final UsuarioRepository repository;

    public UsuarioService(UsuarioRepository repository) {
        this.repository = repository;
    }

    public List<Usuario> getAll() {
        return repository.findAll();
    }

    public Optional<Usuario> getById(Long id) {
        return repository.findById(id);
    }

    public Usuario crear(Usuario nuevo) {
        validarDatosBase(nuevo);
        repository.findByEmail(nuevo.getEmail()).ifPresent(ignored -> {
            throw new IllegalArgumentException("Ya existe un usuario con ese email");
        });
        if (nuevo.getSaldo() == null) {
            nuevo.setSaldo(BigDecimal.ZERO);
        }
        if (nuevo.getRol() == null || nuevo.getRol().isBlank()) {
            nuevo.setRol(Usuario.ROL_JUGADOR);
        }
        return repository.save(nuevo);
    }

    public Usuario actualizar(Usuario usuario) {
        if (usuario.getId() == null) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }
        obtenerPorId(usuario.getId());
        return repository.update(usuario);
    }

    public void eliminar(Long id) {
        obtenerPorId(id);
        if (!repository.delete(id)) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }
    }

    public Usuario crear(Usuario actor, Usuario nuevo) {
        requireAdmin(actor);
        return crear(nuevo);
    }

    public Usuario actualizar(Usuario actor, Long id, Usuario cambios) {
        requireAdmin(actor);
        Usuario existente = obtenerPorId(id);
        if (cambios.getEmail() != null) {
            repository.findByEmail(cambios.getEmail())
                    .filter(u -> !u.getId().equals(id))
                    .ifPresent(ignored -> {
                        throw new IllegalArgumentException("Ya existe un usuario con ese email");
                    });
            existente.setEmail(cambios.getEmail());
        }
        if (cambios.getNombre() != null) {
            existente.setNombre(cambios.getNombre());
        }
        if (cambios.getApellido() != null) {
            existente.setApellido(cambios.getApellido());
        }
        if (cambios.getPassword() != null) {
            existente.setPassword(cambios.getPassword());
        }
        if (cambios.getRol() != null) {
            existente.setRol(cambios.getRol());
        }
        if (cambios.getSaldo() != null) {
            existente.setSaldo(cambios.getSaldo());
        }
        return repository.update(existente);
    }

    public void eliminar(Usuario actor, Long id) {
        requireAdmin(actor);
        eliminar(id);
    }

    public Usuario obtener(Usuario actor, Long id) {
        if (actor == null) {
            throw new SecurityException("Operacion no autorizada");
        }
        if (actor.esJugador() && !actor.getId().equals(id)) {
            throw new SecurityException("Un jugador solo puede consultar su propio usuario");
        }
        return obtenerPorId(id);
    }

    public List<Usuario> listar(Usuario actor) {
        requireAdmin(actor);
        return getAll();
    }

    public Usuario autenticar(String email, String password) {
        Usuario usuario = repository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Credenciales invalidas"));
        if (!usuario.getPassword().equals(password)) {
            throw new IllegalArgumentException("Credenciales invalidas");
        }
        return usuario;
    }

    public BigDecimal depositar(Usuario actor, Long usuarioId, BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto a depositar debe ser mayor a cero");
        }
        validarAccesoSaldo(actor, usuarioId);
        Usuario usuario = obtenerPorId(usuarioId);
        usuario.setSaldo(usuario.getSaldo().add(monto));
        repository.save(usuario);
        return usuario.getSaldo();
    }

    public BigDecimal retirar(Usuario actor, Long usuarioId, BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto a retirar debe ser mayor a cero");
        }
        validarAccesoSaldo(actor, usuarioId);
        Usuario usuario = obtenerPorId(usuarioId);
        if (usuario.getSaldo().compareTo(monto) < 0) {
            throw new IllegalArgumentException("Saldo insuficiente para retirar");
        }
        usuario.setSaldo(usuario.getSaldo().subtract(monto));
        repository.save(usuario);
        return usuario.getSaldo();
    }

    public Usuario obtenerPorId(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private void validarDatosBase(Usuario usuario) {
        if (usuario.getNombre() == null || usuario.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            throw new IllegalArgumentException("El email es obligatorio");
        }
        if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            throw new IllegalArgumentException("El password es obligatorio");
        }
        if (usuario.getRol() == null || usuario.getRol().isBlank()) {
            throw new IllegalArgumentException("El rol es obligatorio");
        }
    }

    private void validarAccesoSaldo(Usuario actor, Long usuarioId) {
        if (actor == null) {
            throw new SecurityException("Operacion no autorizada");
        }
        if (actor.esAdmin()) {
            return;
        }
        if (actor.esJugador() && actor.getId().equals(usuarioId)) {
            return;
        }
        throw new SecurityException("No autorizado para operar saldo de otro usuario");
    }

    private void requireAdmin(Usuario actor) {
        if (actor == null || !actor.esAdmin()) {
            throw new SecurityException("Operacion solo permitida para ADMIN");
        }
    }
}
