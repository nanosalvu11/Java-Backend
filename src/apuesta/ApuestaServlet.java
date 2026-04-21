package apuesta;

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
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet(ApuestaServlet.BASE_PATH)
public class ApuestaServlet extends HttpServlet {
    public static final String BASE_PATH = "/api/apuestas/*";
    private static final String ACTOR_HEADER = "X-Actor-Id";
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private transient ApuestaService apuestaService;
    private transient UsuarioService usuarioService;
    private transient ObjectMapper objectMapper;

    @Override
    public void init() {
        ServletContext servletContext = getServletContext();
        Object appAttr = servletContext == null ? null : servletContext.getAttribute(CasinoApp.CONTEXT_ATTRIBUTE);
        if (appAttr instanceof CasinoApp app) {
            this.apuestaService = app.getApuestaService();
            this.usuarioService = app.getUsuarioService();
        } else {
            throw new IllegalStateException("CasinoApp no esta inicializado en el ServletContext");
        }
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        setupJsonResponse(res);
        String pathInfo = normalizePathInfo(req.getPathInfo());
        try {
            Usuario actor = resolveActor(req);
            Map<String, Object> body = readJsonBody(req);
            Long usuarioId = optionalLong(body, "usuarioId");
            Long mesaId = optionalLong(body, "mesaId");
            BigDecimal monto = optionalBigDecimal(body, "monto");

            Apuesta resultado;
            if ("/blackjack".equals(pathInfo)) {
                List<String> decisiones = optionalStringList(body, "decisiones");
                resultado = apuestaService.jugarBlackjack(actor, usuarioId, mesaId, monto, decisiones);
            } else if ("/".equals(pathInfo)) {
                resultado = apuestaService.realizarApuesta(actor, usuarioId, mesaId, monto);
            } else {
                writeJson(res, HttpServletResponse.SC_NOT_FOUND, error("Ruta no encontrada"));
                return;
            }

            writeJson(res, HttpServletResponse.SC_CREATED, apuestaToMap(resultado));
        } catch (SecurityException e) {
            writeJson(res, HttpServletResponse.SC_FORBIDDEN, error(e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            writeJson(res, resolveStatusFromMessage(e.getMessage()), error(e.getMessage()));
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        setupJsonResponse(res);
        String pathInfo = normalizePathInfo(req.getPathInfo());
        try {
            Usuario actor = resolveActor(req);
            if (pathInfo.startsWith("/historial/")) {
                Long usuarioId = parseTailId(pathInfo, "/historial/");
                List<Map<String, Object>> historial = apuestaService.obtenerHistorial(actor, usuarioId).stream()
                        .map(this::historialToMap)
                        .toList();
                writeJson(res, HttpServletResponse.SC_OK, historial);
                return;
            }
            if ("/recaudacion".equals(pathInfo)) {
                LocalDateTime desde = parseDateTimeParam(req.getParameter("desde"), "desde");
                LocalDateTime hasta = parseDateTimeParam(req.getParameter("hasta"), "hasta");
                Long juegoId = optionalLongParam(req.getParameter("juegoId"));
                List<Map<String, Object>> reporte = apuestaService.reporteRecaudacion(actor, desde, hasta, juegoId).stream()
                        .map(this::reporteToMap)
                        .toList();
                writeJson(res, HttpServletResponse.SC_OK, reporte);
                return;
            }
            writeJson(res, HttpServletResponse.SC_NOT_FOUND, error("Ruta no encontrada"));
        } catch (SecurityException e) {
            writeJson(res, HttpServletResponse.SC_FORBIDDEN, error(e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
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

    private LocalDateTime parseDateTimeParam(String value, String paramName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Parametro " + paramName + " requerido");
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Parametro " + paramName + " invalido. Formato esperado ISO-8601");
        }
    }

    private Long optionalLongParam(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parametro juegoId invalido");
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

    private List<String> optionalStringList(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> rawList)) {
            throw new IllegalArgumentException("El campo " + key + " debe ser un arreglo");
        }
        List<String> result = new ArrayList<>();
        for (Object item : rawList) {
            result.add(String.valueOf(item));
        }
        return result;
    }

    private Map<String, Object> apuestaToMap(Apuesta apuesta) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", apuesta.getId());
        map.put("idUsuario", apuesta.getIdUsuario());
        map.put("idMesa", apuesta.getIdMesa());
        map.put("montoApostado", apuesta.getMontoApostado());
        map.put("resultadoMonto", apuesta.getResultadoMonto());
        map.put("fecha", apuesta.getFecha() == null ? null : apuesta.getFecha().toString());
        return map;
    }

    private Map<String, Object> historialToMap(ApuestaService.HistorialPartida historial) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("idApuesta", historial.getIdApuesta());
        map.put("fecha", historial.getFecha() == null ? null : historial.getFecha().toString());
        map.put("juego", historial.getJuego());
        map.put("montoApostado", historial.getMontoApostado());
        map.put("resultadoMonto", historial.getResultadoMonto());
        return map;
    }

    private Map<String, Object> reporteToMap(ApuestaService.ReporteRecaudacion reporte) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("juego", reporte.getJuego());
        map.put("cantidadApuestas", reporte.getCantidadApuestas());
        map.put("totalApostado", reporte.getTotalApostado());
        map.put("resultadoCasino", reporte.getResultadoCasino());
        return map;
    }

    private int resolveStatusFromMessage(String message) {
        if (message != null && message.toLowerCase().contains("no encontrado")) {
            return HttpServletResponse.SC_NOT_FOUND;
        }
        if (message != null && message.toLowerCase().contains("no encontrada")) {
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
