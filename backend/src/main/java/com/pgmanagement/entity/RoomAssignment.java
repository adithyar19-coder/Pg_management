package com.pgmanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_assignments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RoomAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    private LocalDate joinDate;
    private LocalDate leaveDate;

    @Builder.Default
    private Boolean isActive = true;

    // ── Vacate request workflow ──────────────────────────────
    // When the tenant requests to vacate, these are populated.
    // The owner then approves (sets isActive=false, leaveDate=requestedLeaveDate)
    // or rejects (clears these fields).
    private LocalDateTime vacateRequestedAt;
    private LocalDate requestedLeaveDate;

    @Column(columnDefinition = "TEXT")
    private String vacateReason;
}
