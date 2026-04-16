package apuesta;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class ApuestaRepository {
    private final ConcurrentHashMap<Long, Apuesta> data = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    public Apuesta save(Apuesta apuesta) {
        if (apuesta.getId() == null) {
            apuesta.setId(sequence.incrementAndGet());
        }
        data.put(apuesta.getId(), apuesta);
        return apuesta;
    }

    public List<Apuesta> findAll() {
        return new ArrayList<>(data.values());
    }

    public List<Apuesta> findByUsuarioId(Long usuarioId) {
        return data.values().stream()
                .filter(a -> a.getIdUsuario() != null && a.getIdUsuario().equals(usuarioId))
                .collect(Collectors.toList());
    }

    public List<Apuesta> findByFecha(LocalDateTime desde, LocalDateTime hasta) {
        return data.values().stream()
                .filter(a -> !a.getFecha().isBefore(desde) && !a.getFecha().isAfter(hasta))
                .collect(Collectors.toList());
    }
}
