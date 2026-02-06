package com.domu.service;

import com.domu.database.UserRepository;
import com.domu.database.UserBuildingRepository;
import com.domu.database.UserConfirmationRepository;
import com.domu.database.StaffRepository;
import com.domu.domain.core.User;
import com.domu.security.PasswordHasher;
import com.domu.dto.StaffRequest;
import com.google.inject.Inject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

public class UserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE);
    private static final Long ADMIN_ROLE_ID = 1L;

    private final UserRepository userRepository;
    private final UserBuildingRepository userBuildingRepository;
    private final UserConfirmationRepository userConfirmationRepository;
    private final StaffRepository staffRepository;
    private final PasswordHasher passwordHasher;
    private final MarketplaceStorageService storageService;

    @Inject
    public UserService(UserRepository userRepository, UserBuildingRepository userBuildingRepository,
            UserConfirmationRepository userConfirmationRepository,
            StaffRepository staffRepository,
            PasswordHasher passwordHasher,
            MarketplaceStorageService storageService) {
        this.userRepository = userRepository;
        this.userBuildingRepository = userBuildingRepository;
        this.userConfirmationRepository = userConfirmationRepository;
        this.staffRepository = staffRepository;
        this.passwordHasher = passwordHasher;
        this.storageService = storageService;
    }

    public void updateAvatar(User user, String fileName, byte[] content) {
        String url = storageService.uploadProfileImage(user.id(), fileName, content);
        userRepository.updateAvatar(user.id(), url);
    }

    public void updatePrivacyAvatar(User user, String fileName, byte[] content) {
        String url = storageService.uploadProfileImage(user.id(), fileName, content);
        userRepository.updatePrivacyAvatar(user.id(), url);
    }

    public User adminCreateUser(
            Long unitId,
            Long roleId,
            String firstName,
            String lastName,
            LocalDate birthDate,
            String email,
            String phone,
            String documentNumber,
            Boolean resident,
            String rawPassword,
            Long buildingId) {
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
                "PENDING",
                null,
                null,
                null);
        User saved = userRepository.save(user);
        
        if (buildingId != null) {
            userBuildingRepository.addUserToBuilding(saved.id(), buildingId);
        }
        
        // Si el usuario es staff o conserje, crear registro en tabla staff
        // Nota: Asumiendo que roleId 2 o 3 son staff/conserje, o puedes verificar por nombre del rol
        // Por ahora, crearemos staff si roleId != 1 (admin) y != null y resident == false
        if (buildingId != null && roleId != null && roleId != 1L && (resident == null || !resident)) {
            try {
                // Determinar posición basada en roleId o usar un valor por defecto
                String position = "Personal"; // Valor por defecto
                // Puedes agregar lógica aquí para determinar la posición según el roleId
                
                StaffRequest staffRequest = new StaffRequest(
                    buildingId,
                    firstName.trim(),
                    lastName.trim(),
                    documentNumber,
                    email.toLowerCase(),
                    phone,
                    position,
                    true // activo por defecto
                );
                
                // Solo crear si no existe ya un staff con este RUT
                staffRepository.findByRut(documentNumber).ifPresentOrElse(
                    existing -> {
                        // Ya existe, no hacer nada o actualizar
                    },
                    () -> {
                        // Crear nuevo registro de staff
                        staffRepository.insert(staffRequest);
                    }
                );
            } catch (Exception e) {
                // Log el error pero no fallar la creación del usuario
                System.err.println("Error creando registro de staff: " + e.getMessage());
            }
        }
        
        // Generate confirmation token (7 days)
        String token = java.util.UUID.randomUUID().toString();
        userConfirmationRepository.insert(saved.id(), token, LocalDateTime.now().plusDays(7));
        
        return saved;
    }

    public void confirmUser(String token) {
        UserConfirmationRepository.ConfirmationRow confirmation = userConfirmationRepository.findByToken(token)
                .orElseThrow(() -> new ValidationException("Token de confirmación inválido"));

        if (confirmation.confirmedAt() != null) {
            throw new ValidationException("Este token ya ha sido utilizado");
        }

        if (confirmation.expiresAt().isBefore(LocalDateTime.now())) {
            throw new ValidationException("El token ha expirado (validez de 7 días)");
        }

        userRepository.setStatus(confirmation.userId(), "ACTIVE");
        userConfirmationRepository.markAsConfirmed(token);
    }

    public String getConfirmationToken(Long userId) {
        return userConfirmationRepository.findLatestTokenForUser(userId).orElse(null);
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
            String rawPassword) {
        validateRegistration(firstName, lastName, email, phone, documentNumber, resident, rawPassword);
        if (roleId != null && ADMIN_ROLE_ID.equals(roleId) && unitId == null) {
            throw new ValidationException("Los administradores deben estar asociados a un edificio/unidad");
        }
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
                "ACTIVE",
                null,
                null,
                null);
        return userRepository.save(user);
    }

    public User createAdminForBuilding(String email, String phone, String documentNumber, String firstName,
            String lastName, String rawPassword, Long buildingId) {
        if (buildingId == null) {
            throw new ValidationException("buildingId es requerido para crear administrador");
        }
        validateRegistration(firstName, lastName, email, phone, documentNumber, false, rawPassword);
        String normalizedEmail = email.toLowerCase();
        userRepository.findByEmail(normalizedEmail).ifPresent(existing -> {
            throw new ValidationException("Ya existe un usuario con este correo");
        });
        String passwordHash = passwordHasher.hash(rawPassword);
        User user = new User(
                null,
                null,
                ADMIN_ROLE_ID,
                firstName.trim(),
                lastName.trim(),
                normalizedEmail,
                phone,
                null,
                passwordHash,
                documentNumber,
                false,
                LocalDateTime.now(),
                "ACTIVE",
                null,
                null,
                null);
        User saved = userRepository.save(user);
        userBuildingRepository.addUserToBuilding(saved.id(), buildingId);
        return saved;
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

    public Optional<User> findByEmail(String email) {
        if (isBlank(email)) {
            return Optional.empty();
        }
        return userRepository.findByEmail(email.toLowerCase());
    }

    public User updateProfile(User user, String firstName, String lastName, String phone, String documentNumber) {
        if (user == null || user.id() == null) {
            throw new ValidationException("Usuario inválido");
        }
        if (isBlank(firstName) || isBlank(lastName)) {
            throw new ValidationException("Debes ingresar nombre y apellido");
        }
        if (isBlank(phone)) {
            throw new ValidationException("Debes ingresar un teléfono de contacto");
        }
        if (isBlank(documentNumber)) {
            throw new ValidationException("Debes ingresar un documento de identidad");
        }
        return userRepository.updateProfile(user.id(), firstName.trim(), lastName.trim(), phone.trim(),
                documentNumber.trim());
    }

    public void changePassword(User user, String oldPassword, String newPassword) {
        if (user == null || user.id() == null) {
            throw new ValidationException("Usuario inválido");
        }
        if (isBlank(newPassword) || newPassword.length() < 10) {
            throw new ValidationException("La contraseña debe tener al menos 10 caracteres");
        }
        if (isBlank(oldPassword)) {
            throw new ValidationException("Debes ingresar la contraseña actual");
        }
        if (!passwordHasher.matches(oldPassword, user.passwordHash())) {
            throw new ValidationException("La contraseña actual no es correcta");
        }
        String hash = passwordHasher.hash(newPassword);
        userRepository.updatePassword(user.id(), hash);
    }

    /**
     * Crea un admin si no existe; si ya existe como admin, solo lo asocia al
     * edificio.
     * Si existe con otro rol, se informa al usuario.
     */
    public User ensureAdminForBuilding(
            String email,
            String phone,
            String documentNumber,
            String firstName,
            String lastName,
            String rawPassword,
            Long buildingId) {
        if (buildingId == null) {
            throw new ValidationException("buildingId es requerido para crear administrador");
        }
        String normalizedEmail = email.toLowerCase();
        Optional<User> existing = userRepository.findByEmail(normalizedEmail);
        if (existing.isPresent()) {
            User user = existing.get();
            if (!ADMIN_ROLE_ID.equals(user.roleId())) {
                throw new ValidationException("El correo ya existe con otro rol; usa un correo de administrador.");
            }
            userBuildingRepository.addUserToBuilding(user.id(), buildingId);
            return user;
        }
        // Si no existe, se crea
        return createAdminForBuilding(email, phone, documentNumber, firstName, lastName, rawPassword, buildingId);
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
            String rawPassword) {
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