package com.domu.database;

import com.domu.dto.StaffRequest;
import com.google.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StaffRepository {

    private final DataSource dataSource;

    @Inject
    public StaffRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<StaffResponse> findByBuilding(Long buildingId) {
        String sql = "SELECT * FROM staff WHERE building_id = ? ORDER BY last_name, first_name";
        List<StaffResponse> staffList = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, buildingId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    staffList.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error listing staff by building", e);
        }
        return staffList;
    }

    public List<StaffResponse> findActiveByBuilding(Long buildingId) {
        String sql = "SELECT * FROM staff WHERE building_id = ? AND active = TRUE ORDER BY last_name, first_name";
        List<StaffResponse> staffList = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, buildingId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    staffList.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error listing active staff by building", e);
        }
        return staffList;
    }

    public Optional<StaffResponse> findById(Long id) {
        String sql = "SELECT * FROM staff WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error finding staff", e);
        }
        return Optional.empty();
    }

    public Optional<StaffResponse> findByRut(String rut) {
        String sql = "SELECT * FROM staff WHERE rut = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, rut);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error finding staff by rut", e);
        }
        return Optional.empty();
    }

    public Optional<StaffResponse> findByEmail(String email) {
        String sql = "SELECT * FROM staff WHERE email = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error finding staff by email", e);
        }
        return Optional.empty();
    }

    public StaffResponse insert(StaffRequest req) {
        String sql = "INSERT INTO staff (building_id, first_name, last_name, rut, email, phone, position, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, req.buildingId());
            stmt.setString(2, req.firstName());
            stmt.setString(3, req.lastName());
            stmt.setString(4, req.rut());
            if (req.email() != null && !req.email().isBlank()) {
                stmt.setString(5, req.email());
            } else {
                stmt.setNull(5, Types.VARCHAR);
            }
            if (req.phone() != null && !req.phone().isBlank()) {
                stmt.setString(6, req.phone());
            } else {
                stmt.setNull(6, Types.VARCHAR);
            }
            stmt.setString(7, req.position());
            stmt.setBoolean(8, req.active());
            
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1)).orElseThrow();
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error inserting staff", e);
        }
        throw new RepositoryException("Failed to insert staff");
    }

    public StaffResponse update(Long id, StaffRequest req) {
        String sql = "UPDATE staff SET first_name = ?, last_name = ?, rut = ?, email = ?, phone = ?, position = ?, active = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, req.firstName());
            stmt.setString(2, req.lastName());
            stmt.setString(3, req.rut());
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
            stmt.setString(6, req.position());
            stmt.setBoolean(7, req.active());
            stmt.setLong(8, id);
            
            stmt.executeUpdate();
            return findById(id).orElseThrow();
        } catch (SQLException e) {
            throw new RepositoryException("Error updating staff", e);
        }
    }

    public void delete(Long id) {
        String sql = "DELETE FROM staff WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error deleting staff", e);
        }
    }

    private StaffResponse mapRow(ResultSet rs) throws SQLException {
        return new StaffResponse(
                rs.getLong("id"),
                rs.getLong("building_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("rut"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("position"),
                rs.getBoolean("active"),
                rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null
        );
    }

    public record StaffResponse(
            Long id,
            Long buildingId,
            String firstName,
            String lastName,
            String rut,
            String email,
            String phone,
            String position,
            Boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
