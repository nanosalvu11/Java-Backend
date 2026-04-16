package usuario;

import java.math.BigDecimal;
import java.util.List;

public class UsuarioServlet {
    public static final String BASE_PATH = "/api/usuarios/*";

    private final UsuarioService usuarioService;

    public UsuarioServlet(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    public Usuario postCrear(Usuario actor, Usuario nuevo) {
        return usuarioService.crear(actor, nuevo);
    }

    public Usuario putActualizar(Usuario actor, Long id, Usuario cambios) {
        return usuarioService.actualizar(actor, id, cambios);
    }

    public void deleteEliminar(Usuario actor, Long id) {
        usuarioService.eliminar(actor, id);
    }

    public Usuario getPorId(Usuario actor, Long id) {
        return usuarioService.obtener(actor, id);
    }

    public List<Usuario> getListado(Usuario actor) {
        return usuarioService.listar(actor);
    }

    public BigDecimal postDeposito(Usuario actor, Long usuarioId, BigDecimal monto) {
        return usuarioService.depositar(actor, usuarioId, monto);
    }

    public BigDecimal postRetiro(Usuario actor, Long usuarioId, BigDecimal monto) {
        return usuarioService.retirar(actor, usuarioId, monto);
    }
}
