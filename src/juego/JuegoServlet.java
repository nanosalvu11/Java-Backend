package juego;

import usuario.Usuario;

import java.util.List;

public class JuegoServlet {
    public static final String BASE_PATH = "/api/juegos/*";

    private final JuegoService juegoService;

    public JuegoServlet(JuegoService juegoService) {
        this.juegoService = juegoService;
    }

    public Juego postCrear(Usuario actor, Juego juego) {
        return juegoService.crear(actor, juego);
    }

    public Juego putActualizar(Usuario actor, Long id, Juego cambios) {
        return juegoService.actualizar(actor, id, cambios);
    }

    public void deleteEliminar(Usuario actor, Long id) {
        juegoService.eliminar(actor, id);
    }

    public Juego getPorId(Long id) {
        return juegoService.obtener(id);
    }

    public List<Juego> getListado() {
        return juegoService.listar();
    }
}
