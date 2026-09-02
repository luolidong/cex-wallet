import { http } from './http';

export interface WithdrawalRule {
  tokenId: number;
  symbol: string;
  tokenType: string;
  tokenAddress?: string;
  decimals: number;
  minWithdrawAmount: string;
  displayMinWithdrawAmount: string;
  withdrawFee: string;
  displayWithdrawFee: string;
  maxWithdrawAmount?: string;
  displayMaxWithdrawAmount: string;
  dailyWithdrawLimit?: string;
  displayDailyWithdrawLimit: string;
  withdrawEnabled: boolean;
}

export interface ChainOption {
  id: number;
  chainType: string;
  chainId: number;
  name: string;
  status: string;
}

export interface UpdateWithdrawalRuleInput {
  maxWithdrawAmount?: string;
  dailyWithdrawLimit?: string;
}

export interface BlacklistAddress {
  id: number;
  chainId: number;
  chainName: string;
  address: string;
  reason?: string;
  status: string;
  createdAt: string;
}

export interface AddBlacklistAddressInput {
  chainId: number;
  address: string;
  reason?: string;
}

export async function listWithdrawalRules(): Promise<WithdrawalRule[]> {
  const response = await http.get<{ success: boolean; data: WithdrawalRule[] }>('/risk/withdrawal-rules');
  return response.data.data;
}

export async function listRiskChains(): Promise<ChainOption[]> {
  const response = await http.get<{ success: boolean; data: ChainOption[] }>('/risk/chains');
  return response.data.data;
}

export async function updateWithdrawalRule(tokenId: number, input: UpdateWithdrawalRuleInput): Promise<WithdrawalRule[]> {
  const response = await http.put<{ success: boolean; data: WithdrawalRule[] }>(`/risk/withdrawal-rules/${tokenId}`, input);
  return response.data.data;
}

export async function listBlacklistAddresses(): Promise<BlacklistAddress[]> {
  const response = await http.get<{ success: boolean; data: BlacklistAddress[] }>('/risk/withdrawal-address-blacklist');
  return response.data.data;
}

export async function addBlacklistAddress(input: AddBlacklistAddressInput): Promise<BlacklistAddress> {
  const response = await http.post<{ success: boolean; data: BlacklistAddress }>('/risk/withdrawal-address-blacklist', input);
  return response.data.data;
}

export async function disableBlacklistAddress(id: number): Promise<BlacklistAddress[]> {
  const response = await http.delete<{ success: boolean; data: BlacklistAddress[] }>(`/risk/withdrawal-address-blacklist/${id}`);
  return response.data.data;
}

export async function enableBlacklistAddress(id: number): Promise<BlacklistAddress[]> {
  const response = await http.post<{ success: boolean; data: BlacklistAddress[] }>(`/risk/withdrawal-address-blacklist/${id}/enable`);
  return response.data.data;
}
