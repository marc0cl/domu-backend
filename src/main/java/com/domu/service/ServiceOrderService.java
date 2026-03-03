package com.domu.service;

import com.domu.database.ProviderRepository;
import com.domu.database.QuotationRepository;
import com.domu.database.ServiceOrderRepository;
import com.domu.dto.QuotationRequest;
import com.domu.dto.ServiceOrderRequest;
import com.google.inject.Inject;

import java.util.List;
import java.util.Set;

public class ServiceOrderService {

    private static final Set<String> VALID_STATUSES = Set.of(
        "PENDING", "ACCEPTED", "REJECTED", "IN_PROGRESS", "COMPLETED", "CANCELLED"
    );

    private final ServiceOrderRepository orderRepository;
    private final QuotationRepository quotationRepository;
    private final ProviderRepository providerRepository;

    @Inject
    public ServiceOrderService(ServiceOrderRepository orderRepository,
                                QuotationRepository quotationRepository,
                                ProviderRepository providerRepository) {
        this.orderRepository = orderRepository;
        this.quotationRepository = quotationRepository;
        this.providerRepository = providerRepository;
    }

    public List<ServiceOrderRepository.ServiceOrderResponse> listByBuilding(Long buildingId) {
        return orderRepository.findByBuilding(buildingId);
    }

    public List<ServiceOrderRepository.ServiceOrderResponse> listByProvider(Long providerId) {
        return orderRepository.findByProvider(providerId);
    }

    public ServiceOrderRepository.ServiceOrderResponse findById(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Orden de servicio no encontrada: " + id));
    }

    public ServiceOrderRepository.ServiceOrderResponse create(ServiceOrderRequest request, Long createdBy) {
        providerRepository.findById(request.providerId())
            .orElseThrow(() -> new RuntimeException("Proveedor no encontrado: " + request.providerId()));
        return orderRepository.insert(request, createdBy);
    }

    public ServiceOrderRepository.ServiceOrderResponse update(Long id, ServiceOrderRequest request) {
        orderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Orden de servicio no encontrada: " + id));
        providerRepository.findById(request.providerId())
            .orElseThrow(() -> new RuntimeException("Proveedor no encontrado: " + request.providerId()));
        return orderRepository.update(id, request);
    }

    public ServiceOrderRepository.ServiceOrderResponse updateStatus(Long id, String newStatus, String notes) {
        if (!VALID_STATUSES.contains(newStatus)) {
            throw new RuntimeException("Estado invalido: " + newStatus);
        }

        var order = orderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Orden de servicio no encontrada: " + id));

        validateTransition(order.status(), newStatus);

        return orderRepository.updateStatus(id, newStatus, notes);
    }

    public ServiceOrderRepository.ServiceOrderResponse acceptOrder(Long orderId, Long providerId) {
        var order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Orden de servicio no encontrada: " + orderId));
        if (!order.providerId().equals(providerId)) {
            throw new RuntimeException("Esta orden no esta asignada a este proveedor");
        }
        validateTransition(order.status(), "ACCEPTED");
        return orderRepository.updateStatus(orderId, "ACCEPTED", null);
    }

    public ServiceOrderRepository.ServiceOrderResponse rejectOrder(Long orderId, Long providerId, String notes) {
        var order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Orden de servicio no encontrada: " + orderId));
        if (!order.providerId().equals(providerId)) {
            throw new RuntimeException("Esta orden no esta asignada a este proveedor");
        }
        validateTransition(order.status(), "REJECTED");
        return orderRepository.updateStatus(orderId, "REJECTED", notes);
    }

    public ServiceOrderRepository.ServiceOrderResponse completeOrder(Long orderId, Long providerId, String notes) {
        var order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Orden de servicio no encontrada: " + orderId));
        if (!order.providerId().equals(providerId)) {
            throw new RuntimeException("Esta orden no esta asignada a este proveedor");
        }
        validateTransition(order.status(), "COMPLETED");
        return orderRepository.updateStatus(orderId, "COMPLETED", notes);
    }

    public List<QuotationRepository.QuotationResponse> listQuotations(Long serviceOrderId) {
        return quotationRepository.findByServiceOrder(serviceOrderId);
    }

    public QuotationRepository.QuotationResponse submitQuotation(Long serviceOrderId, Long providerId, QuotationRequest request) {
        var order = orderRepository.findById(serviceOrderId)
            .orElseThrow(() -> new RuntimeException("Orden de servicio no encontrada: " + serviceOrderId));
        if (!order.providerId().equals(providerId)) {
            throw new RuntimeException("Esta orden no esta asignada a este proveedor");
        }
        return quotationRepository.insert(serviceOrderId, providerId, request);
    }

    private void validateTransition(String currentStatus, String newStatus) {
        boolean valid = switch (newStatus) {
            case "ACCEPTED" -> "PENDING".equals(currentStatus);
            case "REJECTED" -> "PENDING".equals(currentStatus);
            case "IN_PROGRESS" -> "ACCEPTED".equals(currentStatus);
            case "COMPLETED" -> "ACCEPTED".equals(currentStatus) || "IN_PROGRESS".equals(currentStatus);
            case "CANCELLED" -> !"COMPLETED".equals(currentStatus) && !"CANCELLED".equals(currentStatus);
            default -> false;
        };
        if (!valid) {
            throw new RuntimeException(
                String.format("Transicion de estado invalida: %s -> %s", currentStatus, newStatus));
        }
    }
}
