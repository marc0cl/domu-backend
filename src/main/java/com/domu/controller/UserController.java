package com.domu.controller;

import com.domu.model.User;
import com.domu.service.UserService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    
    public UserController() {
        this.userService = new UserService();
    }
    
    public void getAllUsers(Context ctx) {
        logger.info("Fetching all users");
        ctx.json(userService.getAllUsers());
    }
    
    public void getUserById(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            logger.info("Fetching user with id: {}", id);
            
            userService.getUserById(id)
                    .ifPresentOrElse(
                            ctx::json,
                            () -> {
                                ctx.status(HttpStatus.NOT_FOUND);
                                ctx.json(createErrorResponse("User not found"));
                            }
                    );
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(createErrorResponse("Invalid user ID format"));
        }
    }
    
    public void createUser(Context ctx) {
        try {
            User user = ctx.bodyAsClass(User.class);
            logger.info("Creating new user: {}", user.getName());
            
            if (user.getName() == null || user.getName().trim().isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(createErrorResponse("Name is required"));
                return;
            }
            
            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(createErrorResponse("Email is required"));
                return;
            }
            
            User createdUser = userService.createUser(user);
            ctx.status(HttpStatus.CREATED);
            ctx.json(createdUser);
        } catch (Exception e) {
            logger.error("Error creating user", e);
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(createErrorResponse("Invalid request body"));
        }
    }
    
    public void updateUser(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            User user = ctx.bodyAsClass(User.class);
            logger.info("Updating user with id: {}", id);
            
            userService.updateUser(id, user)
                    .ifPresentOrElse(
                            ctx::json,
                            () -> {
                                ctx.status(HttpStatus.NOT_FOUND);
                                ctx.json(createErrorResponse("User not found"));
                            }
                    );
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(createErrorResponse("Invalid user ID format"));
        } catch (Exception e) {
            logger.error("Error updating user", e);
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(createErrorResponse("Invalid request body"));
        }
    }
    
    public void deleteUser(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            logger.info("Deleting user with id: {}", id);
            
            if (userService.deleteUser(id)) {
                ctx.status(HttpStatus.NO_CONTENT);
            } else {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.json(createErrorResponse("User not found"));
            }
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(createErrorResponse("Invalid user ID format"));
        }
    }
    
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}
