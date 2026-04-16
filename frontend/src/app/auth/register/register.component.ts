import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.component.html'
})
export class RegisterComponent {
  name = '';
  email = '';
  password = '';
  phone = '';
  role: 'OWNER' | 'TENANT' = 'OWNER';
  loading = false;
  error = '';

  constructor(private auth: AuthService, private router: Router) {}

  selectRole(r: 'OWNER' | 'TENANT') { this.role = r; }

  submit() {
    if (!this.name || !this.email || !this.password) { this.error = 'Please fill all required fields'; return; }
    if (this.password.length < 6) { this.error = 'Password must be at least 6 characters'; return; }
    this.loading = true; this.error = '';
    this.auth.register({ name: this.name, email: this.email, password: this.password, phone: this.phone, role: this.role }).subscribe({
      next: res => {
        this.loading = false;
        if (res.success) {
          if (this.role === 'OWNER') this.router.navigate(['/owner/dashboard']);
          else this.router.navigate(['/tenant/dashboard']);
        } else { this.error = res.message; }
      },
      error: err => {
        this.loading = false;
        this.error = err.error?.message || 'Registration failed. Please try again.';
      }
    });
  }
}
