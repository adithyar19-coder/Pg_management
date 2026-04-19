package com.pgmanagement.service;

import com.pgmanagement.entity.*;
import com.pgmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final RoomAssignmentRepository roomAssignmentRepository;
    private final RentRecordRepository rentRecordRepository;
    private final ComplaintRepository complaintRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final AnnouncementRepository announcementRepository;
    private final NotificationRepository notificationRepository;
    private final PGRepository pgRepository;

    // ── Room ─────────────────────────────────────────────
    public Optional<RoomAssignment> getMyRoom(User tenant) {
        return roomAssignmentRepository.findByTenantAndIsActive(tenant, true);
    }

    // ── Vacate request workflow ──────────────────────────
    @Transactional
    public RoomAssignment requestVacate(User tenant, LocalDate requestedLeaveDate, String reason) {
        RoomAssignment assignment = roomAssignmentRepository.findByTenantAndIsActive(tenant, true)
                .orElseThrow(() -> new IllegalArgumentException("You are not currently assigned to any room"));
        if (assignment.getVacateRequestedAt() != null) {
            throw new IllegalArgumentException("You already have a pending vacate request");
        }
        if (requestedLeaveDate == null || requestedLeaveDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Requested leave date must be today or later");
        }
        assignment.setVacateRequestedAt(LocalDateTime.now());
        assignment.setRequestedLeaveDate(requestedLeaveDate);
        assignment.setVacateReason(reason);
        RoomAssignment saved = roomAssignmentRepository.save(assignment);

        // Notify the PG owner so they see it on their dashboard
        User owner = assignment.getRoom().getPg().getOwner();
        Notification notif = Notification.builder()
                .user(owner)
                .message(tenant.getName() + " has requested to vacate Room "
                        + assignment.getRoom().getRoomNumber() + " on " + requestedLeaveDate)
                .type("VACATE_REQUEST")
                .build();
        notificationRepository.save(notif);
        return saved;
    }

    @Transactional
    public void cancelVacateRequest(User tenant) {
        RoomAssignment assignment = roomAssignmentRepository.findByTenantAndIsActive(tenant, true)
                .orElseThrow(() -> new IllegalArgumentException("You are not currently assigned to any room"));
        if (assignment.getVacateRequestedAt() == null) {
            throw new IllegalArgumentException("No pending vacate request to cancel");
        }
        assignment.setVacateRequestedAt(null);
        assignment.setRequestedLeaveDate(null);
        assignment.setVacateReason(null);
        roomAssignmentRepository.save(assignment);
    }

    // ── Complaints ───────────────────────────────────────
    @Transactional
    public Complaint raiseComplaint(User tenant, String title, String description, List<String> imageUrls) {
        Optional<RoomAssignment> assignment = roomAssignmentRepository.findByTenantAndIsActive(tenant, true);
        PG pg = assignment.map(a -> a.getRoom().getPg()).orElse(null);

        Complaint complaint = Complaint.builder()
                .tenant(tenant)
                .pg(pg)
                .title(title)
                .description(description)
                .imageUrls(imageUrls)
                .build();
        return complaintRepository.save(complaint);
    }

    public List<Complaint> getMyComplaints(User tenant) {
        return complaintRepository.findByTenantOrderByCreatedAtDesc(tenant);
    }

    // ── Maintenance ──────────────────────────────────────
    @Transactional
    public MaintenanceRequest raiseMaintenance(User tenant, String title, String issue,
                                               String priority, List<String> imageUrls) {
        Optional<RoomAssignment> assignment = roomAssignmentRepository.findByTenantAndIsActive(tenant, true);
        Room room = assignment.map(RoomAssignment::getRoom).orElse(null);

        MaintenanceRequest req = MaintenanceRequest.builder()
                .tenant(tenant)
                .room(room)
                .title(title)
                .issue(issue)
                .priority(MaintenanceRequest.Priority.valueOf(priority != null ? priority : "MEDIUM"))
                .imageUrls(imageUrls)
                .build();
        return maintenanceRequestRepository.save(req);
    }

    public List<MaintenanceRequest> getMyMaintenanceRequests(User tenant) {
        return maintenanceRequestRepository.findByTenantOrderByCreatedAtDesc(tenant);
    }

    // ── Rent ─────────────────────────────────────────────
    public List<RentRecord> getMyRentHistory(User tenant) {
        return rentRecordRepository.findByTenant(tenant);
    }

    // ── Announcements ────────────────────────────────────
    public List<Announcement> getMyAnnouncements(User tenant) {
        Optional<RoomAssignment> assignment = roomAssignmentRepository.findByTenantAndIsActive(tenant, true);
        if (assignment.isEmpty()) return List.of();
        PG pg = assignment.get().getRoom().getPg();
        return announcementRepository.findByPgInOrderByCreatedAtDesc(List.of(pg));
    }

    // ── Notifications ─────────────────────────────────────
    public List<Notification> getMyNotifications(User tenant) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(tenant);
    }

    @Transactional
    public void markNotificationRead(Long id, User tenant) {
        Notification notif = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));
        if (!notif.getUser().getId().equals(tenant.getId())) throw new RuntimeException("Not authorized");
        notif.setIsRead(true);
        notificationRepository.save(notif);
    }

    public long getUnreadNotificationCount(User tenant) {
        return notificationRepository.countByUserAndIsRead(tenant, false);
    }

    // ── Dashboard Stats ──────────────────────────────────
    public Map<String, Object> getDashboardStats(User tenant) {
        Optional<RoomAssignment> assignment = roomAssignmentRepository.findByTenantAndIsActive(tenant, true);
        long pendingRent = rentRecordRepository.findByTenantAndStatus(tenant, RentRecord.RentStatus.PENDING).size();
        long openComplaints = complaintRepository.findByTenantOrderByCreatedAtDesc(tenant)
                .stream().filter(c -> c.getStatus() == Complaint.ComplaintStatus.OPEN).count();
        long unread = notificationRepository.countByUserAndIsRead(tenant, false);

        Map<String, Object> stats = new HashMap<>();
        stats.put("hasRoom", assignment.isPresent());
        stats.put("roomNumber", assignment.map(a -> a.getRoom().getRoomNumber()).orElse("Not Assigned"));
        stats.put("pgName", assignment.map(a -> a.getRoom().getPg().getName()).orElse("-"));
        stats.put("pendingRent", pendingRent);
        stats.put("openComplaints", openComplaints);
        stats.put("unreadNotifications", unread);
        return stats;
    }
}
