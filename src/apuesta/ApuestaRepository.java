package apuesta;

import config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ApuestaRepository {
    public ApuestaRepository() {
        initSchema();
    }

    public Apuesta save(Apuesta apuesta) {
        if (apuesta.getId() == null) {
            return insert(apuesta);
        }
        return update(apuesta);
    }

    public Optional<Apuesta> findById(Long id) {
        String sql = "SELECT id, id_usuario, id_mesa, monto_apostado, resultado_monto, fecha FROM apuestas WHERE id = ?";
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
            throw new IllegalStateException("Error al buscar apuesta por id", e);
        }
    }

    public List<Apuesta> findAll() {
        String sql = "SELECT id, id_usuario, id_mesa, monto_apostado, resultado_monto, fecha FROM apuestas ORDER BY fecha, id";
        List<Apuesta> apuestas = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                apuestas.add(mapRow(rs));
            }
            return apuestas;
        } catch (SQLException e) {
            throw new IllegalStateException("Error al listar apuestas", e);
        }
    }

    public List<Apuesta> findByUsuarioId(Long usuarioId) {
        String sql = "SELECT id, id_usuario, id_mesa, monto_apostado, resultado_monto, fecha FROM apuestas WHERE id_usuario = ? ORDER BY fecha, id";
        List<Apuesta> apuestas = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, usuarioId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    apuestas.add(mapRow(rs));
                }
            }
            return apuestas;
        } catch (SQLException e) {
            throw new IllegalStateException("Error al listar apuestas por usuario", e);
        }
    }

    public List<Apuesta> findByFecha(LocalDateTime desde, LocalDateTime hasta) {
        String sql = "SELECT id, id_usuario, id_mesa, monto_apostado, resultado_monto, fecha FROM apuestas WHERE fecha BETWEEN ? AND ? ORDER BY fecha, id";
        List<Apuesta> apuestas = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(desde));
            statement.setTimestamp(2, Timestamp.valueOf(hasta));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    apuestas.add(mapRow(rs));
                }
            }
            return apuestas;
        } catch (SQLException e) {
            throw new IllegalStateException("Error al listar apuestas por fecha", e);
        }
    }

    public boolean deleteById(Long id) {
        String sql = "DELETE FROM apuestas WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Error al eliminar apuesta", e);
        }
    }

    private Apuesta insert(Apuesta apuesta) {
        String sql = "INSERT INTO apuestas (id_usuario, id_mesa, monto_apostado, resultado_monto, fecha) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindApuesta(statement, apuesta, false);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    apuesta.setId(generatedKeys.getLong(1));
                }
            }
            return apuesta;
        } catch (SQLException e) {
            throw new IllegalStateException("Error al insertar apuesta", e);
        }
    }

    private Apuesta update(Apuesta apuesta) {
        String sql = "UPDATE apuestas SET id_usuario = ?, id_mesa = ?, monto_apostado = ?, resultado_monto = ?, fecha = ? WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindApuesta(statement, apuesta, true);
            int affected = statement.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Apuesta no encontrada");
            }
            return apuesta;
        } catch (SQLException e) {
            throw new IllegalStateException("Error al actualizar apuesta", e);
        }
    }

    private void bindApuesta(PreparedStatement statement, Apuesta apuesta, boolean includeId) throws SQLException {
        statement.setLong(1, apuesta.getIdUsuario());
        statement.setLong(2, apuesta.getIdMesa());
        statement.setBigDecimal(3, apuesta.getMontoApostado());
        statement.setBigDecimal(4, apuesta.getResultadoMonto());
        statement.setTimestamp(5, Timestamp.valueOf(apuesta.getFecha()));
        if (includeId) {
            statement.setLong(6, apuesta.getId());
        }
    }

    private Apuesta mapRow(ResultSet rs) throws SQLException {
        return new Apuesta(
                rs.getLong("id"),
                rs.getLong("id_usuario"),
                rs.getLong("id_mesa"),
                rs.getBigDecimal("monto_apostado"),
                rs.getBigDecimal("resultado_monto"),
                rs.getTimestamp("fecha").toLocalDateTime()
        );
    }

    private void initSchema() {
        String sql = """
                CREATE TABLE IF NOT EXISTS apuestas (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    id_usuario BIGINT NOT NULL,
                    id_mesa BIGINT NOT NULL,
                    monto_apostado DECIMAL(19,2) NOT NULL,
                    resultado_monto DECIMAL(19,2) NOT NULL,
                    fecha DATETIME NOT NULL,
                    PRIMARY KEY (id),
                    CONSTRAINT fk_apuestas_usuarios FOREIGN KEY (id_usuario) REFERENCES usuarios(id),
                    CONSTRAINT fk_apuestas_mesas FOREIGN KEY (id_mesa) REFERENCES mesas(id)
                )
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("Error al inicializar esquema de apuestas", e);
        }
    }
}
