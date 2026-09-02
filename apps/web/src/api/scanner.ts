import { http } from './http';

export interface ScannerStatus {
  chainId: number;
  chainName: string;
  chainType: string;
  networkChainId: number;
  scanEnabled: boolean;
  confirmBlocks: number;
  scannerName?: string;
  lastScannedBlock?: number;
  lastFinalizedBlock?: number;
  lagBlocks?: number;
  cursorStatus?: string;
  cursorUpdatedAt?: string;
  depositAddressCount: number;
  scannerDepositCount: number;
  depositCount: number;
}

export async function listScannerStatuses(): Promise<ScannerStatus[]> {
  const response = await http.get<{ success: boolean; data: ScannerStatus[] }>('/scanner/statuses');
  return response.data.data;
}
