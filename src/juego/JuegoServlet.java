package juego;

import app.CasinoApp;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import usuario.Usuario;
import usuario.UsuarioService;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet(JuegoServlet.BASE_PATH)
public class JuegoServlet extends HttpServlet {
    public static final String BASE_PATH = "/api/juegos/*";
    private static final String ACTOR_HEADER = "X-Actor-Id";
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private transient JuegoService juegoService;
    private transient UsuarioService usuarioService;
    private transient ObjectMapper objectMapper;

    @Override
    public void init() {
        ServletContext servletContext = getServletContext();
        Object appAttr = servletContext == null ? null : servletContext.getAttribute(CasinoApp.CONTEXT_ATTRIBUTE);
        if (appAttr instanceof CasinoApp app) {
            this.juegoService = app.getJuegoService();
            this.usuarioService = app.getUsuarioService();
        } else {
            throw new IllegalStateException("CasinoApp no esta inicializado en el ServletContext");
        }
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        setupJsonResponse(res);
        String pathInfo = normalizePathInfo(req.getPathInfo());
        try {
            if ("/".equals(pathInfo)) {
                List<Map<String, Object>> juegos = juegoService.listar().stream().map(this::juegoToMap).toList();
                writeJson(res, HttpServletResponse.SC_OK, juegos);
                return;
            }
            Long id = parseId(pathInfo);
            writeJson(res, HttpServletResponse.SC_OK, juegoToMap(juegoService.obtener(id)));
        } catch (IllegalArgumentException e) {
            writeJson(res, resolveStatusFromMessage(e.getMessage()), error(e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        setupJsonResponse(res);
        try {
            Usuario actor = resolveActor(req);
            Map<String, Object> body = readJsonBody(req);
            Juego nuevo = new Juego(null, optionalString(body, "nombre"), optionalString(body, "reglas"));
            Juego creado = juegoService.crear(actor, nuevo);
            writeJson(res, HttpServletResponse.SC_CREATED, juegoToMap(creado));
        } catch (SecurityException e) {
            writeJson(res, HttpServletResponse.SC_FORBIDDEN, error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            writeJson(res, resolveStatusFromMessage(e.getMessage()), error(e.getMessage()));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res) throws IOException {
        setupJsonResponse(res);
        String pathInfo = normalizePathInfo(req.getPathInfo());
        if ("/".equals(pathInfo)) {
            writeJson(res, HttpServletResponse.SC_BAD_REQUEST, error("ID requerido"));
            return;
        }
        try {
            Usuario actor = resolveActor(req);
            Long id = parseId(pathInfo);
            Map<String, Object> body = readJsonBody(req);
            Juego cambios = new Juego(null, optionalString(body, "nombre"), optionalString(body, "reglas"));
            Juego actualizado = juegoService.actualizar(actor, id, cambios);
            writeJson(res, HttpServletResponse.SC_OK, juegoToMap(actualizado));
        } catch (SecurityException e) {
            writeJson(res, HttpServletResponse.SC_FORBIDDEN, error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            writeJson(res, resolveStatusFromMessage(e.getMessage()), error(e.getMessage()));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws IOException {
        setupJsonResponse(res);
        String pathInfo = normalizePathInfo(req.getPathInfo());
        if ("/".equals(pathInfo)) {
            writeJson(res, HttpServletResponse.SC_BAD_REQUEST, error("ID requerido"));
            return;
        }
        try {
            Usuario actor = resolveActor(req);
            Long id = parseId(pathInfo);
            juegoService.eliminar(actor, id);
            res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } catch (SecurityException e) {
            writeJson(res, HttpServletResponse.SC_FORBIDDEN, error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            writeJson(res, resolveStatusFromMessage(e.getMessage()), error(e.getMessage()));
        }
    }

    private Usuario resolveActor(HttpServletRequest req) {
        String rawActorId = req.getHeader(ACTOR_HEADER);
        if (rawActorId == null || rawActorId.isBlank()) {
            throw new SecurityException("Header X-Actor-Id requerido");
        }
        try {
            Long actorId = Long.parseLong(rawActorId.trim());
            return usuarioService.obtenerPorId(actorId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Header X-Actor-Id invalido");
        }
    }

    private String normalizePathInfo(String pathInfo) {
        if (pathInfo == null || pathInfo.isBlank()) {
            return "/";
        }
        if (pathInfo.length() > 1 && pathInfo.endsWith("/")) {
            return pathInfo.substring(0, pathInfo.length() - 1);
        }
        return pathInfo;
    }

    private Long parseId(String pathInfo) {
        String raw = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        if (raw.contains("/")) {
            throw new IllegalArgumentException("Ruta no valida");
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID invalido");
        }
    }

    private Map<String, Object> readJsonBody(HttpServletRequest req) throws IOException {
        String body = req.getReader().lines().reduce("", String::concat).trim();
        if (body.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(body, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON invalido");
        }
    }

    private String optionalString(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Map<String, Object> juegoToMap(Juego juego) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", juego.getId());
        map.put("nombre", juego.getNombre());
        map.put("reglas", juego.getReglas());
        return map;
    }

    private int resolveStatusFromMessage(String message) {
        if (message != null && message.toLowerCase().contains("no encontrado")) {
            return HttpServletResponse.SC_NOT_FOUND;
        }
        return HttpServletResponse.SC_BAD_REQUEST;
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("error", message);
        return map;
    }

    private void setupJsonResponse(HttpServletResponse res) {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
    }

    private void writeJson(HttpServletResponse res, int status, Object body) throws IOException {
        res.setStatus(status);
        res.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
