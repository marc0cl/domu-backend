package com.domu.service;

import com.domu.database.BuildingRepository;
import com.domu.database.PollRepository;
import com.domu.database.PollRepository.PollOptionRow;
import com.domu.database.PollRepository.PollRow;
import com.domu.database.PollRepository.PollVoteRow;
import com.domu.database.UserBuildingRepository;
import com.domu.domain.core.User;
import com.domu.dto.CreatePollRequest;
import com.domu.dto.PollListResponse;
import com.domu.dto.PollOptionResponse;
import com.domu.dto.PollResponse;
import com.domu.dto.VoteRequest;

import com.google.inject.Inject;

import io.javalin.http.UnauthorizedResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class PollService {

    private final PollRepository pollRepository;
    private final BuildingRepository buildingRepository;
    private final UserBuildingRepository userBuildingRepository;

    @Inject
    public PollService(PollRepository pollRepository, BuildingRepository buildingRepository,
            UserBuildingRepository userBuildingRepository) {
        this.pollRepository = pollRepository;
        this.buildingRepository = buildingRepository;
        this.userBuildingRepository = userBuildingRepository;
    }

    public PollResponse create(User user, CreatePollRequest request) {
        ensureAdminOrConcierge(user);
        validateCreateRequest(request);
        Long buildingId = resolveBuildingId(user, request.getBuildingId());
        LocalDateTime closesAt = request.getClosesAt();
        if (closesAt.isBefore(LocalDateTime.now())) {
            throw new ValidationException("La fecha de cierre debe ser futura");
        }

        PollRow saved = pollRepository.insertPoll(new PollRow(
                null,
                buildingId,
                user.id(),
                request.getTitle().trim(),
                request.getDescription(),
                closesAt,
                "OPEN",
                LocalDateTime.now(),
                null));

        List<String> normalizedOptions = request.getOptions().stream()
                .map(String::trim)
                .filter(o -> !o.isBlank())
                .collect(Collectors.toList());
        List<PollOptionRow> options = pollRepository.insertOptions(saved.id(), normalizedOptions);

        return toResponse(saved, options, null);
    }

    public PollListResponse list(User user, String status) {
        ensureAuthenticated(user);
        Long buildingId = resolveBuildingId(user, null);
        String normalizedStatus = normalizeStatus(status);
        refreshExpiredPolls();

        List<PollRow> polls = normalizedStatus == null
                ? pollRepository.findByBuilding(buildingId)
                : pollRepository.findByBuildingAndStatus(buildingId, normalizedStatus);

        List<PollResponse> open = new ArrayList<>();
        List<PollResponse> closed = new ArrayList<>();

        for (PollRow poll : polls) {
            PollRow fresh = ensureFreshStatus(poll);
            List<PollOptionRow> options = pollRepository.findOptions(fresh.id());
            Optional<PollVoteRow> vote = pollRepository.findUserVote(fresh.id(), user.id());
            PollResponse response = toResponse(fresh, options, vote.orElse(null));
            if ("OPEN".equalsIgnoreCase(fresh.status())) {
                open.add(response);
            } else {
                closed.add(response);
            }
        }

        Comparator<PollResponse> byCreated = Comparator.comparing(PollResponse::createdAt, Comparator.nullsLast(
                Comparator.naturalOrder())).reversed();
        open.sort(byCreated);
        closed.sort(byCreated);

        return new PollListResponse(open, closed);
    }

    public PollResponse get(User user, Long pollId) {
        ensureAuthenticated(user);
        PollRow poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ValidationException("Votación no encontrada"));
        ensureSameBuilding(user, poll.buildingId());
        PollRow fresh = ensureFreshStatus(poll);
        List<PollOptionRow> options = pollRepository.findOptions(fresh.id());
        Optional<PollVoteRow> vote = pollRepository.findUserVote(fresh.id(), user.id());
        return toResponse(fresh, options, vote.orElse(null));
    }

    public PollResponse vote(User user, Long pollId, VoteRequest request) {
        ensureAuthenticated(user);
        if (request == null || request.getOptionId() == null) {
            throw new ValidationException("optionId es obligatorio");
        }
        PollRow poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ValidationException("Votación no encontrada"));
        ensureSameBuilding(user, poll.buildingId());
        PollRow current = ensureFreshStatus(poll);

        if (!"OPEN".equalsIgnoreCase(current.status())) {
            throw new ValidationException("La votación está cerrada");
        }
        if (current.closesAt().isBefore(LocalDateTime.now())) {
            current = pollRepository.closePoll(current.id(), LocalDateTime.now());
            throw new ValidationException("La votación ya expiró");
        }

        Optional<PollVoteRow> existing = pollRepository.findUserVote(current.id(), user.id());
        if (existing.isPresent()) {
            throw new ValidationException("Ya registraste tu voto");
        }

        List<PollOptionRow> options = pollRepository.findOptions(current.id());
        PollOptionRow selected = options.stream()
                .filter(opt -> Objects.equals(opt.id(), request.getOptionId()))
                .findFirst()
                .orElseThrow(() -> new ValidationException("Opción inválida"));

        pollRepository.insertVote(current.id(), selected.id(), user.id(), LocalDateTime.now());
        List<PollOptionRow> refreshedOptions = pollRepository.findOptions(current.id());
        return toResponse(current, refreshedOptions,
                new PollVoteRow(null, current.id(), selected.id(), user.id(), LocalDateTime.now()));
    }

    public PollResponse close(User user, Long pollId) {
        ensureAdminOrConcierge(user);
        PollRow poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ValidationException("Votación no encontrada"));
        ensureSameBuilding(user, poll.buildingId());
        PollRow closed = pollRepository.closePoll(poll.id(), LocalDateTime.now());
        List<PollOptionRow> options = pollRepository.findOptions(closed.id());
        return toResponse(closed, options, null);
    }

    public String exportCsv(User user, Long pollId) {
        ensureAuthenticated(user);
        PollRow poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ValidationException("Votación no encontrada"));
        ensureSameBuilding(user, poll.buildingId());
        PollRow fresh = ensureFreshStatus(poll);
        List<PollOptionRow> options = pollRepository.findOptions(fresh.id());

        StringBuilder sb = new StringBuilder();
        sb.append("Pregunta,Estado,Cierra en,Total votos").append("\n");
        long total = options.stream().mapToLong(o -> o.votes() != null ? o.votes() : 0).sum();
        sb.append(sanitizeCsv(fresh.title())).append(',')
                .append(fresh.status()).append(',')
                .append(fresh.closesAt()).append(',')
                .append(total).append("\n\n");
        sb.append("Opción,Votos\n");
        for (PollOptionRow option : options) {
            sb.append(sanitizeCsv(option.label())).append(',')
                    .append(option.votes() != null ? option.votes() : 0).append("\n");
        }
        return sb.toString();
    }

    private void refreshExpiredPolls() {
        List<PollRow> expired = pollRepository.findExpiredOpenPolls(LocalDateTime.now());
        for (PollRow poll : expired) {
            pollRepository.closePoll(poll.id(), LocalDateTime.now());
        }
    }

    private PollRow ensureFreshStatus(PollRow poll) {
        if ("OPEN".equalsIgnoreCase(poll.status()) && poll.closesAt().isBefore(LocalDateTime.now())) {
            return pollRepository.closePoll(poll.id(), LocalDateTime.now());
        }
        return poll;
    }

    private PollResponse toResponse(PollRow poll, List<PollOptionRow> options, PollVoteRow vote) {
        long totalVotes = options.stream().mapToLong(o -> o.votes() != null ? o.votes() : 0).sum();
        List<PollOptionResponse> optionResponses = options.stream()
                .map(o -> new PollOptionResponse(
                        o.id(),
                        o.label(),
                        o.votes(),
                        totalVotes > 0 ? (o.votes() * 100.0) / totalVotes : 0.0))
                .toList();
        Long selectedOptionId = vote != null ? vote.optionId() : null;
        return new PollResponse(
                poll.id(),
                poll.buildingId(),
                poll.createdBy(),
                poll.title(),
                poll.description(),
                poll.status(),
                poll.closesAt(),
                poll.createdAt(),
                poll.closedAt(),
                optionResponses,
                vote != null,
                selectedOptionId,
                totalVotes);
    }

    private void validateCreateRequest(CreatePollRequest request) {
        if (request == null) {
            throw new ValidationException("El cuerpo es obligatorio");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ValidationException("El título es obligatorio");
        }
        if (request.getClosesAt() == null) {
            throw new ValidationException("La fecha de cierre es obligatoria");
        }
        if (request.getOptions() == null || request.getOptions().size() < 2) {
            throw new ValidationException("Debes agregar al menos dos opciones");
        }
    }

    private void ensureAuthenticated(User user) {
        if (user == null) {
            throw new UnauthorizedResponse("Debes iniciar sesión");
        }
    }

    private void ensureAdminOrConcierge(User user) {
        ensureAuthenticated(user);
        if (user.roleId() == null || !(Objects.equals(user.roleId(), 1L) || Objects.equals(user.roleId(), 3L))) {
            throw new UnauthorizedResponse("Solo administradores o conserjes pueden realizar esta acción");
        }
    }

    private Long resolveBuildingId(User user, Long requestBuildingId) {
        if (requestBuildingId != null) {
            return requestBuildingId;
        }
        if (user != null && user.unitId() != null) {
            Long buildingId = buildingRepository.findBuildingIdByUnitId(user.unitId());
            if (buildingId != null) {
                return buildingId;
            }
        }
        if (user != null) {
            var buildings = userBuildingRepository.findBuildingsForUser(user.id());
            if (buildings != null && !buildings.isEmpty()) {
                return buildings.get(0).id();
            }
        }
        throw new ValidationException("No se pudo determinar el edificio asociado");
    }

    private void ensureSameBuilding(User user, Long buildingId) {
        Long userBuilding = resolveBuildingId(user, null);
        if (!Objects.equals(userBuilding, buildingId)) {
            throw new UnauthorizedResponse("No tienes acceso a esta votación");
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        return switch (normalized) {
            case "OPEN", "CLOSED" -> normalized;
            default -> null;
        };
    }

    private String sanitizeCsv(String value) {
        if (value == null) {
            return "";
        }
        String sanitized = value.replace("\"", "\"\"");
        return "\"" + sanitized + "\"";
    }
}
