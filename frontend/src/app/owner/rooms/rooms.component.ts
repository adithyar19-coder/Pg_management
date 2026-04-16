import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { PG, Room } from '../../core/models/models';

@Component({
  selector: 'app-rooms',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './rooms.component.html'
})
export class RoomsComponent implements OnInit {
  rooms: Room[] = [];
  pgs: PG[] = [];
  loading = true;
  showModal = false;
  saving = false;
  form = { pgId: '', roomNumber: '', capacity: '1', rentAmount: '', type: 'SINGLE' };
  roomTypes = ['SINGLE', 'DOUBLE', 'TRIPLE', 'DORMITORY'];
  filterPgId = '';

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.api.getPGs().subscribe(r => { this.pgs = r.data || []; });
    this.loadRooms();
  }

  loadRooms() {
    this.loading = true;
    this.api.getOwnerRooms().subscribe({ next: r => { this.rooms = r.data || []; this.loading = false; }, error: () => this.loading = false });
  }

  get filtered() {
    return this.filterPgId ? this.rooms.filter(r => r.pg?.id == +this.filterPgId) : this.rooms;
  }

  openAdd() { this.form = { pgId: this.pgs[0]?.id?.toString() || '', roomNumber: '', capacity: '1', rentAmount: '', type: 'SINGLE' }; this.showModal = true; }

  save() {
    this.saving = true;
    this.api.addRoom({ pgId: +this.form.pgId, roomNumber: this.form.roomNumber, capacity: +this.form.capacity, rentAmount: +this.form.rentAmount, type: this.form.type }).subscribe({
      next: () => { this.saving = false; this.showModal = false; this.loadRooms(); },
      error: () => this.saving = false
    });
  }

  occupiedCount() { return this.filtered.filter(r => r.isOccupied).length; }
  vacantCount() { return this.filtered.filter(r => !r.isOccupied).length; }
  occupancyPct() {
    if (!this.filtered.length) return 0;
    return Math.round((this.occupiedCount() / this.filtered.length) * 100);
  }
}
