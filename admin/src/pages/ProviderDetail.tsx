import React, { useEffect, useState } from 'react';
import { Card, Descriptions, Table, Button, Tag, Rate, Modal, Form, InputNumber, Input, Space, Typography, Spin, message } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { useParams, useNavigate } from 'react-router-dom';
import { getProvider, getProviderRecords, rateRecord } from '../api/client';
import type { ServiceProviderDto, ServiceRecordDto } from '../api/types';
import { useLang } from '../context/LanguageContext';

const { Title } = Typography;

const TYPE_COLORS: Record<string, string> = {
  MEDICAL_LLM: 'purple',
  ONLINE_CONSULTATION: 'blue',
  OFFLINE_APPOINTMENT: 'green',
  ONLINE_PHARMACY: 'orange',
  OTHER: 'default',
};

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
    Promise.all([getProvider(Number(id)), getProviderRecords(Number(id))])
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
    } catch {
      // stay open
    } finally {
      setRateSaving(false);
    }
  };

  const statusColor = (s: string) => ({ DISPATCHED: 'processing', COMPLETED: 'success', FAILED: 'error' })[s] || 'default';

  const typeLabel = (type: string) => {
    const map: Record<string, string> = {
      MEDICAL_LLM: t('MEDICAL_LLM'),
      ONLINE_CONSULTATION: t('ONLINE_CONSULTATION'),
      OFFLINE_APPOINTMENT: t('OFFLINE_APPOINTMENT'),
      ONLINE_PHARMACY: t('ONLINE_PHARMACY'),
      OTHER: t('OTHER'),
    };
    return map[type] || type;
  };

  const statusLabel = (s: string) => {
    const map: Record<string, string> = {
      DISPATCHED: t('DISPATCHED'),
      COMPLETED: t('COMPLETED'),
      FAILED: t('FAILED'),
    };
    return map[s] || s;
  };

  const recordColumns = [
    { title: t('userName'), dataIndex: 'userName', key: 'userName' },
    {
      title: t('serviceType'), dataIndex: 'serviceType', key: 'serviceType',
      render: (type: string) => <Tag color={TYPE_COLORS[type] || 'default'}>{typeLabel(type)}</Tag>,
    },
    {
      title: t('status'), dataIndex: 'status', key: 'status',
      render: (s: string) => <Tag color={statusColor(s)}>{statusLabel(s)}</Tag>,
    },
    {
      title: t('avgRating'), dataIndex: 'rating', key: 'rating',
      render: (r: number | null) => r != null ? <Rate disabled defaultValue={r} count={5} /> : <span style={{ color: '#999' }}>{t('noRating')}</span>,
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
            <Tag color={provider.enabled ? 'success' : 'default'}>{provider.enabled ? t('enabled') : t('FAILED')}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label={t('priority')}>{provider.priority}</Descriptions.Item>
          <Descriptions.Item label={t('serviceCount')}>{provider.serviceCount}</Descriptions.Item>
          <Descriptions.Item label={t('avgRating')}>
            {provider.avgRating != null ? `${provider.avgRating.toFixed(1)} ⭐` : t('noRating')}
          </Descriptions.Item>
          <Descriptions.Item label={t('description')} span={2}>{provider.description}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title={<Title level={5} style={{ margin: 0 }}>{t('serviceRecords')}</Title>}>
        <Table dataSource={records} columns={recordColumns} rowKey="id" size="middle" pagination={{ pageSize: 20 }} />
      </Card>

      <Modal
        title={t('rateService')}
        open={!!ratingRecord}
        onOk={handleRate}
        onCancel={() => setRatingRecord(null)}
        confirmLoading={rateSaving}
        destroyOnClose
      >
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
