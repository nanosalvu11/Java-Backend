package config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public final class DatabaseConfig {

    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/casino"));
        config.setUsername(System.getenv().getOrDefault("DB_USER", "javauser"));
        config.setPassword(System.getenv().getOrDefault("DB_PASSWORD", "java1234"));
        config.setMaximumPoolSize(Integer.parseInt(System.getenv().getOrDefault("DB_POOL_MAX_SIZE", "10")));
        config.setMinimumIdle(Integer.parseInt(System.getenv().getOrDefault("DB_POOL_MIN_IDLE", "2")));
        config.setConnectionTimeout(Long.parseLong(System.getenv().getOrDefault("DB_CONNECTION_TIMEOUT_MS", "30000")));
        config.setPoolName("casino-hikari-pool");
        config.setAutoCommit(true); // las transacciones manuales se manejan por método donde se necesiten
        dataSource = new HikariDataSource(config);
    }

    private DatabaseConfig() {}

    public static HikariDataSource getDataSource() {
        return dataSource;
    }

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    public static void closeDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}