import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Subscription, filter } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { ApiService } from '../../core/services/api.service';

@Component({
  selector: 'app-owner-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './owner-shell.component.html'
})
export class OwnerShellComponent implements OnInit, OnDestroy {
  user = this.auth.getCurrentUser();
  pageTitle = 'Dashboard';
  unreadMessages = 0;
  private routerSub?: Subscription;

  navItems = [
    { label: 'Dashboard', icon: '📊', route: '/owner/dashboard' },
    { label: 'My PGs', icon: '🏢', route: '/owner/pgs' },
    { label: 'Rooms', icon: '🚪', route: '/owner/rooms' },
    { label: 'Tenants', icon: '👥', route: '/owner/tenants' },
    { label: 'Complaints', icon: '📋', route: '/owner/complaints' },
    { label: 'Maintenance', icon: '🔧', route: '/owner/maintenance' },
    { label: 'Announcements', icon: '📢', route: '/owner/announcements' },
    { label: 'Rent Tracker', icon: '💰', route: '/owner/rent' },
    { label: 'Food Menu', icon: '🍽️', route: '/owner/food-menu' },
    { label: 'Community', icon: '💬', route: '/owner/community' },
    { label: 'Messages', icon: '✉️', route: '/owner/messages', badge: 'msg' }
  ];

  constructor(private auth: AuthService, private router: Router, private api: ApiService) {}

  ngOnInit() {
    this.refreshCounts();
    this.routerSub = this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe(() => this.refreshCounts());
  }

  ngOnDestroy() { this.routerSub?.unsubscribe(); }

  refreshCounts() {
    this.api.getUnreadMessageCount().subscribe({
      next: r => this.unreadMessages = r.data || 0,
      error: () => {}
    });
  }

  badgeValue(badge: string | undefined): number {
    if (badge === 'msg') return this.unreadMessages;
    return 0;
  }

  get initials() {
    return this.user?.name?.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2) ?? 'OW';
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
