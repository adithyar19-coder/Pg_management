package com.pgmanagement.controller;

import com.pgmanagement.dto.ApiResponse;
import com.pgmanagement.entity.*;
import com.pgmanagement.repository.UserRepository;
import com.pgmanagement.service.OwnerService;
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

    @PostMapping("/rooms/{roomId}/assign")
    public ResponseEntity<ApiResponse<RoomAssignment>> assignTenant(@PathVariable Long roomId,
                                                                      @RequestBody Map<String, Long> body,
                                                                      @AuthenticationPrincipal UserDetails ud) {
        Long tenantId = body.get("tenantId");
        return ResponseEntity.ok(ApiResponse.success("Tenant assigned",
                ownerService.assignTenant(roomId, tenantId, getUser(ud))));
    }

    // ── Tenants ──────────────────────────────────────────
    @GetMapping("/tenants")
    public ResponseEntity<ApiResponse<List<RoomAssignment>>> getTenants(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", ownerService.getAllTenants(getUser(ud))));
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
}
