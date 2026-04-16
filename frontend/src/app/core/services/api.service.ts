import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, Announcement, Complaint, MaintenanceRequest, PG, RentRecord, Room, RoomAssignment, User } from '../models/models';

const BASE = 'http://localhost:8082/api';

@Injectable({ providedIn: 'root' })
export class ApiService {
  constructor(private http: HttpClient) {}

  // ── Owner ──────────────────────────────────────────
  ownerDashboard(): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${BASE}/owner/dashboard`);
  }
  // PG
  createPG(data: any): Observable<ApiResponse<PG>> {
    return this.http.post<ApiResponse<PG>>(`${BASE}/owner/pg`, data);
  }
  getPGs(): Observable<ApiResponse<PG[]>> {
    return this.http.get<ApiResponse<PG[]>>(`${BASE}/owner/pg`);
  }
  updatePG(id: number, data: any): Observable<ApiResponse<PG>> {
    return this.http.put<ApiResponse<PG>>(`${BASE}/owner/pg/${id}`, data);
  }
  // Rooms
  addRoom(data: any): Observable<ApiResponse<Room>> {
    return this.http.post<ApiResponse<Room>>(`${BASE}/owner/rooms`, data);
  }
  bulkAddRooms(pgId: number, rooms: any[]): Observable<ApiResponse<Room[]>> {
    return this.http.post<ApiResponse<Room[]>>(`${BASE}/owner/rooms/bulk`, { pgId, rooms });
  }
  getOwnerRooms(): Observable<ApiResponse<Room[]>> {
    return this.http.get<ApiResponse<Room[]>>(`${BASE}/owner/rooms`);
  }
  getRoomsByPG(pgId: number): Observable<ApiResponse<Room[]>> {
    return this.http.get<ApiResponse<Room[]>>(`${BASE}/owner/pg/${pgId}/rooms`);
  }
  assignTenant(roomId: number, tenantId: number): Observable<ApiResponse<RoomAssignment>> {
    return this.http.post<ApiResponse<RoomAssignment>>(`${BASE}/owner/rooms/${roomId}/assign`, { tenantId });
  }
  // Tenants
  getActiveTenants(): Observable<ApiResponse<RoomAssignment[]>> {
    return this.http.get<ApiResponse<RoomAssignment[]>>(`${BASE}/owner/tenants`);
  }
  getAllTenantUsers(): Observable<ApiResponse<User[]>> {
    return this.http.get<ApiResponse<User[]>>(`${BASE}/owner/users/tenants`);
  }
  // Complaints
  getOwnerComplaints(): Observable<ApiResponse<Complaint[]>> {
    return this.http.get<ApiResponse<Complaint[]>>(`${BASE}/owner/complaints`);
  }
  updateComplaint(id: number, data: any): Observable<ApiResponse<Complaint>> {
    return this.http.patch<ApiResponse<Complaint>>(`${BASE}/owner/complaints/${id}`, data);
  }
  // Maintenance
  getOwnerMaintenance(): Observable<ApiResponse<MaintenanceRequest[]>> {
    return this.http.get<ApiResponse<MaintenanceRequest[]>>(`${BASE}/owner/maintenance`);
  }
  updateMaintenance(id: number, data: any): Observable<ApiResponse<MaintenanceRequest>> {
    return this.http.patch<ApiResponse<MaintenanceRequest>>(`${BASE}/owner/maintenance/${id}`, data);
  }
  // Announcements
  createAnnouncement(data: any): Observable<ApiResponse<Announcement>> {
    return this.http.post<ApiResponse<Announcement>>(`${BASE}/owner/announcements`, data);
  }
  getOwnerAnnouncements(): Observable<ApiResponse<Announcement[]>> {
    return this.http.get<ApiResponse<Announcement[]>>(`${BASE}/owner/announcements`);
  }
  // Rent
  getOwnerRent(): Observable<ApiResponse<RentRecord[]>> {
    return this.http.get<ApiResponse<RentRecord[]>>(`${BASE}/owner/rent`);
  }
  markRentPaid(id: number): Observable<ApiResponse<RentRecord>> {
    return this.http.patch<ApiResponse<RentRecord>>(`${BASE}/owner/rent/${id}/mark-paid`, {});
  }

  // ── Tenant ─────────────────────────────────────────
  tenantDashboard(): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${BASE}/tenant/dashboard`);
  }
  getMyRoom(): Observable<ApiResponse<RoomAssignment>> {
    return this.http.get<ApiResponse<RoomAssignment>>(`${BASE}/tenant/my-room`);
  }
  raiseComplaint(data: any): Observable<ApiResponse<Complaint>> {
    return this.http.post<ApiResponse<Complaint>>(`${BASE}/tenant/complaints`, data);
  }
  getMyComplaints(): Observable<ApiResponse<Complaint[]>> {
    return this.http.get<ApiResponse<Complaint[]>>(`${BASE}/tenant/complaints`);
  }
  raiseMaintenance(data: any): Observable<ApiResponse<MaintenanceRequest>> {
    return this.http.post<ApiResponse<MaintenanceRequest>>(`${BASE}/tenant/maintenance`, data);
  }
  getMyMaintenance(): Observable<ApiResponse<MaintenanceRequest[]>> {
    return this.http.get<ApiResponse<MaintenanceRequest[]>>(`${BASE}/tenant/maintenance`);
  }
  getMyRent(): Observable<ApiResponse<RentRecord[]>> {
    return this.http.get<ApiResponse<RentRecord[]>>(`${BASE}/tenant/rent`);
  }
  getTenantAnnouncements(): Observable<ApiResponse<Announcement[]>> {
    return this.http.get<ApiResponse<Announcement[]>>(`${BASE}/tenant/announcements`);
  }
  getNotifications(): Observable<ApiResponse<any[]>> {
    return this.http.get<ApiResponse<any[]>>(`${BASE}/tenant/notifications`);
  }
  markNotifRead(id: number): Observable<any> {
    return this.http.patch(`${BASE}/tenant/notifications/${id}/read`, {});
  }
  getUnreadCount(): Observable<ApiResponse<number>> {
    return this.http.get<ApiResponse<number>>(`${BASE}/tenant/notifications/count`);
  }

  // ── Files ──────────────────────────────────────────
  uploadFiles(files: FileList): Observable<ApiResponse<string[]>> {
    const form = new FormData();
    for (let i = 0; i < files.length; i++) form.append('files', files[i]);
    return this.http.post<ApiResponse<string[]>>(`${BASE}/files/upload`, form);
  }
}
