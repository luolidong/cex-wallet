import { http } from './http';
import type { Deposit } from './users';
import type { PageResult } from './wallets';

export interface ListDepositsParams {
  keyword?: string;
  chainId?: number;
  tokenId?: number;
  status?: string;
  page?: number;
  pageSize?: number;
}

export async function listDeposits(params: ListDepositsParams): Promise<PageResult<Deposit>> {
  const response = await http.get<{ success: boolean; data: PageResult<Deposit> }>('/deposits', { params });
  return response.data.data;
}
