import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { Complaint } from '../../core/models/models';

@Component({
  selector: 'app-complaints',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './complaints.component.html'
})
export class ComplaintsComponent implements OnInit {
  complaints: Complaint[] = [];
  loading = true;
  selected: Complaint | null = null;
  showModal = false;
  saving = false;
  newStatus = '';
  ownerNote = '';
  filterStatus = '';

  statuses = ['OPEN','IN_PROGRESS','RESOLVED','REJECTED'];

  constructor(private api: ApiService) {}

  ngOnInit() { this.load(); }

  load() {
    this.loading = true;
    this.api.getOwnerComplaints().subscribe({ next: r => { this.complaints = r.data || []; this.loading = false; }, error: () => this.loading = false });
  }

  get filtered() {
    return this.filterStatus ? this.complaints.filter(c => c.status === this.filterStatus) : this.complaints;
  }

  open(c: Complaint) { this.selected = c; this.newStatus = c.status; this.ownerNote = c.ownerNote || ''; this.showModal = true; }

  update() {
    if (!this.selected) return;
    this.saving = true;
    this.api.updateComplaint(this.selected.id, { status: this.newStatus, ownerNote: this.ownerNote }).subscribe({
      next: () => { this.saving = false; this.showModal = false; this.load(); },
      error: () => this.saving = false
    });
  }

  badgeClass(s: string) { return { OPEN:'badge-red', IN_PROGRESS:'badge-amber', RESOLVED:'badge-green', REJECTED:'badge-gray' }[s] || 'badge-gray'; }
  badgeIcon(s: string) { return { OPEN:'🔴', IN_PROGRESS:'🟡', RESOLVED:'🟢', REJECTED:'⚫' }[s] || ''; }
  countByStatus(s: string) { return this.complaints.filter(c => c.status === s as any).length; }
}
