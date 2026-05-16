import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
import { RoomAssignment, User } from '../../core/models/models';

@Component({
  selector: 'app-tenants',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './tenants.component.html'
})
export class TenantsComponent implements OnInit {
  assignments: RoomAssignment[] = [];
  vacateRequests: RoomAssignment[] = [];
  tenantUsers: User[] = [];
  loading = true;
  search = '';

  // Reject-vacate inline state
  rejectingId: number | null = null;
  rejectNote = '';

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.loadAll();
  }

  loadAll() {
    this.load();
    this.api.getAllTenantUsers().subscribe(r => this.tenantUsers = r.data || []);
    this.loadVacateRequests();
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
