import type { Host, ListHostsParams, PageResponse } from '../types';
import { apiClient } from './client';

export async function listHosts(params: ListHostsParams = {}): Promise<PageResponse<Host>> {
  const { data } = await apiClient.get<PageResponse<Host>>('/api/v1/hosts', { params });
  return data;
}

export async function getHost(id: string): Promise<Host> {
  const { data } = await apiClient.get<Host>(`/api/v1/hosts/${id}`);
  return data;
}
