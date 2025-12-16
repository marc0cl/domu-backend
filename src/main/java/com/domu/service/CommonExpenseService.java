package com.domu.service;

import com.domu.database.CommonExpenseRepository;
import com.domu.domain.core.User;
import com.domu.domain.finance.CommonCharge;
import com.domu.domain.finance.CommonExpensePeriod;
import com.domu.domain.finance.CommonPayment;
import com.domu.dto.AddCommonChargesRequest;
import com.domu.dto.CommonExpensePeriodResponse;
import com.domu.dto.CommonPaymentRequest;
import com.domu.dto.CommonPaymentResponse;
import com.domu.dto.CreateCommonChargeRequest;
import com.domu.dto.CreateCommonExpensePeriodRequest;
import com.domu.dto.UnitChargeResponse;
import com.google.inject.Inject;

import io.javalin.http.UnauthorizedResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CommonExpenseService {

    private final CommonExpenseRepository repository;

    @Inject
    public CommonExpenseService(CommonExpenseRepository repository) {
        this.repository = repository;
    }

    public CommonExpensePeriodResponse createPeriod(CreateCommonExpensePeriodRequest request) {
        validatePeriodRequest(request);
        List<CommonExpenseRepository.UnitShare> units = repository.findUnitsForBuilding(request.getBuildingId());
        if (units.isEmpty()) {
            throw new ValidationException("El edificio no tiene unidades registradas para prorratear");
        }

        BigDecimal reserveAmount = normalizeAmount(request.getReserveAmount());
        LocalDate generatedAt = LocalDate.now();
        CommonExpensePeriod period = repository.insertPeriod(new CommonExpensePeriod(
                null,
                request.getBuildingId(),
                request.getYear(),
                request.getMonth(),
                generatedAt,
                request.getDueDate(),
                reserveAmount,
                BigDecimal.ZERO,
                "OPEN"
        ));

        List<CommonCharge> charges = buildChargesForPeriod(period.id(), request.getCharges(), reserveAmount, units);
        List<CommonCharge> savedCharges = repository.insertCharges(charges);
        BigDecimal total = savedCharges.stream()
                .map(CommonCharge::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        repository.updatePeriodTotals(period.id(), total, reserveAmount);

        return new CommonExpensePeriodResponse(
                period.id(),
                period.buildingId(),
                period.year(),
                period.month(),
                period.dueDate(),
                reserveAmount,
                total,
                period.status(),
                savedCharges.size()
        );
    }

    public CommonExpensePeriodResponse addCharges(Long periodId, AddCommonChargesRequest request) {
        if (request == null || request.getCharges() == null || request.getCharges().isEmpty()) {
            throw new ValidationException("Debes incluir al menos un cargo");
        }
        CommonExpensePeriod period = repository.findPeriodById(periodId)
                .orElseThrow(() -> new ValidationException("Período no encontrado"));
        List<CommonExpenseRepository.UnitShare> units = repository.findUnitsForBuilding(period.buildingId());
        if (units.isEmpty()) {
            throw new ValidationException("El edificio no tiene unidades registradas para prorratear");
        }

        List<CommonCharge> charges = buildChargesForPeriod(period.id(), request.getCharges(), BigDecimal.ZERO, units);
        List<CommonCharge> saved = repository.insertCharges(charges);
        BigDecimal added = saved.stream()
                .map(CommonCharge::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal newTotal = period.totalAmount().add(added);
        repository.updatePeriodTotals(period.id(), newTotal, period.reserveAmount());

        return new CommonExpensePeriodResponse(
                period.id(),
                period.buildingId(),
                period.year(),
                period.month(),
                period.dueDate(),
                period.reserveAmount(),
                newTotal,
                period.status(),
                saved.size()
        );
    }

    public List<UnitChargeResponse> getChargesForUser(User user) {
        if (user.unitId() == null) {
            throw new ValidationException("El usuario no tiene una unidad asociada");
        }
        List<CommonExpenseRepository.ChargeBalanceRow> rows = repository.findChargesForUnit(user.unitId());
        return rows.stream()
                .filter(row -> !"CONSTRUCTION".equalsIgnoreCase(row.charge().payerType()))
                .map(this::toUnitChargeResponse)
                .toList();
    }

    public CommonPaymentResponse payCharge(Long chargeId, User user, CommonPaymentRequest request) {
        if (request == null || request.getAmount() == null) {
            throw new ValidationException("El monto es obligatorio");
        }
        BigDecimal amount = normalizeAmount(request.getAmount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("El monto debe ser mayor a cero");
        }

        CommonExpenseRepository.ChargeBalanceRow balanceRow = repository.findChargeBalance(chargeId)
                .orElseThrow(() -> new ValidationException("Cargo no encontrado"));

        if ("RESIDENT".equalsIgnoreCase(balanceRow.charge().payerType())) {
            if (user.unitId() == null || !Objects.equals(user.unitId(), balanceRow.charge().unitId())) {
                throw new UnauthorizedResponse("No puedes pagar cargos de otra unidad");
            }
        }

        BigDecimal paid = balanceRow.paidAmount() != null ? balanceRow.paidAmount() : BigDecimal.ZERO;
        BigDecimal pending = balanceRow.charge().amount().subtract(paid);
        if (pending.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("El cargo ya está pagado");
        }
        if (amount.compareTo(pending) > 0) {
            throw new ValidationException("El monto excede el saldo pendiente");
        }

        CommonPayment payment = new CommonPayment(
                null,
                balanceRow.charge().unitId(),
                balanceRow.charge().id(),
                user.id(),
                LocalDate.now(),
                amount,
                request.getPaymentMethod(),
                request.getReference(),
                "CONFIRMED",
                request.getReceiptText()
        );
        CommonPayment saved = repository.insertPayment(payment);

        BigDecimal newPending = pending.subtract(saved.amount());
        CommonPaymentResponse.PaymentLine line = new CommonPaymentResponse.PaymentLine(
                saved.amount(),
                balanceRow.charge().description(),
                saved.issuedAt(),
                saved.receiptText()
        );

        return new CommonPaymentResponse(saved.chargeId(), newPending, List.of(line));
    }

    private List<CommonCharge> buildChargesForPeriod(
            Long periodId,
            List<CreateCommonChargeRequest> chargeRequests,
            BigDecimal reserveAmount,
            List<CommonExpenseRepository.UnitShare> units
    ) {
        List<CommonCharge> charges = new ArrayList<>();
        BigDecimal reserve = reserveAmount != null ? reserveAmount : BigDecimal.ZERO;
        if (reserve.compareTo(BigDecimal.ZERO) > 0) {
            charges.addAll(prorateCharge(periodId, "Fondo de reserva", reserve, "RESERVE", true, null, units));
        }
        if (chargeRequests != null) {
            for (CreateCommonChargeRequest req : chargeRequests) {
                charges.addAll(expandCharge(periodId, req, units));
            }
        }
        return charges;
    }

    private List<CommonCharge> expandCharge(
            Long periodId,
            CreateCommonChargeRequest request,
            List<CommonExpenseRepository.UnitShare> units
    ) {
        validateChargeRequest(request);
        BigDecimal amount = normalizeAmount(request.getAmount());
        if (Boolean.TRUE.equals(request.getProrateable())) {
            return prorateCharge(periodId, request.getDescription(), amount, request.getType(), true, request.getReceiptText(), units);
        }

        Long unitId = request.getUnitId();
        if (unitId == null) {
            throw new ValidationException("unitId es obligatorio cuando el cargo no es prorrateable");
        }

        CommonExpenseRepository.UnitShare unit = units.stream()
                .filter(u -> u.unitId().equals(unitId))
                .findFirst()
                .orElseThrow(() -> new ValidationException("La unidad no pertenece al edificio del período"));
        String payerType = unit.hasUser() ? "RESIDENT" : "CONSTRUCTION";

        return List.of(new CommonCharge(
                null,
                periodId,
                unitId,
                request.getDescription().trim(),
                amount,
                request.getType(),
                false,
                payerType,
                request.getReceiptText()
        ));
    }

    private List<CommonCharge> prorateCharge(
            Long periodId,
            String description,
            BigDecimal amount,
            String type,
            boolean prorateable,
            String receiptText,
            List<CommonExpenseRepository.UnitShare> units
    ) {
        List<CommonCharge> charges = new ArrayList<>();
        BigDecimal totalWeight = units.stream()
                .map(unit -> unit.weight() != null && unit.weight().compareTo(BigDecimal.ZERO) > 0 ? unit.weight() : BigDecimal.ONE)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWeight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("No hay coeficientes válidos para prorratear");
        }

        BigDecimal remaining = amount;
        for (Integer i = 0; i < units.size(); i++) {
            CommonExpenseRepository.UnitShare unit = units.get(i);
            BigDecimal weight = unit.weight() != null && unit.weight().compareTo(BigDecimal.ZERO) > 0 ? unit.weight() : BigDecimal.ONE;
            BigDecimal share = amount.multiply(weight).divide(totalWeight, 2, RoundingMode.HALF_UP);
            if (i.equals(units.size() - 1)) {
                share = remaining;
            }
            remaining = remaining.subtract(share);
            String payerType = unit.hasUser() ? "RESIDENT" : "CONSTRUCTION";
            charges.add(new CommonCharge(
                    null,
                    periodId,
                    unit.unitId(),
                    description.trim(),
                    share,
                    type,
                    prorateable,
                    payerType,
                    receiptText
            ));
        }
        return charges;
    }

    private void validatePeriodRequest(CreateCommonExpensePeriodRequest request) {
        if (request == null) {
            throw new ValidationException("El cuerpo de la solicitud es obligatorio");
        }
        if (request.getBuildingId() == null) {
            throw new ValidationException("buildingId es obligatorio");
        }
        if (request.getYear() == null || request.getYear() < 2000 || request.getYear() > 2100) {
            throw new ValidationException("year inválido");
        }
        if (request.getMonth() == null || request.getMonth() < 1 || request.getMonth() > 12) {
            throw new ValidationException("month inválido");
        }
        if (request.getDueDate() == null) {
            throw new ValidationException("dueDate es obligatorio");
        }
        if (repository.periodExists(request.getBuildingId(), request.getYear(), request.getMonth())) {
            throw new ValidationException("Ya existe un período para ese edificio y mes");
        }
    }

    private void validateChargeRequest(CreateCommonChargeRequest request) {
        if (request == null) {
            throw new ValidationException("El cargo es obligatorio");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new ValidationException("description es obligatorio");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("amount debe ser mayor a cero");
        }
        if (request.getType() == null || request.getType().isBlank()) {
            throw new ValidationException("type es obligatorio");
        }
    }

    private BigDecimal normalizeAmount(BigDecimal raw) {
        if (raw == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return raw.setScale(2, RoundingMode.HALF_UP);
    }

    private UnitChargeResponse toUnitChargeResponse(CommonExpenseRepository.ChargeBalanceRow row) {
        BigDecimal paid = row.paidAmount() != null ? row.paidAmount() : BigDecimal.ZERO;
        BigDecimal pending = row.charge().amount().subtract(paid);
        String status = pending.compareTo(BigDecimal.ZERO) <= 0
                ? "PAID"
                : paid.compareTo(BigDecimal.ZERO) > 0 ? "PARTIAL" : "PENDING";

        return new UnitChargeResponse(
                row.charge().id(),
                row.charge().periodId(),
                row.year(),
                row.month(),
                row.charge().description(),
                row.charge().amount(),
                paid,
                pending,
                row.dueDate(),
                status,
                row.charge().type(),
                row.charge().payerType(),
                row.charge().receiptText()
        );
    }
}

