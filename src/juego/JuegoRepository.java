package juego;

import config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings({"SqlNoDataSourceInspection", "SqlResolve", "SqlDialectInspection"})
public class JuegoRepository {
    public JuegoRepository() {
        initSchema();
    }

    public Juego save(Juego juego) {
        if (juego.getId() == null) {
            return insert(juego);
        }
        return update(juego);
    }

    public Optional<Juego> findById(Long id) {
        String sql = "SELECT id, nombre, reglas FROM juegos WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error al buscar juego por id", e);
        }
    }

    public Optional<Juego> findByNombre(String nombre) {
        String sql = "SELECT id, nombre, reglas FROM juegos WHERE LOWER(nombre) = LOWER(?) LIMIT 1";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nombre);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error al buscar juego por nombre", e);
        }
    }

    public List<Juego> findAll() {
        String sql = "SELECT id, nombre, reglas FROM juegos ORDER BY id";
        List<Juego> juegos = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                juegos.add(mapRow(rs));
            }
            return juegos;
        } catch (SQLException e) {
            throw new IllegalStateException("Error al listar juegos", e);
        }
    }

    public boolean deleteById(Long id) {
        String sql = "DELETE FROM juegos WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Error al eliminar juego", e);
        }
    }

    private Juego insert(Juego juego) {
        String sql = "INSERT INTO juegos (nombre, reglas) VALUES (?, ?)";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindJuego(statement, juego, false);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    juego.setId(generatedKeys.getLong(1));
                }
            }
            return juego;
        } catch (SQLException e) {
            throw new IllegalStateException("Error al insertar juego", e);
        }
    }

    private Juego update(Juego juego) {
        String sql = "UPDATE juegos SET nombre = ?, reglas = ? WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindJuego(statement, juego, true);
            int affected = statement.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Juego no encontrado");
            }
            return juego;
        } catch (SQLException e) {
            throw new IllegalStateException("Error al actualizar juego", e);
        }
    }

    private void bindJuego(PreparedStatement statement, Juego juego, boolean includeId) throws SQLException {
        statement.setString(1, juego.getNombre());
        statement.setString(2, juego.getReglas());
        if (includeId) {
            statement.setLong(3, juego.getId());
        }
    }

    private Juego mapRow(ResultSet rs) throws SQLException {
        return new Juego(
                rs.getLong("id"),
                rs.getString("nombre"),
                rs.getString("reglas")
        );
    }

    private void initSchema() {
        String sql = """
                CREATE TABLE IF NOT EXISTS juegos (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    nombre VARCHAR(120) NOT NULL,
                    reglas TEXT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_juegos_nombre (nombre)
                )
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("Error al inicializar esquema de juegos", e);
        }
    }
}
