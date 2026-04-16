package http;

import app.CasinoApp;
import apuesta.Apuesta;
import apuesta.ApuestaService;
import juego.Juego;
import mesa.Mesa;
import usuario.Usuario;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ApiServer implements AutoCloseable {
    private static final String API_PREFIX = "/api";
    private final CasinoApp app;
    private final HttpServer server;
    private final Json json = new Json();

    public ApiServer(CasinoApp app, int port) throws IOException {
        this.app = Objects.requireNonNull(app, "app");
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext(API_PREFIX, this::handleApi);
        this.server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
    }

    public static ApiServer createDefault(int port) throws IOException {
        return new ApiServer(CasinoApp.createDefault(), port);
    }

    public void start() {
        server.start();
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handleApi(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendEmpty(exchange, 204);
            return;
        }

        try {
            String path = normalizePath(exchange.getRequestURI().getPath());
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod()) && "/api/health".equals(path)) {
                sendJson(exchange, 200, ok("ok", true));
                return;
            }
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod()) && "/api/auth/login".equals(path)) {
                handleLogin(exchange);
                return;
            }

            List<String> segments = segments(path);
            if (segments.isEmpty()) {
                throw new HttpException(404, "Ruta no encontrada");
            }

            switch (segments.get(0)) {
                case "usuarios" -> handleUsuarios(exchange, segments);
                case "juegos" -> handleJuegos(exchange, segments);
                case "mesas" -> handleMesas(exchange, segments);
                case "apuestas" -> handleApuestas(exchange, segments);
                case "reportes" -> handleReportes(exchange, segments);
                default -> throw new HttpException(404, "Ruta no encontrada");
            }
        } catch (HttpException e) {
            sendJson(exchange, e.status, error(e.getMessage()));
        } catch (Exception e) {
            sendJson(exchange, 500, error(e.getMessage() == null ? "Error interno" : e.getMessage()));
        } finally {
            exchange.close();
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        Map<String, Object> body = readJsonBody(exchange);
        String email = requireString(body, "email");
        String password = requireString(body, "password");

        Usuario usuario = app.getUsuarioService().autenticar(email, password);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("token", String.valueOf(usuario.getId()));
        response.put("user", usuarioToMap(usuario));
        sendJson(exchange, 200, response);
    }

    private void handleUsuarios(HttpExchange exchange, List<String> segments) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        if (segments.size() == 1) {
            Usuario actor = resolveActor(exchange);
            switch (method) {
                case "GET" -> sendJson(exchange, 200, app.getUsuarioService().listar(actor).stream().map(this::usuarioToMap).toList());
                case "POST" -> {
                    Map<String, Object> body = readJsonBody(exchange);
                    Usuario nuevo = new Usuario(
                            null,
                            requireString(body, "nombre"),
                            optionalString(body, "apellido"),
                            requireString(body, "email"),
                            requireString(body, "password"),
                            optionalBigDecimal(body, "saldo"),
                            requireString(body, "rol")
                    );
                    Usuario creado = app.getUsuarioService().crear(actor, nuevo);
                    sendJson(exchange, 201, usuarioToMap(creado));
                }
                default -> throw new HttpException(405, "Metodo no permitido");
            }
            return;
        }

        long id = parseLong(segments.get(1), "id de usuario");
        if (segments.size() == 2) {
            Usuario actor = resolveActor(exchange);
            switch (method) {
                case "GET" -> sendJson(exchange, 200, usuarioToMap(app.getUsuarioService().obtener(actor, id)));
                case "PUT" -> {
                    Map<String, Object> body = readJsonBody(exchange);
                    Usuario cambios = new Usuario(
                            null,
                            optionalString(body, "nombre"),
                            optionalString(body, "apellido"),
                            optionalString(body, "email"),
                            optionalString(body, "password"),
                            optionalBigDecimal(body, "saldo"),
                            optionalString(body, "rol")
                    );
                    Usuario actualizado = app.getUsuarioService().actualizar(actor, id, cambios);
                    sendJson(exchange, 200, usuarioToMap(actualizado));
                }
                case "DELETE" -> {
                    app.getUsuarioService().eliminar(actor, id);
                    sendEmpty(exchange, 204);
                }
                default -> throw new HttpException(405, "Metodo no permitido");
            }
            return;
        }

        if (segments.size() == 3) {
            String action = segments.get(2);
            Usuario actor = resolveActor(exchange);
            switch (action) {
                case "depositos" -> {
                    ensureMethod(method, "POST");
                    Map<String, Object> body = readJsonBody(exchange);
                    BigDecimal saldo = app.getUsuarioService().depositar(actor, id, requireBigDecimal(body, "monto"));
                    sendJson(exchange, 200, ok("saldo", saldo));
                }
                case "retiros" -> {
                    ensureMethod(method, "POST");
                    Map<String, Object> body = readJsonBody(exchange);
                    BigDecimal saldo = app.getUsuarioService().retirar(actor, id, requireBigDecimal(body, "monto"));
                    sendJson(exchange, 200, ok("saldo", saldo));
                }
                case "historial" -> {
                    ensureMethod(method, "GET");
                    List<Map<String, Object>> historial = app.getApuestaService().obtenerHistorial(actor, id).stream()
                            .map(this::historialToMap)
                            .toList();
                    sendJson(exchange, 200, historial);
                }
                default -> throw new HttpException(404, "Ruta no encontrada");
            }
            return;
        }

        throw new HttpException(404, "Ruta no encontrada");
    }

    private void handleJuegos(HttpExchange exchange, List<String> segments) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        if (segments.size() == 1) {
            switch (method) {
                case "GET" -> sendJson(exchange, 200, app.getJuegoService().listar().stream().map(this::juegoToMap).toList());
                case "POST" -> {
                    Usuario actor = resolveActor(exchange);
                    Map<String, Object> body = readJsonBody(exchange);
                    Juego creado = app.getJuegoService().crear(actor, new Juego(null, requireString(body, "nombre"), optionalString(body, "reglas")));
                    sendJson(exchange, 201, juegoToMap(creado));
                }
                default -> throw new HttpException(405, "Metodo no permitido");
            }
            return;
        }

        long id = parseLong(segments.get(1), "id de juego");
        Usuario actor = resolveActor(exchange);
        switch (method) {
            case "GET" -> sendJson(exchange, 200, juegoToMap(app.getJuegoService().obtener(id)));
            case "PUT" -> {
                Map<String, Object> body = readJsonBody(exchange);
                Juego actualizado = app.getJuegoService().actualizar(actor, id, new Juego(null, optionalString(body, "nombre"), optionalString(body, "reglas")));
                sendJson(exchange, 200, juegoToMap(actualizado));
            }
            case "DELETE" -> {
                app.getJuegoService().eliminar(actor, id);
                sendEmpty(exchange, 204);
            }
            default -> throw new HttpException(405, "Metodo no permitido");
        }
    }

    private void handleMesas(HttpExchange exchange, List<String> segments) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        if (segments.size() == 1) {
            switch (method) {
                case "GET" -> {
                    Long juegoId = optionalLong(queryParam(exchange.getRequestURI(), "juegoId"));
                    List<Mesa> mesas = juegoId == null ? app.getMesaService().listar() : app.getMesaService().listarPorJuego(juegoId);
                    sendJson(exchange, 200, mesas.stream().map(this::mesaToMap).toList());
                }
                case "POST" -> {
                    Usuario actor = resolveActor(exchange);
                    Map<String, Object> body = readJsonBody(exchange);
                    Mesa creada = app.getMesaService().crear(actor, new Mesa(
                            null,
                            requireLong(body, "idJuego"),
                            requireBigDecimal(body, "apuestaMinima"),
                            requireBigDecimal(body, "apuestaMaxima"),
                            optionalString(body, "estado")
                    ));
                    sendJson(exchange, 201, mesaToMap(creada));
                }
                default -> throw new HttpException(405, "Metodo no permitido");
            }
            return;
        }

        long id = parseLong(segments.get(1), "id de mesa");
        Usuario actor = resolveActor(exchange);
        switch (method) {
            case "GET" -> sendJson(exchange, 200, mesaToMap(app.getMesaService().obtener(id)));
            case "PUT" -> {
                Map<String, Object> body = readJsonBody(exchange);
                Mesa actualizada = app.getMesaService().actualizar(actor, id, new Mesa(
                        null,
                        optionalLong(body.get("idJuego")),
                        optionalBigDecimal(body, "apuestaMinima"),
                        optionalBigDecimal(body, "apuestaMaxima"),
                        optionalString(body, "estado")
                ));
                sendJson(exchange, 200, mesaToMap(actualizada));
            }
            case "DELETE" -> {
                app.getMesaService().eliminar(actor, id);
                sendEmpty(exchange, 204);
            }
            default -> throw new HttpException(405, "Metodo no permitido");
        }
    }

    private void handleApuestas(HttpExchange exchange, List<String> segments) throws IOException {
        if (segments.size() == 1 && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            Usuario actor = resolveActor(exchange);
            Map<String, Object> body = readJsonBody(exchange);
            long usuarioId = optionalLong(body.get("usuarioId")) == null ? actor.getId() : optionalLong(body.get("usuarioId"));
            Apuesta apuesta = app.getApuestaService().realizarApuesta(
                    actor,
                    usuarioId,
                    requireLong(body, "mesaId"),
                    requireBigDecimal(body, "monto")
            );
            sendJson(exchange, 201, apuestaToMap(apuesta));
            return;
        }

        if (segments.size() == 2 && "blackjack".equalsIgnoreCase(segments.get(1)) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            Usuario actor = resolveActor(exchange);
            Map<String, Object> body = readJsonBody(exchange);
            long usuarioId = optionalLong(body.get("usuarioId")) == null ? actor.getId() : optionalLong(body.get("usuarioId"));
            List<String> decisiones = stringList(body.get("decisiones"));
            Apuesta apuesta = app.getApuestaService().jugarBlackjack(
                    actor,
                    usuarioId,
                    requireLong(body, "mesaId"),
                    requireBigDecimal(body, "monto"),
                    decisiones
            );
            sendJson(exchange, 201, apuestaToMap(apuesta));
            return;
        }

        throw new HttpException(404, "Ruta no encontrada");
    }

    private void handleReportes(HttpExchange exchange, List<String> segments) throws IOException {
        if (segments.size() == 2 && "recaudacion".equalsIgnoreCase(segments.get(1)) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            Usuario actor = resolveActor(exchange);
            LocalDateTime desde = optionalDateTime(queryParam(exchange.getRequestURI(), "desde"), LocalDateTime.now().minusDays(1));
            LocalDateTime hasta = optionalDateTime(queryParam(exchange.getRequestURI(), "hasta"), LocalDateTime.now().plusDays(1));
            Long juegoId = optionalLong(queryParam(exchange.getRequestURI(), "juegoId"));
            List<Map<String, Object>> reporte = app.getApuestaService().reporteRecaudacion(actor, desde, hasta, juegoId).stream()
                    .map(this::reporteToMap)
                    .toList();
            sendJson(exchange, 200, reporte);
            return;
        }
        throw new HttpException(404, "Ruta no encontrada");
    }

    private Usuario resolveActor(HttpExchange exchange) {
        String token = header(exchange, "Authorization");
        if (token != null && token.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
            token = token.substring(7).trim();
        }
        if (token == null || token.isBlank()) {
            token = header(exchange, "X-User-Id");
        }
        if (token == null || token.isBlank()) {
            throw new HttpException(401, "Falta autenticacion");
        }
        try {
            return app.getUsuarioService().obtenerPorId(Long.parseLong(token.trim()));
        } catch (RuntimeException e) {
            throw new HttpException(401, "Usuario no autenticado");
        }
    }

    private Map<String, Object> readJsonBody(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        return body.isBlank() ? new LinkedHashMap<>() : json.parseObject(body);
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-User-Id");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    }

    private void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = json.stringify(body).getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private void sendEmpty(HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, -1);
    }

    private Map<String, Object> ok(String key, Object value) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put(key, value);
        return response;
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("error", message);
        return response;
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        if (path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    private List<String> segments(String path) {
        String normalized = normalizePath(path);
        if (!normalized.startsWith(API_PREFIX)) {
            return List.of();
        }
        String remainder = normalized.substring(API_PREFIX.length());
        if (remainder.startsWith("/")) {
            remainder = remainder.substring(1);
        }
        if (remainder.isBlank()) {
            return List.of();
        }
        return Arrays.stream(remainder.split("/")).filter(part -> !part.isBlank()).collect(Collectors.toList());
    }

    private String queryParam(URI uri, String name) {
        String query = uri.getRawQuery();
        if (query == null || query.isBlank()) {
            return null;
        }
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length > 0 && name.equals(parts[0])) {
                return parts.length == 2 ? decode(parts[1]) : "";
            }
        }
        return null;
    }

    private String decode(String value) {
        return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String header(HttpExchange exchange, String name) {
        return exchange.getRequestHeaders().getFirst(name);
    }

    private void ensureMethod(String actual, String expected) {
        if (!expected.equalsIgnoreCase(actual)) {
            throw new HttpException(405, "Metodo no permitido");
        }
    }

    private long parseLong(String value, String fieldName) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new HttpException(400, "Valor invalido para " + fieldName);
        }
    }

    private Long optionalLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value instanceof String string && !string.isBlank()) {
                return Long.parseLong(string);
            }
            return null;
        } catch (NumberFormatException e) {
            throw new HttpException(400, "Valor numerico invalido");
        }
    }

    private String requireString(Map<String, Object> body, String key) {
        String value = optionalString(body, key);
        if (value == null || value.isBlank()) {
            throw new HttpException(400, "Falta el campo: " + key);
        }
        return value;
    }

    private long requireLong(Map<String, Object> body, String key) {
        Long value = optionalLong(body.get(key));
        if (value == null) {
            throw new HttpException(400, "Falta el campo: " + key);
        }
        return value;
    }

    private String optionalString(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private BigDecimal requireBigDecimal(Map<String, Object> body, String key) {
        BigDecimal value = optionalBigDecimal(body, key);
        if (value == null) {
            throw new HttpException(400, "Falta el campo: " + key);
        }
        return value;
    }

    private BigDecimal optionalBigDecimal(Map<String, Object> body, String key) {
        return optionalBigDecimal(body.get(key));
    }

    private BigDecimal optionalBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof BigDecimal bigDecimal) {
                return bigDecimal;
            }
            if (value instanceof Number number) {
                return new BigDecimal(number.toString());
            }
            if (value instanceof String string && !string.isBlank()) {
                return new BigDecimal(string);
            }
            return null;
        } catch (NumberFormatException e) {
            throw new HttpException(400, "Valor numerico invalido");
        }
    }

    private LocalDateTime optionalDateTime(String value, LocalDateTime defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new HttpException(400, "Fecha invalida: " + value);
        }
    }

    private List<String> stringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                result.add(String.valueOf(item));
            }
            return result;
        }
        if (value instanceof String string && !string.isBlank()) {
            return List.of(string);
        }
        return List.of();
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

    private Map<String, Object> juegoToMap(Juego juego) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", juego.getId());
        map.put("nombre", juego.getNombre());
        map.put("reglas", juego.getReglas());
        return map;
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

    private Map<String, Object> historialToMap(ApuestaService.HistorialPartida partida) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("idApuesta", partida.getIdApuesta());
        map.put("fecha", partida.getFecha() == null ? null : partida.getFecha().toString());
        map.put("juego", partida.getJuego());
        map.put("montoApostado", partida.getMontoApostado());
        map.put("resultadoMonto", partida.getResultadoMonto());
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

    private static final class HttpException extends RuntimeException {
        private final int status;

        private HttpException(int status, String message) {
            super(message);
            this.status = status;
        }
    }
}



