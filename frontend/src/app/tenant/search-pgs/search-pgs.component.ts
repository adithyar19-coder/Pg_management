import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { PG } from '../../core/models/models';

@Component({
  selector: 'app-search-pgs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './search-pgs.component.html'
})
export class SearchPgsComponent implements OnInit {
  pgs: PG[] = [];
  loading = true;
  searching = false;
  city = '';
  locality = '';

  // Room request modal state
  showRequestModal = false;
  selectedPg: PG | null = null;
  submitting = false;
  requestError = '';
  successMsg = '';
  form = {
    preferredType: '',
    preferredFloor: '',
    acPreference: '',  // 'true' | 'false' | ''
    notes: ''
  };

  roomTypes = ['SINGLE', 'DOUBLE', 'TRIPLE', 'DORMITORY'];

  constructor(private api: ApiService) {}

  ngOnInit() { this.search(); }

  search() {
    this.searching = true;
    this.api.searchPgs(this.city.trim() || undefined, this.locality.trim() || undefined).subscribe({
      next: r => { this.pgs = r.data || []; this.loading = false; this.searching = false; },
      error: () => { this.loading = false; this.searching = false; }
    });
  }

  clearFilters() { this.city = ''; this.locality = ''; this.search(); }

  openRequest(pg: PG) {
    this.selectedPg = pg;
    this.form = { preferredType: '', preferredFloor: '', acPreference: '', notes: '' };
    this.requestError = '';
    this.successMsg = '';
    this.showRequestModal = true;
  }

  submitRequest() {
    if (!this.selectedPg) return;
    this.submitting = true;
    this.requestError = '';
    const payload: any = { pgId: this.selectedPg.id };
    if (this.form.preferredType) payload.preferredType = this.form.preferredType;
    if (this.form.preferredFloor) payload.preferredFloor = +this.form.preferredFloor;
    if (this.form.acPreference !== '') payload.acPreference = this.form.acPreference === 'true';
    if (this.form.notes?.trim()) payload.notes = this.form.notes.trim();

    this.api.submitRoomRequest(payload).subscribe({
      next: () => {
        this.submitting = false;
        this.successMsg = '✓ Request submitted! The owner will review and respond.';
        setTimeout(() => { this.showRequestModal = false; this.successMsg = ''; }, 1800);
      },
      error: (err) => {
        this.submitting = false;
        this.requestError = err.error?.message || 'Failed to submit request';
      }
    });
  }
}
