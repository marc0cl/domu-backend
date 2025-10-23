package com.domu.backend.services;

import com.domu.backend.domain.Community;
import com.domu.backend.domain.MaintenanceLog;
import com.domu.backend.domain.MaintenanceSchedule;
import com.domu.backend.domain.Provider;
import com.domu.backend.domain.ProviderRequest;
import com.domu.backend.domain.Shift;
import com.domu.backend.domain.Staff;
import com.domu.backend.domain.Task;
import com.domu.backend.domain.TaskAttachment;
import com.domu.backend.dto.MaintenanceLogRequest;
import com.domu.backend.dto.MaintenanceScheduleRequest;
import com.domu.backend.dto.ProviderCreateRequest;
import com.domu.backend.dto.ProviderServiceRequest;
import com.domu.backend.dto.ShiftRequest;
import com.domu.backend.dto.StaffRequest;
import com.domu.backend.dto.TaskAttachmentRequest;
import com.domu.backend.dto.TaskRequest;
import com.domu.backend.exceptions.ResourceNotFoundException;
import com.domu.backend.repository.CommunityRepository;
import com.domu.backend.repository.MaintenanceLogRepository;
import com.domu.backend.repository.MaintenanceScheduleRepository;
import com.domu.backend.repository.ProviderRepository;
import com.domu.backend.repository.ProviderRequestRepository;
import com.domu.backend.repository.ShiftRepository;
import com.domu.backend.repository.StaffRepository;
import com.domu.backend.repository.TaskAttachmentRepository;
import com.domu.backend.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OperationsService {

    private final StaffRepository staffRepository;
    private final CommunityRepository communityRepository;
    private final TaskRepository taskRepository;
    private final TaskAttachmentRepository taskAttachmentRepository;
    private final ShiftRepository shiftRepository;
    private final ProviderRepository providerRepository;
    private final ProviderRequestRepository providerRequestRepository;
    private final MaintenanceScheduleRepository maintenanceScheduleRepository;
    private final MaintenanceLogRepository maintenanceLogRepository;

    public OperationsService(StaffRepository staffRepository,
                             CommunityRepository communityRepository,
                             TaskRepository taskRepository,
                             TaskAttachmentRepository taskAttachmentRepository,
                             ShiftRepository shiftRepository,
                             ProviderRepository providerRepository,
                             ProviderRequestRepository providerRequestRepository,
                             MaintenanceScheduleRepository maintenanceScheduleRepository,
                             MaintenanceLogRepository maintenanceLogRepository) {
        this.staffRepository = staffRepository;
        this.communityRepository = communityRepository;
        this.taskRepository = taskRepository;
        this.taskAttachmentRepository = taskAttachmentRepository;
        this.shiftRepository = shiftRepository;
        this.providerRepository = providerRepository;
        this.providerRequestRepository = providerRequestRepository;
        this.maintenanceScheduleRepository = maintenanceScheduleRepository;
        this.maintenanceLogRepository = maintenanceLogRepository;
    }

    public Staff createStaff(StaffRequest request) {
        Community community = communityRepository.findById(request.communityId())
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));
        Staff staff = new Staff();
        staff.setCommunity(community);
        staff.setFirstName(request.firstName());
        staff.setLastName(request.lastName());
        staff.setRut(request.rut());
        staff.setEmail(request.email());
        staff.setPhone(request.phone());
        staff.setPosition(request.position());
        staff.setActive(request.active());
        return staffRepository.save(staff);
    }

    public List<Staff> listStaff() {
        return staffRepository.findAll();
    }

    public Task createTask(TaskRequest request) {
        Community community = communityRepository.findById(request.communityId())
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));
        Task task = new Task();
        task.setCommunity(community);
        task.setTitle(request.title());
        task.setDescription(request.description());
        if (request.assigneeId() != null) {
            Staff assignee = staffRepository.findById(request.assigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));
            task.setAssignee(assignee);
        }
        if (request.status() != null) {
            task.setStatus(request.status());
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        task.setDueDate(request.dueDate());
        task.setCompletedAt(request.completedAt());
        return taskRepository.save(task);
    }

    public List<Task> listTasks() {
        return taskRepository.findAll();
    }

    public TaskAttachment createTaskAttachment(TaskAttachmentRequest request) {
        Task task = taskRepository.findById(request.taskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        TaskAttachment attachment = new TaskAttachment();
        attachment.setTask(task);
        attachment.setUrl(request.url());
        if (request.createdAt() != null) {
            attachment.setCreatedAt(request.createdAt());
        }
        return taskAttachmentRepository.save(attachment);
    }

    public List<TaskAttachment> listTaskAttachments() {
        return taskAttachmentRepository.findAll();
    }

    public Shift createShift(ShiftRequest request) {
        Staff staff = staffRepository.findById(request.staffId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));
        Shift shift = new Shift();
        shift.setStaff(staff);
        if (request.startedAt() != null) {
            shift.setStartedAt(request.startedAt());
        }
        shift.setEndedAt(request.endedAt());
        shift.setNotes(request.notes());
        return shiftRepository.save(shift);
    }

    public List<Shift> listShifts() {
        return shiftRepository.findAll();
    }

    public Provider createProvider(ProviderCreateRequest request) {
        Community community = communityRepository.findById(request.communityId())
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));
        Provider provider = new Provider();
        provider.setCommunity(community);
        provider.setName(request.name());
        provider.setServiceType(request.serviceType());
        provider.setEmail(request.email());
        provider.setPhone(request.phone());
        provider.setRating(request.rating());
        return providerRepository.save(provider);
    }

    public List<Provider> listProviders() {
        return providerRepository.findAll();
    }

    public ProviderRequest createProviderRequest(ProviderServiceRequest request) {
        Community community = communityRepository.findById(request.communityId())
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));
        Provider provider = providerRepository.findById(request.providerId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
        ProviderRequest providerRequest = new ProviderRequest();
        providerRequest.setCommunity(community);
        providerRequest.setProvider(provider);
        providerRequest.setDescription(request.description());
        if (request.status() != null) {
            providerRequest.setStatus(request.status());
        }
        providerRequest.setQuotationUrl(request.quotationUrl());
        if (request.createdAt() != null) {
            providerRequest.setCreatedAt(request.createdAt());
        }
        return providerRequestRepository.save(providerRequest);
    }

    public List<ProviderRequest> listProviderRequests() {
        return providerRequestRepository.findAll();
    }

    public MaintenanceSchedule createMaintenanceSchedule(MaintenanceScheduleRequest request) {
        Community community = communityRepository.findById(request.communityId())
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));
        MaintenanceSchedule schedule = new MaintenanceSchedule();
        schedule.setCommunity(community);
        if (request.providerId() != null) {
            Provider provider = providerRepository.findById(request.providerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
            schedule.setProvider(provider);
        }
        schedule.setAssetName(request.assetName());
        schedule.setDescription(request.description());
        schedule.setScheduledDate(request.scheduledDate());
        schedule.setAlertDate(request.alertDate());
        if (request.status() != null) {
            schedule.setStatus(request.status());
        }
        return maintenanceScheduleRepository.save(schedule);
    }

    public List<MaintenanceSchedule> listMaintenanceSchedules() {
        return maintenanceScheduleRepository.findAll();
    }

    public MaintenanceLog createMaintenanceLog(MaintenanceLogRequest request) {
        MaintenanceSchedule schedule = maintenanceScheduleRepository.findById(request.scheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance schedule not found"));
        MaintenanceLog log = new MaintenanceLog();
        log.setSchedule(schedule);
        log.setNotes(request.notes());
        log.setCompletedAt(request.completedAt());
        log.setAttachmentUrl(request.attachmentUrl());
        return maintenanceLogRepository.save(log);
    }

    public List<MaintenanceLog> listMaintenanceLogs() {
        return maintenanceLogRepository.findAll();
    }
}
