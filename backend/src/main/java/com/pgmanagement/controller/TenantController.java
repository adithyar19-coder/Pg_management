package com.pgmanagement.controller;

import com.pgmanagement.dto.ApiResponse;
import com.pgmanagement.entity.*;
import com.pgmanagement.repository.UserRepository;
import com.pgmanagement.service.FoodMenuService;
import com.pgmanagement.service.ForumService;
import com.pgmanagement.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tenant")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;
    private final UserRepository userRepository;
    private final FoodMenuService foodMenuService;
    private final ForumService forumService;

    private User getUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ── Dashboard ────────────────────────────────────────
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", tenantService.getDashboardStats(getUser(ud))));
    }

    // ── Room ─────────────────────────────────────────────
    @GetMapping("/my-room")
    public ResponseEntity<?> getMyRoom(@AuthenticationPrincipal UserDetails ud) {
        var result = tenantService.getMyRoom(getUser(ud));
        if (result.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success("OK", result.get()));
        }
        return ResponseEntity.ok(ApiResponse.success("No room assigned", null));
    }

    // ── PG search + Room request workflow ─────────────────
    @GetMapping("/pgs/search")
    public ResponseEntity<ApiResponse<List<PG>>> searchPgs(@RequestParam(required = false) String city,
                                                             @RequestParam(required = false) String locality) {
        return ResponseEntity.ok(ApiResponse.success("OK", tenantService.searchPGs(city, locality)));
    }

    @GetMapping("/pgs/{pgId}")
    public ResponseEntity<ApiResponse<PG>> getPgDetails(@PathVariable Long pgId) {
        return ResponseEntity.ok(ApiResponse.success("OK", tenantService.getPgById(pgId)));
    }

    @PostMapping("/room-requests")
    public ResponseEntity<ApiResponse<RoomRequest>> submitRoomRequest(@AuthenticationPrincipal UserDetails ud,
                                                                        @RequestBody Map<String, Object> body) {
        Object pgIdObj = body.get("pgId");
        if (pgIdObj == null) throw new IllegalArgumentException("pgId is required");
        Long pgId = Long.valueOf(pgIdObj.toString());
        String preferredType = body.get("preferredType") != null ? body.get("preferredType").toString() : null;
        Integer preferredFloor = body.get("preferredFloor") != null && !body.get("preferredFloor").toString().isBlank()
                ? Integer.valueOf(body.get("preferredFloor").toString()) : null;
        Boolean acPreference = body.get("acPreference") != null
                ? Boolean.valueOf(body.get("acPreference").toString()) : null;
        String notes = body.get("notes") != null ? body.get("notes").toString() : null;
        return ResponseEntity.ok(ApiResponse.success("Room request submitted",
                tenantService.submitRoomRequest(getUser(ud), pgId, preferredType, preferredFloor, acPreference, notes)));
    }

    @GetMapping("/room-requests")
    public ResponseEntity<ApiResponse<List<RoomRequest>>> getMyRoomRequests(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", tenantService.getMyRoomRequests(getUser(ud))));
    }

    @DeleteMapping("/room-requests/{id}")
    public ResponseEntity<ApiResponse<RoomRequest>> cancelRoomRequest(@PathVariable Long id,
                                                                        @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("Room request cancelled",
                tenantService.cancelRoomRequest(getUser(ud), id)));
    }

    // ── Vacate request ───────────────────────────────────
    @PostMapping("/vacate")
    public ResponseEntity<ApiResponse<RoomAssignment>> requestVacate(@AuthenticationPrincipal UserDetails ud,
                                                                       @RequestBody Map<String, Object> body) {
        Object dateObj = body.get("leaveDate");
        if (dateObj == null || dateObj.toString().isBlank()) {
            throw new IllegalArgumentException("leaveDate is required (YYYY-MM-DD)");
        }
        java.time.LocalDate leaveDate = java.time.LocalDate.parse(dateObj.toString());
        String reason = body.get("reason") != null ? body.get("reason").toString() : null;
        return ResponseEntity.ok(ApiResponse.success("Vacate request submitted",
                tenantService.requestVacate(getUser(ud), leaveDate, reason)));
    }

    @DeleteMapping("/vacate")
    public ResponseEntity<ApiResponse<Void>> cancelVacateRequest(@AuthenticationPrincipal UserDetails ud) {
        tenantService.cancelVacateRequest(getUser(ud));
        return ResponseEntity.ok(ApiResponse.success("Vacate request cancelled", null));
    }

    // ── Complaints ───────────────────────────────────────
    @PostMapping("/complaints")
    public ResponseEntity<ApiResponse<Complaint>> raiseComplaint(@AuthenticationPrincipal UserDetails ud,
                                                                   @RequestBody Map<String, Object> body) {
        String title = body.getOrDefault("title", "").toString();
        String description = body.getOrDefault("description", "").toString();
        if (title.isBlank()) throw new IllegalArgumentException("Title is required");
        @SuppressWarnings("unchecked")
        List<String> imageUrls = body.containsKey("imageUrls") && body.get("imageUrls") != null
                ? (List<String>) body.get("imageUrls") : List.of();
        return ResponseEntity.ok(ApiResponse.success("Complaint raised",
                tenantService.raiseComplaint(getUser(ud), title, description, imageUrls)));
    }

    @GetMapping("/complaints")
    public ResponseEntity<ApiResponse<List<Complaint>>> getMyComplaints(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", tenantService.getMyComplaints(getUser(ud))));
    }

    // ── Maintenance ──────────────────────────────────────
    @PostMapping("/maintenance")
    public ResponseEntity<ApiResponse<MaintenanceRequest>> raiseMaintenance(@AuthenticationPrincipal UserDetails ud,
                                                                              @RequestBody Map<String, Object> body) {
        String title = body.getOrDefault("title", "").toString();
        String issue = body.getOrDefault("issue", "").toString();
        if (title.isBlank()) throw new IllegalArgumentException("Title is required");
        if (issue.isBlank()) throw new IllegalArgumentException("Issue description is required");
        String priority = body.containsKey("priority") && body.get("priority") != null
                ? body.get("priority").toString() : "MEDIUM";
        @SuppressWarnings("unchecked")
        List<String> imageUrls = body.containsKey("imageUrls") && body.get("imageUrls") != null
                ? (List<String>) body.get("imageUrls") : List.of();
        return ResponseEntity.ok(ApiResponse.success("Maintenance request raised",
                tenantService.raiseMaintenance(getUser(ud), title, issue, priority, imageUrls)));
    }

    @GetMapping("/maintenance")
    public ResponseEntity<ApiResponse<List<MaintenanceRequest>>> getMyMaintenance(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", tenantService.getMyMaintenanceRequests(getUser(ud))));
    }

    // ── Rent ─────────────────────────────────────────────
    @GetMapping("/rent")
    public ResponseEntity<ApiResponse<List<RentRecord>>> getRentHistory(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", tenantService.getMyRentHistory(getUser(ud))));
    }

    // ── Announcements ────────────────────────────────────
    @GetMapping("/announcements")
    public ResponseEntity<ApiResponse<List<Announcement>>> getAnnouncements(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", tenantService.getMyAnnouncements(getUser(ud))));
    }

    // ── Notifications ─────────────────────────────────────
    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<List<Notification>>> getNotifications(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", tenantService.getMyNotifications(getUser(ud))));
    }

    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable Long id,
                                                       @AuthenticationPrincipal UserDetails ud) {
        tenantService.markNotificationRead(id, getUser(ud));
        return ResponseEntity.ok(ApiResponse.success("Marked as read", null));
    }

    @PostMapping("/notifications/mark-all-read")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAllRead(@AuthenticationPrincipal UserDetails ud) {
        int n = tenantService.markAllNotificationsRead(getUser(ud));
        return ResponseEntity.ok(ApiResponse.success(n + " notification(s) marked as read", Map.of("count", n)));
    }

    @GetMapping("/notifications/count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", tenantService.getUnreadNotificationCount(getUser(ud))));
    }

    // ── Food Menu (my PG) ────────────────────────────────
    @GetMapping("/food-menu")
    public ResponseEntity<ApiResponse<List<FoodMenu>>> getFoodMenu(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", foodMenuService.getMenuForTenant(getUser(ud))));
    }

    // ── Forum (community board for my PG) ────────────────
    @GetMapping("/forum/posts")
    public ResponseEntity<ApiResponse<List<ForumPost>>> listForumPosts(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", forumService.listForTenant(getUser(ud))));
    }

    @PostMapping("/forum/posts")
    public ResponseEntity<ApiResponse<ForumPost>> createForumPost(@RequestBody Map<String, Object> body,
                                                                    @AuthenticationPrincipal UserDetails ud) {
        String title = body.get("title") != null ? body.get("title").toString() : "";
        String txt = body.get("body") != null ? body.get("body").toString() : "";
        return ResponseEntity.ok(ApiResponse.success("Posted",
                forumService.createPost(getUser(ud), null, title, txt)));
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
