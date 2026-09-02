import { http } from './http';
import type { Withdrawal } from './users';

export async function listWithdrawals(status?: string): Promise<Withdrawal[]> {
  const response = await http.get<{ success: boolean; data: Withdrawal[] }>('/withdrawals', {
    params: status ? { status } : undefined
  });
  return response.data.data;
}

export async function approveWithdrawal(id: number): Promise<Withdrawal> {
  const response = await http.post<{ success: boolean; data: Withdrawal }>(`/withdrawals/${id}/approve`);
  return response.data.data;
}

export async function rejectWithdrawal(id: number, reason?: string): Promise<Withdrawal> {
  const response = await http.post<{ success: boolean; data: Withdrawal }>(`/withdrawals/${id}/reject`, { reason });
  return response.data.data;
}

export async function confirmWithdrawal(id: number, txHash?: string): Promise<Withdrawal> {
  const response = await http.post<{ success: boolean; data: Withdrawal }>(`/withdrawals/${id}/confirm`, { txHash });
  return response.data.data;
}
