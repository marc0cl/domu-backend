package com.domu.backend.controllers;

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
import com.domu.backend.services.OperationsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/operations")
public class OperationsController {

    private final OperationsService operationsService;

    public OperationsController(OperationsService operationsService) {
        this.operationsService = operationsService;
    }

    @PostMapping("/staff")
    public Staff createStaff(@Valid @RequestBody StaffRequest request) {
        return operationsService.createStaff(request);
    }

    @GetMapping("/staff")
    public List<Staff> listStaff() {
        return operationsService.listStaff();
    }

    @PostMapping("/tasks")
    public Task createTask(@Valid @RequestBody TaskRequest request) {
        return operationsService.createTask(request);
    }

    @GetMapping("/tasks")
    public List<Task> listTasks() {
        return operationsService.listTasks();
    }

    @PostMapping("/task-attachments")
    public TaskAttachment createTaskAttachment(@Valid @RequestBody TaskAttachmentRequest request) {
        return operationsService.createTaskAttachment(request);
    }

    @GetMapping("/task-attachments")
    public List<TaskAttachment> listTaskAttachments() {
        return operationsService.listTaskAttachments();
    }

    @PostMapping("/shifts")
    public Shift createShift(@Valid @RequestBody ShiftRequest request) {
        return operationsService.createShift(request);
    }

    @GetMapping("/shifts")
    public List<Shift> listShifts() {
        return operationsService.listShifts();
    }

    @PostMapping("/providers")
    public Provider createProvider(@Valid @RequestBody ProviderCreateRequest request) {
        return operationsService.createProvider(request);
    }

    @GetMapping("/providers")
    public List<Provider> listProviders() {
        return operationsService.listProviders();
    }

    @PostMapping("/provider-requests")
    public ProviderRequest createProviderRequest(@Valid @RequestBody ProviderServiceRequest request) {
        return operationsService.createProviderRequest(request);
    }

    @GetMapping("/provider-requests")
    public List<ProviderRequest> listProviderRequests() {
        return operationsService.listProviderRequests();
    }

    @PostMapping("/maintenance-schedules")
    public MaintenanceSchedule createMaintenanceSchedule(@Valid @RequestBody MaintenanceScheduleRequest request) {
        return operationsService.createMaintenanceSchedule(request);
    }

    @GetMapping("/maintenance-schedules")
    public List<MaintenanceSchedule> listMaintenanceSchedules() {
        return operationsService.listMaintenanceSchedules();
    }

    @PostMapping("/maintenance-logs")
    public MaintenanceLog createMaintenanceLog(@Valid @RequestBody MaintenanceLogRequest request) {
        return operationsService.createMaintenanceLog(request);
    }

    @GetMapping("/maintenance-logs")
    public List<MaintenanceLog> listMaintenanceLogs() {
        return operationsService.listMaintenanceLogs();
    }
}
