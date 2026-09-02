import { http } from './http';

export interface User {
  id: number;
  username: string;
  email?: string;
  phone?: string;
  status: string;
  kycLevel: number;
  createdAt: string;
}

export interface Balance {
  tokenId: number;
  symbol: string;
  decimals: number;
  available: string;
  frozen: string;
  displayAvailable: string;
  displayFrozen: string;
}

export interface CreateUserInput {
  username: string;
  email?: string;
  phone?: string;
}

export interface Wallet {
  id: number;
  userId: number;
  chainId: number;
  chainName: string;
  address: string;
  addressType: string;
  status: string;
  createdAt: string;
}

export interface Deposit {
  id: number;
  userId: number;
  walletId: number;
  chainId: number;
  chainName: string;
  tokenId: number;
  symbol: string;
  txHash: string;
  eventIndex: number;
  fromAddress?: string;
  toAddress: string;
  amount: string;
  displayAmount: string;
  blockNumber?: number;
  confirmationCount: number;
  status: string;
  detectedAt: string;
  confirmedAt?: string;
}

export interface Withdrawal {
  id: number;
  userId: number;
  chainId: number;
  chainName: string;
  tokenId: number;
  symbol: string;
  tokenType: string;
  tokenAddress?: string;
  decimals: number;
  toAddress: string;
  amount: string;
  displayAmount: string;
  fee: string;
  displayFee: string;
  status: string;
  txHash?: string;
  rejectReason?: string;
  requestedAt: string;
  createdAt: string;
}

export interface CreateWithdrawalInput {
  tokenId: number;
  toAddress: string;
  amount: string;
}

export async function listUsers(): Promise<User[]> {
  const response = await http.get<{ success: boolean; data: User[] }>('/users');
  return response.data.data;
}

export async function createUser(input: CreateUserInput): Promise<User> {
  const response = await http.post<{ success: boolean; data: User }>('/users', input);
  return response.data.data;
}

export async function getUser(userId: number): Promise<User> {
  const response = await http.get<{ success: boolean; data: User }>(`/users/${userId}`);
  return response.data.data;
}

export async function getUserBalances(userId: number): Promise<Balance[]> {
  const response = await http.get<{ success: boolean; data: Balance[] }>(`/users/${userId}/balances`);
  return response.data.data;
}

export async function listUserWallets(userId: number): Promise<Wallet[]> {
  const response = await http.get<{ success: boolean; data: Wallet[] }>(`/users/${userId}/wallets`);
  return response.data.data;
}

export async function createDepositAddress(userId: number, chainId: number): Promise<Wallet> {
  const response = await http.post<{ success: boolean; data: Wallet }>(`/users/${userId}/deposit-addresses`, {
    chainId
  });
  return response.data.data;
}

export async function listUserDeposits(userId: number): Promise<Deposit[]> {
  const response = await http.get<{ success: boolean; data: Deposit[] }>(`/users/${userId}/deposits`);
  return response.data.data;
}

export async function listUserWithdrawals(userId: number): Promise<Withdrawal[]> {
  const response = await http.get<{ success: boolean; data: Withdrawal[] }>(`/users/${userId}/withdrawals`);
  return response.data.data;
}

export async function createWithdrawal(userId: number, input: CreateWithdrawalInput): Promise<Withdrawal> {
  const response = await http.post<{ success: boolean; data: Withdrawal }>(`/users/${userId}/withdrawals`, input);
  return response.data.data;
}
