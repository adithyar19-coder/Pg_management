import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/services/api.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  stats: any = {};
  complaints: any[] = [];
  rentRecords: any[] = [];
  loading = true;

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.api.ownerDashboard().subscribe({ next: r => { this.stats = r.data; this.loading = false; }, error: () => this.loading = false });
    this.api.getOwnerComplaints().subscribe({ next: r => { this.complaints = (r.data || []).slice(0, 5); } });
    this.api.getOwnerRent().subscribe({ next: r => { this.rentRecords = (r.data || []).filter((x: any) => x.status === 'PENDING').slice(0, 5); } });
  }

  badgeClass(status: string) {
    return { 'OPEN': 'badge-red', 'IN_PROGRESS': 'badge-amber', 'RESOLVED': 'badge-green', 'REJECTED': 'badge-gray' }[status] || 'badge-gray';
  }
}
