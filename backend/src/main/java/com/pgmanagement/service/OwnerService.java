package com.pgmanagement.service;

import com.pgmanagement.entity.*;
import com.pgmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OwnerService {

    private final PGRepository pgRepository;
    private final RoomRepository roomRepository;
    private final RoomAssignmentRepository roomAssignmentRepository;
    private final RentRecordRepository rentRecordRepository;
    private final ComplaintRepository complaintRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final AnnouncementRepository announcementRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // ── PG ──────────────────────────────────────────────
    public PG createPG(User owner, Map<String, String> body) {
        PG pg = PG.builder()
                .owner(owner)
                .name(body.get("name"))
                .address(body.get("address"))
                .rules(body.get("rules"))
                .amenities(body.get("amenities"))
                .phone(body.get("phone"))
                .totalRooms(body.get("totalRooms"))
                .build();
        return pgRepository.save(pg);
    }

    public List<PG> getOwnerPGs(User owner) {
        return pgRepository.findByOwner(owner);
    }

    public PG updatePG(Long pgId, User owner, Map<String, String> body) {
        PG pg = pgRepository.findById(pgId).orElseThrow(() -> new RuntimeException("PG not found"));
        if (!pg.getOwner().getId().equals(owner.getId())) throw new RuntimeException("Not authorized");
        pg.setName(body.getOrDefault("name", pg.getName()));
        pg.setAddress(body.getOrDefault("address", pg.getAddress()));
        pg.setRules(body.getOrDefault("rules", pg.getRules()));
        pg.setAmenities(body.getOrDefault("amenities", pg.getAmenities()));
        pg.setPhone(body.getOrDefault("phone", pg.getPhone()));
        return pgRepository.save(pg);
    }

    // ── Rooms ────────────────────────────────────────────
    public Room addRoom(User owner, Map<String, Object> body) {
        Long pgId = Long.valueOf(body.get("pgId").toString());
        PG pg = pgRepository.findById(pgId).orElseThrow(() -> new RuntimeException("PG not found"));
        if (!pg.getOwner().getId().equals(owner.getId())) throw new RuntimeException("Not authorized");

        Room room = Room.builder()
                .pg(pg)
                .roomNumber(body.get("roomNumber").toString())
                .capacity(Integer.valueOf(body.get("capacity").toString()))
                .rentAmount(new BigDecimal(body.get("rentAmount").toString()))
                .type(Room.RoomType.valueOf(body.getOrDefault("type", "SINGLE").toString()))
                .build();
        return roomRepository.save(room);
    }

    @Transactional
    public List<Room> addRoomsBulk(User owner, Long pgId, List<Map<String, Object>> roomsData) {
        if (pgId == null) throw new IllegalArgumentException("pgId is required");
        if (roomsData == null || roomsData.isEmpty()) throw new IllegalArgumentException("At least one room is required");
        PG pg = pgRepository.findById(pgId).orElseThrow(() -> new RuntimeException("PG not found"));
        if (!pg.getOwner().getId().equals(owner.getId())) throw new RuntimeException("Not authorized");

        // Collect existing room numbers in this PG to skip duplicates silently
        Set<String> existing = roomRepository.findByPg(pg).stream()
                .map(Room::getRoomNumber).collect(Collectors.toSet());

        List<Room> created = new ArrayList<>();
        Set<String> seenInRequest = new HashSet<>();
        for (Map<String, Object> data : roomsData) {
            Object rnObj = data.get("roomNumber");
            if (rnObj == null || rnObj.toString().isBlank()) continue;
            String roomNumber = rnObj.toString().trim();
            if (existing.contains(roomNumber) || !seenInRequest.add(roomNumber)) continue;

            Object rentObj = data.get("rentAmount");
            if (rentObj == null || rentObj.toString().isBlank()) {
                throw new IllegalArgumentException("Rent amount is required for room " + roomNumber);
            }
            int capacity = data.get("capacity") != null && !data.get("capacity").toString().isBlank()
                    ? Integer.parseInt(data.get("capacity").toString()) : 1;
            String type = data.getOrDefault("type", "SINGLE").toString();

            Room room = Room.builder()
                    .pg(pg)
                    .roomNumber(roomNumber)
                    .capacity(capacity)
                    .rentAmount(new BigDecimal(rentObj.toString()))
                    .type(Room.RoomType.valueOf(type))
                    .isOccupied(false)
                    .build();
            created.add(roomRepository.save(room));
        }
        return created;
    }

    public List<Room> getRoomsByPG(Long pgId) {
        PG pg = pgRepository.findById(pgId).orElseThrow(() -> new RuntimeException("PG not found"));
        List<Room> rooms = roomRepository.findByPg(pg);
        populateOccupancy(rooms);
        return rooms;
    }

    public List<Room> getAllOwnerRooms(User owner) {
        List<PG> pgs = pgRepository.findByOwner(owner);
        List<Room> rooms = roomRepository.findByPgIn(pgs);
        populateOccupancy(rooms);
        return rooms;
    }

    /** Populate the transient currentOccupancy field on each Room via a single query. */
    private void populateOccupancy(List<Room> rooms) {
        if (rooms.isEmpty()) return;
        List<RoomAssignment> active = roomAssignmentRepository.findByRoomInAndIsActive(rooms, true);
        Map<Long, Long> countByRoom = active.stream()
                .collect(Collectors.groupingBy(a -> a.getRoom().getId(), Collectors.counting()));
        for (Room r : rooms) {
            r.setCurrentOccupancy(countByRoom.getOrDefault(r.getId(), 0L).intValue());
        }
    }

    @Transactional
    public RoomAssignment assignTenant(Long roomId, Long tenantId, User owner) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new RuntimeException("Room not found"));
        if (!room.getPg().getOwner().getId().equals(owner.getId())) throw new RuntimeException("Not authorized");
        User tenant = userRepository.findById(tenantId).orElseThrow(() -> new RuntimeException("Tenant not found"));

        int capacity = room.getCapacity() != null ? room.getCapacity() : 1;
        long activeCount = roomAssignmentRepository.countByRoomAndIsActive(room, true);
        if (activeCount >= capacity) {
            throw new IllegalArgumentException("Room " + room.getRoomNumber() + " is already full (" + activeCount + "/" + capacity + ")");
        }

        // Vacate existing assignment if any (and recompute old room's occupied flag)
        roomAssignmentRepository.findByTenantAndIsActive(tenant, true).ifPresent(a -> {
            a.setIsActive(false);
            a.setLeaveDate(LocalDate.now());
            Room oldRoom = a.getRoom();
            roomAssignmentRepository.save(a);
            long oldActive = roomAssignmentRepository.countByRoomAndIsActive(oldRoom, true);
            int oldCap = oldRoom.getCapacity() != null ? oldRoom.getCapacity() : 1;
            oldRoom.setIsOccupied(oldActive >= oldCap);
            roomRepository.save(oldRoom);
        });

        // Mark new room full iff this assignment fills the last spot
        room.setIsOccupied((activeCount + 1) >= capacity);
        roomRepository.save(room);

        RoomAssignment assignment = RoomAssignment.builder()
                .tenant(tenant)
                .room(room)
                .joinDate(LocalDate.now())
                .isActive(true)
                .build();
        assignment = roomAssignmentRepository.save(assignment);

        // Generate first rent record
        String currentMonth = LocalDate.now().toString().substring(0, 7);
        if (!rentRecordRepository.existsByTenantAndMonth(tenant, currentMonth)) {
            RentRecord rent = RentRecord.builder()
                    .tenant(tenant)
                    .room(room)
                    .month(currentMonth)
                    .amount(room.getRentAmount())
                    .dueDate(LocalDate.now().withDayOfMonth(5))
                    .build();
            rentRecordRepository.save(rent);
        }

        // Notify tenant
        Notification notif = Notification.builder()
                .user(tenant)
                .message("You have been assigned to room " + room.getRoomNumber() + " in " + room.getPg().getName())
                .type("ASSIGNMENT")
                .build();
        notificationRepository.save(notif);

        return assignment;
    }

    public List<RoomAssignment> getAllTenants(User owner) {
        return roomAssignmentRepository.findByRoom_PgOwnerAndIsActive(owner, true);
    }

    // ── Vacate request review ────────────────────────────
    public List<RoomAssignment> getPendingVacateRequests(User owner) {
        return roomAssignmentRepository
                .findByRoom_PgOwnerAndIsActiveAndVacateRequestedAtIsNotNull(owner, true);
    }

    /**
     * Approve a tenant-initiated vacate request: marks assignment inactive,
     * sets leaveDate to the requested leave date, recomputes room occupancy,
     * notifies the tenant.
     */
    @Transactional
    public RoomAssignment approveVacate(Long assignmentId, User owner) {
        RoomAssignment assignment = roomAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        if (!assignment.getRoom().getPg().getOwner().getId().equals(owner.getId())) {
            throw new RuntimeException("Not authorized");
        }
        if (assignment.getVacateRequestedAt() == null) {
            throw new IllegalArgumentException("This assignment has no pending vacate request");
        }
        return finalizeVacate(assignment, assignment.getRequestedLeaveDate(),
                "Your vacate request for Room " + assignment.getRoom().getRoomNumber()
                        + " has been APPROVED. Effective: " + assignment.getRequestedLeaveDate());
    }

    /**
     * Reject a tenant-initiated vacate request: clears the request, notifies tenant.
     */
    @Transactional
    public RoomAssignment rejectVacate(Long assignmentId, User owner, String note) {
        RoomAssignment assignment = roomAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        if (!assignment.getRoom().getPg().getOwner().getId().equals(owner.getId())) {
            throw new RuntimeException("Not authorized");
        }
        if (assignment.getVacateRequestedAt() == null) {
            throw new IllegalArgumentException("This assignment has no pending vacate request");
        }
        assignment.setVacateRequestedAt(null);
        assignment.setRequestedLeaveDate(null);
        assignment.setVacateReason(null);
        RoomAssignment saved = roomAssignmentRepository.save(assignment);

        Notification notif = Notification.builder()
                .user(assignment.getTenant())
                .message("Your vacate request for Room " + assignment.getRoom().getRoomNumber()
                        + " was rejected." + (note != null && !note.isBlank() ? " Note: " + note : ""))
                .type("VACATE_REJECTED")
                .build();
        notificationRepository.save(notif);
        return saved;
    }

    /**
     * Manually evict a tenant (no request needed). Sets leaveDate=today.
     * Use with care — for unpaid rent, policy violations, etc.
     */
    @Transactional
    public RoomAssignment manualVacate(Long assignmentId, User owner, String note) {
        RoomAssignment assignment = roomAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        if (!assignment.getRoom().getPg().getOwner().getId().equals(owner.getId())) {
            throw new RuntimeException("Not authorized");
        }
        if (!Boolean.TRUE.equals(assignment.getIsActive())) {
            throw new IllegalArgumentException("Assignment is already inactive");
        }
        return finalizeVacate(assignment, LocalDate.now(),
                "You have been moved out of Room " + assignment.getRoom().getRoomNumber()
                        + " by the owner." + (note != null && !note.isBlank() ? " Note: " + note : ""));
    }

    /** Shared finishing logic for approveVacate and manualVacate. */
    private RoomAssignment finalizeVacate(RoomAssignment assignment, LocalDate effectiveDate, String tenantMessage) {
        assignment.setIsActive(false);
        assignment.setLeaveDate(effectiveDate != null ? effectiveDate : LocalDate.now());
        // Clear vacate-request markers since the request is fulfilled
        assignment.setVacateRequestedAt(null);
        assignment.setRequestedLeaveDate(null);
        assignment.setVacateReason(null);
        RoomAssignment saved = roomAssignmentRepository.save(assignment);

        // Recompute the room's "isOccupied" cached flag now that someone left
        Room room = assignment.getRoom();
        long active = roomAssignmentRepository.countByRoomAndIsActive(room, true);
        int cap = room.getCapacity() != null ? room.getCapacity() : 1;
        room.setIsOccupied(active >= cap);
        roomRepository.save(room);

        Notification notif = Notification.builder()
                .user(assignment.getTenant())
                .message(tenantMessage)
                .type("VACATE")
                .build();
        notificationRepository.save(notif);
        return saved;
    }

    // ── Complaints ───────────────────────────────────────
    public List<Complaint> getAllComplaints(User owner) {
        return complaintRepository.findByPgOwnerOrderByCreatedAtDesc(owner);
    }

    @Transactional
    public Complaint updateComplaintStatus(Long id, String status, String note, User owner) {
        Complaint complaint = complaintRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        complaint.setStatus(Complaint.ComplaintStatus.valueOf(status));
        complaint.setOwnerNote(note);
        if (status.equals("RESOLVED")) {
            complaint.setResolvedAt(LocalDateTime.now());
        }
        complaint = complaintRepository.save(complaint);

        // Notify tenant
        Notification notif = Notification.builder()
                .user(complaint.getTenant())
                .message("Your complaint '" + complaint.getTitle() + "' status updated to " + status)
                .type("COMPLAINT")
                .build();
        notificationRepository.save(notif);

        return complaint;
    }

    // ── Maintenance ──────────────────────────────────────
    public List<MaintenanceRequest> getAllMaintenanceRequests(User owner) {
        return maintenanceRequestRepository.findByRoomPgOwnerOrderByCreatedAtDesc(owner);
    }

    @Transactional
    public MaintenanceRequest updateMaintenanceStatus(Long id, String status, String assignedTo, User owner) {
        MaintenanceRequest req = maintenanceRequestRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        req.setStatus(MaintenanceRequest.MaintenanceStatus.valueOf(status));
        if (assignedTo != null) req.setAssignedTo(assignedTo);
        if (status.equals("COMPLETED")) req.setCompletedAt(LocalDateTime.now());
        req = maintenanceRequestRepository.save(req);

        Notification notif = Notification.builder()
                .user(req.getTenant())
                .message("Your maintenance request '" + req.getTitle() + "' is now " + status)
                .type("MAINTENANCE")
                .build();
        notificationRepository.save(notif);

        return req;
    }

    // ── Announcements ────────────────────────────────────
    public Announcement createAnnouncement(User owner, Map<String, String> body) {
        Long pgId = body.get("pgId") != null ? Long.valueOf(body.get("pgId")) : null;
        PG pg = pgId != null ? pgRepository.findById(pgId).orElse(null) : null;

        Announcement ann = Announcement.builder()
                .owner(owner)
                .pg(pg)
                .title(body.get("title"))
                .message(body.get("message"))
                .priority(Announcement.Priority.valueOf(body.getOrDefault("priority", "NORMAL")))
                .build();
        return announcementRepository.save(ann);
    }

    public List<Announcement> getOwnerAnnouncements(User owner) {
        return announcementRepository.findByOwnerOrderByCreatedAtDesc(owner);
    }

    // ── Rent ─────────────────────────────────────────────
    public List<RentRecord> getAllRentRecords(User owner) {
        return rentRecordRepository.findByRoom_PgOwner(owner);
    }

    @Transactional
    public RentRecord markRentPaid(Long id, User owner) {
        RentRecord record = rentRecordRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        record.setStatus(RentRecord.RentStatus.PAID);
        record.setPaidDate(LocalDate.now());
        record = rentRecordRepository.save(record);

        Notification notif = Notification.builder()
                .user(record.getTenant())
                .message("Rent for " + record.getMonth() + " marked as PAID. Amount: ₹" + record.getAmount())
                .type("RENT")
                .build();
        notificationRepository.save(notif);

        return record;
    }

    // ── Dashboard Stats ──────────────────────────────────
    public Map<String, Object> getDashboardStats(User owner) {
        List<PG> pgs = pgRepository.findByOwner(owner);
        List<Room> rooms = roomRepository.findByPgIn(pgs);
        populateOccupancy(rooms);

        long fullyOccupied = rooms.stream()
                .filter(r -> r.getCapacity() != null && r.getCurrentOccupancy() != null
                        && r.getCurrentOccupancy() >= r.getCapacity())
                .count();
        long partiallyOccupied = rooms.stream()
                .filter(r -> r.getCurrentOccupancy() != null && r.getCurrentOccupancy() > 0
                        && r.getCapacity() != null && r.getCurrentOccupancy() < r.getCapacity())
                .count();
        long totalRooms = rooms.size();
        long vacantRooms = rooms.stream()
                .filter(r -> r.getCurrentOccupancy() == null || r.getCurrentOccupancy() == 0)
                .count();

        long totalBeds = rooms.stream()
                .mapToInt(r -> r.getCapacity() != null ? r.getCapacity() : 0).sum();
        long occupiedBeds = rooms.stream()
                .mapToInt(r -> r.getCurrentOccupancy() != null ? r.getCurrentOccupancy() : 0).sum();

        long openComplaints = complaintRepository.countByPgOwnerAndStatus(owner, Complaint.ComplaintStatus.OPEN);
        List<RentRecord> allRent = rentRecordRepository.findByRoom_PgOwner(owner);
        long pendingRent = allRent.stream().filter(r -> r.getStatus() == RentRecord.RentStatus.PENDING).count();
        BigDecimal monthlyRevenue = allRent.stream()
                .filter(r -> r.getStatus() == RentRecord.RentStatus.PAID)
                .map(RentRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPGs", pgs.size());
        stats.put("totalRooms", totalRooms);
        stats.put("occupiedRooms", fullyOccupied);
        stats.put("partiallyOccupiedRooms", partiallyOccupied);
        stats.put("vacantRooms", vacantRooms);
        stats.put("totalBeds", totalBeds);
        stats.put("occupiedBeds", occupiedBeds);
        stats.put("openComplaints", openComplaints);
        stats.put("pendingRent", pendingRent);
        stats.put("monthlyRevenue", monthlyRevenue);
        return stats;
    }
}
