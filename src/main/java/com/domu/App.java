/*
 * Domu Backend Application
 * Main application entry point
 */
package com.domu;

import com.domu.config.AppConfig;
import com.domu.controller.HealthController;
import com.domu.controller.UserController;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    
    public static void main(String[] args) {
        App app = new App();
        app.start();
    }
    
    public void start() {
        start(AppConfig.DEFAULT_PORT);
    }
    
    public void start(int port) {
        Javalin app = createApp();
        
        app.start(port);
        logger.info("Domu Backend Server started on port {}", port);
    }
    
    public Javalin createApp() {
        Javalin app = Javalin.create(config -> {
            // Configure CORS for development
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.anyHost();
                });
            });
            
            // Enable request logging
            config.bundledPlugins.enableDevLogging();
            
            // Configure JSON mapper
            config.jsonMapper(new AppConfig.CustomJsonMapper());
        });
        
        // Register routes
        registerRoutes(app);
        
        // Exception handling
        app.exception(Exception.class, (e, ctx) -> {
            logger.error("Unhandled exception", e);
            ctx.status(500).json(new ErrorResponse("Internal server error"));
        });
        
        return app;
    }
    
    private void registerRoutes(Javalin app) {
        // Health check endpoints
        HealthController healthController = new HealthController();
        app.get("/health", healthController::health);
        app.get("/", healthController::welcome);
        
        // API v1 routes
        UserController userController = new UserController();
        app.get("/api/v1/users", userController::getAllUsers);
        app.get("/api/v1/users/{id}", userController::getUserById);
        app.post("/api/v1/users", userController::createUser);
        app.put("/api/v1/users/{id}", userController::updateUser);
        app.delete("/api/v1/users/{id}", userController::deleteUser);
    }
    
    // Simple error response class
    public static class ErrorResponse {
        public final String error;
        
        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}
