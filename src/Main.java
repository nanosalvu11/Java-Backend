import app.CasinoApp;
import apuesta.ApuestaServlet;
import config.DatabaseConfig;
import juego.JuegoServlet;
import mesa.MesaServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import usuario.UsuarioServlet;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        CasinoApp app = CasinoApp.create();

        Server server = new Server(port);
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.setAttribute(CasinoApp.CONTEXT_ATTRIBUTE, app);
        context.addServlet(UsuarioServlet.class, UsuarioServlet.BASE_PATH);
        context.addServlet(JuegoServlet.class, JuegoServlet.BASE_PATH);
        context.addServlet(MesaServlet.class, MesaServlet.BASE_PATH);
        context.addServlet(ApuestaServlet.class, ApuestaServlet.BASE_PATH);
        server.setHandler(context);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.stop();
            } catch (Exception ignored) {
                // El proceso ya puede estar bajando el servidor.
            } finally {
                DatabaseConfig.closeDataSource();
                System.out.println("Pool de conexiones cerrado.");
            }
        }));

        server.start();
        System.out.println("Casino API corriendo en http://localhost:" + port);
        server.join();
    }
}
