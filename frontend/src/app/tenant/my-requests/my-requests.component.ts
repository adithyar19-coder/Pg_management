import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
import { RoomRequest } from '../../core/models/models';

@Component({
  selector: 'app-my-requests',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './my-requests.component.html'
})
export class MyRequestsComponent implements OnInit {
  requests: RoomRequest[] = [];
  loading = true;
  error = '';

  constructor(private api: ApiService) {}

  ngOnInit() { this.load(); }

  load() {
    this.loading = true;
    this.api.getMyRoomRequests().subscribe({
      next: r => { this.requests = r.data || []; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  cancel(req: RoomRequest) {
    if (!confirm(`Cancel your room request for ${req.pg?.name}?`)) return;
    this.api.cancelRoomRequest(req.id).subscribe({
      next: () => this.load(),
      error: (err) => this.error = err.error?.message || 'Failed to cancel'
    });
  }

  statusClass(s: string) {
    switch (s) {
      case 'PENDING': return 'badge-yellow';
      case 'APPROVED': return 'badge-green';
      case 'REJECTED': return 'badge-red';
      case 'CANCELLED': return 'badge-gray';
      default: return 'badge-gray';
    }
  }

  statusIcon(s: string) {
    switch (s) {
      case 'PENDING': return '⏳';
      case 'APPROVED': return '✅';
      case 'REJECTED': return '✕';
      case 'CANCELLED': return '⊘';
      default: return '?';
    }
  }
}
