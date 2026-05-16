import React, { useState } from 'react';
import { Button, Card, Form, Input, Typography, Alert, message } from 'antd';
import { changePassword } from '../api/client';
import { useLang } from '../context/LanguageContext';

const { Title } = Typography;

const Settings: React.FC = () => {
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);
  const [success, setSuccess] = useState(false);
  const { t } = useLang();

  const onFinish = async (values: { currentPassword: string; newPassword: string; confirmPassword: string }) => {
    if (values.newPassword !== values.confirmPassword) {
      message.error(t('passwordMismatch'));
      return;
    }
    setSaving(true);
    setSuccess(false);
    try {
      await changePassword(values.currentPassword, values.newPassword);
      message.success(t('passwordChanged'));
      setSuccess(true);
      form.resetFields();
      // Token is invalidated on backend — force re-login
      setTimeout(() => {
        localStorage.removeItem('admin_token');
        window.location.reload();
      }, 1500);
    } catch {
      message.error(t('passwordWrong'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Card title={<Title level={5} style={{ margin: 0 }}>{t('settings')}</Title>} style={{ maxWidth: 480 }}>
      <Title level={5} style={{ marginTop: 0 }}>{t('changePassword')}</Title>

      {success && (
        <Alert type="success" message={t('passwordChanged')} style={{ marginBottom: 16 }} showIcon />
      )}

      <Form form={form} layout="vertical" onFinish={onFinish}>
        <Form.Item name="currentPassword" label={t('currentPassword')} rules={[{ required: true }]}>
          <Input.Password placeholder="••••••••" />
        </Form.Item>
        <Form.Item name="newPassword" label={t('newPassword')} rules={[{ required: true, min: 4 }]}>
          <Input.Password placeholder="••••••••" />
        </Form.Item>
        <Form.Item name="confirmPassword" label={t('confirmPassword')} rules={[{ required: true }]}>
          <Input.Password placeholder="••••••••" />
        </Form.Item>
        <Form.Item style={{ marginBottom: 0 }}>
          <Button type="primary" htmlType="submit" loading={saving}>
            {t('save')}
          </Button>
        </Form.Item>
      </Form>
    </Card>
  );
};

export default Settings;
