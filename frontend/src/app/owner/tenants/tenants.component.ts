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
  vacateRequests: RoomAssignment[] = [];
  tenantUsers: User[] = [];
  rooms: Room[] = [];
  loading = true;
  showModal = false;
  saving = false;
  selectedRoomId = '';
  selectedTenantId = '';
  search = '';
  error = '';

  // Reject-vacate inline state
  rejectingId: number | null = null;
  rejectNote = '';

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.loadAll();
  }

  /** Load every list this page depends on. Called on init AND every time the assign modal opens. */
  loadAll() {
    this.load();
    this.api.getAllTenantUsers().subscribe(r => this.tenantUsers = r.data || []);
    this.loadAvailableRooms();
    this.loadVacateRequests();
  }

  /** A room is "available" for assignment if it still has free beds (occupancy < capacity). */
  loadAvailableRooms() {
    this.api.getOwnerRooms().subscribe(r => {
      this.rooms = (r.data || []).filter(room => (room.currentOccupancy ?? 0) < (room.capacity ?? 1));
    });
  }

  loadVacateRequests() {
    this.api.getPendingVacateRequests().subscribe({
      next: r => this.vacateRequests = r.data || [],
      error: () => this.vacateRequests = []
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

  /** Per-tenant label for the assign dropdown — tells the owner which tenants are already in a room. */
  tenantOptionLabel(t: User): string {
    const active = this.assignments.find(a => a.tenant?.id === t.id);
    if (active) {
      return `${t.name} (${t.email}) — currently in Room ${active.room?.roomNumber}`;
    }
    return `${t.name} (${t.email}) — unassigned`;
  }

  unassignedCount(): number {
    const assignedIds = new Set(this.assignments.map(a => a.tenant?.id));
    return this.tenantUsers.filter(t => !assignedIds.has(t.id)).length;
  }

  roomLabel(r: Room): string {
    const occ = r.currentOccupancy ?? 0;
    const cap = r.capacity ?? 1;
    const free = cap - occ;
    return `Room ${r.roomNumber} — ${r.pg?.name} (₹${r.rentAmount}/mo) · ${free} of ${cap} bed(s) free`;
  }

  openAssignModal() {
    this.selectedRoomId = '';
    this.selectedTenantId = '';
    this.error = '';
    // Refresh both lists so newly registered tenants & changed room states show up immediately
    this.api.getAllTenantUsers().subscribe(r => this.tenantUsers = r.data || []);
    this.loadAvailableRooms();
    this.showModal = true;
  }

  assign() {
    if (!this.selectedRoomId || !this.selectedTenantId) return;
    this.saving = true;
    this.error = '';
    this.api.assignTenant(+this.selectedRoomId, +this.selectedTenantId).subscribe({
      next: () => {
        this.saving = false;
        this.showModal = false;
        this.loadAll();
      },
      error: (err) => {
        this.saving = false;
        this.error = err.error?.message || 'Failed to assign tenant';
      }
    });
  }

  // ── Vacate request actions ──────────────────────────────
  approveVacate(a: RoomAssignment) {
    if (!confirm(`Approve vacate for ${a.tenant?.name} (Room ${a.room?.roomNumber})?\nLeave date: ${a.requestedLeaveDate}`)) return;
    this.api.approveVacate(a.id).subscribe({
      next: () => this.loadAll(),
      error: (err) => alert(err.error?.message || 'Failed to approve')
    });
  }

  startReject(a: RoomAssignment) {
    this.rejectingId = a.id;
    this.rejectNote = '';
  }

  cancelReject() { this.rejectingId = null; this.rejectNote = ''; }

  confirmReject(a: RoomAssignment) {
    this.api.rejectVacate(a.id, this.rejectNote || undefined).subscribe({
      next: () => { this.rejectingId = null; this.rejectNote = ''; this.loadAll(); },
      error: (err) => alert(err.error?.message || 'Failed to reject')
    });
  }

  manualVacate(a: RoomAssignment) {
    const note = prompt(`Vacate ${a.tenant?.name} from Room ${a.room?.roomNumber} now?\nOptional note for the tenant:`);
    if (note === null) return; // cancelled
    this.api.manualVacate(a.id, note || undefined).subscribe({
      next: () => this.loadAll(),
      error: (err) => alert(err.error?.message || 'Failed to vacate')
    });
  }
}
