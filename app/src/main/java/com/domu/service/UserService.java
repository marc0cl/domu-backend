package com.domu.service;

import com.domu.model.User;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class UserService {
    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    public UserService() {
        // Add some sample data
        createUser(new User(null, "John Doe", "john.doe@example.com"));
        createUser(new User(null, "Jane Smith", "jane.smith@example.com"));
    }
    
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }
    
    public Optional<User> getUserById(Long id) {
        return Optional.ofNullable(users.get(id));
    }
    
    public User createUser(User user) {
        Long id = idGenerator.getAndIncrement();
        user.setId(id);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        users.put(id, user);
        return user;
    }
    
    public Optional<User> updateUser(Long id, User updatedUser) {
        User existingUser = users.get(id);
        if (existingUser == null) {
            return Optional.empty();
        }
        
        existingUser.setName(updatedUser.getName());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setUpdatedAt(LocalDateTime.now());
        users.put(id, existingUser);
        return Optional.of(existingUser);
    }
    
    public boolean deleteUser(Long id) {
        return users.remove(id) != null;
    }
}
