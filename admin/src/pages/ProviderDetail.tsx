import React, { useEffect, useMemo, useState } from 'react';
import { Card, Descriptions, Table, Button, Tag, Rate, Modal, Form, InputNumber, Input,
  Space, Typography, Spin, message, Divider, Select, Radio, Alert } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { useParams, useNavigate } from 'react-router-dom';
import { getProvider, getProviderRecords, rateRecord, updateProvider } from '../api/client';
import type { ServiceProviderDto, ServiceRecordDto } from '../api/types';
import { useLang } from '../context/LanguageContext';

const { Title, Text } = Typography;

const TYPE_COLORS: Record<string, string> = {
  MEDICAL_LLM: 'purple',
  ONLINE_CONSULTATION: 'blue',
  OFFLINE_APPOINTMENT: 'green',
  ONLINE_PHARMACY: 'orange',
  OTHER: 'default',
};

const PROVIDERS = [
  { value: 'anthropic', label: 'Anthropic (Claude)',  color: 'blue'     },
  { value: 'openai',    label: 'OpenAI (ChatGPT)',    color: 'green'    },
  { value: 'qwen',      label: '阿里云 (千问 Qwen)',   color: 'orange'   },
  { value: 'deepseek',  label: 'DeepSeek',            color: 'geekblue' },
];

const MODEL_PRESETS: Record<string, string[]> = {
  anthropic: ['claude-sonnet-4-6', 'claude-opus-4-7', 'claude-haiku-4-5-20251001'],
  openai:    ['gpt-4o', 'gpt-4o-mini', 'gpt-4-turbo'],
  qwen:      ['qwen-max', 'qwen-plus', 'qwen-turbo'],
  deepseek:  ['deepseek-chat', 'deepseek-reasoner'],
};

// ── LLM Config sub-form (only for MEDICAL_LLM providers) ─────────────────────
const LlmConfigCard: React.FC<{ provider: ServiceProviderDto; onSaved: () => void }> = ({ provider, onSaved }) => {
  const { t } = useLang();
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);
  const [llmProvider, setLlmProvider] = useState('anthropic');

  const existingConfig = useMemo(() => {
    try { return provider.config ? JSON.parse(provider.config) : {}; } catch { return {}; }
  }, [provider.config]);

  const isMock = !!existingConfig.mockMode;

  useEffect(() => {
    setLlmProvider(existingConfig.provider || 'anthropic');
    form.setFieldsValue({
      provider: existingConfig.provider || 'anthropic',
      model: existingConfig.model || '',
      mockScript: existingConfig.mockScript || 'MEDICATION',
      systemPrompt: existingConfig.systemPrompt || '',
    });
  }, [provider.config]);

  const handleProviderChange = (val: string) => {
    setLlmProvider(val);
    const presets = MODEL_PRESETS[val] ?? [];
    if (presets.length > 0) form.setFieldValue('model', presets[0]);
  };

  const buildModelOptions = (prov: string) => {
    const presets = MODEL_PRESETS[prov] ?? [];
    const current = existingConfig.model;
    const all = current && !presets.includes(current) ? [current, ...presets] : presets;
    return all.map(m => ({ value: m, label: m }));
  };

  const onSave = async (values: Record<string, unknown>) => {
    setSaving(true);
    try {
      const newConfig = { ...existingConfig, ...values };
      if (!values.apiKey) delete newConfig.apiKey;
      const updated = { ...provider, config: JSON.stringify(newConfig) };
      await updateProvider(provider.id, updated);
      message.success(t('saved'));
      onSaved();
    } catch {
      message.error('Save failed');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Card title={<Title level={5} style={{ margin: 0 }}>{t('llmConfigCard')}</Title>}>
      {isMock ? (
        <>
          <Alert type="warning" showIcon style={{ marginBottom: 16 }}
            message={t('mockModeHint')} />
          <Form form={form} layout="vertical" onFinish={onSave}>
            <Form.Item name="mockScript" label={t('mockScript')}>
              <Radio.Group>
                <Radio.Button value="MEDICATION">{t('mockScriptMedication')}</Radio.Button>
                <Radio.Button value="ONLINE_CONSULTATION">{t('mockScriptOnline')}</Radio.Button>
                <Radio.Button value="OFFLINE_APPOINTMENT">{t('mockScriptOffline')}</Radio.Button>
              </Radio.Group>
            </Form.Item>
            <Alert type="info" showIcon style={{ marginBottom: 16 }}
              message="Config is applied on the next chat request via routing rules." />
            <Form.Item>
              <Button type="primary" htmlType="submit" loading={saving}>{t('save')}</Button>
            </Form.Item>
          </Form>
        </>
      ) : (
        <>
          {existingConfig.provider && (
            <div style={{ marginBottom: 16 }}>
              <Text type="secondary">Current: </Text>
              <Tag color={PROVIDERS.find(p => p.value === existingConfig.provider)?.color ?? 'blue'}>{existingConfig.provider}</Tag>
              <Tag color="purple">{existingConfig.model}</Tag>
            </div>
          )}
          <Form form={form} layout="vertical" onFinish={onSave}>
            <Divider orientation="left">LLM Provider</Divider>
            <Form.Item name="provider" label={t('provider')} rules={[{ required: true }]}>
              <Select
                options={PROVIDERS.map(p => ({ value: p.value, label: p.label }))}
                onChange={handleProviderChange}
              />
            </Form.Item>
            <Form.Item name="model" label={t('model')} rules={[{ required: true }]}>
              <Select showSearch options={buildModelOptions(llmProvider)}
                filterOption={(input, opt) => String(opt?.label ?? '').toLowerCase().includes(input.toLowerCase())} />
            </Form.Item>
            <Form.Item name="apiKey" label={t('apiKey')}
              extra={existingConfig.apiKey ? `Current: ****...${String(existingConfig.apiKey).slice(-4)}` : 'No key configured'}>
              <Input.Password placeholder="Enter new API key (leave empty to keep existing)" />
            </Form.Item>
            <Form.Item name="systemPrompt" label={t('systemPrompt')}>
              <Input.TextArea rows={4} />
            </Form.Item>
            <Alert type="info" showIcon style={{ marginBottom: 16 }}
              message="Config is applied on the next chat request via routing rules." />
            <Form.Item>
              <Button type="primary" htmlType="submit" loading={saving}>{t('save')}</Button>
            </Form.Item>
          </Form>
        </>
      )}
    </Card>
  );
};

// ── Main page ─────────────────────────────────────────────────────────────────
const ProviderDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { t } = useLang();
  const [provider, setProvider] = useState<ServiceProviderDto | null>(null);
  const [records, setRecords] = useState<ServiceRecordDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [ratingRecord, setRatingRecord] = useState<ServiceRecordDto | null>(null);
  const [rateForm] = Form.useForm();
  const [rateSaving, setRateSaving] = useState(false);

  const load = () => {
    setLoading(true);
    const numId = Number(id);
    Promise.all([getProvider(numId), getProviderRecords(numId)])
      .then(([p, r]) => { setProvider(p); setRecords(r); })
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, [id]);

  const openRate = (r: ServiceRecordDto) => {
    setRatingRecord(r);
    rateForm.setFieldsValue({ rating: r.rating ?? 0, notes: r.notes ?? '' });
  };

  const handleRate = async () => {
    try {
      const values = await rateForm.validateFields();
      setRateSaving(true);
      await rateRecord(ratingRecord!.id, values.rating, values.notes);
      message.success(t('saved'));
      setRatingRecord(null);
      load();
    } catch { } finally { setRateSaving(false); }
  };

  const typeLabel = (type: string) => {
    const map: Record<string, string> = {
      MEDICAL_LLM: t('MEDICAL_LLM'), ONLINE_CONSULTATION: t('ONLINE_CONSULTATION'),
      OFFLINE_APPOINTMENT: t('OFFLINE_APPOINTMENT'), ONLINE_PHARMACY: t('ONLINE_PHARMACY'), OTHER: t('OTHER'),
    };
    return map[type] || type;
  };

  const statusColor = (s: string) => ({ DISPATCHED: 'processing', COMPLETED: 'success', FAILED: 'error' }[s] || 'default');

  const recordColumns = [
    { title: t('userName'), dataIndex: 'userName', key: 'userName' },
    {
      title: t('serviceType'), dataIndex: 'serviceType', key: 'serviceType',
      render: (type: string) => <Tag color={TYPE_COLORS[type] || 'default'}>{typeLabel(type)}</Tag>,
    },
    {
      title: t('status'), dataIndex: 'status', key: 'status',
      render: (s: string) => <Tag color={statusColor(s)}>{s}</Tag>,
    },
    {
      title: t('avgRating'), dataIndex: 'rating', key: 'rating',
      render: (r: number | null) => r != null ? <Rate disabled value={r} count={5} /> : <span style={{ color: '#999' }}>{t('noRating')}</span>,
    },
    { title: t('startTime'), dataIndex: 'createdAt', key: 'createdAt', render: (v: string) => new Date(v).toLocaleString() },
    {
      title: '', key: 'actions',
      render: (_: unknown, r: ServiceRecordDto) => (
        <Button size="small" onClick={() => openRate(r)}>{t('rateService')}</Button>
      ),
    },
  ];

  if (loading) return <Spin style={{ display: 'block', marginTop: 80 }} />;
  if (!provider) return null;

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="middle">
      <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/providers')}>{t('back')}</Button>

      <Card title={<Title level={5} style={{ margin: 0 }}>{provider.name}</Title>}>
        <Descriptions column={2} size="middle">
          <Descriptions.Item label={t('type')}>
            <Tag color={TYPE_COLORS[provider.type] || 'default'}>{typeLabel(provider.type)}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label={t('company')}>{provider.company}</Descriptions.Item>
          <Descriptions.Item label={t('enabled')}>
            <Tag color={provider.enabled ? 'success' : 'default'}>{provider.enabled ? t('enabled') : 'Disabled'}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label={t('priority')}>{provider.priority}</Descriptions.Item>
          <Descriptions.Item label={t('serviceCount')}>{provider.serviceCount}</Descriptions.Item>
          <Descriptions.Item label={t('avgRating')}>
            {provider.avgRating != null ? `${provider.avgRating.toFixed(1)} ⭐` : t('noRating')}
          </Descriptions.Item>
          <Descriptions.Item label={t('description')} span={2}>{provider.description}</Descriptions.Item>
        </Descriptions>
      </Card>

      {provider.type === 'MEDICAL_LLM' && (
        <LlmConfigCard provider={provider} onSaved={load} />
      )}

      <Card title={<Title level={5} style={{ margin: 0 }}>{t('serviceRecords')}</Title>}>
        <Table dataSource={records} columns={recordColumns} rowKey="id" size="middle" pagination={{ pageSize: 20 }} />
      </Card>

      <Modal title={t('rateService')} open={!!ratingRecord} onOk={handleRate}
        onCancel={() => setRatingRecord(null)} confirmLoading={rateSaving} destroyOnClose>
        <Form form={rateForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="rating" label={t('avgRating')} rules={[{ required: true }]}>
            <InputNumber min={1} max={5} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="notes" label="Notes">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  );
};

export default ProviderDetail;
