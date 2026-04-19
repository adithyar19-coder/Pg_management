package com.pgmanagement.controller;

import com.pgmanagement.dto.ApiResponse;
import com.pgmanagement.entity.*;
import com.pgmanagement.repository.UserRepository;
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

    @GetMapping("/notifications/count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", tenantService.getUnreadNotificationCount(getUser(ud))));
    }
}
