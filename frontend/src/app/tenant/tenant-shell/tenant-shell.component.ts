import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ApiService } from '../../core/services/api.service';

@Component({
  selector: 'app-tenant-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './tenant-shell.component.html'
})
export class TenantShellComponent implements OnInit {
  user = this.auth.getCurrentUser();
  unreadCount = 0;

  navItems = [
    { label: 'Dashboard', icon: '📊', route: '/tenant/dashboard' },
    { label: 'My Room', icon: '🚪', route: '/tenant/my-room' },
    { label: 'Raise Complaint', icon: '📋', route: '/tenant/complaints' },
    { label: 'Maintenance', icon: '🔧', route: '/tenant/maintenance' },
    { label: 'Rent History', icon: '💰', route: '/tenant/rent' },
    { label: 'Announcements', icon: '📢', route: '/tenant/announcements' },
    { label: 'Notifications', icon: '🔔', route: '/tenant/notifications', badge: true }
  ];

  constructor(private auth: AuthService, private router: Router, private api: ApiService) {}

  ngOnInit() {
    this.api.getUnreadCount().subscribe(r => this.unreadCount = r.data || 0);
  }

  get initials() {
    return this.user?.name?.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2) ?? 'TN';
  }

  logout() { this.auth.logout(); this.router.navigate(['/login']); }
}
