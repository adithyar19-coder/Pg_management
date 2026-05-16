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
    private final RoomRequestRepository roomRequestRepository;

    // ── Room ─────────────────────────────────────────────
    public Optional<RoomAssignment> getMyRoom(User tenant) {
        return roomAssignmentRepository.findByTenantAndIsActive(tenant, true);
    }

    // ── PG Search (location-based) ───────────────────────
    public List<PG> searchPGs(String city, String locality) {
        String c = (city != null && !city.isBlank()) ? city.trim() : null;
        String l = (locality != null && !locality.isBlank()) ? locality.trim() : null;
        if (c == null && l == null) return pgRepository.findAll();
        return pgRepository.searchByLocation(c, l);
    }

    public PG getPgById(Long pgId) {
        return pgRepository.findById(pgId)
                .orElseThrow(() -> new RuntimeException("PG not found"));
    }

    // ── Room Request Workflow (tenant submits, owner reviews) ──
    @Transactional
    public RoomRequest submitRoomRequest(User tenant, Long pgId,
                                         String preferredType, Integer preferredFloor,
                                         Boolean acPreference, String notes) {
        PG pg = pgRepository.findById(pgId)
                .orElseThrow(() -> new IllegalArgumentException("PG not found"));

        // Block duplicates: one PENDING request per tenant+PG
        if (roomRequestRepository.existsByTenantAndPgAndStatus(tenant, pg, RoomRequest.Status.PENDING)) {
            throw new IllegalArgumentException("You already have a pending request for this PG");
        }
        // Block if tenant already has an active assignment anywhere
        if (roomAssignmentRepository.findByTenantAndIsActive(tenant, true).isPresent()) {
            throw new IllegalArgumentException("You already have an active room assignment. Vacate it first before requesting another.");
        }

        Room.RoomType typeEnum = null;
        if (preferredType != null && !preferredType.isBlank()) {
            try { typeEnum = Room.RoomType.valueOf(preferredType); }
            catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid preferredType: " + preferredType);
            }
        }

        RoomRequest req = RoomRequest.builder()
                .tenant(tenant)
                .pg(pg)
                .preferredType(typeEnum)
                .preferredFloor(preferredFloor)
                .acPreference(acPreference)
                .notes(notes)
                .status(RoomRequest.Status.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        req = roomRequestRepository.save(req);

        // Notify the PG owner
        Notification notif = Notification.builder()
                .user(pg.getOwner())
                .message(tenant.getName() + " submitted a new room request for " + pg.getName())
                .type("ROOM_REQUEST")
                .build();
        notificationRepository.save(notif);
        return req;
    }

    public List<RoomRequest> getMyRoomRequests(User tenant) {
        return roomRequestRepository.findByTenantOrderByCreatedAtDesc(tenant);
    }

    @Transactional
    public RoomRequest cancelRoomRequest(User tenant, Long requestId) {
        RoomRequest req = roomRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        if (!req.getTenant().getId().equals(tenant.getId())) {
            throw new RuntimeException("Not authorized");
        }
        if (req.getStatus() != RoomRequest.Status.PENDING) {
            throw new IllegalArgumentException("Only PENDING requests can be cancelled (current: " + req.getStatus() + ")");
        }
        req.setStatus(RoomRequest.Status.CANCELLED);
        req.setReviewedAt(LocalDateTime.now());
        return roomRequestRepository.save(req);
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
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (!notif.getUser().getId().equals(tenant.getId())) throw new RuntimeException("Not authorized");
        notif.setIsRead(true);
        notificationRepository.save(notif);
    }

    /** Mark every unread notification for this user as read. Used when the bell page opens. */
    @Transactional
    public int markAllNotificationsRead(User user) {
        List<Notification> unread = notificationRepository.findByUserAndIsRead(user, false);
        for (Notification n : unread) n.setIsRead(true);
        notificationRepository.saveAll(unread);
        return unread.size();
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
