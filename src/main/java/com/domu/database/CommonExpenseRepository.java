package com.domu.database;

import com.domu.domain.finance.CommonCharge;
import com.domu.domain.finance.CommonExpensePeriod;
import com.domu.domain.finance.CommonPayment;
import com.google.inject.Inject;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CommonExpenseRepository {

    private final DataSource dataSource;

    @Inject
    public CommonExpenseRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean periodExists(Long buildingId, Integer year, Integer month) {
        String sql = "SELECT 1 FROM common_expense_periods WHERE building_id = ? AND year = ? AND month = ? LIMIT 1";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, buildingId);
            statement.setInt(2, year);
            statement.setInt(3, month);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error verificando período de gasto común", e);
        }
    }

    public Optional<CommonExpensePeriod> findPeriodById(Long periodId) {
        String sql = """
                SELECT id, building_id, year, month, generated_at, due_date, reserve_amount, total_amount, status
                FROM common_expense_periods
                WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, periodId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapPeriod(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo período de gasto común", e);
        }
    }

    public CommonExpensePeriod insertPeriod(CommonExpensePeriod period, Long createdByUserId) {
        String sql = """
                INSERT INTO common_expense_periods
                (building_id, year, month, generated_at, due_date, reserve_amount, total_amount, status, created_by_user_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, period.buildingId());
            statement.setInt(2, period.year());
            statement.setInt(3, period.month());
            statement.setDate(4, Date.valueOf(period.generatedAt()));
            statement.setDate(5, Date.valueOf(period.dueDate()));
            statement.setBigDecimal(6, period.reserveAmount());
            statement.setBigDecimal(7, period.totalAmount());
            statement.setString(8, period.status());
            if (createdByUserId != null) {
                statement.setLong(9, createdByUserId);
            } else {
                statement.setNull(9, java.sql.Types.BIGINT);
            }
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    Long id = keys.getLong(1);
                    return new CommonExpensePeriod(
                            id,
                            period.buildingId(),
                            period.year(),
                            period.month(),
                            period.generatedAt(),
                            period.dueDate(),
                            period.reserveAmount(),
                            period.totalAmount(),
                            period.status()
                    );
                }
            }
            throw new RepositoryException("No se pudo obtener el ID generado para el período");
        } catch (SQLException e) {
            throw new RepositoryException("Error guardando período de gastos comunes", e);
        }
    }

    public void updatePeriodTotals(Long periodId, BigDecimal totalAmount, BigDecimal reserveAmount, Long updatedByUserId) {
        String sql = """
                UPDATE common_expense_periods
                SET total_amount = ?, reserve_amount = ?, updated_by_user_id = ?, updated_at = NOW()
                WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, totalAmount);
            statement.setBigDecimal(2, reserveAmount);
            if (updatedByUserId != null) {
                statement.setLong(3, updatedByUserId);
            } else {
                statement.setNull(3, java.sql.Types.BIGINT);
            }
            statement.setLong(4, periodId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error actualizando totales del período", e);
        }
    }

    public List<CommonCharge> insertCharges(List<CommonCharge> charges) {
        if (charges == null || charges.isEmpty()) {
            return List.of();
        }
        String sql = """
                INSERT INTO common_charges
                (period_id, unit_id, description, amount, type, origin, prorateable, payer_type, receipt_text,
                 receipt_file_id, receipt_file_name, receipt_folder_id, receipt_mime_type, receipt_uploaded_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        List<CommonCharge> persisted = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (CommonCharge charge : charges) {
                statement.setLong(1, charge.periodId());
                if (charge.unitId() != null) {
                    statement.setLong(2, charge.unitId());
                } else {
                    statement.setNull(2, java.sql.Types.BIGINT);
                }
                statement.setString(3, charge.description());
                statement.setBigDecimal(4, charge.amount());
                statement.setString(5, charge.type());
                statement.setString(6, charge.origin());
                statement.setBoolean(7, charge.prorateable() != null && charge.prorateable());
                statement.setString(8, charge.payerType() != null ? charge.payerType() : "RESIDENT");
                statement.setString(9, charge.receiptText());
                statement.setString(10, charge.receiptFileId());
                statement.setString(11, charge.receiptFileName());
                statement.setString(12, charge.receiptFolderId());
                statement.setString(13, charge.receiptMimeType());
                if (charge.receiptUploadedAt() != null) {
                    statement.setTimestamp(14, java.sql.Timestamp.valueOf(charge.receiptUploadedAt()));
                } else {
                    statement.setNull(14, java.sql.Types.TIMESTAMP);
                }
                statement.addBatch();
            }
            statement.executeBatch();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                Integer index = 0;
                while (keys.next() && index < charges.size()) {
                    Long id = keys.getLong(1);
                    CommonCharge original = charges.get(index);
                    persisted.add(new CommonCharge(
                            id,
                            original.periodId(),
                            original.unitId(),
                            original.description(),
                            original.amount(),
                            original.type(),
                            original.origin(),
                            original.prorateable(),
                            original.payerType(),
                            original.receiptText(),
                            original.receiptFileId(),
                            original.receiptFileName(),
                            original.receiptFolderId(),
                            original.receiptMimeType(),
                            original.receiptUploadedAt()
                    ));
                    index++;
                }
            }
            return persisted;
        } catch (SQLException e) {
            throw new RepositoryException("Error guardando cargos de gastos comunes", e);
        }
    }

    public List<UnitShare> findUnitsForBuilding(Long buildingId) {
        String sql = """
                SELECT hu.id, hu.aliquot_percentage,
                       EXISTS (SELECT 1 FROM users u WHERE u.unit_id = hu.id AND u.status = 'ACTIVE') AS has_user
                FROM housing_units hu
                WHERE hu.building_id = ?
                ORDER BY hu.id
                """;
        List<UnitShare> units = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, buildingId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Long unitId = rs.getLong("id");
                    BigDecimal weight = rs.getBigDecimal("aliquot_percentage");
                    boolean hasUser = rs.getBoolean("has_user");
                    units.add(new UnitShare(unitId, weight, hasUser));
                }
            }
            return units;
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo unidades del edificio", e);
        }
    }

    public Optional<ChargeBalanceRow> findChargeBalance(Long chargeId) {
        String sql = """
                SELECT c.id AS charge_id, c.period_id, c.unit_id, c.description, c.amount, c.type, c.origin,
                       c.prorateable, c.payer_type, c.receipt_text,
                       c.receipt_file_id, c.receipt_file_name, c.receipt_folder_id, c.receipt_mime_type,
                       c.receipt_uploaded_at,
                       p.year, p.month, p.due_date, p.status,
                       COALESCE(SUM(pay.amount), 0) AS paid
                FROM common_charges c
                JOIN common_expense_periods p ON p.id = c.period_id
                LEFT JOIN common_payments pay ON pay.charge_id = c.id
                WHERE c.id = ?
                GROUP BY c.id, c.period_id, c.unit_id, c.description, c.amount, c.type, c.origin, c.prorateable,
                         c.payer_type, c.receipt_text, c.receipt_file_id, c.receipt_file_name, c.receipt_folder_id,
                         c.receipt_mime_type, c.receipt_uploaded_at, p.year, p.month, p.due_date, p.status
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, chargeId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapChargeBalance(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo saldo del cargo", e);
        }
    }

    public List<ChargeBalanceRow> findChargesForUnit(Long unitId) {
        String sql = """
                SELECT c.id AS charge_id, c.period_id, c.unit_id, c.description, c.amount, c.type, c.origin,
                       c.prorateable, c.payer_type, c.receipt_text,
                       c.receipt_file_id, c.receipt_file_name, c.receipt_folder_id, c.receipt_mime_type,
                       c.receipt_uploaded_at,
                       p.year, p.month, p.due_date, p.status,
                       COALESCE(SUM(pay.amount), 0) AS paid
                FROM common_charges c
                JOIN common_expense_periods p ON p.id = c.period_id
                LEFT JOIN common_payments pay ON pay.charge_id = c.id
                WHERE c.unit_id = ?
                GROUP BY c.id, c.period_id, c.unit_id, c.description, c.amount, c.type, c.origin, c.prorateable,
                         c.payer_type, c.receipt_text, c.receipt_file_id, c.receipt_file_name, c.receipt_folder_id,
                         c.receipt_mime_type, c.receipt_uploaded_at, p.year, p.month, p.due_date, p.status
                ORDER BY p.year DESC, p.month DESC, c.id DESC
                """;
        List<ChargeBalanceRow> rows = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, unitId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapChargeBalance(rs));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo cargos por unidad", e);
        }
    }

    public List<ChargeBalanceRow> findChargesForUnitAndPeriod(Long unitId, Long periodId) {
        String sql = """
                SELECT c.id AS charge_id, c.period_id, c.unit_id, c.description, c.amount, c.type, c.origin,
                       c.prorateable, c.payer_type, c.receipt_text,
                       c.receipt_file_id, c.receipt_file_name, c.receipt_folder_id, c.receipt_mime_type,
                       c.receipt_uploaded_at,
                       p.year, p.month, p.due_date, p.status,
                       COALESCE(SUM(pay.amount), 0) AS paid
                FROM common_charges c
                JOIN common_expense_periods p ON p.id = c.period_id
                LEFT JOIN common_payments pay ON pay.charge_id = c.id
                WHERE c.unit_id = ? AND c.period_id = ?
                GROUP BY c.id, c.period_id, c.unit_id, c.description, c.amount, c.type, c.origin, c.prorateable,
                         c.payer_type, c.receipt_text, c.receipt_file_id, c.receipt_file_name, c.receipt_folder_id,
                         c.receipt_mime_type, c.receipt_uploaded_at, p.year, p.month, p.due_date, p.status
                ORDER BY c.id
                """;
        List<ChargeBalanceRow> rows = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, unitId);
            statement.setLong(2, periodId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapChargeBalance(rs));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo cargos por período y unidad", e);
        }
    }

    public CommonPayment insertPayment(CommonPayment payment) {
        String sql = "INSERT INTO common_payments (unit_id, charge_id, user_id, issued_at, amount, payment_method, reference, receipt_text, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, payment.unitId());
            statement.setLong(2, payment.chargeId());
            if (payment.userId() != null) {
                statement.setLong(3, payment.userId());
            } else {
                statement.setNull(3, java.sql.Types.BIGINT);
            }
            statement.setDate(4, Date.valueOf(payment.issuedAt()));
            statement.setBigDecimal(5, payment.amount());
            statement.setString(6, payment.paymentMethod());
            statement.setString(7, payment.reference());
            statement.setString(8, payment.receiptText());
            statement.setString(9, payment.status());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    Long id = keys.getLong(1);
                    return new CommonPayment(
                            id,
                            payment.unitId(),
                            payment.chargeId(),
                            payment.userId(),
                            payment.issuedAt(),
                            payment.amount(),
                            payment.paymentMethod(),
                            payment.reference(),
                            payment.status(),
                            payment.receiptText()
                    );
                }
            }
            throw new RepositoryException("No se pudo obtener el ID generado para el pago");
        } catch (SQLException e) {
            throw new RepositoryException("Error guardando pago de gasto común", e);
        }
    }

    public Optional<CommonPayment> findPaymentById(Long paymentId) {
        String sql = "SELECT id, unit_id, charge_id, user_id, issued_at, amount, payment_method, reference, status, receipt_text FROM common_payments WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, paymentId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new CommonPayment(
                            rs.getLong("id"),
                            rs.getLong("unit_id"),
                            rs.getLong("charge_id"),
                            rs.getObject("user_id", Long.class),
                            rs.getDate("issued_at").toLocalDate(),
                            rs.getBigDecimal("amount"),
                            rs.getString("payment_method"),
                            rs.getString("reference"),
                            rs.getString("status"),
                            rs.getString("receipt_text")
                    ));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error buscando pago por ID", e);
        }
    }

    public boolean periodBelongsToBuilding(Long periodId, Long buildingId) {
        String sql = "SELECT 1 FROM common_expense_periods WHERE id = ? AND building_id = ? LIMIT 1";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, periodId);
            statement.setLong(2, buildingId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error validando período con edificio", e);
        }
    }

    public List<PeriodSummaryRow> findPeriodSummaries(Long buildingId, Integer fromIndex, Integer toIndex) {
        String sql = """
                SELECT p.id, p.year, p.month, p.due_date, p.reserve_amount, p.total_amount, p.status,
                       COUNT(DISTINCT c.id) AS charges_count,
                       COUNT(DISTINCT r.id) AS revisions_count,
                       MAX(r.created_at) AS last_revision_at
                FROM common_expense_periods p
                LEFT JOIN common_charges c ON c.period_id = p.id
                LEFT JOIN common_expense_revisions r ON r.period_id = p.id
                WHERE p.building_id = ?
                  AND (? IS NULL OR (p.year * 100 + p.month) >= ?)
                  AND (? IS NULL OR (p.year * 100 + p.month) <= ?)
                GROUP BY p.id
                ORDER BY p.year DESC, p.month DESC
                """;
        List<PeriodSummaryRow> rows = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, buildingId);
            if (fromIndex != null) {
                statement.setInt(2, fromIndex);
                statement.setInt(3, fromIndex);
            } else {
                statement.setNull(2, java.sql.Types.INTEGER);
                statement.setNull(3, java.sql.Types.INTEGER);
            }
            if (toIndex != null) {
                statement.setInt(4, toIndex);
                statement.setInt(5, toIndex);
            } else {
                statement.setNull(4, java.sql.Types.INTEGER);
                statement.setNull(5, java.sql.Types.INTEGER);
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new PeriodSummaryRow(
                            rs.getLong("id"),
                            (Integer) rs.getObject("year"),
                            (Integer) rs.getObject("month"),
                            rs.getDate("due_date").toLocalDate(),
                            rs.getBigDecimal("reserve_amount"),
                            rs.getBigDecimal("total_amount"),
                            rs.getString("status"),
                            rs.getInt("charges_count"),
                            rs.getInt("revisions_count"),
                            rs.getTimestamp("last_revision_at") != null
                                    ? rs.getTimestamp("last_revision_at").toLocalDateTime()
                                    : null
                    ));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo períodos de gastos comunes", e);
        }
    }

    public List<UnitPeriodSummaryRow> findUnitPeriodSummaries(Long unitId, Long buildingId, Integer fromIndex,
                                                               Integer toIndex) {
        String sql = """
                SELECT p.id, p.year, p.month, p.due_date, p.status,
                       (SELECT COALESCE(SUM(c2.amount), 0)
                        FROM common_charges c2
                        WHERE c2.period_id = p.id AND c2.unit_id = ?) AS total_amount,
                       (SELECT COALESCE(SUM(pay.amount), 0)
                        FROM common_payments pay
                        JOIN common_charges c3 ON c3.id = pay.charge_id
                        WHERE c3.period_id = p.id AND c3.unit_id = ?) AS paid_amount
                FROM common_expense_periods p
                WHERE p.building_id = ?
                  AND (? IS NULL OR (p.year * 100 + p.month) >= ?)
                  AND (? IS NULL OR (p.year * 100 + p.month) <= ?)
                ORDER BY p.year DESC, p.month DESC
                """;
        List<UnitPeriodSummaryRow> rows = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, unitId);
            statement.setLong(2, unitId);
            statement.setLong(3, buildingId);
            if (fromIndex != null) {
                statement.setInt(4, fromIndex);
                statement.setInt(5, fromIndex);
            } else {
                statement.setNull(4, java.sql.Types.INTEGER);
                statement.setNull(5, java.sql.Types.INTEGER);
            }
            if (toIndex != null) {
                statement.setInt(6, toIndex);
                statement.setInt(7, toIndex);
            } else {
                statement.setNull(6, java.sql.Types.INTEGER);
                statement.setNull(7, java.sql.Types.INTEGER);
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new UnitPeriodSummaryRow(
                            rs.getLong("id"),
                            (Integer) rs.getObject("year"),
                            (Integer) rs.getObject("month"),
                            rs.getDate("due_date").toLocalDate(),
                            rs.getBigDecimal("total_amount"),
                            rs.getBigDecimal("paid_amount"),
                            rs.getString("status")
                    ));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo resumen de gastos comunes", e);
        }
    }

    public List<CommonCharge> findChargesForPeriodAndUnit(Long periodId, Long unitId) {
        String sql = """
                SELECT id AS charge_id, period_id, unit_id, description, amount, type, origin, prorateable, payer_type,
                       receipt_text, receipt_file_id, receipt_file_name, receipt_folder_id, receipt_mime_type,
                       receipt_uploaded_at
                FROM common_charges
                WHERE period_id = ? AND unit_id = ?
                ORDER BY id
                """;
        List<CommonCharge> charges = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, periodId);
            statement.setLong(2, unitId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    charges.add(mapCharge(rs));
                }
            }
            return charges;
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo cargos por período/unidad", e);
        }
    }

    public List<CommonCharge> findChargesForPeriod(Long periodId) {
        String sql = """
                SELECT id AS charge_id, period_id, unit_id, description, amount, type, origin, prorateable, payer_type,
                       receipt_text, receipt_file_id, receipt_file_name, receipt_folder_id, receipt_mime_type,
                       receipt_uploaded_at
                FROM common_charges
                WHERE period_id = ?
                ORDER BY id
                """;
        List<CommonCharge> charges = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, periodId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    charges.add(mapCharge(rs));
                }
            }
            return charges;
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo cargos del período", e);
        }
    }

    public void updateChargeReceipt(Long chargeId, ReceiptMetadata metadata) {
        String sql = """
                UPDATE common_charges
                SET receipt_file_id = ?, receipt_file_name = ?, receipt_folder_id = ?, receipt_mime_type = ?,
                    receipt_uploaded_at = NOW()
                WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, metadata.fileId());
            statement.setString(2, metadata.fileName());
            statement.setString(3, metadata.folderId());
            statement.setString(4, metadata.mimeType());
            statement.setLong(5, chargeId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error actualizando boleta del cargo", e);
        }
    }

    public Optional<ChargeContextRow> findChargeContext(Long chargeId) {
        String sql = """
                SELECT c.id AS charge_id, c.period_id, c.unit_id, c.description, c.amount, c.type, c.origin,
                       c.prorateable, c.payer_type, c.receipt_text,
                       c.receipt_file_id, c.receipt_file_name, c.receipt_folder_id, c.receipt_mime_type,
                       c.receipt_uploaded_at,
                       p.building_id, p.year, p.month, p.due_date
                FROM common_charges c
                JOIN common_expense_periods p ON p.id = c.period_id
                WHERE c.id = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, chargeId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    CommonCharge charge = mapCharge(rs);
                    Date dueDate = rs.getDate("due_date");
                    return Optional.of(new ChargeContextRow(
                            charge,
                            rs.getLong("building_id"),
                            (Integer) rs.getObject("year"),
                            (Integer) rs.getObject("month"),
                            dueDate != null ? dueDate.toLocalDate() : null
                    ));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo contexto del cargo", e);
        }
    }

    public RevisionRow insertRevision(Long periodId, Long userId, String action, String note, String changesJson) {
        String sql = """
                INSERT INTO common_expense_revisions
                (period_id, created_by_user_id, action, note, changes_json)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, periodId);
            if (userId != null) {
                statement.setLong(2, userId);
            } else {
                statement.setNull(2, java.sql.Types.BIGINT);
            }
            statement.setString(3, action);
            statement.setString(4, note);
            statement.setString(5, changesJson);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    Long id = keys.getLong(1);
                    return new RevisionRow(id, periodId, userId, action, note, changesJson,
                            java.time.LocalDateTime.now());
                }
            }
            throw new RepositoryException("No se pudo obtener el ID de la revisión");
        } catch (SQLException e) {
            throw new RepositoryException("Error guardando revisión de gasto común", e);
        }
    }

    public List<RevisionRow> findRevisions(Long periodId) {
        String sql = """
                SELECT id, period_id, created_by_user_id, action, note, changes_json, created_at
                FROM common_expense_revisions
                WHERE period_id = ?
                ORDER BY created_at DESC
                """;
        List<RevisionRow> rows = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, periodId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new RevisionRow(
                            rs.getLong("id"),
                            rs.getLong("period_id"),
                            (Long) rs.getObject("created_by_user_id"),
                            rs.getString("action"),
                            rs.getString("note"),
                            rs.getString("changes_json"),
                            rs.getTimestamp("created_at") != null
                                    ? rs.getTimestamp("created_at").toLocalDateTime()
                                    : null
                    ));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo revisiones del período", e);
        }
    }

    public record PaymentDetailRow(
            Long id,
            Long chargeId,
            String chargeDescription,
            java.math.BigDecimal amount,
            String paymentMethod,
            String reference,
            String status,
            java.time.LocalDate issuedAt
    ) {
    }

    public List<PaymentDetailRow> findPaymentsForUnitAndPeriod(Long unitId, Long periodId) {
        String sql = """
                SELECT pay.id, pay.charge_id, c.description AS charge_description,
                       pay.amount, pay.payment_method, pay.reference, pay.status, pay.issued_at
                FROM common_payments pay
                JOIN common_charges c ON c.id = pay.charge_id
                WHERE c.unit_id = ? AND c.period_id = ?
                ORDER BY pay.issued_at DESC
                """;
        List<PaymentDetailRow> rows = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, unitId);
            statement.setLong(2, periodId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new PaymentDetailRow(
                            rs.getLong("id"),
                            rs.getLong("charge_id"),
                            rs.getString("charge_description"),
                            rs.getBigDecimal("amount"),
                            rs.getString("payment_method"),
                            rs.getString("reference"),
                            rs.getString("status"),
                            rs.getDate("issued_at") != null ? rs.getDate("issued_at").toLocalDate() : null
                    ));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo pagos por período y unidad", e);
        }
    }

    private CommonExpensePeriod mapPeriod(ResultSet rs) throws SQLException {
        return new CommonExpensePeriod(
                rs.getLong("id"),
                rs.getLong("building_id"),
                (Integer) rs.getObject("year"),
                (Integer) rs.getObject("month"),
                rs.getDate("generated_at").toLocalDate(),
                rs.getDate("due_date").toLocalDate(),
                rs.getBigDecimal("reserve_amount"),
                rs.getBigDecimal("total_amount"),
                rs.getString("status")
        );
    }

    private CommonCharge mapCharge(ResultSet rs) throws SQLException {
        return new CommonCharge(
                rs.getLong("charge_id"),
                rs.getLong("period_id"),
                (Long) rs.getObject("unit_id"),
                rs.getString("description"),
                rs.getBigDecimal("amount"),
                rs.getString("type"),
                rs.getString("origin"),
                rs.getBoolean("prorateable"),
                rs.getString("payer_type"),
                rs.getString("receipt_text"),
                rs.getString("receipt_file_id"),
                rs.getString("receipt_file_name"),
                rs.getString("receipt_folder_id"),
                rs.getString("receipt_mime_type"),
                rs.getTimestamp("receipt_uploaded_at") != null
                        ? rs.getTimestamp("receipt_uploaded_at").toLocalDateTime()
                        : null
        );
    }

    private ChargeBalanceRow mapChargeBalance(ResultSet rs) throws SQLException {
        CommonCharge charge = new CommonCharge(
                rs.getLong("charge_id"),
                rs.getLong("period_id"),
                (Long) rs.getObject("unit_id"),
                rs.getString("description"),
                rs.getBigDecimal("amount"),
                rs.getString("type"),
                rs.getString("origin"),
                rs.getBoolean("prorateable"),
                rs.getString("payer_type"),
                rs.getString("receipt_text"),
                rs.getString("receipt_file_id"),
                rs.getString("receipt_file_name"),
                rs.getString("receipt_folder_id"),
                rs.getString("receipt_mime_type"),
                rs.getTimestamp("receipt_uploaded_at") != null
                        ? rs.getTimestamp("receipt_uploaded_at").toLocalDateTime()
                        : null
        );
        Integer year = (Integer) rs.getObject("year");
        Integer month = (Integer) rs.getObject("month");
        LocalDate dueDate = rs.getDate("due_date").toLocalDate();
        String status = rs.getString("status");
        BigDecimal paid = rs.getBigDecimal("paid");
        return new ChargeBalanceRow(charge, year, month, dueDate, status, paid);
    }

    public record UnitShare(Long unitId, BigDecimal weight, boolean hasUser) {
    }

    public record PeriodSummaryRow(
            Long periodId,
            Integer year,
            Integer month,
            LocalDate dueDate,
            BigDecimal reserveAmount,
            BigDecimal totalAmount,
            String status,
            Integer chargesCount,
            Integer revisionsCount,
            java.time.LocalDateTime lastRevisionAt
    ) {
    }

    public record UnitPeriodSummaryRow(
            Long periodId,
            Integer year,
            Integer month,
            LocalDate dueDate,
            BigDecimal totalAmount,
            BigDecimal paidAmount,
            String status
    ) {
    }

    public record ReceiptMetadata(String fileId, String fileName, String folderId, String mimeType) {
    }

    public record RevisionRow(
            Long id,
            Long periodId,
            Long createdByUserId,
            String action,
            String note,
            String changesJson,
            java.time.LocalDateTime createdAt
    ) {
    }

    public record ChargeBalanceRow(
            CommonCharge charge,
            Integer year,
            Integer month,
            LocalDate dueDate,
            String periodStatus,
            BigDecimal paidAmount
    ) {
    }

    public record ChargeContextRow(
            CommonCharge charge,
            Long buildingId,
            Integer year,
            Integer month,
            LocalDate dueDate
    ) {
    }
}
