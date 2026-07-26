import type { AuthResponse, LoginRequest, User } from '../types';
import { apiClient } from './client';

export async function login(payload: LoginRequest): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/api/v1/auth/login', payload);
  return data;
}

export async function getCurrentUser(): Promise<User> {
  const { data } = await apiClient.get<User>('/api/v1/auth/me');
  return data;
}
