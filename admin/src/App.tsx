import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import { LanguageProvider } from './context/LanguageContext';
import AppLayout from './components/Layout';
import Dashboard from './pages/Dashboard';
import Consultations from './pages/Consultations';
import Purchases from './pages/Purchases';
import LlmConfig from './pages/LlmConfig';

const App: React.FC = () => (
  <ConfigProvider theme={{ token: { colorPrimary: '#1677ff' } }}>
    <LanguageProvider>
      <BrowserRouter>
        <AppLayout>
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/consultations" element={<Consultations />} />
            <Route path="/purchases" element={<Purchases />} />
            <Route path="/llm-config" element={<LlmConfig />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </AppLayout>
      </BrowserRouter>
    </LanguageProvider>
  </ConfigProvider>
);

export default App;
