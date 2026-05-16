import React, { useEffect, useState } from 'react';
import { Card, Form, Input, Switch, Button, message, Typography, Spin, Divider, Tag, Select, Alert, Radio } from 'antd';
import { getLlmConfig, updateLlmConfig } from '../api/client';
import type { LlmConfigDto } from '../api/types';
import { useLang } from '../context/LanguageContext';

const { Title, Text } = Typography;

const PROVIDERS = [
  { value: 'anthropic', label: 'Anthropic (Claude)',  color: 'blue'   },
  { value: 'openai',    label: 'OpenAI (ChatGPT)',    color: 'green'  },
  { value: 'qwen',      label: '阿里云 (千问 Qwen)',   color: 'orange' },
  { value: 'deepseek',  label: 'DeepSeek',            color: 'geekblue' },
];

const MODEL_PRESETS: Record<string, string[]> = {
  anthropic: ['claude-sonnet-4-6', 'claude-opus-4-7', 'claude-haiku-4-5-20251001'],
  openai:    ['gpt-4o', 'gpt-4o-mini', 'gpt-4-turbo', 'gpt-3.5-turbo'],
  qwen:      ['qwen-max', 'qwen-plus', 'qwen-turbo'],
  deepseek:  ['deepseek-chat', 'deepseek-reasoner', 'deepseek-v4-flash'],
};

const LlmConfig: React.FC = () => {
  const [form] = Form.useForm();
  const [config, setConfig] = useState<LlmConfigDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [provider, setProvider] = useState<string>('qwen');
  const [mockMode, setMockMode] = useState(false);
  const { t } = useLang();

  useEffect(() => {
    getLlmConfig()
      .then((data: LlmConfigDto) => {
        setConfig(data);
        setProvider(data.provider || 'qwen');
        setMockMode(data.mockMode ?? false);
        form.setFieldsValue({
          provider: data.provider,
          model: data.model,
          mockMode: data.mockMode ?? false,
          mockScript: data.mockScript ?? 'MEDICATION',
        });
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [form]);

  const handleProviderChange = (val: string) => {
    setProvider(val);
    const presets = MODEL_PRESETS[val] ?? [];
    if (presets.length > 0) form.setFieldValue('model', presets[0]);
  };

  const onSave = async (values: Record<string, unknown>) => {
    setSaving(true);
    try {
      const updated = await updateLlmConfig(values) as LlmConfigDto;
      setConfig(updated);
      setMockMode(updated.mockMode ?? false);
      message.success(t('configSaved'));
    } catch {
      message.error('Failed to save — is the LLM service running?');
    } finally {
      setSaving(false);
    }
  };

  const buildModelOptions = (prov: string, current?: string) => {
    const presets = MODEL_PRESETS[prov] ?? [];
    const all = current && !presets.includes(current) ? [current, ...presets] : presets;
    return all.map(m => ({ value: m, label: m }));
  };

  if (loading) return <Spin size="large" style={{ display: 'block', margin: '100px auto' }} />;

  const providerMeta = PROVIDERS.find(p => p.value === config?.provider);

  return (
    <Card title={<Title level={5} style={{ margin: 0 }}>{t('llmConfig')}</Title>} style={{ maxWidth: 720 }}>

      {/* Current status tags */}
      {config && (
        <div style={{ marginBottom: 16 }}>
          <Text type="secondary">Current: </Text>
          <Tag color={providerMeta?.color ?? 'blue'}>{config.provider}</Tag>
          <Tag color="purple">{config.model}</Tag>
          {config.mockMode
            ? <Tag color="volcano">Mock Mode ON</Tag>
            : <Tag color="green">Live LLM</Tag>}
        </div>
      )}

      <Form form={form} layout="vertical" onFinish={onSave}>

        {/* Mock Mode — top of form, most important for demos */}
        <div style={{ background: '#fffbe6', border: '1px solid #ffe58f', borderRadius: 8, padding: '16px 20px', marginBottom: 24 }}>
          <Form.Item name="mockMode" valuePropName="checked" style={{ marginBottom: 8 }}>
            <Switch
              checkedChildren="Mock ON"
              unCheckedChildren="Mock OFF"
              onChange={setMockMode}
            />
          </Form.Item>
          <Text type="secondary" style={{ fontSize: 12 }}>{t('mockModeHint')}</Text>

          {mockMode && (
            <Form.Item name="mockScript" label={t('mockScript')} style={{ marginTop: 16, marginBottom: 0 }}>
              <Radio.Group>
                <Radio.Button value="MEDICATION">{t('mockScriptMedication')}</Radio.Button>
                <Radio.Button value="ONLINE_CONSULTATION">{t('mockScriptOnline')}</Radio.Button>
                <Radio.Button value="OFFLINE_APPOINTMENT">{t('mockScriptOffline')}</Radio.Button>
              </Radio.Group>
            </Form.Item>
          )}
        </div>

        <Divider orientation="left">LLM Provider</Divider>

        <Form.Item name="provider" label={t('provider')} rules={[{ required: true }]}>
          <Select
            options={PROVIDERS.map(p => ({ value: p.value, label: p.label }))}
            onChange={handleProviderChange}
          />
        </Form.Item>

        <Form.Item name="model" label={t('model')} rules={[{ required: true }]}>
          <Select
            showSearch
            options={buildModelOptions(provider, config?.model)}
            filterOption={(input, opt) =>
              String(opt?.label ?? '').toLowerCase().includes(input.toLowerCase())
            }
          />
        </Form.Item>

        <Form.Item
          name="apiKey"
          label={t('apiKey')}
          extra={config?.apiKeyMasked ? `Current: ${config.apiKeyMasked}` : 'No key configured'}
        >
          <Input.Password placeholder="Enter new API key (leave empty to keep existing)" />
        </Form.Item>

        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          message="Changes take effect immediately — no restart needed."
        />

        <Form.Item>
          <Button type="primary" htmlType="submit" loading={saving} size="large">
            {t('save')}
          </Button>
        </Form.Item>
      </Form>
    </Card>
  );
};

export default LlmConfig;
