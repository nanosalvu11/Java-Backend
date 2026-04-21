package mesa;

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
public class MesaRepository {
    public MesaRepository() {
        initSchema();
    }

    public Mesa save(Mesa mesa) {
        if (mesa.getId() == null) {
            return insert(mesa);
        }
        return update(mesa);
    }

    public Optional<Mesa> findById(Long id) {
        String sql = "SELECT id, id_juego, apuesta_minima, apuesta_maxima, estado FROM mesas WHERE id = ?";
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
            throw new IllegalStateException("Error al buscar mesa por id", e);
        }
    }

    public List<Mesa> findAll() {
        String sql = "SELECT id, id_juego, apuesta_minima, apuesta_maxima, estado FROM mesas ORDER BY id";
        List<Mesa> mesas = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                mesas.add(mapRow(rs));
            }
            return mesas;
        } catch (SQLException e) {
            throw new IllegalStateException("Error al listar mesas", e);
        }
    }

    public List<Mesa> findByJuegoId(Long juegoId) {
        String sql = "SELECT id, id_juego, apuesta_minima, apuesta_maxima, estado FROM mesas WHERE id_juego = ? ORDER BY id";
        List<Mesa> mesas = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, juegoId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    mesas.add(mapRow(rs));
                }
            }
            return mesas;
        } catch (SQLException e) {
            throw new IllegalStateException("Error al listar mesas por juego", e);
        }
    }

    public boolean deleteById(Long id) {
        String sql = "DELETE FROM mesas WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Error al eliminar mesa", e);
        }
    }

    private Mesa insert(Mesa mesa) {
        String sql = "INSERT INTO mesas (id_juego, apuesta_minima, apuesta_maxima, estado) VALUES (?, ?, ?, ?)";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindMesa(statement, mesa, false);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    mesa.setId(generatedKeys.getLong(1));
                }
            }
            return mesa;
        } catch (SQLException e) {
            throw new IllegalStateException("Error al insertar mesa", e);
        }
    }

    private Mesa update(Mesa mesa) {
        String sql = "UPDATE mesas SET id_juego = ?, apuesta_minima = ?, apuesta_maxima = ?, estado = ? WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindMesa(statement, mesa, true);
            int affected = statement.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Mesa no encontrada");
            }
            return mesa;
        } catch (SQLException e) {
            throw new IllegalStateException("Error al actualizar mesa", e);
        }
    }

    private void bindMesa(PreparedStatement statement, Mesa mesa, boolean includeId) throws SQLException {
        statement.setLong(1, mesa.getIdJuego());
        statement.setBigDecimal(2, mesa.getApuestaMinima());
        statement.setBigDecimal(3, mesa.getApuestaMaxima());
        statement.setString(4, mesa.getEstado());
        if (includeId) {
            statement.setLong(5, mesa.getId());
        }
    }

    private Mesa mapRow(ResultSet rs) throws SQLException {
        return new Mesa(
                rs.getLong("id"),
                rs.getLong("id_juego"),
                rs.getBigDecimal("apuesta_minima"),
                rs.getBigDecimal("apuesta_maxima"),
                rs.getString("estado")
        );
    }

    private void initSchema() {
        String sql = """
                CREATE TABLE IF NOT EXISTS mesas (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    id_juego BIGINT NOT NULL,
                    apuesta_minima DECIMAL(19,2) NOT NULL,
                    apuesta_maxima DECIMAL(19,2) NOT NULL,
                    estado VARCHAR(30) NOT NULL,
                    PRIMARY KEY (id),
                    CONSTRAINT fk_mesas_juegos FOREIGN KEY (id_juego) REFERENCES juegos(id)
                )
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("Error al inicializar esquema de mesas", e);
        }
    }
}
