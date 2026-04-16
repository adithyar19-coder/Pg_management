package com.pgmanagement.config;

import com.pgmanagement.entity.*;
import com.pgmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepo;
    private final PGRepository pgRepo;
    private final RoomRepository roomRepo;
    private final RoomAssignmentRepository assignmentRepo;
    private final RentRecordRepository rentRepo;
    private final ComplaintRepository complaintRepo;
    private final MaintenanceRequestRepository maintenanceRepo;
    private final AnnouncementRepository announcementRepo;
    private final NotificationRepository notifRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepo.existsByEmail("owner@demo.com")) {
            log.info("Demo data already seeded — skipping.");
            return;
        }

        log.info("Seeding demo data...");

        // ── Users ──────────────────────────────────────────────────────────
        User owner = userRepo.save(User.builder()
                .name("Rajesh Kumar")
                .email("owner@demo.com")
                .password(passwordEncoder.encode("demo123"))
                .role(User.Role.OWNER)
                .phone("+91 98765 43210")
                .build());

        User tenant1 = userRepo.save(User.builder()
                .name("Arjun Sharma")
                .email("tenant@demo.com")
                .password(passwordEncoder.encode("demo123"))
                .role(User.Role.TENANT)
                .phone("+91 91234 56789")
                .build());

        User tenant2 = userRepo.save(User.builder()
                .name("Priya Patel")
                .email("priya@demo.com")
                .password(passwordEncoder.encode("demo123"))
                .role(User.Role.TENANT)
                .phone("+91 98000 12345")
                .build());

        User tenant3 = userRepo.save(User.builder()
                .name("Vikram Singh")
                .email("vikram@demo.com")
                .password(passwordEncoder.encode("demo123"))
                .role(User.Role.TENANT)
                .phone("+91 87654 32109")
                .build());

        // ── PG Properties ──────────────────────────────────────────────────
        PG pg1 = pgRepo.save(PG.builder()
                .owner(owner)
                .name("Sunrise Boys PG")
                .address("12, MG Road, Koramangala, Bengaluru - 560034")
                .rules("No smoking indoors. Visitors allowed till 9 PM. Common area must be kept clean.")
                .amenities("WiFi, AC, Hot Water, Meals (Breakfast & Dinner), Laundry, Parking")
                .phone("+91 98765 43210")
                .totalRooms("10")
                .build());

        PG pg2 = pgRepo.save(PG.builder()
                .owner(owner)
                .name("Green Valley PG")
                .address("45, HSR Layout, Sector 2, Bengaluru - 560102")
                .rules("Quiet hours after 11 PM. No pets allowed. Monthly rent due by 5th.")
                .amenities("WiFi, Geyser, TV in common area, 24/7 Security, Power Backup")
                .phone("+91 98765 43220")
                .totalRooms("6")
                .build());

        // ── Rooms ──────────────────────────────────────────────────────────
        Room r101 = roomRepo.save(Room.builder().pg(pg1).roomNumber("101").capacity(1)
                .rentAmount(new BigDecimal("8500")).type(Room.RoomType.SINGLE).isOccupied(true).build());
        Room r102 = roomRepo.save(Room.builder().pg(pg1).roomNumber("102").capacity(2)
                .rentAmount(new BigDecimal("6000")).type(Room.RoomType.DOUBLE).isOccupied(true).build());
        Room r103 = roomRepo.save(Room.builder().pg(pg1).roomNumber("103").capacity(1)
                .rentAmount(new BigDecimal("9000")).type(Room.RoomType.SINGLE).isOccupied(true).build());
        Room r104 = roomRepo.save(Room.builder().pg(pg1).roomNumber("104").capacity(3)
                .rentAmount(new BigDecimal("4500")).type(Room.RoomType.TRIPLE).isOccupied(false).build());
        Room r105 = roomRepo.save(Room.builder().pg(pg1).roomNumber("105").capacity(1)
                .rentAmount(new BigDecimal("8000")).type(Room.RoomType.SINGLE).isOccupied(false).build());

        Room r201 = roomRepo.save(Room.builder().pg(pg2).roomNumber("201").capacity(2)
                .rentAmount(new BigDecimal("5500")).type(Room.RoomType.DOUBLE).isOccupied(false).build());
        Room r202 = roomRepo.save(Room.builder().pg(pg2).roomNumber("202").capacity(1)
                .rentAmount(new BigDecimal("7500")).type(Room.RoomType.SINGLE).isOccupied(false).build());

        // ── Room Assignments ───────────────────────────────────────────────
        RoomAssignment a1 = assignmentRepo.save(RoomAssignment.builder()
                .tenant(tenant1).room(r101).joinDate(LocalDate.now().minusMonths(3)).isActive(true).build());
        RoomAssignment a2 = assignmentRepo.save(RoomAssignment.builder()
                .tenant(tenant2).room(r102).joinDate(LocalDate.now().minusMonths(1)).isActive(true).build());
        RoomAssignment a3 = assignmentRepo.save(RoomAssignment.builder()
                .tenant(tenant3).room(r103).joinDate(LocalDate.now().minusMonths(2)).isActive(true).build());

        // ── Rent Records ───────────────────────────────────────────────────
        String currentMonth = LocalDate.now().toString().substring(0, 7);
        String lastMonth    = LocalDate.now().minusMonths(1).toString().substring(0, 7);
        String prevMonth    = LocalDate.now().minusMonths(2).toString().substring(0, 7);

        // Tenant 1 — Arjun: 2 months paid, current pending
        rentRepo.save(RentRecord.builder().tenant(tenant1).room(r101).month(prevMonth)
                .amount(new BigDecimal("8500")).status(RentRecord.RentStatus.PAID)
                .dueDate(LocalDate.now().minusMonths(2).withDayOfMonth(5))
                .paidDate(LocalDate.now().minusMonths(2).withDayOfMonth(3)).build());
        rentRepo.save(RentRecord.builder().tenant(tenant1).room(r101).month(lastMonth)
                .amount(new BigDecimal("8500")).status(RentRecord.RentStatus.PAID)
                .dueDate(LocalDate.now().minusMonths(1).withDayOfMonth(5))
                .paidDate(LocalDate.now().minusMonths(1).withDayOfMonth(4)).build());
        rentRepo.save(RentRecord.builder().tenant(tenant1).room(r101).month(currentMonth)
                .amount(new BigDecimal("8500")).status(RentRecord.RentStatus.PENDING)
                .dueDate(LocalDate.now().withDayOfMonth(5)).build());

        // Tenant 2 — Priya: current pending
        rentRepo.save(RentRecord.builder().tenant(tenant2).room(r102).month(currentMonth)
                .amount(new BigDecimal("6000")).status(RentRecord.RentStatus.PENDING)
                .dueDate(LocalDate.now().withDayOfMonth(5)).build());

        // Tenant 3 — Vikram: paid this month
        rentRepo.save(RentRecord.builder().tenant(tenant3).room(r103).month(lastMonth)
                .amount(new BigDecimal("9000")).status(RentRecord.RentStatus.PAID)
                .dueDate(LocalDate.now().minusMonths(1).withDayOfMonth(5))
                .paidDate(LocalDate.now().minusMonths(1).withDayOfMonth(2)).build());
        rentRepo.save(RentRecord.builder().tenant(tenant3).room(r103).month(currentMonth)
                .amount(new BigDecimal("9000")).status(RentRecord.RentStatus.PENDING)
                .dueDate(LocalDate.now().withDayOfMonth(5)).build());

        // ── Complaints ─────────────────────────────────────────────────────
        Complaint c1 = complaintRepo.save(Complaint.builder()
                .tenant(tenant1).pg(pg1)
                .title("Water leakage in bathroom")
                .description("There is a water leakage near the shower area. The floor gets wet and is causing slipping hazard. Needs urgent attention.")
                .status(Complaint.ComplaintStatus.IN_PROGRESS)
                .ownerNote("Plumber has been informed, will fix by tomorrow.")
                .createdAt(LocalDateTime.now().minusDays(3))
                .build());

        Complaint c2 = complaintRepo.save(Complaint.builder()
                .tenant(tenant2).pg(pg1)
                .title("WiFi not working in room 102")
                .description("The WiFi signal is very weak in room 102. Other rooms seem fine. Please check the router placement.")
                .status(Complaint.ComplaintStatus.OPEN)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build());

        Complaint c3 = complaintRepo.save(Complaint.builder()
                .tenant(tenant3).pg(pg1)
                .title("AC making noise")
                .description("The AC unit in my room makes a loud rattling noise every night. It is disturbing my sleep. Please get it serviced.")
                .status(Complaint.ComplaintStatus.RESOLVED)
                .ownerNote("AC has been serviced and the issue is resolved.")
                .createdAt(LocalDateTime.now().minusDays(10))
                .resolvedAt(LocalDateTime.now().minusDays(7))
                .build());

        Complaint c4 = complaintRepo.save(Complaint.builder()
                .tenant(tenant1).pg(pg1)
                .title("Room door lock is broken")
                .description("The lock on my room door is not working properly. It doesn't lock from inside properly. This is a security concern.")
                .status(Complaint.ComplaintStatus.OPEN)
                .createdAt(LocalDateTime.now().minusHours(5))
                .build());

        // ── Maintenance Requests ───────────────────────────────────────────
        maintenanceRepo.save(MaintenanceRequest.builder()
                .tenant(tenant1).room(r101)
                .title("Replace tube light")
                .issue("The tube light in my room has stopped working. Need a replacement urgently as the room is very dark at night.")
                .priority(MaintenanceRequest.Priority.MEDIUM)
                .status(MaintenanceRequest.MaintenanceStatus.ASSIGNED)
                .assignedTo("Ramu Electrician")
                .createdAt(LocalDateTime.now().minusDays(2))
                .build());

        maintenanceRepo.save(MaintenanceRequest.builder()
                .tenant(tenant2).room(r102)
                .title("Wardrobe door hinge broken")
                .issue("The wardrobe hinge is broken and the door keeps falling off. Cannot use the wardrobe properly.")
                .priority(MaintenanceRequest.Priority.LOW)
                .status(MaintenanceRequest.MaintenanceStatus.PENDING)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build());

        maintenanceRepo.save(MaintenanceRequest.builder()
                .tenant(tenant3).room(r103)
                .title("Fan speed not working")
                .issue("The ceiling fan speed regulator is not working. Fan only runs on one speed regardless of regulator setting.")
                .priority(MaintenanceRequest.Priority.HIGH)
                .status(MaintenanceRequest.MaintenanceStatus.IN_PROGRESS)
                .assignedTo("Suresh Electrician")
                .createdAt(LocalDateTime.now().minusDays(4))
                .build());

        // ── Announcements ──────────────────────────────────────────────────
        announcementRepo.save(Announcement.builder()
                .owner(owner).pg(pg1)
                .title("Water supply disruption — 15th April")
                .message("Dear residents, there will be a water supply disruption on 15th April (Thursday) from 9 AM to 2 PM due to maintenance work in the area. Please store water accordingly. Sorry for the inconvenience.")
                .priority(Announcement.Priority.HIGH)
                .createdAt(LocalDateTime.now().minusDays(2))
                .build());

        announcementRepo.save(Announcement.builder()
                .owner(owner).pg(pg1)
                .title("Rent due reminder — Pay by 5th")
                .message("This is a friendly reminder that monthly rent is due by the 5th of every month. Please ensure timely payment to avoid late fees. You can pay via UPI, bank transfer, or cash to the caretaker.")
                .priority(Announcement.Priority.NORMAL)
                .createdAt(LocalDateTime.now().minusDays(5))
                .build());

        announcementRepo.save(Announcement.builder()
                .owner(owner).pg(pg1)
                .title("New WiFi router installed")
                .message("We have installed a new high-speed WiFi router. New password: SunrisePG@2024. Please update your saved networks. Speed should be significantly improved now.")
                .priority(Announcement.Priority.NORMAL)
                .createdAt(LocalDateTime.now().minusDays(7))
                .build());

        announcementRepo.save(Announcement.builder()
                .owner(owner).pg(null)
                .title("Festival celebration — Diwali party!")
                .message("All residents of Sunrise and Green Valley PGs are invited to the Diwali celebration on the terrace. Date: 1st November, 7 PM onwards. Sweets and snacks will be provided. Join us for a fun evening!")
                .priority(Announcement.Priority.URGENT)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build());

        // ── Notifications ──────────────────────────────────────────────────
        notifRepo.save(Notification.builder().user(tenant1)
                .message("You have been assigned to Room 101 in Sunrise Boys PG")
                .type("ASSIGNMENT").isRead(true).createdAt(LocalDateTime.now().minusMonths(3)).build());
        notifRepo.save(Notification.builder().user(tenant1)
                .message("Your complaint 'Water leakage in bathroom' is now IN PROGRESS")
                .type("COMPLAINT").isRead(false).createdAt(LocalDateTime.now().minusDays(2)).build());
        notifRepo.save(Notification.builder().user(tenant1)
                .message("Rent for " + lastMonth + " marked as PAID. Amount: ₹8500")
                .type("RENT").isRead(false).createdAt(LocalDateTime.now().minusMonths(1)).build());

        notifRepo.save(Notification.builder().user(tenant2)
                .message("You have been assigned to Room 102 in Sunrise Boys PG")
                .type("ASSIGNMENT").isRead(true).createdAt(LocalDateTime.now().minusMonths(1)).build());
        notifRepo.save(Notification.builder().user(tenant2)
                .message("New announcement: Water supply disruption — 15th April")
                .type("ANNOUNCEMENT").isRead(false).createdAt(LocalDateTime.now().minusDays(2)).build());

        notifRepo.save(Notification.builder().user(tenant3)
                .message("Your complaint 'AC making noise' has been RESOLVED!")
                .type("COMPLAINT").isRead(false).createdAt(LocalDateTime.now().minusDays(7)).build());
        notifRepo.save(Notification.builder().user(tenant3)
                .message("Rent for " + lastMonth + " marked as PAID. Amount: ₹9000")
                .type("RENT").isRead(false).createdAt(LocalDateTime.now().minusMonths(1)).build());

        log.info("✅ Demo data seeded successfully!");
        log.info("   Owner login  → owner@demo.com  / demo123");
        log.info("   Tenant login → tenant@demo.com / demo123");
    }
}
