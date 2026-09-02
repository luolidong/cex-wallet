import { http } from './http';

export interface AdminUser {
  id: number;
  username: string;
  displayName: string;
  permissions: string[];
}

export interface LoginResponse {
  accessToken: string;
  expiresIn: number;
  adminUser: AdminUser;
}

export async function login(username: string, password: string): Promise<LoginResponse> {
  const response = await http.post<{ success: boolean; data: LoginResponse }>('/auth/login', {
    username,
    password
  });
  return response.data.data;
}

export async function getAdminProfile(): Promise<AdminUser> {
  const response = await http.get<{ success: boolean; data: AdminUser }>('/admin/profile');
  return response.data.data;
}
