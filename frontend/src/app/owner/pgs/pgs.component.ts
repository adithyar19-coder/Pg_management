import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { PG } from '../../core/models/models';

@Component({
  selector: 'app-pgs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pgs.component.html'
})
export class PgsComponent implements OnInit {
  pgs: PG[] = [];
  loading = true;
  showModal = false;
  editMode = false;
  saving = false;
  selectedId: number | null = null;

  form = { name: '', address: '', phone: '', rules: '', amenities: '', totalRooms: '' };

  constructor(private api: ApiService) {}

  ngOnInit() { this.load(); }

  load() {
    this.loading = true;
    this.api.getPGs().subscribe({ next: r => { this.pgs = r.data || []; this.loading = false; }, error: () => this.loading = false });
  }

  openAdd() { this.form = { name: '', address: '', phone: '', rules: '', amenities: '', totalRooms: '' }; this.editMode = false; this.selectedId = null; this.showModal = true; }

  openEdit(pg: PG) {
    this.form = { name: pg.name, address: pg.address, phone: pg.phone || '', rules: pg.rules || '', amenities: pg.amenities || '', totalRooms: pg.totalRooms || '' };
    this.editMode = true; this.selectedId = pg.id; this.showModal = true;
  }

  save() {
    this.saving = true;
    const obs = this.editMode ? this.api.updatePG(this.selectedId!, this.form) : this.api.createPG(this.form);
    obs.subscribe({ next: () => { this.saving = false; this.showModal = false; this.load(); }, error: () => this.saving = false });
  }
}
