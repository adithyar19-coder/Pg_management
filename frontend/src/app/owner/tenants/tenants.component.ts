import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { Room, RoomAssignment, User } from '../../core/models/models';

@Component({
  selector: 'app-tenants',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tenants.component.html'
})
export class TenantsComponent implements OnInit {
  assignments: RoomAssignment[] = [];
  tenantUsers: User[] = [];
  rooms: Room[] = [];
  loading = true;
  showModal = false;
  saving = false;
  selectedRoomId = '';
  selectedTenantId = '';
  search = '';
  error = '';

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.load();
    this.api.getAllTenantUsers().subscribe(r => this.tenantUsers = r.data || []);
    this.loadAvailableRooms();
  }

  /** A room is "available" for assignment if it still has free beds (occupancy < capacity). */
  loadAvailableRooms() {
    this.api.getOwnerRooms().subscribe(r => {
      this.rooms = (r.data || []).filter(room => (room.currentOccupancy ?? 0) < (room.capacity ?? 1));
    });
  }

  load() {
    this.loading = true;
    this.api.getActiveTenants().subscribe({ next: r => { this.assignments = r.data || []; this.loading = false; }, error: () => this.loading = false });
  }

  get filtered() {
    const q = this.search.toLowerCase();
    return q ? this.assignments.filter(a => a.tenant?.name?.toLowerCase().includes(q) || a.room?.roomNumber?.includes(q)) : this.assignments;
  }

  roomLabel(r: Room): string {
    const occ = r.currentOccupancy ?? 0;
    const cap = r.capacity ?? 1;
    const free = cap - occ;
    return `Room ${r.roomNumber} — ${r.pg?.name} (₹${r.rentAmount}/mo) · ${free} of ${cap} bed(s) free`;
  }

  assign() {
    if (!this.selectedRoomId || !this.selectedTenantId) return;
    this.saving = true;
    this.error = '';
    this.api.assignTenant(+this.selectedRoomId, +this.selectedTenantId).subscribe({
      next: () => {
        this.saving = false;
        this.showModal = false;
        this.load();
        this.loadAvailableRooms();
      },
      error: (err) => {
        this.saving = false;
        this.error = err.error?.message || 'Failed to assign tenant';
      }
    });
  }
}
