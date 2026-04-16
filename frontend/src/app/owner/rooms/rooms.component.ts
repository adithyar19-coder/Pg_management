import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { PG, Room } from '../../core/models/models';

interface RoomRow {
  roomNumber: string;
  type: string;
  capacity: number;
  rentAmount: number | null;
}

@Component({
  selector: 'app-rooms',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './rooms.component.html'
})
export class RoomsComponent implements OnInit {
  rooms: Room[] = [];
  pgs: PG[] = [];
  loading = true;
  showModal = false;
  saving = false;
  form = { pgId: '', roomNumber: '', capacity: '1', rentAmount: '', type: 'SINGLE' };
  roomTypes = ['SINGLE', 'DOUBLE', 'TRIPLE', 'DORMITORY'];
  filterPgId = '';

  // Bulk-add state
  showBulkModal = false;
  bulkPgId = '';
  bulkCount = 5;
  rows: RoomRow[] = [];
  savingBulk = false;
  bulkError = '';

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.api.getPGs().subscribe(r => { this.pgs = r.data || []; });
    this.loadRooms();
  }

  loadRooms() {
    this.loading = true;
    this.api.getOwnerRooms().subscribe({ next: r => { this.rooms = r.data || []; this.loading = false; }, error: () => this.loading = false });
  }

  get filtered() {
    return this.filterPgId ? this.rooms.filter(r => r.pg?.id == +this.filterPgId) : this.rooms;
  }

  openAdd() { this.form = { pgId: this.pgs[0]?.id?.toString() || '', roomNumber: '', capacity: '1', rentAmount: '', type: 'SINGLE' }; this.showModal = true; }

  save() {
    this.saving = true;
    this.api.addRoom({ pgId: +this.form.pgId, roomNumber: this.form.roomNumber, capacity: +this.form.capacity, rentAmount: +this.form.rentAmount, type: this.form.type }).subscribe({
      next: () => { this.saving = false; this.showModal = false; this.loadRooms(); },
      error: () => this.saving = false
    });
  }

  // ── Occupancy helpers ─────────────────────────────────────
  occ(r: Room): number { return r.currentOccupancy ?? 0; }
  cap(r: Room): number { return r.capacity ?? 1; }
  isFull(r: Room): boolean { return this.occ(r) >= this.cap(r); }
  isPartial(r: Room): boolean { return this.occ(r) > 0 && !this.isFull(r); }
  isEmpty(r: Room): boolean { return this.occ(r) === 0; }
  occLabel(r: Room): string { return `${this.occ(r)}/${this.cap(r)}`; }
  occPct(r: Room): number {
    const c = this.cap(r);
    return c > 0 ? Math.round((this.occ(r) / c) * 100) : 0;
  }
  badgeClass(r: Room): string {
    if (this.isFull(r)) return 'badge-green';
    if (this.isPartial(r)) return 'badge-blue';
    return 'badge-gray';
  }
  borderColor(r: Room): string {
    if (this.isFull(r)) return 'var(--secondary)';
    if (this.isPartial(r)) return 'var(--primary)';
    return 'var(--gray-300)';
  }
  barColor(r: Room): string {
    if (this.isFull(r)) return 'var(--secondary)';
    if (this.isPartial(r)) return 'var(--primary)';
    return 'var(--gray-200)';
  }

  fullCount() { return this.filtered.filter(r => this.isFull(r)).length; }
  partialCount() { return this.filtered.filter(r => this.isPartial(r)).length; }
  vacantCount() { return this.filtered.filter(r => this.isEmpty(r)).length; }
  totalBeds() { return this.filtered.reduce((s, r) => s + this.cap(r), 0); }
  occupiedBeds() { return this.filtered.reduce((s, r) => s + this.occ(r), 0); }

  // ── Bulk add ──────────────────────────────────────────────
  openBulk() {
    this.bulkPgId = this.pgs[0]?.id?.toString() || '';
    this.bulkCount = 5;
    this.bulkError = '';
    this.generateRows();
    this.showBulkModal = true;
  }

  generateRows() {
    this.rows = [];
    const existingNumbers = new Set(
      this.rooms.filter(r => r.pg?.id == +this.bulkPgId).map(r => r.roomNumber)
    );
    // Try to continue numbering from the highest existing room number in this PG
    let start = 101;
    const existingNums = Array.from(existingNumbers)
      .map(n => parseInt(n, 10))
      .filter(n => !isNaN(n));
    if (existingNums.length > 0) start = Math.max(...existingNums) + 1;

    for (let i = 0; i < this.bulkCount; i++) {
      let rn = (start + i).toString();
      while (existingNumbers.has(rn)) rn = (parseInt(rn, 10) + 1).toString();
      existingNumbers.add(rn);
      this.rows.push({ roomNumber: rn, type: 'SINGLE', capacity: 1, rentAmount: null });
    }
  }

  addRow() {
    const lastNum = this.rows.length ? parseInt(this.rows[this.rows.length - 1].roomNumber, 10) : 100;
    const next = isNaN(lastNum) ? this.rows.length + 101 : lastNum + 1;
    this.rows.push({ roomNumber: next.toString(), type: 'SINGLE', capacity: 1, rentAmount: null });
  }

  removeRow(i: number) { this.rows.splice(i, 1); }

  onTypeChange(row: RoomRow) {
    if (row.type === 'SINGLE') row.capacity = 1;
    else if (row.type === 'DOUBLE') row.capacity = 2;
    else if (row.type === 'TRIPLE') row.capacity = 3;
    else if (row.type === 'DORMITORY' && row.capacity < 4) row.capacity = 4;
  }

  saveBulk() {
    this.bulkError = '';
    if (!this.bulkPgId) { this.bulkError = 'Please select a PG'; return; }
    const valid = this.rows.filter(r => r.roomNumber.trim() && r.rentAmount != null && (r.rentAmount as number) > 0);
    if (valid.length === 0) { this.bulkError = 'Enter at least one row with room number and rent'; return; }
    this.savingBulk = true;
    this.api.bulkAddRooms(+this.bulkPgId, valid).subscribe({
      next: () => { this.savingBulk = false; this.showBulkModal = false; this.rows = []; this.loadRooms(); },
      error: (err) => { this.savingBulk = false; this.bulkError = err.error?.message || 'Failed to save rooms'; }
    });
  }
}
