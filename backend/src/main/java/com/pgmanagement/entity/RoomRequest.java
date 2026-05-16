package com.pgmanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A tenant-initiated room request. The tenant submits this with their preferences
 * (room type, floor, AC) and the owner either approves (assigning a specific room)
 * or rejects. The owner can ask the Genetic Algorithm service for a "best match"
 * suggestion before deciding.
 */
@Entity
@Table(name = "room_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RoomRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The tenant who submitted the request. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    /** The PG the tenant wants to join. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pg_id", nullable = false)
    private PG pg;

    /** Preferred room type. Nullable = no preference. */
    @Enumerated(EnumType.STRING)
    private Room.RoomType preferredType;

    /** Preferred floor number. Nullable = no preference. */
    private Integer preferredFloor;

    /** AC required? Nullable = no preference. */
    private Boolean acPreference;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.PENDING;

    /** Room the owner finally assigned (only set when status=APPROVED). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_room_id")
    private Room assignedRoom;

    /** Owner's note on rejection or approval (optional). */
    @Column(columnDefinition = "TEXT")
    private String ownerNote;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime reviewedAt;

    public enum Status {
        PENDING, APPROVED, REJECTED, CANCELLED
    }
}
