import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { Announcement, PG } from '../../core/models/models';

@Component({
  selector: 'app-announcements',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './announcements.component.html'
})
export class AnnouncementsComponent implements OnInit {
  announcements: Announcement[] = [];
  pgs: PG[] = [];
  loading = true;
  showModal = false;
  saving = false;
  form = { title: '', message: '', pgId: '', priority: 'NORMAL' };
  priorities = ['LOW','NORMAL','HIGH','URGENT'];

  constructor(private api: ApiService) {}
  ngOnInit() {
    this.load();
    this.api.getPGs().subscribe(r => this.pgs = r.data || []);
  }

  load() {
    this.loading = true;
    this.api.getOwnerAnnouncements().subscribe({ next: r => { this.announcements = r.data || []; this.loading = false; }, error: () => this.loading = false });
  }

  save() {
    this.saving = true;
    const payload: any = { title: this.form.title, message: this.form.message, priority: this.form.priority };
    if (this.form.pgId) payload.pgId = this.form.pgId;
    this.api.createAnnouncement(payload).subscribe({
      next: () => { this.saving = false; this.showModal = false; this.form = { title:'', message:'', pgId:'', priority:'NORMAL' }; this.load(); },
      error: () => this.saving = false
    });
  }

  priorityClass(p: string) { return { LOW:'badge-gray', NORMAL:'badge-blue', HIGH:'badge-amber', URGENT:'badge-red' }[p] || 'badge-gray'; }
  priorityIcon(p: string) { return { LOW:'📢', NORMAL:'📣', HIGH:'🔔', URGENT:'🚨' }[p] || '📢'; }
}
