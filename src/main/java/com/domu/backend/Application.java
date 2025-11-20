package com.domu.backend;

import com.domu.backend.config.AppConfig;
import com.domu.backend.web.WebServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    private Application() {
    }

    public static void main(String[] args) {
        AppConfig appConfig = AppConfig.fromEnv();
        LOGGER.info("Starting server on port {}", appConfig.serverPort());
        WebServer webServer = new WebServer(appConfig);
        webServer.start();
    }
}
