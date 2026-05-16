import axios from 'axios';
import type { ActiveUserEntry, ConsultationEntry, PurchaseEntry, LlmConfigDto, UserDto, ChatMessageDto, ServiceProviderDto, ServiceRecordDto, RoutingRuleDto } from './types';

const api = axios.create({ baseURL: '' });

api.interceptors.request.use(config => {
  const token = localStorage.getItem('admin_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401 && !err.config.url.includes('/admin/auth/')) {
      localStorage.removeItem('admin_token');
      window.location.reload();
    }
    return Promise.reject(err);
  }
);

export const getActiveUsers = () => api.get<ActiveUserEntry[]>('/admin/users/active').then(r => r.data);
export const getUsers = () => api.get<UserDto[]>('/admin/users').then(r => r.data);
export const getConsultations = () => api.get<ConsultationEntry[]>('/admin/consultations').then(r => r.data);
export const getPurchases = () => api.get<PurchaseEntry[]>('/admin/purchases').then(r => r.data);
export const getLlmConfig = () => api.get<LlmConfigDto>('/admin/llm-config').then(r => r.data);
export const updateLlmConfig = (data: Record<string, unknown>) => api.put<LlmConfigDto>('/admin/llm-config', data).then(r => r.data);
export const getChatHistory = (userId: number) => api.get<ChatMessageDto[]>(`/admin/chat-history/${userId}`).then(r => r.data);
export const deleteChatHistory = (userId: number) => api.delete(`/admin/chat-history/${userId}`);
export const login = (password: string) => api.post<{ token: string }>('/admin/auth/login', { password }).then(r => r.data);
export const changePassword = (currentPassword: string, newPassword: string) => api.put('/admin/auth/password', { currentPassword, newPassword });

export const getProviders = () => api.get<ServiceProviderDto[]>('/admin/providers').then(r => r.data);
export const getProvider = (id: number) => api.get<ServiceProviderDto>(`/admin/providers/${id}`).then(r => r.data);
export const createProvider = (data: Partial<ServiceProviderDto>) => api.post<ServiceProviderDto>('/admin/providers', data).then(r => r.data);
export const updateProvider = (id: number, data: Partial<ServiceProviderDto>) => api.put<ServiceProviderDto>(`/admin/providers/${id}`, data).then(r => r.data);
export const deleteProvider = (id: number) => api.delete(`/admin/providers/${id}`);
export const toggleProvider = (id: number) => api.patch<ServiceProviderDto>(`/admin/providers/${id}/toggle`).then(r => r.data);
export const getProviderRecords = (id: number) => api.get<ServiceRecordDto[]>(`/admin/providers/${id}/records`).then(r => r.data);
export const rateRecord = (recordId: number, rating: number, notes?: string) => api.patch(`/admin/providers/records/${recordId}/rate`, { rating, notes });

export const getRoutingRules = () => api.get<RoutingRuleDto[]>('/admin/routing-rules').then(r => r.data);
export const createRoutingRule = (data: Partial<RoutingRuleDto>) => api.post<RoutingRuleDto>('/admin/routing-rules', data).then(r => r.data);
export const updateRoutingRule = (id: number, data: Partial<RoutingRuleDto>) => api.put<RoutingRuleDto>(`/admin/routing-rules/${id}`, data).then(r => r.data);
export const deleteRoutingRule = (id: number) => api.delete(`/admin/routing-rules/${id}`);
export const toggleRoutingRule = (id: number) => api.patch<RoutingRuleDto>(`/admin/routing-rules/${id}/toggle`).then(r => r.data);
