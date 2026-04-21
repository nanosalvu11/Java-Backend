package usuario;

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
public class UsuarioRepository {
    public UsuarioRepository() {
        initSchema();
    }

    public Usuario save(Usuario usuario) {
        if (usuario.getId() == null) {
            return insert(usuario);
        }
        return update(usuario);
    }

    public Optional<Usuario> findById(Long id) {
        String sql = "SELECT id, nombre, apellido, email, password, saldo, rol FROM usuarios WHERE id = ?";
        try (Connection connection = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error al buscar usuario por id", e);
        }
    }

    public Optional<Usuario> findByEmail(String email) {
        String sql = "SELECT id, nombre, apellido, email, password, saldo, rol FROM usuarios WHERE LOWER(email) = LOWER(?) LIMIT 1";
        try (Connection connection = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error al buscar usuario por email", e);
        }
    }

    public List<Usuario> findAll() {
        String sql = "SELECT id, nombre, apellido, email, password, saldo, rol FROM usuarios ORDER BY id";
        List<Usuario> usuarios = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                usuarios.add(mapRow(rs));
            }
            return usuarios;
        } catch (SQLException e) {
            throw new IllegalStateException("Error al listar usuarios", e);
        }
    }

    public boolean deleteById(Long id) {
        String sql = "DELETE FROM usuarios WHERE id = ?";
        try (Connection connection = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Error al eliminar usuario", e);
        }
    }

    public Usuario update(Usuario usuario) {
        return updateExisting(usuario);
    }

    public boolean delete(Long id) {
        return deleteById(id);
    }

    private Usuario insert(Usuario usuario) {
        String sql = "INSERT INTO usuarios (nombre, apellido, email, password, saldo, rol) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindUsuario(statement, usuario, false);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    usuario.setId(generatedKeys.getLong(1));
                }
            }
            return usuario;
        } catch (SQLException e) {
            throw new IllegalStateException("Error al insertar usuario", e);
        }
    }

    private Usuario updateExisting(Usuario usuario) {
        String sql = "UPDATE usuarios SET nombre = ?, apellido = ?, email = ?, password = ?, saldo = ?, rol = ? WHERE id = ?";
        try (Connection connection = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindUsuario(statement, usuario, true);
            int affected = statement.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Usuario no encontrado");
            }
            return usuario;
        } catch (SQLException e) {
            throw new IllegalStateException("Error al actualizar usuario", e);
        }
    }

    private void bindUsuario(PreparedStatement statement, Usuario usuario, boolean includeId) throws SQLException {
        statement.setString(1, usuario.getNombre());
        statement.setString(2, usuario.getApellido());
        statement.setString(3, usuario.getEmail());
        statement.setString(4, usuario.getPassword());
        statement.setBigDecimal(5, usuario.getSaldo());
        statement.setString(6, usuario.getRol());
        if (includeId) {
            statement.setLong(7, usuario.getId());
        }
    }

    private Usuario mapRow(ResultSet rs) throws SQLException {
        return new Usuario(
                rs.getLong("id"),
                rs.getString("nombre"),
                rs.getString("apellido"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getBigDecimal("saldo"),
                rs.getString("rol")
        );
    }

    private void initSchema() {
        String sql = """
                CREATE TABLE IF NOT EXISTS usuarios (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    nombre VARCHAR(100) NOT NULL,
                    apellido VARCHAR(100) NULL,
                    email VARCHAR(190) NOT NULL,
                    password VARCHAR(255) NOT NULL,
                    saldo DECIMAL(19,2) NOT NULL DEFAULT 0,
                    rol VARCHAR(30) NOT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_usuarios_email (email)
                )
                """;
        try (Connection connection = DatabaseConfig.getDataSource().getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("Error al inicializar esquema de usuarios", e);
        }
    }

}
