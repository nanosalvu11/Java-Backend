package apuesta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Apuesta {
    private Long id;
    private Long idUsuario;
    private Long idMesa;
    private BigDecimal montoApostado;
    private BigDecimal resultadoMonto;
    private LocalDateTime fecha;
}
