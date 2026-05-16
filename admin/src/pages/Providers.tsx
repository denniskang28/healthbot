import React, { useEffect, useState } from 'react';
import { Card, Table, Button, Switch, Space, Tag, Popconfirm, Modal, Form, Input, InputNumber, Select, message, Typography } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, EyeOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { getProviders, createProvider, updateProvider, deleteProvider, toggleProvider } from '../api/client';
import type { ServiceProviderDto } from '../api/types';
import { useLang } from '../context/LanguageContext';

const { Title } = Typography;

const TYPE_COLORS: Record<string, string> = {
  MEDICAL_LLM: 'purple',
  ONLINE_CONSULTATION: 'blue',
  OFFLINE_APPOINTMENT: 'green',
  ONLINE_PHARMACY: 'orange',
  OTHER: 'default',
};

const Providers: React.FC = () => {
  const [providers, setProviders] = useState<ServiceProviderDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<ServiceProviderDto | null>(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();
  const { t } = useLang();
  const navigate = useNavigate();

  const load = () => {
    setLoading(true);
    getProviders().then(setProviders).catch(() => {}).finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const openAdd = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ enabled: true, priority: 0 });
    setModalOpen(true);
  };

  const openEdit = (p: ServiceProviderDto) => {
    setEditing(p);
    form.setFieldsValue({
      name: p.name, type: p.type, company: p.company,
      description: p.description, priority: p.priority,
      enabled: p.enabled, config: p.config,
    });
    setModalOpen(true);
  };

  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      setSaving(true);
      if (editing) {
        await updateProvider(editing.id, values);
      } else {
        await createProvider(values);
      }
      message.success(t('saved'));
      setModalOpen(false);
      load();
    } catch {
      // validation error or API error — stay open
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: number) => {
    await deleteProvider(id).catch(() => {});
    load();
  };

  const handleToggle = async (id: number) => {
    await toggleProvider(id).catch(() => {});
    load();
  };

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

  const columns = [
    {
      title: t('providerName'), dataIndex: 'name', key: 'name',
      render: (name: string, r: ServiceProviderDto) => (
        <a onClick={() => navigate(`/providers/${r.id}`)}>{name}</a>
      ),
    },
    {
      title: t('type'), dataIndex: 'type', key: 'type',
      render: (type: string) => <Tag color={TYPE_COLORS[type] || 'default'}>{typeLabel(type)}</Tag>,
    },
    { title: t('company'), dataIndex: 'company', key: 'company' },
    {
      title: t('enabled'), dataIndex: 'enabled', key: 'enabled',
      render: (_: boolean, r: ServiceProviderDto) => (
        <Switch checked={r.enabled} onChange={() => handleToggle(r.id)} size="small" />
      ),
    },
    { title: t('priority'), dataIndex: 'priority', key: 'priority', sorter: (a: ServiceProviderDto, b: ServiceProviderDto) => a.priority - b.priority },
    { title: t('serviceCount'), dataIndex: 'serviceCount', key: 'serviceCount' },
    {
      title: t('avgRating'), dataIndex: 'avgRating', key: 'avgRating',
      render: (r: number | null) => r != null ? `${r.toFixed(1)} ⭐` : t('noRating'),
    },
    {
      title: '', key: 'actions',
      render: (_: unknown, r: ServiceProviderDto) => (
        <Space>
          <Button icon={<EyeOutlined />} size="small" onClick={() => navigate(`/providers/${r.id}`)}>{t('viewDetail')}</Button>
          <Button icon={<EditOutlined />} size="small" onClick={() => openEdit(r)} />
          <Popconfirm title={t('confirmDeleteProvider')} onConfirm={() => handleDelete(r.id)} okText={t('confirm')} cancelText={t('cancel')}>
            <Button icon={<DeleteOutlined />} size="small" danger />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const typeOptions = [
    { value: 'MEDICAL_LLM', label: t('MEDICAL_LLM') },
    { value: 'ONLINE_CONSULTATION', label: t('ONLINE_CONSULTATION') },
    { value: 'OFFLINE_APPOINTMENT', label: t('OFFLINE_APPOINTMENT') },
    { value: 'ONLINE_PHARMACY', label: t('ONLINE_PHARMACY') },
    { value: 'OTHER', label: t('OTHER') },
  ];

  return (
    <Card title={
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Title level={5} style={{ margin: 0 }}>{t('providers')}</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={openAdd}>{t('addProvider')}</Button>
      </div>
    }>
      <Table dataSource={providers} columns={columns} rowKey="id" loading={loading} size="middle" pagination={{ pageSize: 20 }} />

      <Modal
        title={editing ? t('editProvider') : t('addProvider')}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        confirmLoading={saving}
        width={600}
        destroyOnClose
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="name" label={t('providerName')} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="type" label={t('type')} rules={[{ required: true }]}>
            <Select options={typeOptions} />
          </Form.Item>
          <Form.Item name="company" label={t('company')}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label={t('description')}>
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="priority" label={t('priority')}>
            <InputNumber min={0} max={1000} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="enabled" label={t('enabled')} valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item name="config" label={t('configJson')}>
            <Input.TextArea rows={3} placeholder='{"apiKey":"...","endpoint":"..."}' />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

export default Providers;
