import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { PG } from '../../core/models/models';

interface RoomRow {
  roomNumber: string;
  type: string;
  capacity: number;
  rentAmount: number | null;
}

@Component({
  selector: 'app-pgs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pgs.component.html'
})
export class PgsComponent implements OnInit {
  pgs: PG[] = [];
  loading = true;
  showModal = false;
  editMode = false;
  saving = false;
  selectedId: number | null = null;

  form = { name: '', address: '', phone: '', rules: '', amenities: '', totalRooms: '' };

  // Setup-rooms modal state (shown after a new PG is created)
  showRoomsModal = false;
  setupPgId: number | null = null;
  setupPgName = '';
  rows: RoomRow[] = [];
  roomTypes = ['SINGLE', 'DOUBLE', 'TRIPLE', 'DORMITORY'];
  savingRooms = false;
  roomsError = '';

  constructor(private api: ApiService) {}

  ngOnInit() { this.load(); }

  load() {
    this.loading = true;
    this.api.getPGs().subscribe({ next: r => { this.pgs = r.data || []; this.loading = false; }, error: () => this.loading = false });
  }

  openAdd() {
    this.form = { name: '', address: '', phone: '', rules: '', amenities: '', totalRooms: '' };
    this.editMode = false;
    this.selectedId = null;
    this.showModal = true;
  }

  openEdit(pg: PG) {
    this.form = { name: pg.name, address: pg.address, phone: pg.phone || '', rules: pg.rules || '', amenities: pg.amenities || '', totalRooms: pg.totalRooms || '' };
    this.editMode = true;
    this.selectedId = pg.id;
    this.showModal = true;
  }

  save() {
    this.saving = true;
    const obs = this.editMode ? this.api.updatePG(this.selectedId!, this.form) : this.api.createPG(this.form);
    obs.subscribe({
      next: (res) => {
        this.saving = false;
        this.showModal = false;
        const createdPg = res.data;
        const count = parseInt(this.form.totalRooms, 10);
        this.load();
        // If creating a NEW PG with a positive totalRooms count, open the Setup Rooms modal
        if (!this.editMode && createdPg && !isNaN(count) && count > 0) {
          this.openSetupRooms(createdPg.id, createdPg.name, count);
        }
      },
      error: () => this.saving = false
    });
  }

  // ── Setup Rooms Flow ──────────────────────────────────────
  openSetupRooms(pgId: number, pgName: string, count: number) {
    this.setupPgId = pgId;
    this.setupPgName = pgName;
    this.rows = [];
    this.roomsError = '';
    const safe = Math.min(Math.max(count, 1), 100);
    for (let i = 0; i < safe; i++) {
      this.rows.push({
        roomNumber: (101 + i).toString(),
        type: 'SINGLE',
        capacity: 1,
        rentAmount: null
      });
    }
    this.showRoomsModal = true;
  }

  addRow() {
    const lastNum = this.rows.length ? parseInt(this.rows[this.rows.length - 1].roomNumber, 10) : 100;
    const next = isNaN(lastNum) ? this.rows.length + 101 : lastNum + 1;
    this.rows.push({ roomNumber: next.toString(), type: 'SINGLE', capacity: 1, rentAmount: null });
  }

  removeRow(i: number) {
    this.rows.splice(i, 1);
  }

  onTypeChange(row: RoomRow) {
    // Auto-adjust capacity when type changes (user can still override the number)
    if (row.type === 'SINGLE') row.capacity = 1;
    else if (row.type === 'DOUBLE') row.capacity = 2;
    else if (row.type === 'TRIPLE') row.capacity = 3;
    else if (row.type === 'DORMITORY' && (row.capacity ?? 1) < 4) row.capacity = 4;
  }

  saveAllRooms() {
    this.roomsError = '';
    const valid = this.rows.filter(r => r.roomNumber.trim() && r.rentAmount != null && (r.rentAmount as number) > 0);
    if (valid.length === 0) {
      this.roomsError = 'Please enter at least one valid row (room number + rent amount).';
      return;
    }
    this.savingRooms = true;
    this.api.bulkAddRooms(this.setupPgId!, valid).subscribe({
      next: () => {
        this.savingRooms = false;
        this.showRoomsModal = false;
        this.rows = [];
      },
      error: (err) => {
        this.savingRooms = false;
        this.roomsError = err.error?.message || 'Failed to save rooms';
      }
    });
  }

  skipSetup() {
    this.showRoomsModal = false;
    this.rows = [];
  }
}
