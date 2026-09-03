import axios from 'axios';
import { message } from 'antd';
import { clearSession, getAccessToken } from '../auth/session';

export const http = axios.create({
  baseURL: '/api',
  timeout: 15000
});

http.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearSession();
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    } else if (error.response?.status === 403) {
      message.error('当前账号没有权限执行此操作。');
    }
    return Promise.reject(error);
  }
);
