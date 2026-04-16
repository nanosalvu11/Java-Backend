package config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public final class DatabaseConfig {
    public static final String DB_URL = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/casino");
    public static final String DB_USER = System.getenv().getOrDefault("DB_USER", "javauser");
    public static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "java1234");

    private static final int MAX_POOL_SIZE = Integer.parseInt(System.getenv().getOrDefault("DB_POOL_MAX_SIZE", "10"));
    private static final int MIN_IDLE = Integer.parseInt(System.getenv().getOrDefault("DB_POOL_MIN_IDLE", "2"));
    private static final long CONNECTION_TIMEOUT_MS = Long.parseLong(System.getenv().getOrDefault("DB_CONNECTION_TIMEOUT_MS", "30000"));

    private static volatile HikariDataSource dataSource;

    private DatabaseConfig() {
    }

    public static HikariDataSource getDataSource() {
        HikariDataSource current = dataSource;
        if (current == null) {
            synchronized (DatabaseConfig.class) {
                current = dataSource;
                if (current == null) {
                    dataSource = current = createDataSource();
                    Runtime.getRuntime().addShutdownHook(new Thread(DatabaseConfig::closeDataSource));
                }
            }
        }
        return current;
    }

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    public static void closeDataSource() {
        HikariDataSource current = dataSource;
        if (current != null) {
            synchronized (DatabaseConfig.class) {
                current = dataSource;
                if (current != null) {
                    current.close();
                    dataSource = null;
                }
            }
        }
    }

    public static boolean isInitialized() {
        return dataSource != null;
    }

    private static HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DB_URL);
        config.setUsername(DB_USER);
        config.setPassword(DB_PASSWORD);
        config.setMaximumPoolSize(MAX_POOL_SIZE);
        config.setMinimumIdle(MIN_IDLE);
        config.setConnectionTimeout(CONNECTION_TIMEOUT_MS);
        config.setPoolName("casino-hikari-pool");
        config.setAutoCommit(true);
        return new HikariDataSource(config);
    }
}
