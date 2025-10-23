package com.domu.backend.services;

import com.domu.backend.domain.Community;
import com.domu.backend.domain.Delivery;
import com.domu.backend.domain.ParkingPermit;
import com.domu.backend.domain.ParkingSpace;
import com.domu.backend.domain.Resident;
import com.domu.backend.domain.Unit;
import com.domu.backend.domain.Visit;
import com.domu.backend.dto.DeliveryRequest;
import com.domu.backend.dto.ParkingPermitRequest;
import com.domu.backend.dto.ParkingSpaceRequest;
import com.domu.backend.dto.VisitRequest;
import com.domu.backend.exceptions.ResourceNotFoundException;
import com.domu.backend.repository.CommunityRepository;
import com.domu.backend.repository.DeliveryRepository;
import com.domu.backend.repository.ParkingPermitRepository;
import com.domu.backend.repository.ParkingSpaceRepository;
import com.domu.backend.repository.ResidentRepository;
import com.domu.backend.repository.UnitRepository;
import com.domu.backend.repository.VisitRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccessService {

    private final VisitRepository visitRepository;
    private final DeliveryRepository deliveryRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final ParkingPermitRepository parkingPermitRepository;
    private final CommunityRepository communityRepository;
    private final ResidentRepository residentRepository;
    private final UnitRepository unitRepository;

    public AccessService(VisitRepository visitRepository,
                         DeliveryRepository deliveryRepository,
                         ParkingSpaceRepository parkingSpaceRepository,
                         ParkingPermitRepository parkingPermitRepository,
                         CommunityRepository communityRepository,
                         ResidentRepository residentRepository,
                         UnitRepository unitRepository) {
        this.visitRepository = visitRepository;
        this.deliveryRepository = deliveryRepository;
        this.parkingSpaceRepository = parkingSpaceRepository;
        this.parkingPermitRepository = parkingPermitRepository;
        this.communityRepository = communityRepository;
        this.residentRepository = residentRepository;
        this.unitRepository = unitRepository;
    }

    public Visit createVisit(VisitRequest request) {
        Community community = communityRepository.findById(request.communityId())
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));
        Resident resident = residentRepository.findById(request.residentId())
                .orElseThrow(() -> new ResourceNotFoundException("Resident not found"));
        Visit visit = new Visit();
        visit.setCommunity(community);
        visit.setResident(resident);
        visit.setVisitorName(request.visitorName());
        visit.setVisitorRut(request.visitorRut());
        visit.setReason(request.reason());
        if (request.checkIn() != null) {
            visit.setCheckIn(request.checkIn());
        }
        visit.setCheckOut(request.checkOut());
        visit.setPreauthorized(request.preauthorized());
        visit.setQrCode(request.qrCode());
        return visitRepository.save(visit);
    }

    public List<Visit> listVisits() {
        return visitRepository.findAll();
    }

    public Delivery createDelivery(DeliveryRequest request) {
        Community community = communityRepository.findById(request.communityId())
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));
        Resident resident = residentRepository.findById(request.residentId())
                .orElseThrow(() -> new ResourceNotFoundException("Resident not found"));
        Delivery delivery = new Delivery();
        delivery.setCommunity(community);
        delivery.setResident(resident);
        delivery.setPackageCode(request.packageCode());
        if (request.receivedAt() != null) {
            delivery.setReceivedAt(request.receivedAt());
        }
        delivery.setDeliveredAt(request.deliveredAt());
        delivery.setEvidencePhotoUrl(request.evidencePhotoUrl());
        delivery.setSignature(request.signature());
        if (request.status() != null) {
            delivery.setStatus(request.status());
        }
        return deliveryRepository.save(delivery);
    }

    public List<Delivery> listDeliveries() {
        return deliveryRepository.findAll();
    }

    public ParkingSpace createParkingSpace(ParkingSpaceRequest request) {
        Community community = communityRepository.findById(request.communityId())
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));
        ParkingSpace space = new ParkingSpace();
        space.setCommunity(community);
        if (request.unitId() != null) {
            Unit unit = unitRepository.findById(request.unitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));
            if (!unit.getBuilding().getCommunity().getId().equals(community.getId())) {
                throw new ResourceNotFoundException("Unit not associated with community");
            }
            space.setUnit(unit);
        }
        space.setCode(request.code());
        if (request.spaceType() != null) {
            space.setSpaceType(request.spaceType());
        }
        if (request.active() != null) {
            space.setActive(request.active());
        }
        return parkingSpaceRepository.save(space);
    }

    public List<ParkingSpace> listParkingSpaces() {
        return parkingSpaceRepository.findAll();
    }

    public ParkingPermit createParkingPermit(ParkingPermitRequest request) {
        ParkingSpace space = parkingSpaceRepository.findById(request.parkingSpaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Parking space not found"));
        ParkingPermit permit = new ParkingPermit();
        permit.setParkingSpace(space);
        if (request.residentId() != null) {
            Resident resident = residentRepository.findById(request.residentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Resident not found"));
            permit.setResident(resident);
        }
        if (request.visitId() != null) {
            Visit visit = visitRepository.findById(request.visitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Visit not found"));
            permit.setVisit(visit);
        }
        permit.setStartTime(request.startTime());
        permit.setEndTime(request.endTime());
        if (request.permitType() != null) {
            permit.setPermitType(request.permitType());
        }
        return parkingPermitRepository.save(permit);
    }

    public List<ParkingPermit> listParkingPermits() {
        return parkingPermitRepository.findAll();
    }
}
