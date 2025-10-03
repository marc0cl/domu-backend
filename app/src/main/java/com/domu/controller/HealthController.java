package com.domu.controller;

import io.javalin.http.Context;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class HealthController {
    
    public void health(Context ctx) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "domu-backend");
        
        ctx.json(response);
    }
    
    public void welcome(Context ctx) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome to Domu Backend API");
        response.put("version", "1.0.0");
        response.put("documentation", "/api/v1/docs");
        
        ctx.json(response);
    }
}
