import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { FoodMenuCell, PG } from '../../core/models/models';

const DAYS = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'] as const;
const MEALS = ['BREAKFAST', 'LUNCH', 'SNACKS', 'DINNER'] as const;
type Day = typeof DAYS[number];
type Meal = typeof MEALS[number];

@Component({
  selector: 'app-food-menu',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './food-menu.component.html'
})
export class OwnerFoodMenuComponent implements OnInit {
  pgs: PG[] = [];
  selectedPgId: number | null = null;
  cells: { [key: string]: FoodMenuCell } = {};   // key = day_meal
  loading = false;
  saving: { [key: string]: boolean } = {};
  saved: { [key: string]: boolean } = {};

  // Edit modal state
  editing: { day: Day; meal: Meal; items: string; notes: string } | null = null;

  days = DAYS;
  meals = MEALS;

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.api.getPGs().subscribe(r => {
      this.pgs = r.data || [];
      if (this.pgs.length > 0) {
        this.selectedPgId = this.pgs[0].id;
        this.load();
      }
    });
  }

  load() {
    if (!this.selectedPgId) return;
    this.loading = true;
    this.cells = {};
    this.api.getFoodMenuOwner(this.selectedPgId).subscribe({
      next: r => {
        for (const c of r.data || []) this.cells[this.key(c.dayOfWeek as Day, c.mealType as Meal)] = c;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  key(d: Day, m: Meal) { return `${d}_${m}`; }

  cellItems(d: Day, m: Meal): string {
    return this.cells[this.key(d, m)]?.items || '';
  }

  startEdit(d: Day, m: Meal) {
    const existing = this.cells[this.key(d, m)];
    this.editing = { day: d, meal: m, items: existing?.items || '', notes: existing?.notes || '' };
  }

  cancelEdit() { this.editing = null; }

  saveCell() {
    if (!this.editing || !this.selectedPgId) return;
    const k = this.key(this.editing.day, this.editing.meal);
    this.saving[k] = true;
    this.api.upsertFoodMenuCell(this.selectedPgId, {
      dayOfWeek: this.editing.day,
      mealType: this.editing.meal,
      items: this.editing.items,
      notes: this.editing.notes
    }).subscribe({
      next: r => {
        this.cells[k] = r.data;
        this.saving[k] = false;
        this.saved[k] = true;
        this.editing = null;
        setTimeout(() => this.saved[k] = false, 1200);
      },
      error: () => { this.saving[k] = false; }
    });
  }

  mealEmoji(m: Meal): string {
    return { BREAKFAST: '🍳', LUNCH: '🍛', SNACKS: '☕', DINNER: '🍽️' }[m];
  }
}
