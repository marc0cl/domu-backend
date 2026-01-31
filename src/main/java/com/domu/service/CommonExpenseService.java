package com.domu.service;

import com.domu.database.CommonExpenseRepository;
import com.domu.database.HousingUnitRepository;
import com.domu.domain.core.HousingUnit;
import com.domu.domain.core.User;
import com.domu.domain.finance.CommonCharge;
import com.domu.domain.finance.CommonExpensePeriod;
import com.domu.domain.finance.CommonPayment;
import com.domu.dto.AddCommonChargesRequest;
import com.domu.dto.BuildingSummaryResponse;
import com.domu.dto.CommonChargeReceiptUploadResult;
import com.domu.dto.CommonChargeDetailResponse;
import com.domu.dto.CommonExpenseReceiptDocument;
import com.domu.dto.CommonExpensePeriodDetailResponse;
import com.domu.dto.CommonExpensePeriodResponse;
import com.domu.dto.CommonExpensePeriodSummaryResponse;
import com.domu.dto.CommonExpenseRevisionResponse;
import com.domu.dto.CommonPaymentRequest;
import com.domu.dto.CommonPaymentResponse;
import com.domu.dto.CreateCommonChargeRequest;
import com.domu.dto.CreateCommonExpensePeriodRequest;
import com.domu.dto.UnitCommonExpenseSummaryResponse;
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
    private final HousingUnitRepository housingUnitRepository;
    private final CommonExpenseReceiptStorageService receiptStorageService;

    @Inject
    public CommonExpenseService(CommonExpenseRepository repository,
                                HousingUnitRepository housingUnitRepository,
                                CommonExpenseReceiptStorageService receiptStorageService) {
        this.repository = repository;
        this.housingUnitRepository = housingUnitRepository;
        this.receiptStorageService = receiptStorageService;
    }

    public CommonExpensePeriodResponse createPeriod(CreateCommonExpensePeriodRequest request, User user, Long buildingId) {
        validatePeriodRequest(request);
        List<CommonExpenseRepository.UnitShare> units = repository.findUnitsForBuilding(buildingId);
        if (units.isEmpty()) {
            throw new ValidationException("El edificio no tiene unidades registradas para prorratear");
        }

        BigDecimal reserveAmount = normalizeAmount(request.getReserveAmount());
        LocalDate generatedAt = LocalDate.now();
        CommonExpensePeriod period = repository.insertPeriod(new CommonExpensePeriod(
                null,
                buildingId,
                request.getYear(),
                request.getMonth(),
                generatedAt,
                request.getDueDate(),
                reserveAmount,
                BigDecimal.ZERO,
                "OPEN"
        ), user != null ? user.id() : null);

        List<CommonCharge> charges = buildChargesForPeriod(period.id(), request.getCharges(), reserveAmount, units);
        List<CommonCharge> savedCharges = repository.insertCharges(charges);
        BigDecimal total = savedCharges.stream()
                .map(CommonCharge::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        repository.updatePeriodTotals(period.id(), total, reserveAmount, user != null ? user.id() : null);
        repository.insertRevision(period.id(), user != null ? user.id() : null, "CREATED",
                request.getNote(), "charges=" + savedCharges.size());

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

    public CommonExpensePeriodResponse addCharges(Long periodId, AddCommonChargesRequest request, User user,
                                                  Long buildingId) {
        if (request == null || request.getCharges() == null || request.getCharges().isEmpty()) {
            throw new ValidationException("Debes incluir al menos un cargo");
        }
        CommonExpensePeriod period = repository.findPeriodById(periodId)
                .orElseThrow(() -> new ValidationException("Período no encontrado"));
        if (buildingId != null && !Objects.equals(period.buildingId(), buildingId)) {
            throw new ValidationException("El período no pertenece al edificio seleccionado");
        }
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
        repository.updatePeriodTotals(period.id(), newTotal, period.reserveAmount(), user != null ? user.id() : null);
        repository.insertRevision(period.id(), user != null ? user.id() : null, "UPDATED",
                request.getNote(), "charges_added=" + saved.size());

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

    public List<CommonExpensePeriodSummaryResponse> listPeriodsForBuilding(Long buildingId, Integer fromIndex,
                                                                          Integer toIndex) {
        List<CommonExpenseRepository.PeriodSummaryRow> rows = repository.findPeriodSummaries(buildingId, fromIndex,
                toIndex);
        return rows.stream()
                .map(row -> new CommonExpensePeriodSummaryResponse(
                        row.periodId(),
                        row.year(),
                        row.month(),
                        row.dueDate(),
                        row.reserveAmount(),
                        row.totalAmount(),
                        row.status(),
                        row.chargesCount(),
                        row.revisionsCount(),
                        row.lastRevisionAt()
                ))
                .toList();
    }

    public List<UnitCommonExpenseSummaryResponse> listPeriodsForUser(User user, Long buildingId, Integer fromIndex,
                                                                     Integer toIndex) {
        if (user == null || user.unitId() == null) {
            throw new ValidationException("El usuario no tiene unidad asociada");
        }
        List<CommonExpenseRepository.UnitPeriodSummaryRow> rows = repository.findUnitPeriodSummaries(
                user.unitId(), buildingId, fromIndex, toIndex);
        return rows.stream()
                .map(row -> {
                    BigDecimal paid = row.paidAmount() != null ? row.paidAmount() : BigDecimal.ZERO;
                    BigDecimal total = row.totalAmount() != null ? row.totalAmount() : BigDecimal.ZERO;
                    BigDecimal pending = total.subtract(paid);
                    String status = pending.compareTo(BigDecimal.ZERO) <= 0
                            ? "PAID"
                            : paid.compareTo(BigDecimal.ZERO) > 0 ? "PARTIAL" : "PENDING";
                    return new UnitCommonExpenseSummaryResponse(
                            row.periodId(),
                            row.year(),
                            row.month(),
                            row.dueDate(),
                            total,
                            paid,
                            pending,
                            status
                    );
                })
                .toList();
    }

    public CommonExpensePeriodDetailResponse getPeriodDetailForUser(User user, Long buildingId, Long periodId,
                                                                    BuildingSummaryResponse building,
                                                                    HousingUnit unit) {
        if (user == null || user.unitId() == null) {
            throw new ValidationException("El usuario no tiene unidad asociada");
        }
        if (!repository.periodBelongsToBuilding(periodId, buildingId)) {
            throw new ValidationException("El período no pertenece al edificio seleccionado");
        }
        List<CommonExpenseRepository.ChargeBalanceRow> rows = repository.findChargesForUnitAndPeriod(unit.id(),
                periodId);
        List<CommonChargeDetailResponse> charges = rows.stream()
                .filter(row -> !"CONSTRUCTION".equalsIgnoreCase(row.charge().payerType()))
                .map(row -> new CommonChargeDetailResponse(
                        row.charge().id(),
                        row.charge().description(),
                        row.charge().type(),
                        row.charge().origin(),
                        row.charge().amount(),
                        row.charge().prorateable(),
                        row.charge().receiptFileName(),
                        row.charge().receiptFileId() != null && !row.charge().receiptFileId().isBlank()
                ))
                .toList();

        BigDecimal unitTotal = rows.stream()
                .filter(row -> !"CONSTRUCTION".equalsIgnoreCase(row.charge().payerType()))
                .map(row -> row.charge().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unitPaid = rows.stream()
                .filter(row -> !"CONSTRUCTION".equalsIgnoreCase(row.charge().payerType()))
                .map(row -> row.paidAmount() != null ? row.paidAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unitPending = unitTotal.subtract(unitPaid);

        CommonExpensePeriod period = repository.findPeriodById(periodId)
                .orElseThrow(() -> new ValidationException("Período no encontrado"));
        List<CommonExpenseRevisionResponse> revisions = repository.findRevisions(periodId).stream()
                .map(rev -> new CommonExpenseRevisionResponse(
                        rev.id(),
                        rev.action(),
                        rev.note(),
                        rev.createdByUserId(),
                        rev.createdAt()
                ))
                .toList();

        String unitLabel = buildUnitLabel(unit);

        return new CommonExpensePeriodDetailResponse(
                period.id(),
                period.year(),
                period.month(),
                period.dueDate(),
                period.reserveAmount(),
                period.totalAmount(),
                period.status(),
                unitTotal,
                unitPaid,
                unitPending,
                building != null ? building.name() : null,
                building != null ? building.address() : null,
                building != null ? building.commune() : null,
                building != null ? building.city() : null,
                unitLabel,
                charges,
                revisions
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

    public CommonChargeReceiptUploadResult uploadChargeReceipt(Long chargeId,
                                                               User user,
                                                               Long buildingId,
                                                               BuildingSummaryResponse building,
                                                               CommonExpenseReceiptDocument document) {
        ensureAdmin(user);
        CommonExpenseRepository.ChargeContextRow context = repository.findChargeContext(chargeId)
                .orElseThrow(() -> new ValidationException("Cargo no encontrado"));
        if (buildingId != null && !Objects.equals(context.buildingId(), buildingId)) {
            throw new ValidationException("El cargo no pertenece al edificio seleccionado");
        }
        HousingUnit unit = null;
        if (context.charge().unitId() != null) {
            unit = housingUnitRepository.findById(context.charge().unitId()).orElse(null);
        }
        CommonChargeReceiptUploadResult uploaded = receiptStorageService.uploadReceipt(
                building,
                unit,
                context.year(),
                context.month(),
                chargeId,
                context.charge().description(),
                document
        );
        repository.updateChargeReceipt(chargeId, new CommonExpenseRepository.ReceiptMetadata(
                uploaded.fileId(),
                uploaded.fileName(),
                uploaded.folderId(),
                uploaded.mimeType()
        ));
        repository.insertRevision(context.charge().periodId(), user != null ? user.id() : null, "RECEIPT_UPLOADED",
                "Boleta adjunta", "chargeId=" + chargeId);
        return uploaded;
    }

    public CommonExpenseReceiptStorageService.DownloadedReceipt downloadReceipt(Long chargeId, User user,
                                                                                Long buildingId) {
        CommonExpenseRepository.ChargeContextRow context = repository.findChargeContext(chargeId)
                .orElseThrow(() -> new ValidationException("Cargo no encontrado"));
        if (user != null && user.roleId() != null && user.roleId() == 1L) {
            if (buildingId != null && !Objects.equals(context.buildingId(), buildingId)) {
                throw new UnauthorizedResponse("No puedes acceder a boletas de otro edificio");
            }
        } else {
            if (user == null || user.unitId() == null ||
                    !Objects.equals(context.charge().unitId(), user.unitId())) {
                throw new UnauthorizedResponse("No puedes acceder a boletas de otra unidad");
            }
        }
        if (context.charge().receiptFileId() == null || context.charge().receiptFileId().isBlank()) {
            throw new ValidationException("No hay boleta registrada para este cargo");
        }
        return receiptStorageService.downloadReceipt(context.charge().receiptFileId());
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
            charges.addAll(prorateCharge(periodId, "Fondo de reserva", "Fondo de reserva", reserve, "RESERVE",
                    true, null, units));
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
            return prorateCharge(periodId, request.getDescription(), request.getOrigin(), amount, request.getType(), true,
                    request.getReceiptText(), units);
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
                request.getOrigin(),
                false,
                payerType,
                request.getReceiptText(),
                null,
                null,
                null,
                null,
                null
        ));
    }

    private List<CommonCharge> prorateCharge(
            Long periodId,
            String description,
            String origin,
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
                    origin,
                    prorateable,
                    payerType,
                    receiptText,
                    null,
                    null,
                    null,
                    null,
                    null
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

    private String buildUnitLabel(HousingUnit unit) {
        if (unit == null) {
            return null;
        }
        String number = unit.number() != null ? unit.number().trim() : "";
        String tower = unit.tower() != null && !unit.tower().isBlank() ? unit.tower().trim() : "";
        String floor = unit.floor() != null && !unit.floor().isBlank() ? unit.floor().trim() : "";
        StringBuilder label = new StringBuilder();
        if (!tower.isEmpty()) {
            label.append("Torre ").append(tower).append(" ");
        }
        label.append("Depto ").append(number);
        if (!floor.isEmpty()) {
            label.append(" - Piso ").append(floor);
        }
        return label.toString().trim();
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

    private void ensureAdmin(User user) {
        if (user == null || user.roleId() == null || user.roleId() != 1L) {
            throw new UnauthorizedResponse("Solo administradores pueden realizar esta acción");
        }
    }
}
