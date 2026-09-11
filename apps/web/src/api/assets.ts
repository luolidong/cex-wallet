import { http } from './http';
import type { PageResult } from './wallets';

export interface ChainAsset {
  id: number;
  chainType: string;
  chainId: number;
  name: string;
  rpcUrl: string;
  explorerUrl?: string;
  confirmBlocks: number;
  scanEnabled: boolean;
  withdrawEnabled: boolean;
  status: string;
}

export interface TokenAsset {
  id: number;
  chainId: number;
  chainName: string;
  symbol: string;
  name: string;
  tokenAddress?: string;
  tokenType: string;
  decimals: number;
  isNative: boolean;
  minDepositAmount: string;
  displayMinDepositAmount: string;
  minWithdrawAmount: string;
  displayMinWithdrawAmount: string;
  withdrawFee: string;
  displayWithdrawFee: string;
  depositEnabled: boolean;
  withdrawEnabled: boolean;
  status: string;
}

export interface PlatformWallet {
  id: number;
  chainId: number;
  chainName: string;
  tokenId?: number;
  tokenSymbol?: string;
  address: string;
  walletRole: string;
  status: string;
  remark?: string;
}

export interface UpdateChainInput {
  name: string;
  rpcUrl: string;
  explorerUrl?: string;
  confirmBlocks: number;
  scanEnabled: boolean;
  withdrawEnabled: boolean;
  status: string;
}

export interface UpdateTokenInput {
  name: string;
  tokenAddress?: string;
  minDepositAmount: string;
  minWithdrawAmount: string;
  withdrawFee: string;
  depositEnabled: boolean;
  withdrawEnabled: boolean;
  status: string;
}

export interface CreatePlatformWalletInput {
  chainId: number;
  tokenId?: number;
  address: string;
  walletRole: string;
  status: string;
  remark?: string;
}

export interface UpdatePlatformWalletInput {
  address: string;
  walletRole: string;
  status: string;
  remark?: string;
}

export interface ListPlatformWalletsParams {
  keyword?: string;
  chainId?: number;
  tokenId?: number;
  walletRole?: string;
  status?: string;
  page?: number;
  pageSize?: number;
}

export async function listChains(): Promise<ChainAsset[]> {
  const response = await http.get<{ success: boolean; data: ChainAsset[] }>('/assets/chains');
  return response.data.data;
}

export async function updateChain(id: number, input: UpdateChainInput): Promise<ChainAsset[]> {
  const response = await http.put<{ success: boolean; data: ChainAsset[] }>(`/assets/chains/${id}`, input);
  return response.data.data;
}

export async function listTokens(): Promise<TokenAsset[]> {
  const response = await http.get<{ success: boolean; data: TokenAsset[] }>('/assets/tokens');
  return response.data.data;
}

export async function updateToken(id: number, input: UpdateTokenInput): Promise<TokenAsset[]> {
  const response = await http.put<{ success: boolean; data: TokenAsset[] }>(`/assets/tokens/${id}`, input);
  return response.data.data;
}

export async function listPlatformWallets(params: ListPlatformWalletsParams): Promise<PageResult<PlatformWallet>> {
  const response = await http.get<{ success: boolean; data: PageResult<PlatformWallet> }>('/assets/platform-wallets', { params });
  return response.data.data;
}

export async function createPlatformWallet(input: CreatePlatformWalletInput): Promise<PlatformWallet> {
  const response = await http.post<{ success: boolean; data: PlatformWallet }>('/assets/platform-wallets', input);
  return response.data.data;
}

export async function updatePlatformWallet(id: number, input: UpdatePlatformWalletInput): Promise<PlatformWallet[]> {
  const response = await http.put<{ success: boolean; data: PlatformWallet[] }>(`/assets/platform-wallets/${id}`, input);
  return response.data.data;
}

export async function disablePlatformWallet(id: number): Promise<PlatformWallet[]> {
  const response = await http.delete<{ success: boolean; data: PlatformWallet[] }>(`/assets/platform-wallets/${id}`);
  return response.data.data;
}

export async function enablePlatformWallet(id: number): Promise<PlatformWallet[]> {
  const response = await http.post<{ success: boolean; data: PlatformWallet[] }>(`/assets/platform-wallets/${id}/enable`);
  return response.data.data;
}
