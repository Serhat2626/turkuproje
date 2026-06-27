import axios from 'axios'

const API_BASE = 'http://localhost:8080/api/v1'

const api = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Response interceptor - ApiResponse wrapper'ını aç
api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const message = error.response?.data?.message || error.message || 'Bir hata oluştu'
    console.error('API Error:', message)
    return Promise.reject(new Error(message))
  }
)

// Project API
export const projectApi = {
  list: () => api.get('/projects'),
  get: (id) => api.get(`/projects/${id}`),
  create: (data) => api.post('/projects', data),
}

// Research API
export const researchApi = {
  generate: (projectId) => api.post(`/projects/${projectId}/research/generate`),
  list: (projectId) => api.get(`/projects/${projectId}/research`),
  get: (projectId, researchId) => api.get(`/projects/${projectId}/research/${researchId}`),
  approve: (projectId, researchId, data) =>
    api.put(`/projects/${projectId}/research/${researchId}/approve`, data),
}

// Script API
export const scriptApi = {
  generate: (projectId) => api.post(`/projects/${projectId}/scripts/generate`),
  list: (projectId) => api.get(`/projects/${projectId}/scripts`),
  get: (projectId, scriptId) => api.get(`/projects/${projectId}/scripts/${scriptId}`),
}

// Prompt Template API
export const promptTemplateApi = {
  list: () => api.get('/prompt-templates'),
  create: (data) => api.post('/prompt-templates', data),
  update: (id, data) => api.put(`/prompt-templates/${id}`, data),
}

export default api
