import React, { useEffect, useState } from 'react';
import { Card, Form, Input, Switch, Button, message, Typography, Spin, Divider, Tag, Select, Alert } from 'antd';
import { getLlmConfig, updateLlmConfig } from '../api/client';
import type { LlmConfigDto } from '../api/types';
import { useLang } from '../context/LanguageContext';

const { Title, Text } = Typography;
const { TextArea } = Input;

const PROVIDERS = [
  { value: 'anthropic', label: 'Anthropic (Claude)',    color: 'blue'   },
  { value: 'openai',    label: 'OpenAI (ChatGPT)',      color: 'green'  },
  { value: 'qwen',      label: '阿里云 (千问 Qwen)',      color: 'orange' },
];

const MODEL_PRESETS: Record<string, string[]> = {
  anthropic: ['claude-sonnet-4-6', 'claude-opus-4-7', 'claude-haiku-4-5-20251001'],
  openai:    ['gpt-4o', 'gpt-4o-mini', 'gpt-4-turbo', 'gpt-3.5-turbo'],
  qwen:      ['qwen-max', 'qwen-plus', 'qwen-turbo'],
};

const ENV_KEYS: Record<string, string> = {
  anthropic: 'ANTHROPIC_API_KEY',
  openai:    'OPENAI_API_KEY',
  qwen:      'QWEN_API_KEY',
};

const LlmConfig: React.FC = () => {
  const [form] = Form.useForm();
  const [config, setConfig] = useState<LlmConfigDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [provider, setProvider] = useState<string>('anthropic');
  const { t } = useLang();

  useEffect(() => {
    getLlmConfig()
      .then(data => {
        setConfig(data);
        setProvider(data.provider || 'anthropic');
        form.setFieldsValue({
          provider: data.provider,
          model: data.model,
          apiUrl: data.apiUrl,
          systemPrompt: data.systemPrompt,
          active: data.active,
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
      const updated = await updateLlmConfig(values);
      setConfig(updated);
      message.success(t('configSaved'));
    } catch {
      message.error('Failed to save configuration');
    } finally {
      setSaving(false);
    }
  };

  // Build model options: presets + current DB value if not already included
  const buildModelOptions = (prov: string, current?: string) => {
    const presets = MODEL_PRESETS[prov] ?? [];
    const all = current && !presets.includes(current) ? [current, ...presets] : presets;
    return all.map(m => ({ value: m, label: m }));
  };

  if (loading) return <Spin size="large" style={{ display: 'block', margin: '100px auto' }} />;

  const providerMeta = PROVIDERS.find(p => p.value === config?.provider);

  return (
    <Card title={<Title level={5} style={{ margin: 0 }}>{t('llmConfig')}</Title>} style={{ maxWidth: 720 }}>
      {config && (
        <div style={{ marginBottom: 12 }}>
          <Text type="secondary">Current: </Text>
          <Tag color={providerMeta?.color ?? 'blue'}>{config.provider}</Tag>
          <Tag color="purple">{config.model}</Tag>
          <Tag color={config.active ? 'green' : 'red'}>{config.active ? 'Active' : 'Inactive'}</Tag>
        </div>
      )}

      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="Provider & API Key are read by the llm-service via environment variables"
        description={
          provider
            ? `Set PROVIDER=${provider} and ${ENV_KEYS[provider] ?? 'API_KEY'}=<your-key> when starting llm-service.`
            : undefined
        }
      />

      <Divider />

      <Form form={form} layout="vertical" onFinish={onSave}>
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
            placeholder="Select a model"
            filterOption={(input, opt) =>
              String(opt?.label ?? '').toLowerCase().includes(input.toLowerCase())
            }
          />
        </Form.Item>

        <Form.Item name="apiUrl" label={t('apiUrl')} rules={[{ required: true }]}>
          <Input placeholder="http://localhost:8000" />
        </Form.Item>

        <Form.Item
          name="apiKey"
          label={t('apiKey')}
          extra={config?.apiKeyMasked ? `Current: ${config.apiKeyMasked}` : 'Leave empty to keep existing key'}
        >
          <Input.Password placeholder="Enter new API key (leave empty to keep existing)" />
        </Form.Item>

        <Form.Item name="systemPrompt" label={t('systemPrompt')}>
          <TextArea rows={5} placeholder="System prompt for the AI model..." />
        </Form.Item>

        <Form.Item name="active" label={t('active')} valuePropName="checked">
          <Switch />
        </Form.Item>

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
