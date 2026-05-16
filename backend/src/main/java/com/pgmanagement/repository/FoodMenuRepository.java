package com.pgmanagement.repository;

import com.pgmanagement.entity.FoodMenu;
import com.pgmanagement.entity.PG;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoodMenuRepository extends JpaRepository<FoodMenu, Long> {
    List<FoodMenu> findByPg(PG pg);
    Optional<FoodMenu> findByPgAndDayOfWeekAndMealType(PG pg, FoodMenu.DayOfWeek day, FoodMenu.MealType meal);
}
