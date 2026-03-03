package com.domu.database;

import com.domu.dto.ProviderRequest;
import com.google.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProviderRepository {

    private final DataSource dataSource;

    @Inject
    public ProviderRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<ProviderResponse> findByBuilding(Long buildingId) {
        String sql = "SELECT * FROM providers WHERE building_id = ? ORDER BY business_name";
        List<ProviderResponse> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, buildingId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error listing providers by building", e);
        }
        return list;
    }

    public List<ProviderResponse> findActiveByBuilding(Long buildingId) {
        String sql = "SELECT * FROM providers WHERE building_id = ? AND active = TRUE ORDER BY business_name";
        List<ProviderResponse> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, buildingId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error listing active providers by building", e);
        }
        return list;
    }

    public Optional<ProviderResponse> findById(Long id) {
        String sql = "SELECT * FROM providers WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error finding provider", e);
        }
        return Optional.empty();
    }

    public Optional<ProviderResponse> findByRut(String rut) {
        String sql = "SELECT * FROM providers WHERE rut = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, rut);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error finding provider by rut", e);
        }
        return Optional.empty();
    }

    public Optional<ProviderResponse> findByUserId(Long userId) {
        String sql = "SELECT * FROM providers WHERE user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error finding provider by userId", e);
        }
        return Optional.empty();
    }

    public ProviderResponse insert(ProviderRequest req) {
        String sql = "INSERT INTO providers (building_id, user_id, business_name, rut, contact_name, email, phone, address, service_category, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, req.buildingId());
            if (req.userId() != null) {
                stmt.setLong(2, req.userId());
            } else {
                stmt.setNull(2, Types.BIGINT);
            }
            stmt.setString(3, req.businessName());
            stmt.setString(4, req.rut());
            if (req.contactName() != null && !req.contactName().isBlank()) {
                stmt.setString(5, req.contactName());
            } else {
                stmt.setNull(5, Types.VARCHAR);
            }
            if (req.email() != null && !req.email().isBlank()) {
                stmt.setString(6, req.email());
            } else {
                stmt.setNull(6, Types.VARCHAR);
            }
            if (req.phone() != null && !req.phone().isBlank()) {
                stmt.setString(7, req.phone());
            } else {
                stmt.setNull(7, Types.VARCHAR);
            }
            if (req.address() != null && !req.address().isBlank()) {
                stmt.setString(8, req.address());
            } else {
                stmt.setNull(8, Types.VARCHAR);
            }
            stmt.setString(9, req.serviceCategory());
            stmt.setBoolean(10, req.active());

            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1)).orElseThrow();
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error inserting provider", e);
        }
        throw new RepositoryException("Failed to insert provider");
    }

    public ProviderResponse update(Long id, ProviderRequest req) {
        String sql = "UPDATE providers SET business_name = ?, rut = ?, contact_name = ?, email = ?, phone = ?, address = ?, service_category = ?, active = ?, user_id = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, req.businessName());
            stmt.setString(2, req.rut());
            if (req.contactName() != null && !req.contactName().isBlank()) {
                stmt.setString(3, req.contactName());
            } else {
                stmt.setNull(3, Types.VARCHAR);
            }
            if (req.email() != null && !req.email().isBlank()) {
                stmt.setString(4, req.email());
            } else {
                stmt.setNull(4, Types.VARCHAR);
            }
            if (req.phone() != null && !req.phone().isBlank()) {
                stmt.setString(5, req.phone());
            } else {
                stmt.setNull(5, Types.VARCHAR);
            }
            if (req.address() != null && !req.address().isBlank()) {
                stmt.setString(6, req.address());
            } else {
                stmt.setNull(6, Types.VARCHAR);
            }
            stmt.setString(7, req.serviceCategory());
            stmt.setBoolean(8, req.active());
            if (req.userId() != null) {
                stmt.setLong(9, req.userId());
            } else {
                stmt.setNull(9, Types.BIGINT);
            }
            stmt.setLong(10, id);

            stmt.executeUpdate();
            return findById(id).orElseThrow();
        } catch (SQLException e) {
            throw new RepositoryException("Error updating provider", e);
        }
    }

    public void delete(Long id) {
        String sql = "DELETE FROM providers WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error deleting provider", e);
        }
    }

    private ProviderResponse mapRow(ResultSet rs) throws SQLException {
        return new ProviderResponse(
                rs.getLong("id"),
                rs.getLong("building_id"),
                rs.getObject("user_id") != null ? rs.getLong("user_id") : null,
                rs.getString("business_name"),
                rs.getString("rut"),
                rs.getString("contact_name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("address"),
                rs.getString("service_category"),
                rs.getBoolean("active"),
                rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null
        );
    }

    public record ProviderResponse(
            Long id,
            Long buildingId,
            Long userId,
            String businessName,
            String rut,
            String contactName,
            String email,
            String phone,
            String address,
            String serviceCategory,
            Boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
