import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../core/services/api.service';
import { Notification } from '../../core/models/models';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notifications.component.html'
})
export class NotificationsComponent implements OnInit {
  notifications: Notification[] = [];
  loading = true;

  constructor(private api: ApiService) {}

  ngOnInit() { this.load(); }

  load() {
    this.loading = true;
    this.api.getNotifications().subscribe({
      next: r => { this.notifications = r.data || []; this.loading = false; },
      error: () => this.loading = false
    });
  }

  markRead(n: Notification) {
    if (n.isRead) return;
    this.api.markNotifRead(n.id).subscribe(() => n.isRead = true);
  }

  markAllRead() {
    this.notifications.filter(n => !n.isRead).forEach(n => this.markRead(n));
  }

  get unreadCount() { return this.notifications.filter(n => !n.isRead).length; }

  typeIcon(t: string) {
    return { COMPLAINT:'📋', RENT:'💰', MAINTENANCE:'🔧', ANNOUNCEMENT:'📢', ASSIGNMENT:'🚪' }[t] || '🔔';
  }

  typeBg(t: string) {
    return { COMPLAINT:'var(--danger-light)', RENT:'var(--warning-light)', MAINTENANCE:'var(--info-light)', ANNOUNCEMENT:'var(--primary-light)', ASSIGNMENT:'var(--secondary-light)' }[t] || 'var(--gray-100)';
  }
}
