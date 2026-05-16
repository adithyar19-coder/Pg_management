import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, Announcement, Complaint, ConversationSummary, DirectMessage, FoodMenuCell, ForumPost, ForumReply, MaintenanceRequest, PG, RentRecord, Room, RoomAssignment, RoomRequest, RoomSuggestion, User } from '../models/models';

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
  // Room Requests (owner side)
  getRoomRequests(status: 'PENDING' | 'ALL' = 'PENDING'): Observable<ApiResponse<RoomRequest[]>> {
    return this.http.get<ApiResponse<RoomRequest[]>>(`${BASE}/owner/room-requests?status=${status}`);
  }
  suggestRoomForRequest(requestId: number): Observable<ApiResponse<RoomSuggestion>> {
    return this.http.post<ApiResponse<RoomSuggestion>>(`${BASE}/owner/room-requests/${requestId}/suggest`, {});
  }
  approveRoomRequest(requestId: number, roomId: number, note?: string): Observable<ApiResponse<RoomRequest>> {
    return this.http.post<ApiResponse<RoomRequest>>(`${BASE}/owner/room-requests/${requestId}/approve`, { roomId, note });
  }
  rejectRoomRequest(requestId: number, note?: string): Observable<ApiResponse<RoomRequest>> {
    return this.http.post<ApiResponse<RoomRequest>>(`${BASE}/owner/room-requests/${requestId}/reject`, { note });
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
  sendRentReminders(): Observable<ApiResponse<{ count: number }>> {
    return this.http.post<ApiResponse<{ count: number }>>(`${BASE}/owner/rent/send-reminders`, {});
  }
  // Food Menu (owner)
  getFoodMenuOwner(pgId: number): Observable<ApiResponse<FoodMenuCell[]>> {
    return this.http.get<ApiResponse<FoodMenuCell[]>>(`${BASE}/owner/pg/${pgId}/food-menu`);
  }
  upsertFoodMenuCell(pgId: number, cell: { dayOfWeek: string; mealType: string; items: string; notes?: string }): Observable<ApiResponse<FoodMenuCell>> {
    return this.http.put<ApiResponse<FoodMenuCell>>(`${BASE}/owner/pg/${pgId}/food-menu`, cell);
  }
  // Forum (owner)
  getForumPostsOwner(pgId?: number): Observable<ApiResponse<ForumPost[]>> {
    const qs = pgId ? `?pgId=${pgId}` : '';
    return this.http.get<ApiResponse<ForumPost[]>>(`${BASE}/owner/forum/posts${qs}`);
  }
  createForumPostOwner(pgId: number, title: string, body: string): Observable<ApiResponse<ForumPost>> {
    return this.http.post<ApiResponse<ForumPost>>(`${BASE}/owner/forum/posts`, { pgId, title, body });
  }
  togglePinPost(id: number): Observable<ApiResponse<ForumPost>> {
    return this.http.post<ApiResponse<ForumPost>>(`${BASE}/owner/forum/posts/${id}/pin`, {});
  }
  deleteForumPostOwner(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${BASE}/owner/forum/posts/${id}`);
  }
  getForumRepliesOwner(postId: number): Observable<ApiResponse<ForumReply[]>> {
    return this.http.get<ApiResponse<ForumReply[]>>(`${BASE}/owner/forum/posts/${postId}/replies`);
  }
  addForumReplyOwner(postId: number, body: string): Observable<ApiResponse<ForumReply>> {
    return this.http.post<ApiResponse<ForumReply>>(`${BASE}/owner/forum/posts/${postId}/replies`, { body });
  }
  deleteForumReplyOwner(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${BASE}/owner/forum/replies/${id}`);
  }
  // Vacate-request review (owner side)
  getPendingVacateRequests(): Observable<ApiResponse<RoomAssignment[]>> {
    return this.http.get<ApiResponse<RoomAssignment[]>>(`${BASE}/owner/vacate-requests`);
  }
  approveVacate(assignmentId: number): Observable<ApiResponse<RoomAssignment>> {
    return this.http.post<ApiResponse<RoomAssignment>>(`${BASE}/owner/vacate-requests/${assignmentId}/approve`, {});
  }
  rejectVacate(assignmentId: number, note?: string): Observable<ApiResponse<RoomAssignment>> {
    return this.http.post<ApiResponse<RoomAssignment>>(`${BASE}/owner/vacate-requests/${assignmentId}/reject`, { note });
  }
  manualVacate(assignmentId: number, note?: string): Observable<ApiResponse<RoomAssignment>> {
    return this.http.post<ApiResponse<RoomAssignment>>(`${BASE}/owner/assignments/${assignmentId}/vacate`, { note });
  }

  // ── Tenant ─────────────────────────────────────────
  tenantDashboard(): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${BASE}/tenant/dashboard`);
  }
  getMyRoom(): Observable<ApiResponse<RoomAssignment>> {
    return this.http.get<ApiResponse<RoomAssignment>>(`${BASE}/tenant/my-room`);
  }
  // PG search + room request submission (tenant)
  searchPgs(city?: string, locality?: string): Observable<ApiResponse<PG[]>> {
    const params: string[] = [];
    if (city) params.push(`city=${encodeURIComponent(city)}`);
    if (locality) params.push(`locality=${encodeURIComponent(locality)}`);
    const qs = params.length ? `?${params.join('&')}` : '';
    return this.http.get<ApiResponse<PG[]>>(`${BASE}/tenant/pgs/search${qs}`);
  }
  getPgDetails(pgId: number): Observable<ApiResponse<PG>> {
    return this.http.get<ApiResponse<PG>>(`${BASE}/tenant/pgs/${pgId}`);
  }
  submitRoomRequest(data: { pgId: number; preferredType?: string; preferredFloor?: number; acPreference?: boolean; notes?: string }): Observable<ApiResponse<RoomRequest>> {
    return this.http.post<ApiResponse<RoomRequest>>(`${BASE}/tenant/room-requests`, data);
  }
  getMyRoomRequests(): Observable<ApiResponse<RoomRequest[]>> {
    return this.http.get<ApiResponse<RoomRequest[]>>(`${BASE}/tenant/room-requests`);
  }
  cancelRoomRequest(id: number): Observable<ApiResponse<RoomRequest>> {
    return this.http.delete<ApiResponse<RoomRequest>>(`${BASE}/tenant/room-requests/${id}`);
  }
  // Food Menu (tenant)
  getFoodMenuTenant(): Observable<ApiResponse<FoodMenuCell[]>> {
    return this.http.get<ApiResponse<FoodMenuCell[]>>(`${BASE}/tenant/food-menu`);
  }
  // Forum (tenant)
  getForumPostsTenant(): Observable<ApiResponse<ForumPost[]>> {
    return this.http.get<ApiResponse<ForumPost[]>>(`${BASE}/tenant/forum/posts`);
  }
  createForumPostTenant(title: string, body: string): Observable<ApiResponse<ForumPost>> {
    return this.http.post<ApiResponse<ForumPost>>(`${BASE}/tenant/forum/posts`, { title, body });
  }
  deleteForumPostTenant(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${BASE}/tenant/forum/posts/${id}`);
  }
  getForumRepliesTenant(postId: number): Observable<ApiResponse<ForumReply[]>> {
    return this.http.get<ApiResponse<ForumReply[]>>(`${BASE}/tenant/forum/posts/${postId}/replies`);
  }
  addForumReplyTenant(postId: number, body: string): Observable<ApiResponse<ForumReply>> {
    return this.http.post<ApiResponse<ForumReply>>(`${BASE}/tenant/forum/posts/${postId}/replies`, { body });
  }
  deleteForumReplyTenant(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${BASE}/tenant/forum/replies/${id}`);
  }
  // Direct Messages (both roles share)
  getConversations(): Observable<ApiResponse<ConversationSummary[]>> {
    return this.http.get<ApiResponse<ConversationSummary[]>>(`${BASE}/messages/conversations`);
  }
  getChatPartners(): Observable<ApiResponse<User[]>> {
    return this.http.get<ApiResponse<User[]>>(`${BASE}/messages/partners`);
  }
  getChatWith(userId: number): Observable<ApiResponse<DirectMessage[]>> {
    return this.http.get<ApiResponse<DirectMessage[]>>(`${BASE}/messages/with/${userId}`);
  }
  sendMessage(recipientId: number, body: string): Observable<ApiResponse<DirectMessage>> {
    return this.http.post<ApiResponse<DirectMessage>>(`${BASE}/messages`, { recipientId, body });
  }
  getUnreadMessageCount(): Observable<ApiResponse<number>> {
    return this.http.get<ApiResponse<number>>(`${BASE}/messages/unread-count`);
  }
  // Vacate-request flow (tenant side)
  requestVacate(leaveDate: string, reason?: string): Observable<ApiResponse<RoomAssignment>> {
    return this.http.post<ApiResponse<RoomAssignment>>(`${BASE}/tenant/vacate`, { leaveDate, reason });
  }
  cancelVacateRequest(): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${BASE}/tenant/vacate`);
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
  markAllNotifsRead(): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${BASE}/tenant/notifications/mark-all-read`, {});
  }

  // Owner notifications (mirrors tenant)
  getOwnerNotifications(): Observable<ApiResponse<any[]>> {
    return this.http.get<ApiResponse<any[]>>(`${BASE}/owner/notifications`);
  }
  getOwnerUnreadCount(): Observable<ApiResponse<number>> {
    return this.http.get<ApiResponse<number>>(`${BASE}/owner/notifications/count`);
  }
  markOwnerNotifRead(id: number): Observable<ApiResponse<void>> {
    return this.http.patch<ApiResponse<void>>(`${BASE}/owner/notifications/${id}/read`, {});
  }
  markAllOwnerNotifsRead(): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${BASE}/owner/notifications/mark-all-read`, {});
  }

  // ── Files ──────────────────────────────────────────
  uploadFiles(files: FileList): Observable<ApiResponse<string[]>> {
    const form = new FormData();
    for (let i = 0; i < files.length; i++) form.append('files', files[i]);
    return this.http.post<ApiResponse<string[]>>(`${BASE}/files/upload`, form);
  }
}
