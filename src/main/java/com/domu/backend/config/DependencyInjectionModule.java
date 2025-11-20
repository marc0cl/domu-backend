package com.domu.backend.config;

import com.domu.backend.database.DataSourceFactory;
import com.domu.backend.database.UserRepository;
import com.domu.backend.security.AuthenticationHandler;
import com.domu.backend.security.BCryptPasswordHasher;
import com.domu.backend.security.JwtProvider;
import com.domu.backend.security.PasswordHasher;
import com.domu.backend.service.UserService;
import com.domu.backend.web.WebServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariDataSource;

public class DependencyInjectionModule extends AbstractModule {

    private static Injector injector;

    public static Injector getInstance() {
        if (injector == null) {
            injector = Guice.createInjector(new DependencyInjectionModule());
        }
        return injector;
    }

    @Override
    protected void configure() {
        bind(AppConfig.class).toInstance(AppConfig.fromEnv());
        bind(WebServer.class).in(Scopes.SINGLETON);
        bind(UserService.class).in(Scopes.SINGLETON);
        bind(UserRepository.class).in(Scopes.SINGLETON);
        bind(AuthenticationHandler.class).in(Scopes.SINGLETON);
        bind(PasswordHasher.class).to(BCryptPasswordHasher.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    HikariDataSource dataSource(final AppConfig config) {
        return DataSourceFactory.create(config);
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
}
