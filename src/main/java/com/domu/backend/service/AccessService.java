package com.domu.backend.service;

import com.domu.backend.domain.access.AccessLog;
import com.domu.backend.domain.access.DeliveryPackage;
import com.domu.backend.domain.access.ParkingPermit;
import com.domu.backend.domain.access.ParkingSpace;
import com.domu.backend.domain.access.Visit;
import com.domu.backend.domain.access.VisitAuthorization;
import com.domu.backend.infrastructure.persistence.repository.AccessLogRepository;
import com.domu.backend.infrastructure.persistence.repository.DeliveryPackageRepository;
import com.domu.backend.infrastructure.persistence.repository.ParkingPermitRepository;
import com.domu.backend.infrastructure.persistence.repository.ParkingSpaceRepository;
import com.domu.backend.infrastructure.persistence.repository.VisitAuthorizationRepository;
import com.domu.backend.infrastructure.persistence.repository.VisitRepository;

import java.util.List;

public class AccessService {

    private final VisitRepository visitRepository;
    private final DeliveryPackageRepository deliveryPackageRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final ParkingPermitRepository parkingPermitRepository;
    private final VisitAuthorizationRepository visitAuthorizationRepository;
    private final AccessLogRepository accessLogRepository;

    public AccessService(VisitRepository visitRepository,
                         DeliveryPackageRepository deliveryPackageRepository,
                         ParkingSpaceRepository parkingSpaceRepository,
                         ParkingPermitRepository parkingPermitRepository,
                         VisitAuthorizationRepository visitAuthorizationRepository,
                         AccessLogRepository accessLogRepository) {
        this.visitRepository = visitRepository;
        this.deliveryPackageRepository = deliveryPackageRepository;
        this.parkingSpaceRepository = parkingSpaceRepository;
        this.parkingPermitRepository = parkingPermitRepository;
        this.visitAuthorizationRepository = visitAuthorizationRepository;
        this.accessLogRepository = accessLogRepository;
    }

    public Visit registerVisit(Visit visit) {
        return visitRepository.save(visit);
    }

    public List<Visit> listVisits() {
        return visitRepository.findAll();
    }

    public VisitAuthorization authorizeVisit(VisitAuthorization authorization) {
        visitRepository.findById(authorization.visitId())
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found"));
        return visitAuthorizationRepository.save(authorization);
    }

    public List<VisitAuthorization> listAuthorizations() {
        return visitAuthorizationRepository.findAll();
    }

    public DeliveryPackage registerDelivery(DeliveryPackage deliveryPackage) {
        visitRepository.findById(deliveryPackage.visitId())
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found"));
        return deliveryPackageRepository.save(deliveryPackage);
    }

    public List<DeliveryPackage> listDeliveries() {
        return deliveryPackageRepository.findAll();
    }

    public ParkingSpace createParkingSpace(ParkingSpace space) {
        return parkingSpaceRepository.save(space);
    }

    public List<ParkingSpace> listParkingSpaces() {
        return parkingSpaceRepository.findAll();
    }

    public ParkingPermit grantParkingPermit(ParkingPermit permit) {
        parkingSpaceRepository.findById(permit.parkingSpaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Parking space not found"));
        return parkingPermitRepository.save(permit);
    }

    public List<ParkingPermit> listParkingPermits() {
        return parkingPermitRepository.findAll();
    }

    public AccessLog recordAccess(AccessLog accessLog) {
        visitRepository.findById(accessLog.visitId())
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found"));
        return accessLogRepository.save(accessLog);
    }

    public List<AccessLog> listAccessLogs() {
        return accessLogRepository.findAll();
    }
}
