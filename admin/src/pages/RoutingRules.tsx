import React, { useEffect, useState } from 'react';
import { Card, Table, Button, Switch, Space, Tag, Popconfirm, Modal, Form, Input, InputNumber, Select, message, Typography } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { getRoutingRules, createRoutingRule, updateRoutingRule, deleteRoutingRule, toggleRoutingRule, getProviders } from '../api/client';
import type { RoutingRuleDto, ServiceProviderDto } from '../api/types';
import { useLang } from '../context/LanguageContext';

const { Title } = Typography;

const SERVICE_TYPES = ['ONLINE_CONSULTATION', 'OFFLINE_APPOINTMENT', 'ONLINE_PHARMACY'];

const RoutingRules: React.FC = () => {
  const [rules, setRules] = useState<RoutingRuleDto[]>([]);
  const [providers, setProviders] = useState<ServiceProviderDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<RoutingRuleDto | null>(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();
  const { t } = useLang();

  const load = () => {
    setLoading(true);
    Promise.all([getRoutingRules(), getProviders()])
      .then(([r, p]) => { setRules(r); setProviders(p); })
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const openAdd = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ enabled: true, priority: 0, conditionJson: '{}' });
    setModalOpen(true);
  };

  const openEdit = (r: RoutingRuleDto) => {
    setEditing(r);
    form.setFieldsValue({
      name: r.name, serviceType: r.serviceType, conditionJson: r.conditionJson,
      targetProviderId: r.targetProviderId, priority: r.priority, enabled: r.enabled,
    });
    setModalOpen(true);
  };

  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      // normalize cleared Select to null so backend receives null (not undefined)
      const payload = { ...values, targetProviderId: values.targetProviderId ?? null };
      setSaving(true);
      if (editing) {
        await updateRoutingRule(editing.id, payload);
      } else {
        await createRoutingRule(payload);
      }
      message.success(t('saved'));
      setModalOpen(false);
      load();
    } catch {
      // stay open
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: number) => {
    await deleteRoutingRule(id).catch(() => {});
    load();
  };

  const handleToggle = async (id: number) => {
    await toggleRoutingRule(id).catch(() => {});
    load();
  };

  const typeLabel = (type: string) => {
    const map: Record<string, string> = {
      ONLINE_CONSULTATION: t('ONLINE_CONSULTATION'),
      OFFLINE_APPOINTMENT: t('OFFLINE_APPOINTMENT'),
      ONLINE_PHARMACY: t('ONLINE_PHARMACY'),
    };
    return map[type] || type;
  };

  const conditionLabel = (json: string) => {
    try {
      const obj = JSON.parse(json);
      if (!obj || Object.keys(obj).length === 0) return t('noTarget');
      if (obj.language) return `Lang: ${obj.language}`;
      return json;
    } catch {
      return json;
    }
  };

  // providers available as dispatch targets (non-MEDICAL_LLM)
  const dispatchableProviders = providers.filter(p => p.type !== 'MEDICAL_LLM');
  const providerOptions = dispatchableProviders.map(p => ({ value: p.id, label: `${p.name} (${typeLabel(p.type)})` }));

  const serviceTypeOptions = SERVICE_TYPES.map(st => ({ value: st, label: typeLabel(st) }));

  const columns = [
    { title: t('ruleName'), dataIndex: 'name', key: 'name' },
    {
      title: t('serviceType'), dataIndex: 'serviceType', key: 'serviceType',
      render: (type: string) => <Tag color="blue">{typeLabel(type)}</Tag>,
    },
    {
      title: t('conditionJson'), dataIndex: 'conditionJson', key: 'conditionJson',
      render: (json: string) => <code>{conditionLabel(json)}</code>,
    },
    {
      title: t('targetProvider'), key: 'targetProvider',
      render: (_: unknown, r: RoutingRuleDto) =>
        r.targetProviderName ? <Tag>{r.targetProviderName}</Tag> : <span style={{ color: '#999' }}>{t('noTarget')}</span>,
    },
    { title: t('priority'), dataIndex: 'priority', key: 'priority', sorter: (a: RoutingRuleDto, b: RoutingRuleDto) => a.priority - b.priority },
    {
      title: t('enabled'), dataIndex: 'enabled', key: 'enabled',
      render: (_: boolean, r: RoutingRuleDto) => (
        <Switch checked={r.enabled} onChange={() => handleToggle(r.id)} size="small" />
      ),
    },
    {
      title: '', key: 'actions',
      render: (_: unknown, r: RoutingRuleDto) => (
        <Space>
          <Button icon={<EditOutlined />} size="small" onClick={() => openEdit(r)} />
          <Popconfirm title={t('confirmDeleteRule')} onConfirm={() => handleDelete(r.id)} okText={t('confirm')} cancelText={t('cancel')}>
            <Button icon={<DeleteOutlined />} size="small" danger />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <Card title={
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Title level={5} style={{ margin: 0 }}>{t('routingRules')}</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={openAdd}>{t('addRule')}</Button>
      </div>
    }>
      <Table dataSource={rules} columns={columns} rowKey="id" loading={loading} size="middle" pagination={{ pageSize: 20 }} />

      <Modal
        title={editing ? t('editRule') : t('addRule')}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        confirmLoading={saving}
        width={600}
        destroyOnClose
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="name" label={t('ruleName')} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="serviceType" label={t('serviceType')} rules={[{ required: true }]}>
            <Select options={serviceTypeOptions} />
          </Form.Item>
          <Form.Item name="conditionJson" label={t('conditionJson')} rules={[{ required: true }]}
            help='Examples: {} for any, {"language":"ZH"} for Chinese users'>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="targetProviderId" label={t('targetProvider')}>
            <Select options={providerOptions} allowClear placeholder={t('noTarget')} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="priority" label={t('priority')}>
            <InputNumber min={0} max={1000} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="enabled" label={t('enabled')} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

export default RoutingRules;
