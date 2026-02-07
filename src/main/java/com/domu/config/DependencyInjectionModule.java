package com.domu.config;

import com.domu.database.*;
import com.domu.security.*;
import com.domu.service.*;
import com.domu.web.WebServer;
import com.domu.web.ChatWebSocketHandler;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariDataSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;

public class DependencyInjectionModule extends AbstractModule {

    private static Injector injector;
    private static final Map<String, String> dotEnvVars = loadDotEnv();

    public static Injector getInstance() {
        if (injector == null) {
            injector = Guice.createInjector(new DependencyInjectionModule());
        }
        return injector;
    }

    @Override
    protected void configure() {
        bind(WebServer.class).in(Scopes.SINGLETON);
        bind(UserService.class).in(Scopes.SINGLETON);
        bind(CommonExpenseService.class).in(Scopes.SINGLETON);
        bind(CommonExpenseReceiptStorageService.class).in(Scopes.SINGLETON);
        bind(CommonExpensePdfService.class).in(Scopes.SINGLETON);
        bind(PaymentReceiptPdfService.class).in(Scopes.SINGLETON);
        bind(ChargeReceiptPdfService.class).in(Scopes.SINGLETON);
        bind(BuildingService.class).in(Scopes.SINGLETON);
        bind(CommunityRegistrationStorageService.class).in(Scopes.SINGLETON);
        bind(VisitService.class).in(Scopes.SINGLETON);
        bind(VisitContactService.class).in(Scopes.SINGLETON);
        bind(IncidentService.class).in(Scopes.SINGLETON);
        bind(PollService.class).in(Scopes.SINGLETON);
        bind(AmenityService.class).in(Scopes.SINGLETON);
        bind(HousingUnitService.class).in(Scopes.SINGLETON);
        bind(MarketService.class).in(Scopes.SINGLETON);
        bind(ChatService.class).in(Scopes.SINGLETON);
        bind(ChatRequestService.class).in(Scopes.SINGLETON);
        bind(UserProfileService.class).in(Scopes.SINGLETON);
        bind(ForumService.class).in(Scopes.SINGLETON);
        bind(GcsStorageService.class).in(Scopes.SINGLETON);
        bind(MarketplaceStorageService.class).in(Scopes.SINGLETON);
        bind(ParcelService.class).in(Scopes.SINGLETON);
        bind(TaskService.class).in(Scopes.SINGLETON);
        bind(StaffService.class).in(Scopes.SINGLETON);

        bind(UserRepository.class).in(Scopes.SINGLETON);
        bind(ForumRepository.class).in(Scopes.SINGLETON);
        bind(CommonExpenseRepository.class).in(Scopes.SINGLETON);
        bind(BuildingRepository.class).in(Scopes.SINGLETON);
        bind(UserBuildingRepository.class).in(Scopes.SINGLETON);
        bind(UserConfirmationRepository.class).in(Scopes.SINGLETON);
        bind(VisitRepository.class).in(Scopes.SINGLETON);
        bind(VisitContactRepository.class).in(Scopes.SINGLETON);
        bind(IncidentRepository.class).in(Scopes.SINGLETON);
        bind(PollRepository.class).in(Scopes.SINGLETON);
        bind(AmenityRepository.class).in(Scopes.SINGLETON);
        bind(HousingUnitRepository.class).in(Scopes.SINGLETON);
        bind(ChatRequestRepository.class).in(Scopes.SINGLETON);
        bind(MarketRepository.class).in(Scopes.SINGLETON);
        bind(ChatRepository.class).in(Scopes.SINGLETON);
        bind(ParcelRepository.class).in(Scopes.SINGLETON);
        bind(TaskRepository.class).in(Scopes.SINGLETON);
        bind(StaffRepository.class).in(Scopes.SINGLETON);

        bind(ChatWebSocketHandler.class).in(Scopes.SINGLETON);
        bind(AuthenticationHandler.class).in(Scopes.SINGLETON);
        bind(PasswordHasher.class).to(BCryptPasswordHasher.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    AppConfig appConfig() {
        Properties properties = loadProperties();

        String dbUser = resolve(properties, "db.user", "DB_USER", DEFAULT_DB_USER);
        String dbPassword = resolve(properties, "db.password", "DB_PASSWORD", DEFAULT_DB_PASSWORD);
        String host = resolve(properties, "db.host", "DB_HOST", DEFAULT_DB_HOST);
        String port = resolve(properties, "db.port", "DB_PORT", DEFAULT_DB_PORT);
        String dbName = resolve(properties, "db.name", "DB_NAME", DEFAULT_DB_NAME);
        String jdbcUrl = resolve(properties, "db.uri", "DB_URI", "");
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            jdbcUrl = String.format(
                    "jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                    host,
                    port,
                    dbName);
        }

        return new AppConfig(
                jdbcUrl,
                dbUser,
                dbPassword,
                resolve(properties, "jwt.secret", "JWT_SECRET", DEFAULT_JWT_SECRET),
                resolve(properties, "jwt.issuer", "JWT_ISSUER", DEFAULT_JWT_ISSUER),
                parseLong(resolve(properties, "jwt.expirationMinutes", "JWT_EXPIRATION_MINUTES",
                        String.valueOf(DEFAULT_JWT_EXPIRATION_MINUTES)), DEFAULT_JWT_EXPIRATION_MINUTES),
                parseInteger(
                        resolve(properties, "server.port", "APP_SERVER_PORT", String.valueOf(AppConfig.DEFAULT_PORT)),
                        AppConfig.DEFAULT_PORT),
                resolve(properties, "box.developerToken", "BOX_TOKEN", ""),
                resolve(properties, "box.rootFolderId", "BOX_ROOT_FOLDER_ID", "0"),
                resolve(properties, "gcs.bucketName", "GCS_BUCKET_NAME", ""),
                resolve(properties, "gcs.keyFilePath", "GCS_KEY_FILE_PATH", ""),
                resolve(properties, "mail.host", "MAIL_HOST", ""),
                parseInteger(resolve(properties, "mail.port", "MAIL_PORT", "587"), 587),
                resolve(properties, "mail.user", "MAIL_USER", ""),
                resolve(properties, "mail.password", "MAIL_PASSWORD", ""),
                resolve(properties, "mail.from", "MAIL_FROM", "no-reply@domu.app"),
                resolve(properties, "approval.baseUrl", "APPROVAL_BASE_URL", "https://domu.app"),
                resolve(properties, "approval.recipient", "APPROVALS_RECIPIENT", ""));
    }

    @Provides
    @Singleton
    HikariDataSource dataSource(final AppConfig config) {
        HikariDataSource ds = DataSourceFactory.create(config);
        runPendingMigrations(ds);
        return ds;
    }

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DependencyInjectionModule.class);

    private static void runPendingMigrations(HikariDataSource ds) {
        String[] migrations = {
            "ALTER TABLE users ADD COLUMN display_name VARCHAR(100) NULL",
            "ALTER TABLE market_item MODIFY COLUMN main_image_url TEXT",
            "ALTER TABLE market_item_image MODIFY COLUMN url TEXT",
            "ALTER TABLE market_item_image MODIFY COLUMN box_file_id TEXT",
            "ALTER TABLE users MODIFY COLUMN avatar_box_id TEXT",
            "ALTER TABLE users MODIFY COLUMN privacy_avatar_box_id TEXT",
            "ALTER TABLE chat_participant ADD COLUMN hidden_at TIMESTAMP NULL DEFAULT NULL"
        };
        try (java.sql.Connection conn = ds.getConnection()) {
            for (String sql : migrations) {
                try (java.sql.Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                    LOG.info("Migration OK: {}", sql);
                } catch (java.sql.SQLException e) {
                    // Error code 1060 = Duplicate column name — safe to ignore
                    if (e.getErrorCode() == 1060) {
                        LOG.info("Column already exists, skipping: {}", sql);
                    } else {
                        LOG.warn("Migration warning: {} — {}", sql, e.getMessage());
                    }
                }
            }
        } catch (java.sql.SQLException e) {
            LOG.error("Error running pending migrations", e);
        }
    }

    @Provides
    @Singleton
    DataSource pooledDataSource(final HikariDataSource hikariDataSource) {
        return hikariDataSource;
    }

    @Provides
    @Singleton
    ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Provides
    @Singleton
    JwtProvider jwtProvider(final AppConfig config) {
        return new JwtProvider(config.jwtSecret(), config.jwtIssuer(), config.jwtExpirationMinutes());
    }

    @Provides
    @Singleton
    com.domu.email.EmailService emailService(final AppConfig config) {
        return new com.domu.email.SmtpEmailService(config);
    }

    private static final String DEFAULT_DB_HOST = "localhost";
    private static final String DEFAULT_DB_PORT = "3306";
    private static final String DEFAULT_DB_NAME = "domu";
    private static final String DEFAULT_DB_USER = "domu";
    private static final String DEFAULT_DB_PASSWORD = "domu";
    private static final String DEFAULT_JWT_SECRET = "change-this-secret";
    private static final String DEFAULT_JWT_ISSUER = "domu-backend";
    private static final Long DEFAULT_JWT_EXPIRATION_MINUTES = 60L;

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = DependencyInjectionModule.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException ignored) {
        }
        return properties;
    }

    /**
     * Loads environment variables from a .env file located at the project root.
     * The .env file is expected to have KEY=VALUE pairs, one per line.
     * Lines starting with '#' are treated as comments and ignored.
     */
    private static Map<String, String> loadDotEnv() {
        Map<String, String> envMap = new HashMap<>();
        Path envPath = Paths.get(".env");
        if (!Files.exists(envPath)) {
            return envMap;
        }
        try (BufferedReader reader = Files.newBufferedReader(envPath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();
                    envMap.put(key, value);
                }
            }
        } catch (IOException e) {
            // Silently ignore if .env file cannot be read
        }
        return envMap;
    }

    /**
     * Retrieves an environment variable value, checking System.getenv() first,
     * then falling back to the .env file values.
     */
    private static String getEnvValue(String envKey) {
        String systemEnv = System.getenv(envKey);
        if (systemEnv != null && !systemEnv.isBlank()) {
            return systemEnv;
        }
        return dotEnvVars.getOrDefault(envKey, null);
    }

    private static String resolve(Properties properties, String propertyKey, String envKey, String defaultValue) {
        String rawValue = properties.getProperty(propertyKey);
        String resolvedFromPlaceholder = resolvePlaceholder(rawValue);
        if (resolvedFromPlaceholder != null && !resolvedFromPlaceholder.isBlank()) {
            return resolvedFromPlaceholder;
        }
        String envValue = getEnvValue(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        if (rawValue != null && !rawValue.isBlank() && !rawValue.startsWith("${")) {
            return rawValue;
        }
        return defaultValue;
    }

    private static String resolvePlaceholder(String rawValue) {
        if (rawValue == null)
            return null;
        if (rawValue.startsWith("${") && rawValue.endsWith("}")) {
            String envKey = rawValue.substring(2, rawValue.length() - 1);
            return getEnvValue(envKey);
        }
        return null;
    }

    private static Long parseLong(String rawValue, Long defaultValue) {
        if (rawValue == null || rawValue.isBlank())
            return defaultValue;
        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static Integer parseInteger(String rawValue, Integer defaultValue) {
        if (rawValue == null || rawValue.isBlank())
            return defaultValue;
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
