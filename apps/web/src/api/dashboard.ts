import { http } from './http';

export interface DashboardTokenBalance {
  tokenId: number;
  symbol: string;
  decimals: number;
  userAvailable: string;
  displayUserAvailable: string;
  userFrozen: string;
  displayUserFrozen: string;
  hotWalletBalance?: string;
  displayHotWalletBalance?: string;
  coverageDifference?: string;
  displayCoverageDifference?: string;
  status: string;
}

export interface DashboardWithdrawal {
  id: number;
  userId: number;
  username: string;
  symbol: string;
  amount: string;
  displayAmount: string;
  fee: string;
  displayFee: string;
  status: string;
  toAddress: string;
  requestedAt: string;
}

export interface DashboardSummary {
  pendingWithdrawalCount: number;
  broadcastedWithdrawalCount: number;
  todayDepositCount: number;
  todayWithdrawalCount: number;
  reconciliationMismatchCount: number;
  serviceDownCount: number;
  tokenBalances: DashboardTokenBalance[];
  recentPendingWithdrawals: DashboardWithdrawal[];
}

export async function getDashboardSummary(): Promise<DashboardSummary> {
  const response = await http.get<{ success: boolean; data: DashboardSummary }>('/dashboard/summary');
  return response.data.data;
}
