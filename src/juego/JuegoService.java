package juego;

import usuario.Usuario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;

public class JuegoService {
    private final JuegoRepository repository;
    private final Random random = new Random();

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

    public List<Juego> listar(int page, int size) {
        int offset = Math.max(0, page) * Math.max(1, size);
        return repository.findPage(offset, size);
    }

    public long contar() {
        return repository.countAll();
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

    // --- Lógica de juegos centralizada ---
    public BigDecimal calcularResultadoAleatorio(Juego juego, BigDecimal monto) {
        boolean gana = random.nextBoolean();
        if (!gana) {
            return monto.negate();
        }
        if ("blackjack".equalsIgnoreCase(juego.getNombre())) {
            return monto.multiply(new BigDecimal("1.5")).setScale(2, RoundingMode.HALF_UP);
        }
        return monto.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal jugarBlackjackResultado(BigDecimal monto, java.util.List<String> decisiones) {
        int totalJugador = 12 + random.nextInt(5);
        int totalCrupier = 15 + random.nextInt(5);

        for (String decision : decisiones) {
            if ("PEDIR".equalsIgnoreCase(decision)) {
                totalJugador += 1 + random.nextInt(10);
                if (totalJugador > 21) {
                    break;
                }
            }
            if ("PLANTARSE".equalsIgnoreCase(decision)) {
                break;
            }
        }

        while (totalJugador <= 21 && totalCrupier < 17) {
            totalCrupier += 1 + random.nextInt(10);
        }

        if (totalJugador > 21) {
            return monto.negate();
        } else if (totalCrupier > 21 || totalJugador > totalCrupier) {
            return monto.setScale(2, RoundingMode.HALF_UP);
        } else if (totalJugador == totalCrupier) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            return monto.negate();
        }
    }
}
