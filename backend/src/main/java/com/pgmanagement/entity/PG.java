package com.pgmanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pgs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PG {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(columnDefinition = "TEXT")
    private String rules;

    @Column(columnDefinition = "TEXT")
    private String amenities;

    private String phone;
    private String totalRooms;

    // ── Location fields (for tenant search) ───────────────────────────
    /** City e.g. "Bengaluru", "Mumbai". Used for primary location filter. */
    private String city;
    /** Sub-area / locality e.g. "Koramangala", "HSR Layout". Used for fine-grained filter. */
    private String locality;
}
