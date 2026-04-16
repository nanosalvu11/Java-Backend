package juego;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class JuegoRepository {
    private final ConcurrentHashMap<Long, Juego> data = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    public Juego save(Juego juego) {
        if (juego.getId() == null) {
            juego.setId(sequence.incrementAndGet());
        }
        data.put(juego.getId(), juego);
        return juego;
    }

    public Optional<Juego> findById(Long id) {
        return Optional.ofNullable(data.get(id));
    }

    public List<Juego> findAll() {
        return new ArrayList<>(data.values());
    }

    public boolean deleteById(Long id) {
        return data.remove(id) != null;
    }
}
