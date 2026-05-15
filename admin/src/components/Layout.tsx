import React, { useState } from 'react';
import { Layout, Menu, Button, Typography, Space, theme } from 'antd';
import {
  DashboardOutlined, MedicineBoxOutlined, ShoppingCartOutlined,
  SettingOutlined, TranslationOutlined, HeartOutlined, MessageOutlined,
} from '@ant-design/icons';
import { useNavigate, useLocation } from 'react-router-dom';
import { useLang } from '../context/LanguageContext';

const { Header, Sider, Content } = Layout;
const { Title } = Typography;

const AppLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [collapsed, setCollapsed] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { t, toggleLang } = useLang();
  const { token } = theme.useToken();

  const menuItems = [
    { key: '/', icon: <DashboardOutlined />, label: t('dashboard') },
    { key: '/consultations', icon: <MedicineBoxOutlined />, label: t('consultations') },
    { key: '/purchases', icon: <ShoppingCartOutlined />, label: t('purchases') },
    { key: '/chat-history', icon: <MessageOutlined />, label: t('chatHistory') },
    { key: '/llm-config', icon: <SettingOutlined />, label: t('llmConfig') },
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider collapsible collapsed={collapsed} onCollapse={setCollapsed}
        style={{ background: '#001529' }}>
        <div style={{ padding: '16px', textAlign: 'center', borderBottom: '1px solid rgba(255,255,255,0.1)' }}>
          <HeartOutlined style={{ color: '#1890ff', fontSize: 24 }} />
          {!collapsed && (
            <div style={{ color: 'white', fontSize: 12, marginTop: 4 }}>HealthBot</div>
          )}
        </div>
        <Menu
          theme="dark"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
          style={{ marginTop: 8 }}
        />
      </Sider>
      <Layout>
        <Header style={{ background: token.colorBgContainer, padding: '0 24px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', boxShadow: '0 1px 4px rgba(0,0,0,0.1)' }}>
          <Title level={4} style={{ margin: 0, color: token.colorPrimary }}>
            {t('appTitle')}
          </Title>
          <Space>
            <Button icon={<TranslationOutlined />} onClick={toggleLang} type="text">
              {t('switchLang')}
            </Button>
          </Space>
        </Header>
        <Content style={{ margin: '24px', background: token.colorBgLayout }}>
          {children}
        </Content>
      </Layout>
    </Layout>
  );
};

export default AppLayout;
