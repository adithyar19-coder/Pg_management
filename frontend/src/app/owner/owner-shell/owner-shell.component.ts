import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-owner-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './owner-shell.component.html'
})
export class OwnerShellComponent {
  user = this.auth.getCurrentUser();
  pageTitle = 'Dashboard';

  navItems = [
    { label: 'Dashboard', icon: '📊', route: '/owner/dashboard' },
    { label: 'My PGs', icon: '🏢', route: '/owner/pgs' },
    { label: 'Rooms', icon: '🚪', route: '/owner/rooms' },
    { label: 'Tenants', icon: '👥', route: '/owner/tenants' },
    { label: 'Complaints', icon: '📋', route: '/owner/complaints' },
    { label: 'Maintenance', icon: '🔧', route: '/owner/maintenance' },
    { label: 'Announcements', icon: '📢', route: '/owner/announcements' },
    { label: 'Rent Tracker', icon: '💰', route: '/owner/rent' }
  ];

  constructor(private auth: AuthService, private router: Router) {}

  get initials() {
    return this.user?.name?.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2) ?? 'OW';
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
