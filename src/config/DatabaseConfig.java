package config;

public final class DatabaseConfig {
    public static final String DB_URL = "jdbc:mysql://localhost:3306/casino";
    public static final String DB_USER = "javauser";
    public static final String DB_PASSWORD = "java1234";

    private DatabaseConfig() {
    }

    // TODO: inicializar y exponer un HikariDataSource singleton.
}
