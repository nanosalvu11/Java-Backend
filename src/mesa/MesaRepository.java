package mesa;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class MesaRepository {
    private final ConcurrentHashMap<Long, Mesa> data = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    public Mesa save(Mesa mesa) {
        if (mesa.getId() == null) {
            mesa.setId(sequence.incrementAndGet());
        }
        data.put(mesa.getId(), mesa);
        return mesa;
    }

    public Optional<Mesa> findById(Long id) {
        return Optional.ofNullable(data.get(id));
    }

    public List<Mesa> findAll() {
        return new ArrayList<>(data.values());
    }

    public List<Mesa> findByJuegoId(Long juegoId) {
        return data.values().stream()
                .filter(m -> m.getIdJuego() != null && m.getIdJuego().equals(juegoId))
                .collect(Collectors.toList());
    }

    public boolean deleteById(Long id) {
        return data.remove(id) != null;
    }
}
