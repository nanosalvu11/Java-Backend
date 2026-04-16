package usuario;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class UsuarioRepository {
    private final ConcurrentHashMap<Long, Usuario> data = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    public Usuario save(Usuario usuario) {
        if (usuario.getId() == null) {
            usuario.setId(sequence.incrementAndGet());
        }
        data.put(usuario.getId(), usuario);
        return usuario;
    }

    public Optional<Usuario> findById(Long id) {
        return Optional.ofNullable(data.get(id));
    }

    public Optional<Usuario> findByEmail(String email) {
        return data.values().stream()
                .filter(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    public List<Usuario> findAll() {
        return new ArrayList<>(data.values());
    }

    public boolean deleteById(Long id) {
        return data.remove(id) != null;
    }
}
