import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { ApiResponse, AuthResponse } from '../models/models';

const API = 'http://localhost:8082/api/auth';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private currentUserSubject = new BehaviorSubject<AuthResponse | null>(this.getStoredUser());
  currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {}

  private getStoredUser(): AuthResponse | null {
    try {
      const stored = localStorage.getItem('pg_user');
      return stored ? JSON.parse(stored) : null;
    } catch { return null; }
  }

  register(data: any): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${API}/register`, data).pipe(
      tap(res => { if (res.success) this.storeUser(res.data); })
    );
  }

  login(data: any): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${API}/login`, data).pipe(
      tap(res => { if (res.success) this.storeUser(res.data); })
    );
  }

  private storeUser(user: AuthResponse) {
    localStorage.setItem('pg_user', JSON.stringify(user));
    localStorage.setItem('pg_token', user.token);
    this.currentUserSubject.next(user);
  }

  logout() {
    localStorage.removeItem('pg_user');
    localStorage.removeItem('pg_token');
    this.currentUserSubject.next(null);
  }

  getToken(): string | null {
    return localStorage.getItem('pg_token');
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  getRole(): string | null {
    return this.currentUserSubject.value?.role ?? null;
  }

  getCurrentUser(): AuthResponse | null {
    return this.currentUserSubject.value;
  }
}
