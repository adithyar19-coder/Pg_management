package com.pgmanagement.service;

import com.pgmanagement.entity.FoodMenu;
import com.pgmanagement.entity.PG;
import com.pgmanagement.entity.RoomAssignment;
import com.pgmanagement.entity.User;
import com.pgmanagement.repository.FoodMenuRepository;
import com.pgmanagement.repository.PGRepository;
import com.pgmanagement.repository.RoomAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FoodMenuService {

    private final FoodMenuRepository foodMenuRepository;
    private final PGRepository pgRepository;
    private final RoomAssignmentRepository roomAssignmentRepository;

    /** All menu cells for a PG (owner view). */
    public List<FoodMenu> getMenuForPg(Long pgId, User requester, boolean ownerScope) {
        PG pg = pgRepository.findById(pgId)
                .orElseThrow(() -> new RuntimeException("PG not found"));
        if (ownerScope && !pg.getOwner().getId().equals(requester.getId())) {
            throw new RuntimeException("Not authorized");
        }
        return foodMenuRepository.findByPg(pg);
    }

    /** Tenant view: return the menu of the PG they're currently assigned to. */
    public List<FoodMenu> getMenuForTenant(User tenant) {
        RoomAssignment a = roomAssignmentRepository.findByTenantAndIsActive(tenant, true)
                .orElseThrow(() -> new RuntimeException("You are not currently assigned to any PG"));
        return foodMenuRepository.findByPg(a.getRoom().getPg());
    }

    /** Upsert a single menu cell. */
    @Transactional
    public FoodMenu upsertCell(Long pgId, User owner, FoodMenu.DayOfWeek day,
                                FoodMenu.MealType meal, String items, String notes) {
        PG pg = pgRepository.findById(pgId)
                .orElseThrow(() -> new RuntimeException("PG not found"));
        if (!pg.getOwner().getId().equals(owner.getId())) throw new RuntimeException("Not authorized");

        FoodMenu cell = foodMenuRepository.findByPgAndDayOfWeekAndMealType(pg, day, meal)
                .orElseGet(() -> FoodMenu.builder().pg(pg).dayOfWeek(day).mealType(meal).build());
        cell.setItems(items);
        cell.setNotes(notes);
        return foodMenuRepository.save(cell);
    }
}
