# Java Backend - Casino Virtual

Backend en Java para el TP de Electiva Java, implementado en capas (`Repository`, `Service`, `Servlet` facade), con pool de conexiones JDBC basado en HikariCP y una API REST liviana sobre `HttpServer`.

## Requisitos del README (1) cubiertos

- ABMC simple de `Usuario` (alta, baja, modificación, consulta) con autorización por rol.
- ABMC simple de `Juego`.
- ABMC dependiente de `Mesa` validando asociación a juego y límites de apuesta.
- CU no-ABMC de apuesta: valida saldo, límites y estado de mesa.
- Listado simple: historial de partidas por jugador cruzando `Apuesta`, `Mesa` y `Juego`.
- CU complejo Blackjack: decisiones `PEDIR` / `PLANTARSE` y resolución de mano.
- Listado complejo: reporte de recaudación con filtros por fecha y juego.
- Niveles de acceso implementados:
  - `JUGADOR`: apuesta, gestiona saldo propio y consulta su historial.
  - `ADMIN`: ABMC de usuarios/juegos/mesas y reporte de recaudación.

## Estructura

- `src/usuario/*`: entidad, repositorio, servicio y fachada de usuario.
- `src/juego/*`: entidad, repositorio, servicio y fachada de juego.
- `src/mesa/*`: entidad, repositorio, servicio y fachada de mesa.
- `src/apuesta/*`: entidad, repositorio, servicio y fachada de apuesta.
- `src/Main.java`: runner demo de punta a punta.
- `src/SmokeTest.java`: prueba rápida de flujos críticos.
- `src/config/DatabaseConfig.java`: singleton del `HikariDataSource`.
- `src/config/DatabasePoolSmokeTest.java`: validación rápida del pool.
- `src/app/CasinoApp.java`: wiring y datos de ejemplo.
- `src/http/ApiServer.java`: servidor REST.
- `src/http/Json.java`: parser/serializador JSON liviano.
- `src/http/RestSmokeTest.java`: prueba de endpoints REST.

## Ejecutar

```bash
cd "/home/sebalx/TPI/Java-Backend"
mkdir -p out
javac -d out $(find src -name "*.java")
java -cp out Main
```

## Probar (smoke test)

```bash
cd "/home/sebalx/TPI/Java-Backend"
mkdir -p out
javac -d out $(find src -name "*.java")
java -cp out SmokeTest
```

## Probar el pool Hikari

Si querés validar la inicialización del pool y tenés Maven instalado:

```bash
cd "/home/sebalx/TPI/Java-Backend"
export JAVA_HOME="$HOME/.jdks/temurin-25.0.2"
export PATH="$JAVA_HOME/bin:$PATH"
mvn test -q
```

O, si preferís correrlo desde IntelliJ, ejecutá `config.DatabasePoolSmokeTest`.

## Levantar la API REST

```bash
cd "/home/sebalx/TPI/Java-Backend"
export JAVA_HOME="$HOME/.jdks/temurin-25.0.2"
export PATH="$JAVA_HOME/bin:$PATH"
java -cp out Main server
```

La API queda en `http://localhost:8080/api` por defecto.

### Credenciales de prueba

- Admin: `admin@casino.com` / `admin123`
- Jugador: `j1@casino.com` / `1234`

### Flujo para el front

1. Hacer `POST /api/auth/login` con `email` y `password`.
2. Guardar el `token` devuelto.
3. En las requests siguientes, enviar `Authorization: Bearer <token>`.
4. Consumir los recursos REST:
   - `GET /api/juegos`
   - `GET /api/mesas`
   - `POST /api/apuestas`
   - `GET /api/usuarios/{id}/historial`
   - `GET /api/reportes/recaudacion?desde=...&hasta=...`

### Ejemplo de request

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@casino.com","password":"admin123"}'
```

## Si usas el JDK de IntelliJ

En este entorno se detecto el JDK en:

`/home/sebalx/.jdks/temurin-25.0.2`

Si `javac` no aparece en `PATH`, ejecuta antes:

```bash
export JAVA_HOME="$HOME/.jdks/temurin-25.0.2"
export PATH="$JAVA_HOME/bin:$PATH"
```

## Notas técnicas

- Persistencia actual: en memoria (repositorios con `ConcurrentHashMap`).
- `config/DatabaseConfig.java` ahora crea y administra un `HikariDataSource` singleton.
- Variables de entorno disponibles: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `DB_POOL_MAX_SIZE`, `DB_POOL_MIN_IDLE`, `DB_CONNECTION_TIMEOUT_MS`.
- Las clases `*Servlet` quedaron como fachadas internas; la API REST real está en `src/http/`.
