package com.domu.service;

import com.domu.database.UserRepository;
import com.domu.domain.core.User;
import com.domu.security.PasswordHasher;
import com.google.inject.Inject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

public class UserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    @Inject
    public UserService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public User registerUser(
        Long unitId,
        Long roleId,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String email,
        String phone,
        String documentNumber,
        Boolean resident,
        String rawPassword
    ) {
        validateRegistration(firstName, lastName, email, phone, documentNumber, resident, rawPassword);
        String normalizedEmail = email.toLowerCase();
        userRepository.findByEmail(normalizedEmail).ifPresent(existing -> {
            throw new UserAlreadyExistsException(normalizedEmail);
        });

        String passwordHash = passwordHasher.hash(rawPassword);
        User user = new User(
            null,
            unitId,
            roleId,
            firstName.trim(),
            lastName.trim(),
            normalizedEmail,
            phone,
            birthDate,
            passwordHash,
            documentNumber,
            resident,
            LocalDateTime.now(),
            "ACTIVE"
        );
        return userRepository.save(user);
    }

    public User authenticate(String email, String rawPassword) {
        String normalizedEmail = email.toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordHasher.matches(rawPassword, user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return user;
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    private void validateRegistration(
        String firstName,
        String lastName,
        String email,
        String phone,
        String documentNumber,
        Boolean resident,
        String rawPassword
    ) {
        if (isBlank(firstName) || isBlank(lastName)) {
            throw new ValidationException("Names and last names are required");
        }
        if (isBlank(phone)) {
            throw new ValidationException("phone is required");
        }
        if (isBlank(documentNumber)) {
            throw new ValidationException("documentNumber is required");
        }
        if (resident == null) {
            throw new ValidationException("resident is required");
        }
        if (isBlank(email) || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("A valid email is required");
        }
        if (isBlank(rawPassword) || rawPassword.length() < 10) {
            throw new ValidationException("Password must be at least 10 characters long");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
