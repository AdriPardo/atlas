import type { DashboardStats } from '../types';
import { apiClient } from './client';

export async function getDashboardStats(): Promise<DashboardStats> {
  const { data } = await apiClient.get<DashboardStats>('/api/v1/dashboard/stats');
  return data;
}
