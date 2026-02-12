package com.domu.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.json.JsonMapper;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public record AppConfig(
        String jdbcUrl,
        String dbUser,
        String dbPassword,
        String jwtSecret,
        String jwtIssuer,
        Long jwtExpirationMinutes,
        Integer serverPort,
        String boxDeveloperToken,
        String boxRootFolderId,
        String gcsBucketName,
        String gcsKeyFilePath,
        String mailHost,
        Integer mailPort,
        String mailUser,
        String mailPassword,
        String mailFrom,
        String approvalBaseUrl,
        String approvalsRecipient
) {

    public static final Integer DEFAULT_PORT = 8080;

    public static class CustomJsonMapper implements JsonMapper {
        private final ObjectMapper objectMapper;

        public CustomJsonMapper() {
            this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }

        @NotNull
        @Override
        public String toJsonString(@NotNull Object obj, @NotNull Type type) {
            try {
                return objectMapper.writeValueAsString(obj);
            } catch (Exception e) {
                throw new RuntimeException("Error serializing to JSON", e);
            }
        }

        @NotNull
        @Override
        public <T> T fromJsonString(@NotNull String json, @NotNull Type targetType) {
            try {
                return objectMapper.readValue(json, objectMapper.constructType(targetType));
            } catch (Exception e) {
                throw new RuntimeException("Error deserializing from JSON", e);
            }
        }
    }
}
