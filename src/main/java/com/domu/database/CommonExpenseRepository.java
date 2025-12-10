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

    public boolean periodExists(Long buildingId, int year, int month) {
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

    public CommonExpensePeriod insertPeriod(CommonExpensePeriod period) {
        String sql = "INSERT INTO common_expense_periods (building_id, year, month, generated_at, due_date, reserve_amount, total_amount, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
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

    public void updatePeriodTotals(Long periodId, BigDecimal totalAmount, BigDecimal reserveAmount) {
        String sql = "UPDATE common_expense_periods SET total_amount = ?, reserve_amount = ? WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, totalAmount);
            statement.setBigDecimal(2, reserveAmount);
            statement.setLong(3, periodId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error actualizando totales del período", e);
        }
    }

    public List<CommonCharge> insertCharges(List<CommonCharge> charges) {
        if (charges == null || charges.isEmpty()) {
            return List.of();
        }
        String sql = "INSERT INTO common_charges (period_id, unit_id, description, amount, type, prorateable, payer_type, receipt_text) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
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
                statement.setBoolean(6, charge.prorateable() != null && charge.prorateable());
                statement.setString(7, charge.payerType() != null ? charge.payerType() : "RESIDENT");
                statement.setString(8, charge.receiptText());
                statement.addBatch();
            }
            statement.executeBatch();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                int index = 0;
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
                            original.prorateable(),
                            original.payerType(),
                            original.receiptText()
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
                SELECT c.id AS charge_id, c.period_id, c.unit_id, c.description, c.amount, c.type, c.prorateable,
                       c.payer_type, c.receipt_text,
                       p.year, p.month, p.due_date, p.status,
                       COALESCE(SUM(pay.amount), 0) AS paid
                FROM common_charges c
                JOIN common_expense_periods p ON p.id = c.period_id
                LEFT JOIN common_payments pay ON pay.charge_id = c.id
                WHERE c.id = ?
                GROUP BY c.id, c.period_id, c.unit_id, c.description, c.amount, c.type, c.prorateable,
                         c.payer_type, c.receipt_text, p.year, p.month, p.due_date, p.status
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
                SELECT c.id AS charge_id, c.period_id, c.unit_id, c.description, c.amount, c.type, c.prorateable,
                       c.payer_type, c.receipt_text,
                       p.year, p.month, p.due_date, p.status,
                       COALESCE(SUM(pay.amount), 0) AS paid
                FROM common_charges c
                JOIN common_expense_periods p ON p.id = c.period_id
                LEFT JOIN common_payments pay ON pay.charge_id = c.id
                WHERE c.unit_id = ?
                GROUP BY c.id, c.period_id, c.unit_id, c.description, c.amount, c.type, c.prorateable,
                         c.payer_type, c.receipt_text, p.year, p.month, p.due_date, p.status
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

    private CommonExpensePeriod mapPeriod(ResultSet rs) throws SQLException {
        return new CommonExpensePeriod(
                rs.getLong("id"),
                rs.getLong("building_id"),
                rs.getInt("year"),
                rs.getInt("month"),
                rs.getDate("generated_at").toLocalDate(),
                rs.getDate("due_date").toLocalDate(),
                rs.getBigDecimal("reserve_amount"),
                rs.getBigDecimal("total_amount"),
                rs.getString("status")
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
                rs.getBoolean("prorateable"),
                rs.getString("payer_type"),
                rs.getString("receipt_text")
        );
        int year = rs.getInt("year");
        int month = rs.getInt("month");
        LocalDate dueDate = rs.getDate("due_date").toLocalDate();
        String status = rs.getString("status");
        BigDecimal paid = rs.getBigDecimal("paid");
        return new ChargeBalanceRow(charge, year, month, dueDate, status, paid);
    }

    public record UnitShare(Long unitId, BigDecimal weight, boolean hasUser) {
    }

    public record ChargeBalanceRow(
            CommonCharge charge,
            int year,
            int month,
            LocalDate dueDate,
            String periodStatus,
            BigDecimal paidAmount
    ) {
    }
}

