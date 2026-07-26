import type { Deployment, ListDeploymentsParams, PageResponse } from '../types';
import { apiClient } from './client';

export async function listDeployments(
  params: ListDeploymentsParams = {},
): Promise<PageResponse<Deployment>> {
  const { data } = await apiClient.get<PageResponse<Deployment>>('/api/v1/deployments', {
    params,
  });
  return data;
}

export async function getDeployment(id: string): Promise<Deployment> {
  const { data } = await apiClient.get<Deployment>(`/api/v1/deployments/${id}`);
  return data;
}
