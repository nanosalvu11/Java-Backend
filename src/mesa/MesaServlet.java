package mesa;

import usuario.Usuario;

import java.util.List;

public class MesaServlet {
    public static final String BASE_PATH = "/api/mesas/*";

    private final MesaService mesaService;

    public MesaServlet(MesaService mesaService) {
        this.mesaService = mesaService;
    }

    public Mesa postCrear(Usuario actor, Mesa mesa) {
        return mesaService.crear(actor, mesa);
    }

    public Mesa putActualizar(Usuario actor, Long id, Mesa cambios) {
        return mesaService.actualizar(actor, id, cambios);
    }

    public void deleteEliminar(Usuario actor, Long id) {
        mesaService.eliminar(actor, id);
    }

    public Mesa getPorId(Long id) {
        return mesaService.obtener(id);
    }

    public List<Mesa> getListado() {
        return mesaService.listar();
    }

    public List<Mesa> getPorJuego(Long juegoId) {
        return mesaService.listarPorJuego(juegoId);
    }
}
