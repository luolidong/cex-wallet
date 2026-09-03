import { http } from './http';
import type { Withdrawal } from './users';
import type { PageResult } from './wallets';

export interface AdminWithdrawalRecord extends Withdrawal {
  username: string;
}

export interface ListWithdrawalRecordsParams {
  keyword?: string;
  chainId?: number;
  tokenId?: number;
  status?: string;
  page?: number;
  pageSize?: number;
}

export async function listWithdrawalRecords(params: ListWithdrawalRecordsParams): Promise<PageResult<AdminWithdrawalRecord>> {
  const response = await http.get<{ success: boolean; data: PageResult<AdminWithdrawalRecord> }>('/withdrawal-records', {
    params
  });
  return response.data.data;
}
