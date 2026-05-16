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
      next: r => {
        this.notifications = r.data || [];
        this.loading = false;
        // Auto-clear the unread badge: mark every unread notification as read server-side
        // The page itself keeps showing the previously-unread ones with a highlighted style
        // so the user can still tell what's new.
        const wasUnread = this.notifications.filter(n => !n.isRead);
        if (wasUnread.length > 0) {
          this.api.markAllNotifsRead().subscribe(() => {
            wasUnread.forEach(n => { (n as any).wasUnread = true; n.isRead = true; });
          });
        }
      },
      error: () => this.loading = false
    });
  }

  markRead(n: Notification) {
    if (n.isRead) return;
    this.api.markNotifRead(n.id).subscribe(() => n.isRead = true);
  }

  markAllRead() {
    this.api.markAllNotifsRead().subscribe(() => {
      this.notifications.forEach(n => n.isRead = true);
    });
  }

  get unreadCount() { return this.notifications.filter(n => !n.isRead).length; }

  typeIcon(t: string) {
    return {
      COMPLAINT: '📋', RENT: '💰', RENT_REMINDER: '⏰',
      MAINTENANCE: '🔧', ANNOUNCEMENT: '📢', ASSIGNMENT: '🚪',
      VACATE: '🚪', VACATE_REJECTED: '✕',
      REQUEST_APPROVED: '✅', REQUEST_REJECTED: '✕'
    }[t] || '🔔';
  }

  typeBg(t: string) {
    return {
      COMPLAINT: 'var(--danger-light)', RENT: 'var(--warning-light)', RENT_REMINDER: 'var(--warning-light)',
      MAINTENANCE: 'var(--info-light)', ANNOUNCEMENT: 'var(--primary-light)', ASSIGNMENT: 'var(--secondary-light)',
      REQUEST_APPROVED: 'var(--secondary-light)', REQUEST_REJECTED: 'var(--danger-light)'
    }[t] || 'var(--gray-100)';
  }
}
