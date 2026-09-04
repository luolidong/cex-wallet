import { http } from './http';
import type { PageResult } from './wallets';

export interface LedgerJournal {
  id: number;
  journalNo: string;
  businessType: string;
  businessId: string;
  idempotencyKey: string;
  status: string;
  description?: string;
  createdAt: string;
}

export interface LedgerEntry {
  id: number;
  journalId: number;
  accountId: number;
  ownerType: string;
  ownerId?: number;
  accountType: string;
  tokenId: number;
  symbol: string;
  decimals: number;
  direction: string;
  amount: string;
  displayAmount: string;
  createdAt: string;
}

export interface ManualAdjustmentInput {
  userId: number;
  tokenId: number;
  direction: string;
  amount: string;
  reason: string;
  idempotencyKey?: string;
}

export interface ListLedgerJournalsParams {
  keyword?: string;
  businessType?: string;
  status?: string;
  page?: number;
  pageSize?: number;
}

export async function listLedgerJournals(params: ListLedgerJournalsParams): Promise<PageResult<LedgerJournal>> {
  const response = await http.get<{ success: boolean; data: PageResult<LedgerJournal> }>('/ledger/journals', { params });
  return response.data.data;
}

export async function listLedgerEntries(journalId: number): Promise<LedgerEntry[]> {
  const response = await http.get<{ success: boolean; data: LedgerEntry[] }>(`/ledger/journals/${journalId}/entries`);
  return response.data.data;
}

export async function createManualAdjustment(input: ManualAdjustmentInput): Promise<LedgerJournal> {
  const response = await http.post<{ success: boolean; data: LedgerJournal }>('/ledger/adjustments', input);
  return response.data.data;
}
