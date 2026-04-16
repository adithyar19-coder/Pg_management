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
  pg?: PG;
}

export interface RoomAssignment {
  id: number;
  tenant?: User;
  room?: Room;
  joinDate: string;
  leaveDate: string;
  isActive: boolean;
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
