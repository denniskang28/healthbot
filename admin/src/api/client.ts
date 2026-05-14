import axios from 'axios';
import type { ActiveUserEntry, ConsultationEntry, PurchaseEntry, LlmConfigDto } from './types';

const api = axios.create({ baseURL: '' });

export const getActiveUsers = () => api.get<ActiveUserEntry[]>('/admin/users/active').then(r => r.data);
export const getConsultations = () => api.get<ConsultationEntry[]>('/admin/consultations').then(r => r.data);
export const getPurchases = () => api.get<PurchaseEntry[]>('/admin/purchases').then(r => r.data);
export const getLlmConfig = () => api.get<LlmConfigDto>('/admin/llm-config').then(r => r.data);
export const updateLlmConfig = (data: Record<string, unknown>) => api.put<LlmConfigDto>('/admin/llm-config', data).then(r => r.data);
