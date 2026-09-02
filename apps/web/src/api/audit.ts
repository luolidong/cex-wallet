import { http } from './http';

export interface AuditLog {
  id: number;
  adminUserId?: number;
  adminUsername?: string;
  action: string;
  targetType: string;
  targetId?: string;
  summary?: string;
  detailJson?: string;
  createdAt: string;
}

export async function listAuditLogs(limit = 100): Promise<AuditLog[]> {
  const response = await http.get<{ success: boolean; data: AuditLog[] }>('/audit-logs', {
    params: { limit }
  });
  return response.data.data;
}
