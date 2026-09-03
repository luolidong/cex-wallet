import { http } from './http';

export interface AdminWallet {
  id: number;
  userId: number;
  username: string;
  chainId: number;
  chainName: string;
  address: string;
  addressType: string;
  status: string;
  createdAt: string;
}

export interface ListWalletsParams {
  keyword?: string;
  chainId?: number;
  status?: string;
  page?: number;
  pageSize?: number;
}

export interface PageResult<T> {
  items: T[];
  page: number;
  pageSize: number;
  total: number;
}

export async function listWallets(params: ListWalletsParams): Promise<PageResult<AdminWallet>> {
  const response = await http.get<{ success: boolean; data: PageResult<AdminWallet> }>('/wallets', { params });
  return response.data.data;
}

export async function enableWallet(id: number): Promise<AdminWallet[]> {
  const response = await http.post<{ success: boolean; data: AdminWallet[] }>(`/wallets/${id}/enable`);
  return response.data.data;
}

export async function disableWallet(id: number): Promise<AdminWallet[]> {
  const response = await http.post<{ success: boolean; data: AdminWallet[] }>(`/wallets/${id}/disable`);
  return response.data.data;
}
