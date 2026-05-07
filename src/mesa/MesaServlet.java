package mesa;

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
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet(MesaServlet.BASE_PATH)
public class MesaServlet extends HttpServlet {
    public static final String BASE_PATH = "/api/mesas/*";
    private static final String ACTOR_HEADER = "X-Actor-Id";
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private transient MesaService mesaService;
    private transient UsuarioService usuarioService;
    private transient ObjectMapper objectMapper;

    @Override
    public void init() {
        ServletContext servletContext = getServletContext();
        Object appAttr = servletContext == null ? null : servletContext.getAttribute(CasinoApp.CONTEXT_ATTRIBUTE);
        if (appAttr instanceof CasinoApp app) {
            this.mesaService = app.getMesaService();
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
                int page = parseIntParam(req, "page", 0);
                int size = parseIntParam(req, "size", 20);
                List<Mesa> mesas = mesaService.listar(page, size);
                long total = mesaService.contarMesas(null);
                List<Map<String, Object>> data = mesas.stream().map(this::mesaToMap).toList();
                Map<String, Object> payload = new java.util.LinkedHashMap<>();
                payload.put("data", data);
                payload.put("page", page);
                payload.put("size", size);
                payload.put("total", total);
                writeJson(res, HttpServletResponse.SC_OK, payload);
                return;
            }
            if (pathInfo.startsWith("/juego/")) {
                Long juegoId = parseTailId(pathInfo, "/juego/");
                int page = parseIntParam(req, "page", 0);
                int size = parseIntParam(req, "size", 20);
                List<Mesa> mesas = mesaService.listarPorJuego(juegoId, page, size);
                long total = mesaService.contarMesas(juegoId);
                List<Map<String, Object>> data = mesas.stream().map(this::mesaToMap).toList();
                Map<String, Object> payload = new java.util.LinkedHashMap<>();
                payload.put("data", data);
                payload.put("page", page);
                payload.put("size", size);
                payload.put("total", total);
                writeJson(res, HttpServletResponse.SC_OK, payload);
                return;
            }
            Long id = parseId(pathInfo);
            writeJson(res, HttpServletResponse.SC_OK, mesaToMap(mesaService.obtener(id)));
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
            Mesa nueva = new Mesa(
                    null,
                    optionalLong(body, "idJuego"),
                    optionalBigDecimal(body, "apuestaMinima"),
                    optionalBigDecimal(body, "apuestaMaxima"),
                    optionalString(body, "estado")
            );
            Mesa creada = mesaService.crear(actor, nueva);
            writeJson(res, HttpServletResponse.SC_CREATED, mesaToMap(creada));
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
            Mesa cambios = new Mesa(
                    null,
                    optionalLong(body, "idJuego"),
                    optionalBigDecimal(body, "apuestaMinima"),
                    optionalBigDecimal(body, "apuestaMaxima"),
                    optionalString(body, "estado")
            );
            Mesa actualizada = mesaService.actualizar(actor, id, cambios);
            writeJson(res, HttpServletResponse.SC_OK, mesaToMap(actualizada));
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
            mesaService.eliminar(actor, id);
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

    private Long parseTailId(String pathInfo, String prefix) {
        if (!pathInfo.startsWith(prefix)) {
            throw new IllegalArgumentException("Ruta no valida");
        }
        String raw = pathInfo.substring(prefix.length());
        if (raw.isBlank() || raw.contains("/")) {
            throw new IllegalArgumentException("Ruta no valida");
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID invalido");
        }
    }

    private int parseIntParam(HttpServletRequest req, String name, int defaultVal) {
        String raw = req.getParameter(name);
        if (raw == null || raw.isBlank()) return defaultVal;
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return defaultVal;
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

    private Long optionalLong(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private BigDecimal optionalBigDecimal(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        String asText = String.valueOf(value);
        if (asText.isBlank()) {
            return null;
        }
        return new BigDecimal(asText);
    }

    private Map<String, Object> mesaToMap(Mesa mesa) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", mesa.getId());
        map.put("idJuego", mesa.getIdJuego());
        map.put("apuestaMinima", mesa.getApuestaMinima());
        map.put("apuestaMaxima", mesa.getApuestaMaxima());
        map.put("estado", mesa.getEstado());
        return map;
    }

    private int resolveStatusFromMessage(String message) {
        if (message != null && message.toLowerCase().contains("no encontrada")) {
            return HttpServletResponse.SC_NOT_FOUND;
        }
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
