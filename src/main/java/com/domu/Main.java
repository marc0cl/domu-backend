package com.domu;

import com.domu.config.AppConfig;
import com.domu.config.DependencyInjectionModule;
import com.domu.web.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final Integer DEFAULT_PORT = 8080;

    private Main() {
    }

    public static void main(final String[] args) {
        Integer port = resolvePort();
        var injector = DependencyInjectionModule.getInstance();
        WebServer server = injector.getInstance(WebServer.class);

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        server.start(port);
        LOGGER.info("domu-backend listening on http://localhost:{}", server.getPort());
    }

    private static Integer resolvePort() {
        String systemPort = System.getProperty("javalin.port");
        if (systemPort != null && !systemPort.isBlank()) {
            try {
                return Integer.parseInt(systemPort);
            } catch (NumberFormatException ignored) {
                // fallback to config
            }
        }

        AppConfig config = DependencyInjectionModule.getInstance().getInstance(AppConfig.class);
        if (config.serverPort() != null && config.serverPort() > 0) {
            return config.serverPort();
        }

        return DEFAULT_PORT;
    }
}