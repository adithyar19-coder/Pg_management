import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const required = route.data['role'];
  if (auth.getRole() === required) return true;
  const role = auth.getRole();
  if (role === 'OWNER') router.navigate(['/owner/dashboard']);
  else if (role === 'TENANT') router.navigate(['/tenant/dashboard']);
  else router.navigate(['/login']);
  return false;
};
