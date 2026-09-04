import { http } from './http';
import type { PageResult } from './wallets';

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

export interface ListAuditLogsParams {
  keyword?: string;
  action?: string;
  targetType?: string;
  page?: number;
  pageSize?: number;
}

export async function listAuditLogs(params: ListAuditLogsParams): Promise<PageResult<AuditLog>> {
  const response = await http.get<{ success: boolean; data: PageResult<AuditLog> }>('/audit-logs', { params });
  return response.data.data;
}
