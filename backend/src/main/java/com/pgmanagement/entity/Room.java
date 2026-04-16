package com.pgmanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pg_id", nullable = false)
    private PG pg;

    @Column(nullable = false)
    private String roomNumber;

    private Integer capacity;

    @Column(precision = 10, scale = 2)
    private BigDecimal rentAmount;

    @Builder.Default
    private Boolean isOccupied = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RoomType type = RoomType.SINGLE;

    public enum RoomType {
        SINGLE, DOUBLE, TRIPLE, DORMITORY
    }
}
