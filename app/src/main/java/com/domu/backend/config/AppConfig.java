package com.domu.backend.config;

import java.util.Optional;

public record AppConfig(
        String jdbcUrl,
        String dbUser,
        String dbPassword,
        String jwtSecret,
        String jwtIssuer,
        long jwtExpirationMinutes,
        int serverPort
) {

    private static final String DEFAULT_JDBC_URL = "jdbc:mysql://localhost:3306/domu?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_DB_USER = "domu";
    private static final String DEFAULT_DB_PASSWORD = "domu";
    private static final String DEFAULT_JWT_SECRET = "change-this-secret";
    private static final String DEFAULT_JWT_ISSUER = "domu-backend";
    private static final long DEFAULT_JWT_EXPIRATION_MINUTES = 60L;
    private static final int DEFAULT_SERVER_PORT = 7070;

    public static AppConfig fromEnv() {
        return new AppConfig(
                envOrDefault("APP_JDBC_URL", DEFAULT_JDBC_URL),
                envOrDefault("APP_DB_USER", DEFAULT_DB_USER),
                envOrDefault("APP_DB_PASSWORD", DEFAULT_DB_PASSWORD),
                envOrDefault("APP_JWT_SECRET", DEFAULT_JWT_SECRET),
                envOrDefault("APP_JWT_ISSUER", DEFAULT_JWT_ISSUER),
                parseLong("APP_JWT_EXP_MINUTES", DEFAULT_JWT_EXPIRATION_MINUTES),
                (int) parseLong("APP_SERVER_PORT", DEFAULT_SERVER_PORT)
        );
    }

    private static String envOrDefault(String key, String defaultValue) {
        return Optional.ofNullable(System.getenv(key))
                .filter(value -> !value.isBlank())
                .orElse(defaultValue);
    }

    private static long parseLong(String key, long defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
