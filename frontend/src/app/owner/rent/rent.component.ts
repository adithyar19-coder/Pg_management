import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { RentRecord } from '../../core/models/models';

@Component({
  selector: 'app-rent',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './rent.component.html'
})
export class RentComponent implements OnInit {
  records: RentRecord[] = [];
  loading = true;
  saving: number | null = null;
  filterStatus = '';
  search = '';

  constructor(private api: ApiService) {}
  ngOnInit() { this.load(); }

  load() {
    this.loading = true;
    this.api.getOwnerRent().subscribe({ next: r => { this.records = r.data || []; this.loading = false; }, error: () => this.loading = false });
  }

  get filtered() {
    let list = this.filterStatus ? this.records.filter(r => r.status === this.filterStatus) : this.records;
    if (this.search) { const q = this.search.toLowerCase(); list = list.filter(r => r.tenant?.name?.toLowerCase().includes(q) || r.month?.includes(q)); }
    return list;
  }

  markPaid(id: number) {
    this.saving = id;
    this.api.markRentPaid(id).subscribe({ next: () => { this.saving = null; this.load(); }, error: () => this.saving = null });
  }

  totalPending() { return this.records.filter(r => r.status === 'PENDING').reduce((s, r) => s + +r.amount, 0); }
  totalCollected() { return this.records.filter(r => r.status === 'PAID').reduce((s, r) => s + +r.amount, 0); }
  pendingCount() { return this.records.filter(r => r.status === 'PENDING').length; }
  paidCount() { return this.records.filter(r => r.status === 'PAID').length; }
  statusClass(s: string) { return { PENDING:'badge-amber', PAID:'badge-green', OVERDUE:'badge-red' }[s] || 'badge-gray'; }
}
