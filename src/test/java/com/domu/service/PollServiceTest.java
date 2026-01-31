package com.domu.service;

import com.domu.database.BuildingRepository;
import com.domu.database.PollRepository;
import com.domu.database.UserBuildingRepository;
import com.domu.domain.core.User;
import com.domu.dto.CreatePollRequest;

import io.javalin.http.UnauthorizedResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PollServiceTest {

    @Mock
    private PollRepository pollRepository;

    @Mock
    private BuildingRepository buildingRepository;

    @Mock
    private UserBuildingRepository userBuildingRepository;

    private PollService pollService;

    @BeforeEach
    void setUp() {
        pollService = new PollService(pollRepository, buildingRepository, userBuildingRepository);
    }

    @Test
    void createShouldRejectNonAdminOrConcierge() {
        User resident = sampleUser(2L);
        CreatePollRequest request = baseRequest();

        assertThrows(UnauthorizedResponse.class, () -> pollService.create(resident, request));
        verifyNoInteractions(pollRepository);
    }

    @Test
    void createShouldRequireAtLeastTwoOptions() {
        User admin = sampleUser(1L);
        CreatePollRequest request = baseRequest();
        request.setOptions(List.of("Única"));

        assertThrows(ValidationException.class, () -> pollService.create(admin, request));
        verifyNoInteractions(pollRepository);
    }

    private CreatePollRequest baseRequest() {
        CreatePollRequest request = new CreatePollRequest();
        request.setTitle("Nueva votación");
        request.setClosesAt(LocalDateTime.now().plusHours(2));
        request.setOptions(List.of("Sí", "No"));
        return request;
    }

    private User sampleUser(Long roleId) {
        return new User(
                1L,
                1L,
                roleId,
                "Nombre",
                "Apellido",
                "correo@test.com",
                "123456789",
                LocalDate.now(),
                "hash",
                "DOC",
                true,
                LocalDateTime.now(),
                "ACTIVE");
    }
}
