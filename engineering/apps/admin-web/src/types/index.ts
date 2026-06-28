export interface HealthResponse {
  status: string;
  service: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string | null;
}
