import React, { useState } from 'react';
import { Button, Card, Form, Input, Typography, message } from 'antd';
import { LockOutlined, HeartOutlined } from '@ant-design/icons';
import { login } from '../api/client';

const { Title, Text } = Typography;

interface Props {
  onLogin: () => void;
}

const Login: React.FC<Props> = ({ onLogin }) => {
  const [loading, setLoading] = useState(false);

  const onFinish = async ({ password }: { password: string }) => {
    setLoading(true);
    try {
      const { token } = await login(password);
      localStorage.setItem('admin_token', token);
      onLogin();
    } catch {
      message.error('Incorrect password');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#f0f2f5' }}>
      <Card style={{ width: 360, boxShadow: '0 4px 24px rgba(0,0,0,0.08)' }}>
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <HeartOutlined style={{ fontSize: 40, color: '#1677ff' }} />
          <Title level={4} style={{ marginTop: 12, marginBottom: 4 }}>HealthBot Admin</Title>
          <Text type="secondary">Enter your password to continue</Text>
        </div>
        <Form onFinish={onFinish} layout="vertical">
          <Form.Item name="password" rules={[{ required: true, message: 'Please enter password' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="Password" size="large" autoFocus />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0 }}>
            <Button type="primary" htmlType="submit" loading={loading} size="large" block>
              Log In
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};

export default Login;
