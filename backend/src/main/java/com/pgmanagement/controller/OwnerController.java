package com.pgmanagement.controller;

import com.pgmanagement.dto.ApiResponse;
import com.pgmanagement.entity.*;
import com.pgmanagement.repository.UserRepository;
import com.pgmanagement.service.FoodMenuService;
import com.pgmanagement.service.ForumService;
import com.pgmanagement.service.OwnerService;
import com.pgmanagement.service.ReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/owner")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerService ownerService;
    private final UserRepository userRepository;
    private final FoodMenuService foodMenuService;
    private final ForumService forumService;
    private final ReminderService reminderService;

    private User getUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ── Dashboard ────────────────────────────────────────
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", ownerService.getDashboardStats(getUser(ud))));
    }

    // ── PG Management ────────────────────────────────────
    @PostMapping("/pg")
    public ResponseEntity<ApiResponse<PG>> createPG(@AuthenticationPrincipal UserDetails ud,
                                                     @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success("PG created", ownerService.createPG(getUser(ud), body)));
    }

    @GetMapping("/pg")
    public ResponseEntity<ApiResponse<List<PG>>> getPGs(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", ownerService.getOwnerPGs(getUser(ud))));
    }

    @PutMapping("/pg/{id}")
    public ResponseEntity<ApiResponse<PG>> updatePG(@PathVariable Long id,
                                                      @AuthenticationPrincipal UserDetails ud,
                                                      @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success("PG updated", ownerService.updatePG(id, getUser(ud), body)));
    }

    // ── Room Management ──────────────────────────────────
    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<Room>> addRoom(@AuthenticationPrincipal UserDetails ud,
                                                      @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.success("Room added", ownerService.addRoom(getUser(ud), body)));
    }

    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<Room>>> getAllRooms(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", ownerService.getAllOwnerRooms(getUser(ud))));
    }

    @PostMapping("/rooms/bulk")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<List<Room>>> addRoomsBulk(@AuthenticationPrincipal UserDetails ud,
                                                                  @RequestBody Map<String, Object> body) {
        Object pgIdObj = body.get("pgId");
        if (pgIdObj == null) throw new IllegalArgumentException("pgId is required");
        Long pgId = Long.valueOf(pgIdObj.toString());
        List<Map<String, Object>> roomsData = (List<Map<String, Object>>) body.get("rooms");
        return ResponseEntity.ok(ApiResponse.success("Rooms added",
                ownerService.addRoomsBulk(getUser(ud), pgId, roomsData)));
    }

    @GetMapping("/pg/{pgId}/rooms")
    public ResponseEntity<ApiResponse<List<Room>>> getRoomsByPG(@PathVariable Long pgId) {
        return ResponseEntity.ok(ApiResponse.success("OK", ownerService.getRoomsByPG(pgId)));
    }

    // ── Room Requests (NEW assignment flow) ─────────────────
    @GetMapping("/room-requests")
    public ResponseEntity<ApiResponse<List<RoomRequest>>> getPendingRoomRequests(@AuthenticationPrincipal UserDetails ud,
                                                                                    @RequestParam(required = false) String status) {
        if ("ALL".equalsIgnoreCase(status)) {
            return ResponseEntity.ok(ApiResponse.success("OK", ownerService.getAllRoomRequests(getUser(ud))));
        }
        return ResponseEntity.ok(ApiResponse.success("OK", ownerService.getPendingRoomRequests(getUser(ud))));
    }

    @PostMapping("/room-requests/{id}/suggest")
    public ResponseEntity<ApiResponse<Map<String, Object>>> suggestRoomForRequest(@PathVariable Long id,
                                                                                    @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("Suggestion ready",
                ownerService.suggestRoomForRequest(id, getUser(ud))));
    }

    @PostMapping("/room-requests/{id}/approve")
    public ResponseEntity<ApiResponse<RoomRequest>> approveRoomRequest(@PathVariable Long id,
                                                                         @RequestBody Map<String, Object> body,
                                                                         @AuthenticationPrincipal UserDetails ud) {
        Object roomIdObj = body.get("roomId");
        if (roomIdObj == null) throw new IllegalArgumentException("roomId is required");
        Long roomId = Long.valueOf(roomIdObj.toString());
        String note = body.get("note") != null ? body.get("note").toString() : null;
        return ResponseEntity.ok(ApiResponse.success("Request approved",
                ownerService.approveRoomRequest(id, roomId, getUser(ud), note)));
    }

    @PostMapping("/room-requests/{id}/reject")
    public ResponseEntity<ApiResponse<RoomRequest>> rejectRoomRequest(@PathVariable Long id,
                                                                        @RequestBody(required = false) Map<String, String> body,
                                                                        @AuthenticationPrincipal UserDetails ud) {
        String note = body != null ? body.get("note") : null;
        return ResponseEntity.ok(ApiResponse.success("Request rejected",
                ownerService.rejectRoomRequest(id, getUser(ud), note)));
    }

    // ── Tenants ──────────────────────────────────────────
    @GetMapping("/tenants")
    public ResponseEntity<ApiResponse<List<RoomAssignment>>> getTenants(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", ownerService.getAllTenants(getUser(ud))));
    }

    // ── Vacate request review ────────────────────────────
    @GetMapping("/vacate-requests")
    public ResponseEntity<ApiResponse<List<RoomAssignment>>> getPendingVacateRequests(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", ownerService.getPendingVacateRequests(getUser(ud))));
    }

    @PostMapping("/vacate-requests/{id}/approve")
    public ResponseEntity<ApiResponse<RoomAssignment>> approveVacate(@PathVariable Long id,
                                                                       @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("Vacate approved",
                ownerService.approveVacate(id, getUser(ud))));
    }

    @PostMapping("/vacate-requests/{id}/reject")
    public ResponseEntity<ApiResponse<RoomAssignment>> rejectVacate(@PathVariable Long id,
                                                                      @RequestBody(required = false) Map<String, String> body,
                                                                      @AuthenticationPrincipal UserDetails ud) {
        String note = body != null ? body.get("note") : null;
        return ResponseEntity.ok(ApiResponse.success("Vacate rejected",
                ownerService.rejectVacate(id, getUser(ud), note)));
    }

    /** Owner-initiated eviction (no tenant request needed). */
    @PostMapping("/assignments/{id}/vacate")
    public ResponseEntity<ApiResponse<RoomAssignment>> manualVacate(@PathVariable Long id,
                                                                      @RequestBody(required = false) Map<String, String> body,
                                                                      @AuthenticationPrincipal UserDetails ud) {
        String note = body != null ? body.get("note") : null;
        return ResponseEntity.ok(ApiResponse.success("Tenant vacated",
                ownerService.manualVacate(id, getUser(ud), note)));
    }

    @GetMapping("/users/tenants")
    public ResponseEntity<ApiResponse<List<User>>> getAllTenantUsers() {
        return ResponseEntity.ok(ApiResponse.success("OK",
                userRepository.findAll().stream()
                        .filter(u -> u.getRole() == User.Role.TENANT)
                        .toList()));
    }

    // ── Complaints ───────────────────────────────────────
    @GetMapping("/complaints")
    public ResponseEntity<ApiResponse<List<Complaint>>> getComplaints(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", ownerService.getAllComplaints(getUser(ud))));
    }

    @PatchMapping("/complaints/{id}")
    public ResponseEntity<ApiResponse<Complaint>> updateComplaint(@PathVariable Long id,
                                                                    @RequestBody Map<String, String> body,
                                                                    @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("Updated",
                ownerService.updateComplaintStatus(id, body.get("status"), body.get("ownerNote"), getUser(ud))));
    }

    // ── Maintenance ──────────────────────────────────────
    @GetMapping("/maintenance")
    public ResponseEntity<ApiResponse<List<MaintenanceRequest>>> getMaintenance(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", ownerService.getAllMaintenanceRequests(getUser(ud))));
    }

    @PatchMapping("/maintenance/{id}")
    public ResponseEntity<ApiResponse<MaintenanceRequest>> updateMaintenance(@PathVariable Long id,
                                                                               @RequestBody Map<String, String> body,
                                                                               @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("Updated",
                ownerService.updateMaintenanceStatus(id, body.get("status"), body.get("assignedTo"), getUser(ud))));
    }

    // ── Announcements ────────────────────────────────────
    @PostMapping("/announcements")
    public ResponseEntity<ApiResponse<Announcement>> createAnnouncement(@AuthenticationPrincipal UserDetails ud,
                                                                          @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success("Announcement posted",
                ownerService.createAnnouncement(getUser(ud), body)));
    }

    @GetMapping("/announcements")
    public ResponseEntity<ApiResponse<List<Announcement>>> getAnnouncements(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", ownerService.getOwnerAnnouncements(getUser(ud))));
    }

    // ── Rent ─────────────────────────────────────────────
    @GetMapping("/rent")
    public ResponseEntity<ApiResponse<List<RentRecord>>> getRent(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", ownerService.getAllRentRecords(getUser(ud))));
    }

    @PatchMapping("/rent/{id}/mark-paid")
    public ResponseEntity<ApiResponse<RentRecord>> markPaid(@PathVariable Long id,
                                                             @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("Marked as paid", ownerService.markRentPaid(id, getUser(ud))));
    }

    // ── Owner Notifications ──────────────────────────────
    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<List<Notification>>> getNotifications(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", ownerService.getOwnerNotifications(getUser(ud))));
    }

    @GetMapping("/notifications/count")
    public ResponseEntity<ApiResponse<Long>> getOwnerUnreadCount(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", ownerService.getOwnerUnreadCount(getUser(ud))));
    }

    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markOwnerNotifRead(@PathVariable Long id,
                                                                  @AuthenticationPrincipal UserDetails ud) {
        ownerService.markOwnerNotificationRead(id, getUser(ud));
        return ResponseEntity.ok(ApiResponse.success("Marked as read", null));
    }

    @PostMapping("/notifications/mark-all-read")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAllOwnerRead(@AuthenticationPrincipal UserDetails ud) {
        int n = ownerService.markAllOwnerNotificationsRead(getUser(ud));
        return ResponseEntity.ok(ApiResponse.success(n + " notification(s) marked as read", Map.of("count", n)));
    }

    /** Manual rent-reminder trigger — pushes notifications to tenants with rent due in <=3 days. */
    @PostMapping("/rent/send-reminders")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendReminders(@AuthenticationPrincipal UserDetails ud) {
        int n = reminderService.sendRemindersForOwner(getUser(ud));
        return ResponseEntity.ok(ApiResponse.success(
                "Reminders sent",
                Map.of("count", n)));
    }

    // ── Food Menu ────────────────────────────────────────
    @GetMapping("/pg/{pgId}/food-menu")
    public ResponseEntity<ApiResponse<List<FoodMenu>>> getFoodMenu(@PathVariable Long pgId,
                                                                     @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK",
                foodMenuService.getMenuForPg(pgId, getUser(ud), true)));
    }

    @PutMapping("/pg/{pgId}/food-menu")
    public ResponseEntity<ApiResponse<FoodMenu>> upsertFoodMenuCell(@PathVariable Long pgId,
                                                                      @RequestBody Map<String, String> body,
                                                                      @AuthenticationPrincipal UserDetails ud) {
        FoodMenu.DayOfWeek day = FoodMenu.DayOfWeek.valueOf(body.get("dayOfWeek"));
        FoodMenu.MealType meal = FoodMenu.MealType.valueOf(body.get("mealType"));
        String items = body.get("items");
        String notes = body.get("notes");
        return ResponseEntity.ok(ApiResponse.success("Saved",
                foodMenuService.upsertCell(pgId, getUser(ud), day, meal, items, notes)));
    }

    // ── Forum (community board) ──────────────────────────
    @GetMapping("/forum/posts")
    public ResponseEntity<ApiResponse<List<ForumPost>>> listForumPosts(@RequestParam(required = false) Long pgId,
                                                                        @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", forumService.listForOwner(getUser(ud), pgId)));
    }

    @PostMapping("/forum/posts")
    public ResponseEntity<ApiResponse<ForumPost>> createForumPost(@RequestBody Map<String, Object> body,
                                                                    @AuthenticationPrincipal UserDetails ud) {
        Long pgId = body.get("pgId") != null ? Long.valueOf(body.get("pgId").toString()) : null;
        String title = body.get("title") != null ? body.get("title").toString() : "";
        String txt = body.get("body") != null ? body.get("body").toString() : "";
        return ResponseEntity.ok(ApiResponse.success("Posted",
                forumService.createPost(getUser(ud), pgId, title, txt)));
    }

    @PostMapping("/forum/posts/{id}/pin")
    public ResponseEntity<ApiResponse<ForumPost>> togglePin(@PathVariable Long id,
                                                              @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("Pin toggled", forumService.togglePin(id, getUser(ud))));
    }

    @DeleteMapping("/forum/posts/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteForumPost(@PathVariable Long id,
                                                               @AuthenticationPrincipal UserDetails ud) {
        forumService.deletePost(id, getUser(ud));
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }

    @GetMapping("/forum/posts/{id}/replies")
    public ResponseEntity<ApiResponse<List<ForumReply>>> listReplies(@PathVariable Long id,
                                                                       @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", forumService.listReplies(id, getUser(ud))));
    }

    @PostMapping("/forum/posts/{id}/replies")
    public ResponseEntity<ApiResponse<ForumReply>> addReply(@PathVariable Long id,
                                                              @RequestBody Map<String, String> body,
                                                              @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("Replied",
                forumService.addReply(id, getUser(ud), body.get("body"))));
    }

    @DeleteMapping("/forum/replies/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReply(@PathVariable Long id,
                                                          @AuthenticationPrincipal UserDetails ud) {
        forumService.deleteReply(id, getUser(ud));
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
}
