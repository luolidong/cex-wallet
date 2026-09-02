import { http } from './http';

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
