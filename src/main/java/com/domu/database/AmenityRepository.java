package com.domu.database;

import com.google.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AmenityRepository {

    private final DataSource dataSource;

    @Inject
    public AmenityRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ==================== AMENITIES CRUD ====================

    public AmenityRow insertAmenity(AmenityRow amenity) {
        String sql = """
                INSERT INTO amenities (building_id, name, description, max_capacity, cost_per_slot, rules, image_url, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, amenity.buildingId());
            statement.setString(2, amenity.name());
            statement.setString(3, amenity.description());
            statement.setObject(4, amenity.maxCapacity());
            statement.setBigDecimal(5, amenity.costPerSlot());
            statement.setString(6, amenity.rules());
            statement.setString(7, amenity.imageUrl());
            statement.setString(8, amenity.status() != null ? amenity.status() : "ACTIVE");
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    Long id = keys.getLong(1);
                    return findAmenityById(id).orElseThrow(() -> new RepositoryException("No se pudo recuperar el área común creada"));
                }
            }
            throw new RepositoryException("No se pudo obtener el id del área común");
        } catch (SQLException e) {
            throw new RepositoryException("Error creando área común", e);
        }
    }

    public AmenityRow updateAmenity(AmenityRow amenity) {
        String sql = """
                UPDATE amenities
                SET name = ?, description = ?, max_capacity = ?, cost_per_slot = ?, rules = ?, image_url = ?, status = ?
                WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, amenity.name());
            statement.setString(2, amenity.description());
            statement.setObject(3, amenity.maxCapacity());
            statement.setBigDecimal(4, amenity.costPerSlot());
            statement.setString(5, amenity.rules());
            statement.setString(6, amenity.imageUrl());
            statement.setString(7, amenity.status());
            statement.setLong(8, amenity.id());
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new RepositoryException("No se encontró el área común para actualizar");
            }
            return findAmenityById(amenity.id()).orElseThrow(() -> new RepositoryException("No se pudo recuperar el área común actualizada"));
        } catch (SQLException e) {
            throw new RepositoryException("Error actualizando área común", e);
        }
    }

    public void deleteAmenity(Long amenityId) {
        String sql = "UPDATE amenities SET status = 'INACTIVE' WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, amenityId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error eliminando área común", e);
        }
    }

    public Optional<AmenityRow> findAmenityById(Long id) {
        String sql = """
                SELECT id, building_id, name, description, max_capacity, cost_per_slot, rules, image_url, status, created_at, updated_at
                FROM amenities
                WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapAmenity(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo área común", e);
        }
    }

    public List<AmenityRow> findAmenitiesByBuilding(Long buildingId) {
        String sql = """
                SELECT id, building_id, name, description, max_capacity, cost_per_slot, rules, image_url, status, created_at, updated_at
                FROM amenities
                WHERE building_id = ? AND status = 'ACTIVE'
                ORDER BY name ASC
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, buildingId);
            try (ResultSet rs = statement.executeQuery()) {
                List<AmenityRow> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapAmenity(rs));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo áreas comunes", e);
        }
    }

    public List<AmenityRow> findAllAmenitiesByBuilding(Long buildingId) {
        String sql = """
                SELECT id, building_id, name, description, max_capacity, cost_per_slot, rules, image_url, status, created_at, updated_at
                FROM amenities
                WHERE building_id = ?
                ORDER BY name ASC
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, buildingId);
            try (ResultSet rs = statement.executeQuery()) {
                List<AmenityRow> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapAmenity(rs));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo áreas comunes", e);
        }
    }

    // ==================== TIME SLOTS ====================

    public TimeSlotRow insertTimeSlot(TimeSlotRow slot) {
        String sql = """
                INSERT INTO amenity_time_slots (amenity_id, day_of_week, start_time, end_time, active)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, slot.amenityId());
            statement.setInt(2, slot.dayOfWeek());
            statement.setTime(3, Time.valueOf(slot.startTime()));
            statement.setTime(4, Time.valueOf(slot.endTime()));
            statement.setBoolean(5, slot.active());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    Long id = keys.getLong(1);
                    return new TimeSlotRow(id, slot.amenityId(), slot.dayOfWeek(), slot.startTime(), slot.endTime(), slot.active(), LocalDateTime.now());
                }
            }
            throw new RepositoryException("No se pudo obtener el id del bloque horario");
        } catch (SQLException e) {
            throw new RepositoryException("Error creando bloque horario", e);
        }
    }

    public void deleteTimeSlotsByAmenity(Long amenityId) {
        String sql = "DELETE FROM amenity_time_slots WHERE amenity_id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, amenityId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error eliminando bloques horarios", e);
        }
    }

    public List<TimeSlotRow> findTimeSlotsByAmenity(Long amenityId) {
        String sql = """
                SELECT id, amenity_id, day_of_week, start_time, end_time, active, created_at
                FROM amenity_time_slots
                WHERE amenity_id = ?
                ORDER BY day_of_week ASC, start_time ASC
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, amenityId);
            try (ResultSet rs = statement.executeQuery()) {
                List<TimeSlotRow> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapTimeSlot(rs));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo bloques horarios", e);
        }
    }

    public List<TimeSlotRow> findActiveTimeSlotsByAmenityAndDay(Long amenityId, int dayOfWeek) {
        String sql = """
                SELECT id, amenity_id, day_of_week, start_time, end_time, active, created_at
                FROM amenity_time_slots
                WHERE amenity_id = ? AND day_of_week = ? AND active = TRUE
                ORDER BY start_time ASC
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, amenityId);
            statement.setInt(2, dayOfWeek);
            try (ResultSet rs = statement.executeQuery()) {
                List<TimeSlotRow> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapTimeSlot(rs));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo bloques horarios del día", e);
        }
    }

    public Optional<TimeSlotRow> findTimeSlotById(Long id) {
        String sql = """
                SELECT id, amenity_id, day_of_week, start_time, end_time, active, created_at
                FROM amenity_time_slots
                WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapTimeSlot(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo bloque horario", e);
        }
    }

    // ==================== RESERVATIONS ====================

    public ReservationRow insertReservation(ReservationRow reservation) {
        String sql = """
                INSERT INTO amenity_reservations (amenity_id, user_id, time_slot_id, reservation_date, status, notes)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, reservation.amenityId());
            statement.setLong(2, reservation.userId());
            statement.setLong(3, reservation.timeSlotId());
            statement.setDate(4, Date.valueOf(reservation.reservationDate()));
            statement.setString(5, reservation.status() != null ? reservation.status() : "CONFIRMED");
            statement.setString(6, reservation.notes());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    Long id = keys.getLong(1);
                    return findReservationById(id).orElseThrow(() -> new RepositoryException("No se pudo recuperar la reserva creada"));
                }
            }
            throw new RepositoryException("No se pudo obtener el id de la reserva");
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate")) {
                throw new RepositoryException("Ya existe una reserva para ese horario y fecha");
            }
            throw new RepositoryException("Error creando reserva", e);
        }
    }

    public ReservationRow cancelReservation(Long reservationId) {
        String sql = "UPDATE amenity_reservations SET status = 'CANCELLED', cancelled_at = ? WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            statement.setLong(2, reservationId);
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new RepositoryException("No se encontró la reserva para cancelar");
            }
            return findReservationById(reservationId).orElseThrow(() -> new RepositoryException("No se pudo recuperar la reserva cancelada"));
        } catch (SQLException e) {
            throw new RepositoryException("Error cancelando reserva", e);
        }
    }

    public Optional<ReservationRow> findReservationById(Long id) {
        String sql = """
                SELECT r.id, r.amenity_id, r.user_id, r.time_slot_id, r.reservation_date, r.status, r.notes, r.created_at, r.cancelled_at,
                       a.name as amenity_name, ts.start_time, ts.end_time,
                       u.first_name, u.last_name, u.email
                FROM amenity_reservations r
                JOIN amenities a ON r.amenity_id = a.id
                JOIN amenity_time_slots ts ON r.time_slot_id = ts.id
                JOIN users u ON r.user_id = u.id
                WHERE r.id = ?
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReservationWithDetails(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo reserva", e);
        }
    }

    public List<ReservationRow> findReservationsByUser(Long userId) {
        String sql = """
                SELECT r.id, r.amenity_id, r.user_id, r.time_slot_id, r.reservation_date, r.status, r.notes, r.created_at, r.cancelled_at,
                       a.name as amenity_name, ts.start_time, ts.end_time,
                       u.first_name, u.last_name, u.email
                FROM amenity_reservations r
                JOIN amenities a ON r.amenity_id = a.id
                JOIN amenity_time_slots ts ON r.time_slot_id = ts.id
                JOIN users u ON r.user_id = u.id
                WHERE r.user_id = ?
                ORDER BY r.reservation_date DESC, ts.start_time DESC
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                List<ReservationRow> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapReservationWithDetails(rs));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo reservas del usuario", e);
        }
    }

    public List<ReservationRow> findReservationsByAmenity(Long amenityId) {
        String sql = """
                SELECT r.id, r.amenity_id, r.user_id, r.time_slot_id, r.reservation_date, r.status, r.notes, r.created_at, r.cancelled_at,
                       a.name as amenity_name, ts.start_time, ts.end_time,
                       u.first_name, u.last_name, u.email
                FROM amenity_reservations r
                JOIN amenities a ON r.amenity_id = a.id
                JOIN amenity_time_slots ts ON r.time_slot_id = ts.id
                JOIN users u ON r.user_id = u.id
                WHERE r.amenity_id = ?
                ORDER BY r.reservation_date DESC, ts.start_time DESC
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, amenityId);
            try (ResultSet rs = statement.executeQuery()) {
                List<ReservationRow> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapReservationWithDetails(rs));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo reservas del área común", e);
        }
    }

    public List<ReservationRow> findReservationsByAmenityAndDate(Long amenityId, LocalDate date) {
        String sql = """
                SELECT r.id, r.amenity_id, r.user_id, r.time_slot_id, r.reservation_date, r.status, r.notes, r.created_at, r.cancelled_at,
                       a.name as amenity_name, ts.start_time, ts.end_time,
                       u.first_name, u.last_name, u.email
                FROM amenity_reservations r
                JOIN amenities a ON r.amenity_id = a.id
                JOIN amenity_time_slots ts ON r.time_slot_id = ts.id
                JOIN users u ON r.user_id = u.id
                WHERE r.amenity_id = ? AND r.reservation_date = ? AND r.status = 'CONFIRMED'
                ORDER BY ts.start_time ASC
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, amenityId);
            statement.setDate(2, Date.valueOf(date));
            try (ResultSet rs = statement.executeQuery()) {
                List<ReservationRow> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapReservationWithDetails(rs));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo reservas por fecha", e);
        }
    }

    public boolean isSlotReserved(Long timeSlotId, LocalDate date) {
        String sql = """
                SELECT COUNT(*) FROM amenity_reservations
                WHERE time_slot_id = ? AND reservation_date = ? AND status = 'CONFIRMED'
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, timeSlotId);
            statement.setDate(2, Date.valueOf(date));
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error verificando disponibilidad", e);
        }
    }

    // ==================== MAPPERS ====================

    private AmenityRow mapAmenity(ResultSet rs) throws SQLException {
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        return new AmenityRow(
                rs.getLong("id"),
                rs.getLong("building_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getObject("max_capacity") != null ? rs.getInt("max_capacity") : null,
                rs.getBigDecimal("cost_per_slot"),
                rs.getString("rules"),
                rs.getString("image_url"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                updatedAt != null ? updatedAt.toLocalDateTime() : null);
    }

    private TimeSlotRow mapTimeSlot(ResultSet rs) throws SQLException {
        return new TimeSlotRow(
                rs.getLong("id"),
                rs.getLong("amenity_id"),
                rs.getInt("day_of_week"),
                rs.getTime("start_time").toLocalTime(),
                rs.getTime("end_time").toLocalTime(),
                rs.getBoolean("active"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    private ReservationRow mapReservationWithDetails(ResultSet rs) throws SQLException {
        Timestamp cancelledAt = rs.getTimestamp("cancelled_at");
        return new ReservationRow(
                rs.getLong("id"),
                rs.getLong("amenity_id"),
                rs.getLong("user_id"),
                rs.getLong("time_slot_id"),
                rs.getDate("reservation_date").toLocalDate(),
                rs.getString("status"),
                rs.getString("notes"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                cancelledAt != null ? cancelledAt.toLocalDateTime() : null,
                rs.getString("amenity_name"),
                rs.getTime("start_time").toLocalTime(),
                rs.getTime("end_time").toLocalTime(),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("email"));
    }

    // ==================== RECORDS ====================

    public record AmenityRow(
            Long id,
            Long buildingId,
            String name,
            String description,
            Integer maxCapacity,
            java.math.BigDecimal costPerSlot,
            String rules,
            String imageUrl,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record TimeSlotRow(
            Long id,
            Long amenityId,
            Integer dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            Boolean active,
            LocalDateTime createdAt) {
    }

    public record ReservationRow(
            Long id,
            Long amenityId,
            Long userId,
            Long timeSlotId,
            LocalDate reservationDate,
            String status,
            String notes,
            LocalDateTime createdAt,
            LocalDateTime cancelledAt,
            String amenityName,
            LocalTime startTime,
            LocalTime endTime,
            String userFirstName,
            String userLastName,
            String userEmail) {
    }
}
