import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () => import('./auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./auth/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'owner',
    loadComponent: () => import('./owner/owner-shell/owner-shell.component').then(m => m.OwnerShellComponent),
    canActivate: [authGuard, roleGuard],
    data: { role: 'OWNER' },
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./owner/dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'pgs', loadComponent: () => import('./owner/pgs/pgs.component').then(m => m.PgsComponent) },
      { path: 'rooms', loadComponent: () => import('./owner/rooms/rooms.component').then(m => m.RoomsComponent) },
      { path: 'tenants', loadComponent: () => import('./owner/tenants/tenants.component').then(m => m.TenantsComponent) },
      { path: 'complaints', loadComponent: () => import('./owner/complaints/complaints.component').then(m => m.ComplaintsComponent) },
      { path: 'maintenance', loadComponent: () => import('./owner/maintenance/maintenance.component').then(m => m.MaintenanceComponent) },
      { path: 'announcements', loadComponent: () => import('./owner/announcements/announcements.component').then(m => m.AnnouncementsComponent) },
      { path: 'rent', loadComponent: () => import('./owner/rent/rent.component').then(m => m.RentComponent) }
    ]
  },
  {
    path: 'tenant',
    loadComponent: () => import('./tenant/tenant-shell/tenant-shell.component').then(m => m.TenantShellComponent),
    canActivate: [authGuard, roleGuard],
    data: { role: 'TENANT' },
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./tenant/dashboard/dashboard.component').then(m => m.TenantDashboardComponent) },
      { path: 'my-room', loadComponent: () => import('./tenant/my-room/my-room.component').then(m => m.MyRoomComponent) },
      { path: 'complaints', loadComponent: () => import('./tenant/complaints/complaints.component').then(m => m.TenantComplaintsComponent) },
      { path: 'maintenance', loadComponent: () => import('./tenant/maintenance/maintenance.component').then(m => m.TenantMaintenanceComponent) },
      { path: 'rent', loadComponent: () => import('./tenant/rent/rent.component').then(m => m.TenantRentComponent) },
      { path: 'announcements', loadComponent: () => import('./tenant/announcements/announcements.component').then(m => m.TenantAnnouncementsComponent) },
      { path: 'notifications', loadComponent: () => import('./tenant/notifications/notifications.component').then(m => m.NotificationsComponent) }
    ]
  },
  { path: '**', redirectTo: '/login' }
];
