import request from './client';
import type {
  ApiResponse,
  RegisterMediaAssetRequest,
  MediaAssetResponse,
} from '../types';

/**
 * Medya varlıkları API çağrıları.
 */
export function registerMediaAsset(
  data: RegisterMediaAssetRequest
): Promise<ApiResponse<MediaAssetResponse>> {
  return request<MediaAssetResponse>('/media-assets', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}
