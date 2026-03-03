package com.domu.database;

import com.domu.dto.QuotationRequest;
import com.google.inject.Inject;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class QuotationRepository {

    private final DataSource dataSource;

    @Inject
    public QuotationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<QuotationResponse> findByServiceOrder(Long serviceOrderId) {
        String sql = "SELECT q.*, p.business_name AS provider_name FROM quotations q JOIN providers p ON p.id = q.provider_id WHERE q.service_order_id = ? ORDER BY q.created_at DESC";
        List<QuotationResponse> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, serviceOrderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error listing quotations by service order", e);
        }
        return list;
    }

    public Optional<QuotationResponse> findById(Long id) {
        String sql = "SELECT q.*, p.business_name AS provider_name FROM quotations q JOIN providers p ON p.id = q.provider_id WHERE q.id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error finding quotation", e);
        }
        return Optional.empty();
    }

    public QuotationResponse insert(Long serviceOrderId, Long providerId, QuotationRequest req) {
        String sql = "INSERT INTO quotations (service_order_id, provider_id, amount, description, valid_until, file_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, serviceOrderId);
            stmt.setLong(2, providerId);
            stmt.setBigDecimal(3, req.amount());
            if (req.description() != null && !req.description().isBlank()) {
                stmt.setString(4, req.description());
            } else {
                stmt.setNull(4, Types.VARCHAR);
            }
            if (req.validUntil() != null) {
                stmt.setDate(5, Date.valueOf(req.validUntil()));
            } else {
                stmt.setNull(5, Types.DATE);
            }
            if (req.fileId() != null && !req.fileId().isBlank()) {
                stmt.setString(6, req.fileId());
            } else {
                stmt.setNull(6, Types.VARCHAR);
            }

            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1)).orElseThrow();
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error inserting quotation", e);
        }
        throw new RepositoryException("Failed to insert quotation");
    }

    public QuotationResponse updateStatus(Long id, String status) {
        String sql = "UPDATE quotations SET status = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setLong(2, id);
            stmt.executeUpdate();
            return findById(id).orElseThrow();
        } catch (SQLException e) {
            throw new RepositoryException("Error updating quotation status", e);
        }
    }

    private QuotationResponse mapRow(ResultSet rs) throws SQLException {
        return new QuotationResponse(
                rs.getLong("id"),
                rs.getLong("service_order_id"),
                rs.getLong("provider_id"),
                rs.getString("provider_name"),
                rs.getBigDecimal("amount"),
                rs.getString("description"),
                rs.getDate("valid_until") != null ? rs.getDate("valid_until").toLocalDate() : null,
                rs.getString("file_id"),
                rs.getString("status"),
                rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null
        );
    }

    public record QuotationResponse(
            Long id,
            Long serviceOrderId,
            Long providerId,
            String providerName,
            BigDecimal amount,
            String description,
            LocalDate validUntil,
            String fileId,
            String status,
            LocalDateTime createdAt
    ) {}
}
