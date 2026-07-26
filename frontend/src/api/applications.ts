import type {
  Application,
  CreateApplicationRequest,
  ListApplicationsParams,
  PageResponse,
  UpdateApplicationRequest,
} from '../types';
import { apiClient } from './client';

export async function listApplications(
  params: ListApplicationsParams = {},
): Promise<PageResponse<Application>> {
  const { data } = await apiClient.get<PageResponse<Application>>('/api/v1/applications', {
    params,
  });
  return data;
}

export async function getApplication(id: string): Promise<Application> {
  const { data } = await apiClient.get<Application>(`/api/v1/applications/${id}`);
  return data;
}

export async function createApplication(
  payload: CreateApplicationRequest,
): Promise<Application> {
  const { data } = await apiClient.post<Application>('/api/v1/applications', payload);
  return data;
}

export async function updateApplication(
  id: string,
  payload: UpdateApplicationRequest,
): Promise<Application> {
  const { data } = await apiClient.put<Application>(`/api/v1/applications/${id}`, payload);
  return data;
}

export async function deleteApplication(id: string): Promise<void> {
  await apiClient.delete(`/api/v1/applications/${id}`);
}
