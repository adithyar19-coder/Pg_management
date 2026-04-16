import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { MaintenanceRequest } from '../../core/models/models';

@Component({
  selector: 'app-maintenance',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './maintenance.component.html'
})
export class MaintenanceComponent implements OnInit {
  requests: MaintenanceRequest[] = [];
  loading = true;
  selected: MaintenanceRequest | null = null;
  showModal = false;
  saving = false;
  newStatus = '';
  assignedTo = '';
  filterStatus = '';

  statuses = ['PENDING','ASSIGNED','IN_PROGRESS','COMPLETED','CANCELLED'];

  constructor(private api: ApiService) {}
  ngOnInit() { this.load(); }

  load() {
    this.loading = true;
    this.api.getOwnerMaintenance().subscribe({ next: r => { this.requests = r.data || []; this.loading = false; }, error: () => this.loading = false });
  }

  get filtered() { return this.filterStatus ? this.requests.filter(r => r.status === this.filterStatus) : this.requests; }

  open(r: MaintenanceRequest) { this.selected = r; this.newStatus = r.status; this.assignedTo = r.assignedTo || ''; this.showModal = true; }

  update() {
    if (!this.selected) return;
    this.saving = true;
    this.api.updateMaintenance(this.selected.id, { status: this.newStatus, assignedTo: this.assignedTo }).subscribe({
      next: () => { this.saving = false; this.showModal = false; this.load(); },
      error: () => this.saving = false
    });
  }

  priorityClass(p: string) { return { LOW:'badge-gray', MEDIUM:'badge-blue', HIGH:'badge-amber', URGENT:'badge-red' }[p] || 'badge-gray'; }
  statusClass(s: string) { return { PENDING:'badge-amber', ASSIGNED:'badge-blue', IN_PROGRESS:'badge-purple', COMPLETED:'badge-green', CANCELLED:'badge-gray' }[s] || 'badge-gray'; }
  countByStatus(s: string) { return this.requests.filter(r => r.status === s as any).length; }
}
