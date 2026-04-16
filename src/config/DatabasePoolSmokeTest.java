package config;

public final class DatabasePoolSmokeTest {
    private DatabasePoolSmokeTest() {
    }

    public static void main(String[] args) {
        System.out.println("Pool inicializado: " + DatabaseConfig.isInitialized());
        System.out.println("DataSource listo: " + (DatabaseConfig.getDataSource() != null));
        System.out.println("DataSource: " + DatabaseConfig.getDataSource());
        DatabaseConfig.closeDataSource();
        System.out.println("Pool cerrado correctamente");
    }
}



