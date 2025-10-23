package com.domu.backend.service;

import com.domu.backend.domain.staff.Personnel;
import com.domu.backend.domain.staff.PersonnelLog;
import com.domu.backend.domain.staff.Shift;
import com.domu.backend.domain.staff.Task;
import com.domu.backend.domain.ticket.Ticket;
import com.domu.backend.domain.ticket.TicketFollowUp;
import com.domu.backend.infrastructure.persistence.repository.PersonnelLogRepository;
import com.domu.backend.infrastructure.persistence.repository.PersonnelRepository;
import com.domu.backend.infrastructure.persistence.repository.ShiftRepository;
import com.domu.backend.infrastructure.persistence.repository.TaskRepository;
import com.domu.backend.infrastructure.persistence.repository.TicketFollowUpRepository;
import com.domu.backend.infrastructure.persistence.repository.TicketRepository;

import java.util.List;

public class OperationsService {

    private final PersonnelRepository personnelRepository;
    private final ShiftRepository shiftRepository;
    private final TaskRepository taskRepository;
    private final PersonnelLogRepository personnelLogRepository;
    private final TicketRepository ticketRepository;
    private final TicketFollowUpRepository ticketFollowUpRepository;

    public OperationsService(PersonnelRepository personnelRepository,
                             ShiftRepository shiftRepository,
                             TaskRepository taskRepository,
                             PersonnelLogRepository personnelLogRepository,
                             TicketRepository ticketRepository,
                             TicketFollowUpRepository ticketFollowUpRepository) {
        this.personnelRepository = personnelRepository;
        this.shiftRepository = shiftRepository;
        this.taskRepository = taskRepository;
        this.personnelLogRepository = personnelLogRepository;
        this.ticketRepository = ticketRepository;
        this.ticketFollowUpRepository = ticketFollowUpRepository;
    }

    public Personnel registerPersonnel(Personnel personnel) {
        return personnelRepository.save(personnel);
    }

    public List<Personnel> listPersonnel() {
        return personnelRepository.findAll();
    }

    public Shift scheduleShift(Shift shift) {
        personnelRepository.findById(shift.personnelId())
                .orElseThrow(() -> new ResourceNotFoundException("Personnel not found"));
        return shiftRepository.save(shift);
    }

    public List<Shift> listShifts() {
        return shiftRepository.findAll();
    }

    public Task assignTask(Task task) {
        personnelRepository.findById(task.personnelId())
                .orElseThrow(() -> new ResourceNotFoundException("Personnel not found"));
        return taskRepository.save(task);
    }

    public List<Task> listTasks() {
        return taskRepository.findAll();
    }

    public PersonnelLog logPersonnelEvent(PersonnelLog log) {
        personnelRepository.findById(log.personnelId())
                .orElseThrow(() -> new ResourceNotFoundException("Personnel not found"));
        return personnelLogRepository.save(log);
    }

    public List<PersonnelLog> listPersonnelLogs() {
        return personnelLogRepository.findAll();
    }

    public Ticket createTicket(Ticket ticket) {
        return ticketRepository.save(ticket);
    }

    public List<Ticket> listTickets() {
        return ticketRepository.findAll();
    }

    public TicketFollowUp addTicketFollowUp(TicketFollowUp followUp) {
        ticketRepository.findById(followUp.ticketId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        return ticketFollowUpRepository.save(followUp);
    }

    public List<TicketFollowUp> listTicketFollowUps() {
        return ticketFollowUpRepository.findAll();
    }
}
