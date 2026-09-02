import { http } from './http';

export interface ServiceStatus {
  name: string;
  type: string;
  status: string;
  endpoint: string;
  latencyMs?: number;
  message?: string;
  checkedAt: string;
  details?: Record<string, unknown>;
}

export async function listSystemStatuses(): Promise<ServiceStatus[]> {
  const response = await http.get<{ success: boolean; data: ServiceStatus[] }>('/system/statuses');
  return response.data.data;
}
