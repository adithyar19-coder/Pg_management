import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { Room, RoomRequest, RoomSuggestion } from '../../core/models/models';

@Component({
  selector: 'app-room-requests',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './room-requests.component.html'
})
export class RoomRequestsComponent implements OnInit {
  requests: RoomRequest[] = [];
  loading = true;
  filterStatus: 'PENDING' | 'ALL' = 'PENDING';
  error = '';

  // Suggest + Approve flow per-request
  suggesting: { [id: number]: boolean } = {};
  suggestions: { [id: number]: RoomSuggestion } = {};
  approving: { [id: number]: boolean } = {};
  rejectingId: number | null = null;
  rejectNote = '';

  // Override modal — picks a different vacant room manually
  showOverrideModal = false;
  overrideRequest: RoomRequest | null = null;
  overrideRooms: Room[] = [];
  overrideRoomId = '';
  overrideLoading = false;

  constructor(private api: ApiService) {}

  ngOnInit() { this.load(); }

  load() {
    this.loading = true;
    this.api.getRoomRequests(this.filterStatus).subscribe({
      next: r => { this.requests = r.data || []; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  changeFilter(s: 'PENDING' | 'ALL') {
    this.filterStatus = s;
    this.load();
  }

  // ── GA suggestion ─────────────────────────────────────
  suggest(req: RoomRequest) {
    this.suggesting[req.id] = true;
    this.error = '';
    this.api.suggestRoomForRequest(req.id).subscribe({
      next: r => {
        this.suggestions[req.id] = r.data;
        this.suggesting[req.id] = false;
      },
      error: (err) => {
        this.suggesting[req.id] = false;
        this.error = err.error?.message || 'Failed to get suggestion';
      }
    });
  }

  approveWithSuggestion(req: RoomRequest) {
    const sug = this.suggestions[req.id];
    if (!sug) return;
    this.approve(req, sug.roomId);
  }

  approve(req: RoomRequest, roomId: number, note?: string) {
    this.approving[req.id] = true;
    this.error = '';
    this.api.approveRoomRequest(req.id, roomId, note).subscribe({
      next: () => {
        this.approving[req.id] = false;
        this.showOverrideModal = false;
        this.load();
      },
      error: (err) => {
        this.approving[req.id] = false;
        this.error = err.error?.message || 'Failed to approve';
      }
    });
  }

  // ── Override flow (pick a different vacant room) ──────
  openOverride(req: RoomRequest) {
    this.overrideRequest = req;
    this.overrideRoomId = '';
    this.overrideLoading = true;
    this.showOverrideModal = true;
    if (!req.pg) { this.overrideLoading = false; return; }
    this.api.getRoomsByPG(req.pg.id).subscribe({
      next: r => {
        // Only show rooms with free beds
        this.overrideRooms = (r.data || []).filter(rm => (rm.currentOccupancy ?? 0) < (rm.capacity ?? 1));
        this.overrideLoading = false;
      },
      error: () => { this.overrideLoading = false; }
    });
  }

  confirmOverride() {
    if (!this.overrideRequest || !this.overrideRoomId) return;
    this.approve(this.overrideRequest, +this.overrideRoomId);
  }

  roomLabel(r: Room): string {
    const free = (r.capacity ?? 1) - (r.currentOccupancy ?? 0);
    return `Room ${r.roomNumber} · Floor ${r.floor ?? '?'} · ${r.type} · ${r.hasAc ? 'AC' : 'non-AC'} · ${free}/${r.capacity ?? 1} bed(s) free · ₹${r.rentAmount}/mo`;
  }

  // ── Reject flow ───────────────────────────────────────
  startReject(req: RoomRequest) {
    this.rejectingId = req.id;
    this.rejectNote = '';
  }

  cancelReject() { this.rejectingId = null; this.rejectNote = ''; }

  confirmReject(req: RoomRequest) {
    this.api.rejectRoomRequest(req.id, this.rejectNote || undefined).subscribe({
      next: () => { this.rejectingId = null; this.rejectNote = ''; this.load(); },
      error: (err) => this.error = err.error?.message || 'Failed to reject'
    });
  }

  // ── Helpers ───────────────────────────────────────────
  statusClass(s: string) {
    switch (s) {
      case 'PENDING': return 'badge-yellow';
      case 'APPROVED': return 'badge-green';
      case 'REJECTED': return 'badge-red';
      case 'CANCELLED': return 'badge-gray';
      default: return 'badge-gray';
    }
  }

  countPending() { return this.requests.filter(r => r.status === 'PENDING').length; }
}
