import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-tenant-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html'
})
export class TenantDashboardComponent implements OnInit {
  stats: any = {};
  complaints: any[] = [];
  rentRecords: any[] = [];
  announcements: any[] = [];
  loading = true;
  user = this.auth.getCurrentUser();

  constructor(private api: ApiService, private auth: AuthService) {}

  ngOnInit() {
    this.api.tenantDashboard().subscribe({ next: r => { this.stats = r.data; this.loading = false; }, error: () => this.loading = false });
    this.api.getMyComplaints().subscribe(r => this.complaints = (r.data || []).slice(0, 3));
    this.api.getMyRent().subscribe(r => this.rentRecords = (r.data || []).filter((x: any) => x.status === 'PENDING').slice(0, 3));
    this.api.getTenantAnnouncements().subscribe(r => this.announcements = (r.data || []).slice(0, 3));
  }

  badgeClass(s: string) { return { OPEN:'badge-red', IN_PROGRESS:'badge-amber', RESOLVED:'badge-green', REJECTED:'badge-gray' }[s] || 'badge-gray'; }
  priorityClass(p: string) { return { LOW:'badge-gray', NORMAL:'badge-blue', HIGH:'badge-amber', URGENT:'badge-red' }[p] || 'badge-gray'; }
}
