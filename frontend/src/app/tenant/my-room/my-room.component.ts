import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { RoomAssignment } from '../../core/models/models';

@Component({
  selector: 'app-my-room',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './my-room.component.html'
})
export class MyRoomComponent implements OnInit {
  assignment: RoomAssignment | null = null;
  loading = true;

  // Vacate-request modal state
  showVacateModal = false;
  vacateLeaveDate = '';   // YYYY-MM-DD
  vacateReason = '';
  vacateSaving = false;
  vacateError = '';

  cancelling = false;

  constructor(private api: ApiService) {}

  ngOnInit() { this.load(); }

  load() {
    this.loading = true;
    this.api.getMyRoom().subscribe({
      next: r => { this.assignment = r.data || null; this.loading = false; },
      error: () => this.loading = false
    });
  }

  hasVacateRequest(): boolean {
    return !!this.assignment?.vacateRequestedAt;
  }

  openVacateModal() {
    // Default leave date = today + 30 days notice
    const d = new Date();
    d.setDate(d.getDate() + 30);
    this.vacateLeaveDate = d.toISOString().slice(0, 10);
    this.vacateReason = '';
    this.vacateError = '';
    this.showVacateModal = true;
  }

  submitVacate() {
    if (!this.vacateLeaveDate) {
      this.vacateError = 'Please choose a leave date';
      return;
    }
    this.vacateSaving = true;
    this.vacateError = '';
    this.api.requestVacate(this.vacateLeaveDate, this.vacateReason || undefined).subscribe({
      next: () => {
        this.vacateSaving = false;
        this.showVacateModal = false;
        this.load();
      },
      error: (err) => {
        this.vacateSaving = false;
        this.vacateError = err.error?.message || 'Could not submit vacate request';
      }
    });
  }

  cancelVacate() {
    if (!confirm('Cancel your vacate request?')) return;
    this.cancelling = true;
    this.api.cancelVacateRequest().subscribe({
      next: () => { this.cancelling = false; this.load(); },
      error: () => { this.cancelling = false; }
    });
  }
}
