package com.domu.database;

import com.domu.dto.ServiceOrderRequest;
import com.google.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServiceOrderRepository {

    private final DataSource dataSource;

    @Inject
    public ServiceOrderRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<ServiceOrderResponse> findByBuilding(Long buildingId) {
        String sql = baseSelect() + " WHERE so.building_id = ? ORDER BY so.created_at DESC";
        return queryList(sql, buildingId);
    }

    public List<ServiceOrderResponse> findByProvider(Long providerId) {
        String sql = baseSelect() + " WHERE so.provider_id = ? ORDER BY so.created_at DESC";
        return queryList(sql, providerId);
    }

    public List<ServiceOrderResponse> findByBuildingAndStatus(Long buildingId, String status) {
        String sql = baseSelect() + " WHERE so.building_id = ? AND so.status = ? ORDER BY so.created_at DESC";
        List<ServiceOrderResponse> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, buildingId);
            stmt.setString(2, status);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error listing service orders by building and status", e);
        }
        return list;
    }

    public Optional<ServiceOrderResponse> findById(Long id) {
        String sql = baseSelect() + " WHERE so.id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error finding service order", e);
        }
        return Optional.empty();
    }

    public ServiceOrderResponse insert(ServiceOrderRequest req, Long createdBy) {
        String sql = "INSERT INTO service_orders (building_id, provider_id, created_by, title, description, scheduled_date, priority, admin_notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, req.buildingId());
            stmt.setLong(2, req.providerId());
            stmt.setLong(3, createdBy);
            stmt.setString(4, req.title());
            if (req.description() != null && !req.description().isBlank()) {
                stmt.setString(5, req.description());
            } else {
                stmt.setNull(5, Types.VARCHAR);
            }
            if (req.scheduledDate() != null) {
                stmt.setDate(6, Date.valueOf(req.scheduledDate()));
            } else {
                stmt.setNull(6, Types.DATE);
            }
            stmt.setString(7, req.priority() != null ? req.priority() : "NORMAL");
            if (req.adminNotes() != null && !req.adminNotes().isBlank()) {
                stmt.setString(8, req.adminNotes());
            } else {
                stmt.setNull(8, Types.VARCHAR);
            }

            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1)).orElseThrow();
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error inserting service order", e);
        }
        throw new RepositoryException("Failed to insert service order");
    }

    public ServiceOrderResponse update(Long id, ServiceOrderRequest req) {
        String sql = "UPDATE service_orders SET title = ?, description = ?, scheduled_date = ?, priority = ?, admin_notes = ?, provider_id = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, req.title());
            if (req.description() != null && !req.description().isBlank()) {
                stmt.setString(2, req.description());
            } else {
                stmt.setNull(2, Types.VARCHAR);
            }
            if (req.scheduledDate() != null) {
                stmt.setDate(3, Date.valueOf(req.scheduledDate()));
            } else {
                stmt.setNull(3, Types.DATE);
            }
            stmt.setString(4, req.priority() != null ? req.priority() : "NORMAL");
            if (req.adminNotes() != null && !req.adminNotes().isBlank()) {
                stmt.setString(5, req.adminNotes());
            } else {
                stmt.setNull(5, Types.VARCHAR);
            }
            stmt.setLong(6, req.providerId());
            stmt.setLong(7, id);

            stmt.executeUpdate();
            return findById(id).orElseThrow();
        } catch (SQLException e) {
            throw new RepositoryException("Error updating service order", e);
        }
    }

    public ServiceOrderResponse updateStatus(Long id, String status, String notes) {
        String sql;
        if ("COMPLETED".equals(status)) {
            sql = "UPDATE service_orders SET status = ?, provider_notes = COALESCE(?, provider_notes), completed_at = NOW(), updated_at = NOW() WHERE id = ?";
        } else {
            sql = "UPDATE service_orders SET status = ?, provider_notes = COALESCE(?, provider_notes), updated_at = NOW() WHERE id = ?";
        }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            if (notes != null && !notes.isBlank()) {
                stmt.setString(2, notes);
            } else {
                stmt.setNull(2, Types.VARCHAR);
            }
            stmt.setLong(3, id);
            stmt.executeUpdate();
            return findById(id).orElseThrow();
        } catch (SQLException e) {
            throw new RepositoryException("Error updating service order status", e);
        }
    }

    private List<ServiceOrderResponse> queryList(String sql, Long paramId) {
        List<ServiceOrderResponse> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, paramId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error listing service orders", e);
        }
        return list;
    }

    private String baseSelect() {
        return """
            SELECT so.*, p.business_name AS provider_name
            FROM service_orders so
            JOIN providers p ON p.id = so.provider_id
            """;
    }

    private ServiceOrderResponse mapRow(ResultSet rs) throws SQLException {
        return new ServiceOrderResponse(
                rs.getLong("id"),
                rs.getLong("building_id"),
                rs.getLong("provider_id"),
                rs.getString("provider_name"),
                rs.getLong("created_by"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getDate("scheduled_date") != null ? rs.getDate("scheduled_date").toLocalDate() : null,
                rs.getString("status"),
                rs.getString("priority"),
                rs.getString("admin_notes"),
                rs.getString("provider_notes"),
                rs.getTimestamp("completed_at") != null ? rs.getTimestamp("completed_at").toLocalDateTime() : null,
                rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null
        );
    }

    public record ServiceOrderResponse(
            Long id,
            Long buildingId,
            Long providerId,
            String providerName,
            Long createdBy,
            String title,
            String description,
            LocalDate scheduledDate,
            String status,
            String priority,
            String adminNotes,
            String providerNotes,
            LocalDateTime completedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
