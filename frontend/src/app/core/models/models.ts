export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface AuthResponse {
  token: string;
  userId: number;
  name: string;
  email: string;
  role: 'OWNER' | 'TENANT';
}

export interface PG {
  id: number;
  name: string;
  address: string;
  rules: string;
  amenities: string;
  phone: string;
  totalRooms: string;
  city?: string;
  locality?: string;
  owner?: User;
}

export interface Room {
  id: number;
  roomNumber: string;
  capacity: number;
  rentAmount: number;
  isOccupied: boolean;      // true only when currentOccupancy >= capacity
  currentOccupancy?: number; // count of active RoomAssignments, populated by backend
  type: string;
  floor?: number;
  hasAc?: boolean;
  pg?: PG;
}

export interface RoomRequest {
  id: number;
  tenant?: User;
  pg?: PG;
  preferredType?: 'SINGLE' | 'DOUBLE' | 'TRIPLE' | 'DORMITORY';
  preferredFloor?: number;
  acPreference?: boolean;
  notes?: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
  assignedRoom?: Room;
  ownerNote?: string;
  createdAt: string;
  reviewedAt?: string;
}

export interface RoomSuggestion {
  roomId: number;
  roomNumber: string;
  floor: number;
  type: string;
  hasAc: boolean;
  capacity: number;
  currentOccupants: number;
  rentAmount: number;
  score: number;
  reasoning: string;
}

export interface RoomAssignment {
  id: number;
  tenant?: User;
  room?: Room;
  joinDate: string;
  leaveDate: string;
  isActive: boolean;
  vacateRequestedAt?: string;   // ISO datetime; null/undefined when no request
  requestedLeaveDate?: string;  // ISO date when tenant wants to leave
  vacateReason?: string;
}

export interface RentRecord {
  id: number;
  tenant?: User;
  room?: Room;
  month: string;
  amount: number;
  dueDate: string;
  paidDate: string;
  status: 'PENDING' | 'PAID' | 'OVERDUE';
}

export interface Complaint {
  id: number;
  tenant?: User;
  pg?: PG;
  title: string;
  description: string;
  imageUrls: string[];
  status: 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'REJECTED';
  createdAt: string;
  resolvedAt: string;
  ownerNote: string;
}

export interface MaintenanceRequest {
  id: number;
  tenant?: User;
  room?: Room;
  title: string;
  issue: string;
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  imageUrls: string[];
  status: 'PENDING' | 'ASSIGNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  assignedTo: string;
  createdAt: string;
  completedAt: string;
}

export interface Announcement {
  id: number;
  owner?: User;
  pg?: PG;
  title: string;
  message: string;
  priority: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
  createdAt: string;
}

export interface Notification {
  id: number;
  user?: User;
  message: string;
  type: string;
  isRead: boolean;
  createdAt: string;
}

export interface User {
  id: number;
  name: string;
  email: string;
  role: string;
  phone: string;
}

export interface FoodMenuCell {
  id?: number;
  pg?: PG;
  dayOfWeek: 'MON' | 'TUE' | 'WED' | 'THU' | 'FRI' | 'SAT' | 'SUN';
  mealType: 'BREAKFAST' | 'LUNCH' | 'SNACKS' | 'DINNER';
  items: string;
  notes?: string;
}

export interface ForumPost {
  id: number;
  pg?: PG;
  author?: User;
  title: string;
  body: string;
  isPinned: boolean;
  createdAt: string;
  replyCount?: number;
}

export interface ForumReply {
  id: number;
  author?: User;
  body: string;
  createdAt: string;
}

export interface DirectMessage {
  id: number;
  sender?: User;
  recipient?: User;
  body: string;
  isRead: boolean;
  createdAt: string;
}

export interface ConversationSummary {
  partnerId: number;
  partnerName: string;
  partnerEmail: string;
  partnerRole: 'OWNER' | 'TENANT';
  lastMessage: string;
  lastMessageAt: string;
  lastFromMe: boolean;
  unreadCount: number;
}
