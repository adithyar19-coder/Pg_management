import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  email = '';
  password = '';
  loading = false;
  error = '';

  demoAccounts = [
    { label: 'Owner Demo', icon: '🏢', email: 'owner@demo.com', password: 'demo123', color: 'var(--primary)' },
    { label: 'Tenant Demo', icon: '👤', email: 'tenant@demo.com', password: 'demo123', color: 'var(--secondary)' }
  ];

  constructor(private auth: AuthService, private router: Router) {
    if (auth.isLoggedIn()) this.redirect();
  }

  loginAs(account: { email: string; password: string }) {
    this.email = account.email;
    this.password = account.password;
    this.loading = true;
    this.error = '';
    this.auth.login({ email: account.email, password: account.password }).subscribe({
      next: res => {
        this.loading = false;
        if (res.success) this.redirect();
        else this.error = res.message;
      },
      error: err => {
        this.loading = false;
        this.error = err.error?.message || 'Could not connect to server. Make sure the backend is running on port 8082.';
      }
    });
  }

  submit() {
    if (!this.email || !this.password) { this.error = 'Please fill all fields'; return; }
    this.loading = true; this.error = '';
    this.auth.login({ email: this.email, password: this.password }).subscribe({
      next: res => {
        this.loading = false;
        if (res.success) this.redirect();
        else this.error = res.message;
      },
      error: err => {
        this.loading = false;
        this.error = err.error?.message || 'Could not connect to server. Make sure the backend is running on port 8082.';
      }
    });
  }

  private redirect() {
    const role = this.auth.getRole();
    if (role === 'OWNER') this.router.navigate(['/owner/dashboard']);
    else if (role === 'TENANT') this.router.navigate(['/tenant/dashboard']);
  }
}
