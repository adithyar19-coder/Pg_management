import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../core/services/api.service';
import { Announcement } from '../../core/models/models';

@Component({
  selector: 'app-tenant-announcements',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './announcements.component.html'
})
export class TenantAnnouncementsComponent implements OnInit {
  announcements: Announcement[] = [];
  loading = true;

  constructor(private api: ApiService) {}
  ngOnInit() {
    this.api.getTenantAnnouncements().subscribe({ next: r => { this.announcements = r.data || []; this.loading = false; }, error: () => this.loading = false });
  }

  priorityClass(p: string) { return { LOW:'badge-gray', NORMAL:'badge-blue', HIGH:'badge-amber', URGENT:'badge-red' }[p] || 'badge-gray'; }
  priorityIcon(p: string) { return { LOW:'📢', NORMAL:'📣', HIGH:'🔔', URGENT:'🚨' }[p] || '📢'; }
  priorityBg(p: string) { return { LOW:'var(--gray-50)', NORMAL:'var(--info-light)', HIGH:'var(--warning-light)', URGENT:'var(--danger-light)' }[p] || 'var(--gray-50)'; }
  priorityBorder(p: string) { return { LOW:'var(--gray-300)', NORMAL:'var(--info)', HIGH:'var(--warning)', URGENT:'var(--danger)' }[p] || 'var(--gray-300)'; }
}
