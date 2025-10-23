package com.domu.backend;

import com.domu.backend.repository.AmenityRepository;
import com.domu.backend.repository.BackupRecordRepository;
import com.domu.backend.repository.BuildingRepository;
import com.domu.backend.repository.CommunityRepository;
import com.domu.backend.repository.DeliveryRepository;
import com.domu.backend.repository.ExpenseStatementRepository;
import com.domu.backend.repository.ForumCategoryRepository;
import com.domu.backend.repository.ForumPostRepository;
import com.domu.backend.repository.ForumThreadRepository;
import com.domu.backend.repository.MaintenanceLogRepository;
import com.domu.backend.repository.MaintenanceScheduleRepository;
import com.domu.backend.repository.NotificationRepository;
import com.domu.backend.repository.ParkingPermitRepository;
import com.domu.backend.repository.ParkingSpaceRepository;
import com.domu.backend.repository.PaymentRepository;
import com.domu.backend.repository.PermissionRepository;
import com.domu.backend.repository.ProviderRepository;
import com.domu.backend.repository.ProviderRequestRepository;
import com.domu.backend.repository.ReservationRepository;
import com.domu.backend.repository.ResidentRepository;
import com.domu.backend.repository.RoleRepository;
import com.domu.backend.repository.ShiftRepository;
import com.domu.backend.repository.StaffRepository;
import com.domu.backend.repository.TaskAttachmentRepository;
import com.domu.backend.repository.TaskRepository;
import com.domu.backend.repository.TicketRepository;
import com.domu.backend.repository.TicketUpdateRepository;
import com.domu.backend.repository.UnitRepository;
import com.domu.backend.repository.VisitRepository;
import com.domu.backend.repository.VoteEventRepository;
import com.domu.backend.repository.VoteOptionRepository;
import com.domu.backend.repository.VoteRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

@SpringBootApplication
public class DomuBackendApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(DomuBackendApplication.class, args);
        bindRepositories(context);
    }

    private static void bindRepositories(ConfigurableApplicationContext context) {
        List<Class<?>> repositories = List.of(
                AmenityRepository.class,
                BackupRecordRepository.class,
                BuildingRepository.class,
                CommunityRepository.class,
                DeliveryRepository.class,
                ExpenseStatementRepository.class,
                ForumCategoryRepository.class,
                ForumPostRepository.class,
                ForumThreadRepository.class,
                MaintenanceLogRepository.class,
                MaintenanceScheduleRepository.class,
                NotificationRepository.class,
                ParkingPermitRepository.class,
                ParkingSpaceRepository.class,
                PaymentRepository.class,
                PermissionRepository.class,
                ProviderRepository.class,
                ProviderRequestRepository.class,
                ReservationRepository.class,
                ResidentRepository.class,
                RoleRepository.class,
                ShiftRepository.class,
                StaffRepository.class,
                TaskAttachmentRepository.class,
                TaskRepository.class,
                TicketRepository.class,
                TicketUpdateRepository.class,
                UnitRepository.class,
                VisitRepository.class,
                VoteEventRepository.class,
                VoteOptionRepository.class,
                VoteRepository.class
        );
        repositories.forEach(context::getBean);
    }
}
