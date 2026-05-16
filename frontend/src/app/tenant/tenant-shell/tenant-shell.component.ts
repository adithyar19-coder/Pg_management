import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Subscription, filter } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { ApiService } from '../../core/services/api.service';

@Component({
  selector: 'app-tenant-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './tenant-shell.component.html'
})
export class TenantShellComponent implements OnInit, OnDestroy {
  user = this.auth.getCurrentUser();
  unreadNotifications = 0;
  unreadMessages = 0;
  private routerSub?: Subscription;

  navItems = [
    { label: 'Dashboard', icon: '📊', route: '/tenant/dashboard' },
    { label: 'Find a PG', icon: '🔍', route: '/tenant/search-pgs' },
    { label: 'My Room', icon: '🚪', route: '/tenant/my-room' },
    { label: 'Food Menu', icon: '🍽️', route: '/tenant/food-menu' },
    { label: 'Community', icon: '💬', route: '/tenant/community' },
    { label: 'Messages', icon: '✉️', route: '/tenant/messages', badge: 'msg' },
    { label: 'Raise Complaint', icon: '📋', route: '/tenant/complaints' },
    { label: 'Maintenance', icon: '🔧', route: '/tenant/maintenance' },
    { label: 'Rent History', icon: '💰', route: '/tenant/rent' },
    { label: 'Announcements', icon: '📢', route: '/tenant/announcements' },
    { label: 'Notifications', icon: '🔔', route: '/tenant/notifications', badge: 'notif' }
  ];

  constructor(private auth: AuthService, private router: Router, private api: ApiService) {}

  ngOnInit() {
    this.refreshCounts();
    // Re-fetch counts after every successful navigation so badges reflect
    // freshly-read items as soon as the user comes back to any page.
    this.routerSub = this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe(() => this.refreshCounts());
  }

  ngOnDestroy() { this.routerSub?.unsubscribe(); }

  /** Pull both unread counts from server. Errors are swallowed silently — the badge just won't update. */
  refreshCounts() {
    this.api.getUnreadCount().subscribe({
      next: r => this.unreadNotifications = r.data || 0,
      error: () => {}
    });
    this.api.getUnreadMessageCount().subscribe({
      next: r => this.unreadMessages = r.data || 0,
      error: () => {}
    });
  }

  badgeValue(badge: string | undefined): number {
    if (badge === 'notif') return this.unreadNotifications;
    if (badge === 'msg') return this.unreadMessages;
    return 0;
  }

  get initials() {
    return this.user?.name?.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2) ?? 'TN';
  }

  logout() { this.auth.logout(); this.router.navigate(['/login']); }
}
