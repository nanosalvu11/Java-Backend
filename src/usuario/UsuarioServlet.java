package usuario;

import app.CasinoApp;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@WebServlet(UsuarioServlet.BASE_PATH)
public class UsuarioServlet extends HttpServlet {
    public static final String BASE_PATH = "/api/usuarios/*";
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private transient UsuarioService usuarioService;
    private transient ObjectMapper objectMapper;

    @Override
    public void init() {
        ServletContext servletContext = getServletContext();
        Object appAttr = servletContext == null ? null : servletContext.getAttribute(CasinoApp.CONTEXT_ATTRIBUTE);
        if (appAttr instanceof CasinoApp app) {
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
                List<Map<String, Object>> usuarios = usuarioService.getAll().stream().map(this::usuarioToMap).toList();
                writeJson(res, HttpServletResponse.SC_OK, usuarios);
                return;
            }

            Long id = parseId(pathInfo);
            Optional<Usuario> usuario = usuarioService.getById(id);
            if (usuario.isEmpty()) {
                writeJson(res, HttpServletResponse.SC_NOT_FOUND, error("Usuario no encontrado"));
                return;
            }
            writeJson(res, HttpServletResponse.SC_OK, usuarioToMap(usuario.get()));
        } catch (IllegalArgumentException e) {
            writeJson(res, HttpServletResponse.SC_BAD_REQUEST, error(e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        setupJsonResponse(res);
        String pathInfo = normalizePathInfo(req.getPathInfo());
        if ("/login".equals(pathInfo)) {
            try {
                Map<String, Object> body = readJsonBody(req);
                String email = optionalString(body, "email");
                String password = optionalString(body, "password");
                Usuario usuario = usuarioService.autenticar(email, password);
                writeJson(res, HttpServletResponse.SC_OK, usuarioToMap(usuario));
            } catch (IllegalArgumentException e) {
                writeJson(res, HttpServletResponse.SC_UNAUTHORIZED, error(e.getMessage()));
            }
            return;
        }
        try {
            Map<String, Object> body = readJsonBody(req);
            Usuario nuevo = new Usuario(
                    null,
                    optionalString(body, "nombre"),
                    optionalString(body, "apellido"),
                    optionalString(body, "email"),
                    optionalString(body, "password"),
                    optionalBigDecimal(body, "saldo"),
                    optionalString(body, "rol")
            );
            Usuario creado = usuarioService.crear(nuevo);
            writeJson(res, HttpServletResponse.SC_CREATED, usuarioToMap(creado));
        } catch (IllegalArgumentException e) {
            writeJson(res, HttpServletResponse.SC_BAD_REQUEST, error(e.getMessage()));
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
            Long id = parseId(pathInfo);
            Map<String, Object> body = readJsonBody(req);
            Usuario existente = usuarioService.obtenerPorId(id);

            if (body.containsKey("nombre")) {
                existente.setNombre(optionalString(body, "nombre"));
            }
            if (body.containsKey("apellido")) {
                existente.setApellido(optionalString(body, "apellido"));
            }
            if (body.containsKey("email")) {
                existente.setEmail(optionalString(body, "email"));
            }
            if (body.containsKey("password")) {
                existente.setPassword(optionalString(body, "password"));
            }
            if (body.containsKey("rol")) {
                existente.setRol(optionalString(body, "rol"));
            }
            if (body.containsKey("saldo")) {
                existente.setSaldo(optionalBigDecimal(body, "saldo"));
            }

            Usuario actualizado = usuarioService.actualizar(existente);
            writeJson(res, HttpServletResponse.SC_OK, usuarioToMap(actualizado));
        } catch (IllegalArgumentException e) {
            int status = "Usuario no encontrado".equals(e.getMessage()) ? HttpServletResponse.SC_NOT_FOUND : HttpServletResponse.SC_BAD_REQUEST;
            writeJson(res, status, error(e.getMessage()));
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
            Long id = parseId(pathInfo);
            usuarioService.eliminar(id);
            res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } catch (IllegalArgumentException e) {
            int status = "Usuario no encontrado".equals(e.getMessage()) ? HttpServletResponse.SC_NOT_FOUND : HttpServletResponse.SC_BAD_REQUEST;
            writeJson(res, status, error(e.getMessage()));
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

    private void setupJsonResponse(HttpServletResponse res) {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
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

    private void writeJson(HttpServletResponse res, int status, Object body) throws IOException {
        res.setStatus(status);
        res.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("error", message);
        return map;
    }

    private String optionalString(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value == null ? null : String.valueOf(value);
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

    private Map<String, Object> usuarioToMap(Usuario usuario) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", usuario.getId());
        map.put("nombre", usuario.getNombre());
        map.put("apellido", usuario.getApellido());
        map.put("email", usuario.getEmail());
        map.put("saldo", usuario.getSaldo());
        map.put("rol", usuario.getRol());
        return map;
    }
}
