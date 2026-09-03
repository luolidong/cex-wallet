import { http } from './http';

export interface TokenReconciliation {
  tokenId: number;
  symbol: string;
  tokenType: string;
  tokenAddress?: string;
  rpcUrl: string;
  decimals: number;
  userAvailable: string;
  displayUserAvailable: string;
  userFrozen: string;
  displayUserFrozen: string;
  ledgerTotal: string;
  displayLedgerTotal: string;
  confirmedDeposits: string;
  displayConfirmedDeposits: string;
  pendingWithdrawals: string;
  displayPendingWithdrawals: string;
  confirmedWithdrawals: string;
  displayConfirmedWithdrawals: string;
  expectedLedgerTotal: string;
  displayExpectedLedgerTotal: string;
  difference: string;
  displayDifference: string;
  hotWalletBalance?: string;
  displayHotWalletBalance: string;
  coverageDifference?: string;
  displayCoverageDifference: string;
  status: string;
}

export async function listTokenReconciliations(): Promise<TokenReconciliation[]> {
  const response = await http.get<{ success: boolean; data: TokenReconciliation[] }>('/reconciliation/tokens');
  return response.data.data;
}
