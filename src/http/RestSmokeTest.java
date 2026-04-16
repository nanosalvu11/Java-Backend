package http;

import app.CasinoApp;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RestSmokeTest {
    private RestSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        CasinoApp app = CasinoApp.createDefault();
        try (ApiServer server = new ApiServer(app, 0)) {
            server.start();
            int port = server.getPort();
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            Json json = new Json();

            assertStatus(client, json, port, "GET", "/api/health", null, 200);

            Map<String, Object> adminLogin = postJson(client, json, port, "/api/auth/login", Map.of(
                    "email", "admin@casino.com",
                    "password", "admin123"
            ));
            String adminToken = String.valueOf(adminLogin.get("token"));

            Map<String, Object> nuevoUsuario = postJson(client, json, port, "/api/usuarios", bearer(adminToken), Map.of(
                    "nombre", "Rest",
                    "apellido", "Player",
                    "email", "rest@casino.com",
                    "password", "pass123",
                    "saldo", new BigDecimal("500.00"),
                    "rol", "JUGADOR"
            ));
            Long nuevoUsuarioId = toLong(nuevoUsuario.get("id"));

            Map<String, Object> playerLogin = postJson(client, json, port, "/api/auth/login", Map.of(
                    "email", "rest@casino.com",
                    "password", "pass123"
            ));
            String playerToken = String.valueOf(playerLogin.get("token"));

            Map<String, Object> perfil = getJson(client, json, port, "/api/usuarios/" + nuevoUsuarioId, bearer(playerToken));
            if (!"rest@casino.com".equals(perfil.get("email"))) {
                throw new IllegalStateException("El perfil REST no coincide");
            }

            List<Object> mesas = getArray(client, json, port, "/api/mesas", bearer(adminToken));
            if (mesas.isEmpty()) {
                throw new IllegalStateException("No se encontraron mesas");
            }
            Map<String, Object> primeraMesa = castMap(mesas.get(0));
            Long mesaId = toLong(primeraMesa.get("id"));

            Map<String, Object> apuesta = postJson(client, json, port, "/api/apuestas", bearer(playerToken), Map.of(
                    "usuarioId", nuevoUsuarioId,
                    "mesaId", mesaId,
                    "monto", new BigDecimal("50")
            ));
            if (apuesta.get("id") == null) {
                throw new IllegalStateException("La apuesta no devolvio ID");
            }

            List<Object> historial = getArray(client, json, port, "/api/usuarios/" + nuevoUsuarioId + "/historial", bearer(playerToken));
            if (historial.isEmpty()) {
                throw new IllegalStateException("El historial REST no contiene apuestas");
            }

            System.out.println("RestSmokeTest OK");
        }
    }

    private static Map<String, Object> postJson(HttpClient client, Json json, int port, String path, Map<String, String> headers, Object body) throws Exception {
        HttpRequest.Builder builder = baseRequest(port, path, "POST", headers);
        builder.POST(HttpRequest.BodyPublishers.ofString(json.stringify(body), StandardCharsets.UTF_8));
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("POST " + path + " fallo: " + response.statusCode() + " -> " + response.body());
        }
        return json.parseObject(response.body());
    }

    private static Map<String, Object> postJson(HttpClient client, Json json, int port, String path, Object body) throws Exception {
        return postJson(client, json, port, path, Map.of(), body);
    }

    private static Map<String, Object> getJson(HttpClient client, Json json, int port, String path, Map<String, String> headers) throws Exception {
        HttpResponse<String> response = client.send(baseRequest(port, path, "GET", headers).GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("GET " + path + " fallo: " + response.statusCode() + " -> " + response.body());
        }
        return json.parseObject(response.body());
    }

    private static List<Object> getArray(HttpClient client, Json json, int port, String path, Map<String, String> headers) throws Exception {
        HttpResponse<String> response = client.send(baseRequest(port, path, "GET", headers).GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("GET " + path + " fallo: " + response.statusCode() + " -> " + response.body());
        }
        return json.parseArray(response.body());
    }

    private static void assertStatus(HttpClient client, Json json, int port, String method, String path, Map<String, String> headers, int expectedStatus) throws Exception {
        HttpRequest.Builder builder = baseRequest(port, path, method, headers);
        if ("GET".equals(method)) {
            builder.GET();
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != expectedStatus) {
            throw new IllegalStateException(method + " " + path + " esperaba " + expectedStatus + " y devolvio " + response.statusCode() + " -> " + response.body());
        }
    }

    private static HttpRequest.Builder baseRequest(int port, String path, String method, Map<String, String> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json");
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }
        return builder;
    }

    private static Map<String, String> bearer(String token) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + token);
        return headers;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private static Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}

