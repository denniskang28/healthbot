import axios from 'axios';
import type { ActiveUserEntry, ConsultationEntry, PurchaseEntry, LlmConfigDto, UserDto, ChatMessageDto } from './types';

const api = axios.create({ baseURL: '' });

export const getActiveUsers = () => api.get<ActiveUserEntry[]>('/admin/users/active').then(r => r.data);
export const getUsers = () => api.get<UserDto[]>('/admin/users').then(r => r.data);
export const getConsultations = () => api.get<ConsultationEntry[]>('/admin/consultations').then(r => r.data);
export const getPurchases = () => api.get<PurchaseEntry[]>('/admin/purchases').then(r => r.data);
export const getLlmConfig = () => api.get<LlmConfigDto>('/admin/llm-config').then(r => r.data);
export const updateLlmConfig = (data: Record<string, unknown>) => api.put<LlmConfigDto>('/admin/llm-config', data).then(r => r.data);
export const getChatHistory = (userId: number) => api.get<ChatMessageDto[]>(`/admin/chat-history/${userId}`).then(r => r.data);
export const deleteChatHistory = (userId: number) => api.delete(`/admin/chat-history/${userId}`);
