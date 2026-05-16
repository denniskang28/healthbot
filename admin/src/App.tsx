import React, { useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import { LanguageProvider } from './context/LanguageContext';
import AppLayout from './components/Layout';
import Dashboard from './pages/Dashboard';
import Consultations from './pages/Consultations';
import Purchases from './pages/Purchases';
import LlmConfig from './pages/LlmConfig';
import ChatHistory from './pages/ChatHistory';
import Settings from './pages/Settings';
import Login from './pages/Login';
import Providers from './pages/Providers';
import ProviderDetail from './pages/ProviderDetail';
import RoutingRules from './pages/RoutingRules';

const App: React.FC = () => {
  const [authed, setAuthed] = useState(!!localStorage.getItem('admin_token'));

  if (!authed) return (
    <ConfigProvider theme={{ token: { colorPrimary: '#1677ff' } }}>
      <Login onLogin={() => setAuthed(true)} />
    </ConfigProvider>
  );

  return (
    <ConfigProvider theme={{ token: { colorPrimary: '#1677ff' } }}>
      <LanguageProvider>
        <BrowserRouter>
          <AppLayout onLogout={() => setAuthed(false)}>
            <Routes>
              <Route path="/" element={<Dashboard />} />
              <Route path="/consultations" element={<Consultations />} />
              <Route path="/purchases" element={<Purchases />} />
              <Route path="/chat-history" element={<ChatHistory />} />
              <Route path="/llm-config" element={<LlmConfig />} />
              <Route path="/settings" element={<Settings />} />
              <Route path="/providers" element={<Providers />} />
              <Route path="/providers/:id" element={<ProviderDetail />} />
              <Route path="/routing-rules" element={<RoutingRules />} />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </AppLayout>
        </BrowserRouter>
      </LanguageProvider>
    </ConfigProvider>
  );
};

export default App;
