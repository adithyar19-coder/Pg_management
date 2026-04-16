import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../core/services/api.service';
import { RentRecord } from '../../core/models/models';

@Component({
  selector: 'app-tenant-rent',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './rent.component.html'
})
export class TenantRentComponent implements OnInit {
  records: RentRecord[] = [];
  loading = true;

  constructor(private api: ApiService) {}
  ngOnInit() {
    this.api.getMyRent().subscribe({ next: r => { this.records = r.data || []; this.loading = false; }, error: () => this.loading = false });
  }

  totalPaid() { return this.records.filter(r => r.status === 'PAID').reduce((s, r) => s + +r.amount, 0); }
  totalPending() { return this.records.filter(r => r.status === 'PENDING').reduce((s, r) => s + +r.amount, 0); }
  pendingCount() { return this.records.filter(r => r.status === 'PENDING').length; }
  paidCount() { return this.records.filter(r => r.status === 'PAID').length; }
  statusClass(s: string) { return { PENDING:'badge-amber', PAID:'badge-green', OVERDUE:'badge-red' }[s] || 'badge-gray'; }
}
