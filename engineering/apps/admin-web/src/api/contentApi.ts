import request from './client';
import type {
  ApiResponse,
  CreateContentProjectRequest,
  ContentProjectResponse,
} from '../types';

/**
 * İçerik projesi API çağrıları.
 */
export function createContentProject(
  data: CreateContentProjectRequest
): Promise<ApiResponse<ContentProjectResponse>> {
  return request<ContentProjectResponse>('/content-projects', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}
