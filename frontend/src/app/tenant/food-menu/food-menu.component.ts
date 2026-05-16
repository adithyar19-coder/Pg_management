import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../core/services/api.service';
import { FoodMenuCell } from '../../core/models/models';

const DAYS = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'] as const;
const MEALS = ['BREAKFAST', 'LUNCH', 'SNACKS', 'DINNER'] as const;
type Day = typeof DAYS[number];
type Meal = typeof MEALS[number];

@Component({
  selector: 'app-tenant-food-menu',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './food-menu.component.html'
})
export class TenantFoodMenuComponent implements OnInit {
  cells: { [k: string]: FoodMenuCell } = {};
  loading = true;
  error = '';
  days = DAYS;
  meals = MEALS;

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.api.getFoodMenuTenant().subscribe({
      next: r => {
        for (const c of r.data || []) this.cells[`${c.dayOfWeek}_${c.mealType}`] = c;
        this.loading = false;
      },
      error: err => {
        this.error = err.error?.message || 'Could not load menu';
        this.loading = false;
      }
    });
  }

  itemsFor(d: Day, m: Meal) { return this.cells[`${d}_${m}`]?.items || ''; }
  notesFor(d: Day, m: Meal) { return this.cells[`${d}_${m}`]?.notes || ''; }

  todayCode(): Day {
    // JS getDay(): 0=Sun..6=Sat. Our enum order is MON..SUN.
    const map: Day[] = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];
    return map[new Date().getDay()];
  }

  mealEmoji(m: Meal): string {
    return { BREAKFAST: '🍳', LUNCH: '🍛', SNACKS: '☕', DINNER: '🍽️' }[m];
  }

  hasMenu(): boolean { return Object.keys(this.cells).length > 0; }
}
