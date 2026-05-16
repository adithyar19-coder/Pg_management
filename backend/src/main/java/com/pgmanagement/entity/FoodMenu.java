package com.pgmanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

/**
 * One menu cell = one (day-of-week × meal-type) slot for a PG.
 * Weekly recurring — same menu repeats every week unless owner edits.
 */
@Entity
@Table(name = "food_menus",
        uniqueConstraints = @UniqueConstraint(columnNames = {"pg_id", "day_of_week", "meal_type"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class FoodMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pg_id", nullable = false)
    private PG pg;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false)
    private MealType mealType;

    /** Comma-separated dish list, e.g. "Idli, Sambar, Coconut Chutney" */
    @Column(columnDefinition = "TEXT")
    private String items;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum DayOfWeek { MON, TUE, WED, THU, FRI, SAT, SUN }
    public enum MealType { BREAKFAST, LUNCH, SNACKS, DINNER }
}
