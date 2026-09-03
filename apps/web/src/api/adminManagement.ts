import { http } from './http';

export interface Permission {
  id: number;
  code: string;
  name: string;
}

export interface Role {
  id: number;
  code: string;
  name: string;
  permissions: string[];
}

export interface AdminAccount {
  id: number;
  username: string;
  displayName?: string;
  status: string;
  lastLoginAt?: string;
  roles: string[];
  permissions: string[];
}

export interface CreateAdminAccountInput {
  username: string;
  password: string;
  displayName?: string;
  roles: string[];
}

export async function listPermissions(): Promise<Permission[]> {
  const response = await http.get<{ success: boolean; data: Permission[] }>('/admin-management/permissions');
  return response.data.data;
}

export async function listRoles(): Promise<Role[]> {
  const response = await http.get<{ success: boolean; data: Role[] }>('/admin-management/roles');
  return response.data.data;
}

export async function updateRolePermissions(roleCode: string, permissions: string[]): Promise<Role[]> {
  const response = await http.put<{ success: boolean; data: Role[] }>(`/admin-management/roles/${roleCode}/permissions`, {
    permissions
  });
  return response.data.data;
}

export async function listAdminAccounts(): Promise<AdminAccount[]> {
  const response = await http.get<{ success: boolean; data: AdminAccount[] }>('/admin-management/admins');
  return response.data.data;
}

export async function createAdminAccount(input: CreateAdminAccountInput): Promise<AdminAccount> {
  const response = await http.post<{ success: boolean; data: AdminAccount }>('/admin-management/admins', input);
  return response.data.data;
}

export async function updateAdminAccountStatus(id: number, status: string): Promise<AdminAccount[]> {
  const response = await http.put<{ success: boolean; data: AdminAccount[] }>(`/admin-management/admins/${id}/status`, {
    status
  });
  return response.data.data;
}

export async function updateAdminAccountRoles(id: number, roles: string[]): Promise<AdminAccount[]> {
  const response = await http.put<{ success: boolean; data: AdminAccount[] }>(`/admin-management/admins/${id}/roles`, {
    roles
  });
  return response.data.data;
}
