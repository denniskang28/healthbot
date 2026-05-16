import React, { createContext, useContext, useState } from 'react';

type Lang = 'EN' | 'ZH';

const translations = {
  EN: {
    appTitle: 'HealthBot Admin Console',
    dashboard: 'Dashboard',
    consultations: 'Consultations',
    purchases: 'Purchases',
    llmConfig: 'LLM Config',
    totalUsers: 'Total Users',
    activeSessions: 'Active Sessions',
    todayConsultations: "Today's Consultations",
    todayPurchases: "Today's Purchases",
    userStatus: 'Real-time User Status',
    IDLE: 'Idle',
    CHATTING: 'Chatting',
    AI_CONSULTATION: 'AI Consultation',
    DOCTOR_CONSULTATION: 'Doctor Consultation',
    APPOINTMENT: 'Appointment',
    PHARMACY: 'Pharmacy',
    ACTIVE: 'Active',
    COMPLETED: 'Completed',
    CANCELLED: 'Cancelled',
    PENDING: 'Pending',
    AI_CONSULTATION_label: 'AI',
    DOCTOR_CONSULTATION_label: 'Doctor',
    save: 'Save',
    provider: 'Provider',
    model: 'Model',
    apiUrl: 'API URL',
    apiKey: 'API Key',
    systemPrompt: 'System Prompt',
    active: 'Active',
    userName: 'User',
    phone: 'Phone',
    state: 'State',
    lastActive: 'Last Active',
    type: 'Type',
    status: 'Status',
    startTime: 'Start Time',
    prescription: 'Prescription',
    amount: 'Amount',
    purchasedAt: 'Purchased At',
    medicines: 'Medicines',
    configSaved: 'Configuration saved — LLM service updated',
    switchLang: '中文',
    chatHistory: 'Chat History',
    selectUser: 'Select a user to view chat history',
    noHistory: 'No chat history',
    clearHistory: 'Clear History',
    confirmClearHistory: 'Delete all messages for this user? This also resets the demo counter.',
    historyCleared: 'Chat history cleared',
    confirm: 'Confirm',
    cancel: 'Cancel',
    logout: 'Logout',
    confirmLogout: 'Log out of admin console?',
    settings: 'Settings',
    changePassword: 'Change Password',
    currentPassword: 'Current Password',
    newPassword: 'New Password',
    confirmPassword: 'Confirm New Password',
    passwordChanged: 'Password updated — please log in again',
    passwordWrong: 'Current password is incorrect',
    passwordMismatch: 'New passwords do not match',
    mockMode: 'Mock Mode',
    mockModeHint: 'When enabled, uses predefined scripts instead of calling the LLM — for reliable demos',
    mockScript: 'Demo Script',
    mockScriptMedication: 'Case 1 — Pharmacy (common cold)',
    mockScriptOnline: 'Case 2 — Online Consultation (migraines)',
    mockScriptOffline: 'Case 3 — Offline Appointment (weight loss)',
  },
  ZH: {
    appTitle: 'HealthBot 管理控制台',
    dashboard: '仪表板',
    consultations: '问诊记录',
    purchases: '购药记录',
    llmConfig: 'AI模型配置',
    totalUsers: '总用户数',
    activeSessions: '活跃会话',
    todayConsultations: '今日问诊',
    todayPurchases: '今日购药',
    userStatus: '实时用户状态',
    IDLE: '空闲',
    CHATTING: '咨询中',
    AI_CONSULTATION: 'AI问诊',
    DOCTOR_CONSULTATION: '医生问诊',
    APPOINTMENT: '预约',
    PHARMACY: '药房',
    ACTIVE: '进行中',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    PENDING: '待处理',
    AI_CONSULTATION_label: 'AI',
    DOCTOR_CONSULTATION_label: '医生',
    save: '保存',
    provider: '服务商',
    model: '模型',
    apiUrl: 'API地址',
    apiKey: 'API密钥',
    systemPrompt: '系统提示词',
    active: '启用',
    userName: '用户',
    phone: '电话',
    state: '状态',
    lastActive: '最后活跃',
    type: '类型',
    status: '状态',
    startTime: '开始时间',
    prescription: '处方',
    amount: '金额',
    purchasedAt: '购买时间',
    medicines: '药品',
    configSaved: '配置已保存并通知LLM服务生效',
    switchLang: 'English',
    chatHistory: '对话历史',
    selectUser: '选择用户查看对话历史',
    noHistory: '暂无对话记录',
    clearHistory: '清除历史',
    confirmClearHistory: '删除该用户的所有消息？同时重置演示计数器。',
    historyCleared: '对话历史已清除',
    confirm: '确认',
    cancel: '取消',
    logout: '退出登录',
    confirmLogout: '确认退出管理控制台？',
    settings: '系统管理',
    changePassword: '修改密码',
    currentPassword: '当前密码',
    newPassword: '新密码',
    confirmPassword: '确认新密码',
    passwordChanged: '密码已更新，请重新登录',
    passwordWrong: '当前密码不正确',
    passwordMismatch: '两次输入的新密码不一致',
    mockMode: 'Mock模式',
    mockModeHint: '开启后使用预设脚本而非真实LLM调用，确保演示路线可预测',
    mockScript: '演示脚本',
    mockScriptMedication: '案例1 — 线上药房（普通感冒）',
    mockScriptOnline: '案例2 — 线上问诊（偏头痛）',
    mockScriptOffline: '案例3 — 线下预约（体重下降）',
  }
};

interface LanguageContextType {
  lang: Lang;
  t: (key: keyof typeof translations.EN) => string;
  toggleLang: () => void;
}

const LanguageContext = createContext<LanguageContextType | null>(null);

export const LanguageProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const stored = (localStorage.getItem('admin_lang') as Lang) || 'EN';
  const [lang, setLang] = useState<Lang>(stored);

  const toggleLang = () => {
    const next: Lang = lang === 'EN' ? 'ZH' : 'EN';
    setLang(next);
    localStorage.setItem('admin_lang', next);
  };

  const t = (key: keyof typeof translations.EN) => translations[lang][key] || key;

  return (
    <LanguageContext.Provider value={{ lang, t, toggleLang }}>
      {children}
    </LanguageContext.Provider>
  );
};

export const useLang = () => {
  const ctx = useContext(LanguageContext);
  if (!ctx) throw new Error('useLang must be used within LanguageProvider');
  return ctx;
};
