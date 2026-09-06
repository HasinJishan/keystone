export type Role = 'DISPATCHER' | 'TECHNICIAN' | 'MANAGER' | 'CUSTOMER';
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type WorkOrderStatus =
  | 'NEW' | 'ASSIGNED' | 'IN_PROGRESS' | 'ON_HOLD' | 'COMPLETED' | 'CLOSED' | 'CANCELLED';

export interface LoginResponse {
  token: string;
  userId: number;
  name: string;
  email: string;
  role: Role;
  customerId: number | null;
}

export interface CustomerDto {
  id: number;
  name: string;
  contactEmail?: string;
  contactPhone?: string;
}

export interface SiteDto {
  id: number;
  customerId: number;
  name: string;
  address?: string;
}

export interface PartDto {
  id: number;
  name: string;
  sku: string;
  unitCost: number;
  stockQty: number;
}

export interface StatusHistoryEntry {
  fromStatus: string | null;
  toStatus: string;
  changedByName: string | null;
  changedAt: string;
  note: string | null;
}

export interface PartUsageEntry {
  partName: string;
  qtyUsed: number;
  loggedAt: string;
}

export interface TimeLogEntry {
  technicianName: string;
  minutes: number;
  note: string | null;
  loggedAt: string;
}

export interface WorkOrderResponse {
  id: number;
  code: string;
  title: string;
  description: string | null;
  priority: Priority;
  status: WorkOrderStatus;
  customerId: number;
  customerName: string | null;
  siteId: number;
  siteName: string | null;
  assignedTo: number | null;
  assignedToName: string | null;
  slaDueAt: string;
  slaBreached: boolean;
  createdAt: string;
  updatedAt: string;
  completedAt: string | null;
  closedAt: string | null;
  history: StatusHistoryEntry[];
  partsUsed: PartUsageEntry[];
  timeLogs: TimeLogEntry[];
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface DashboardSummary {
  countsByStatus: Record<string, number>;
  overdueCount: number;
  slaCompliancePercent: number;
  countByTechnician: Record<string, number>;
}

export interface UserDto {
  id: number;
  name: string;
  email: string;
  role: Role;
}
