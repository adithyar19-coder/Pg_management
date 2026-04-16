import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { Room, RoomAssignment, User } from '../../core/models/models';

@Component({
  selector: 'app-tenants',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tenants.component.html'
})
export class TenantsComponent implements OnInit {
  assignments: RoomAssignment[] = [];
  tenantUsers: User[] = [];
  rooms: Room[] = [];
  loading = true;
  showModal = false;
  saving = false;
  selectedRoomId = '';
  selectedTenantId = '';
  search = '';

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.load();
    this.api.getAllTenantUsers().subscribe(r => this.tenantUsers = r.data || []);
    this.api.getOwnerRooms().subscribe(r => this.rooms = (r.data || []).filter(r => !r.isOccupied));
  }

  load() {
    this.loading = true;
    this.api.getActiveTenants().subscribe({ next: r => { this.assignments = r.data || []; this.loading = false; }, error: () => this.loading = false });
  }

  get filtered() {
    const q = this.search.toLowerCase();
    return q ? this.assignments.filter(a => a.tenant?.name?.toLowerCase().includes(q) || a.room?.roomNumber?.includes(q)) : this.assignments;
  }

  assign() {
    if (!this.selectedRoomId || !this.selectedTenantId) return;
    this.saving = true;
    this.api.assignTenant(+this.selectedRoomId, +this.selectedTenantId).subscribe({
      next: () => { this.saving = false; this.showModal = false; this.load(); this.api.getOwnerRooms().subscribe(r => this.rooms = (r.data || []).filter(r => !r.isOccupied)); },
      error: () => this.saving = false
    });
  }
}
