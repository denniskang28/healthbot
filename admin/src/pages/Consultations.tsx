import React, { useEffect, useState } from 'react';
import { Table, Tag, Card, Select, Space, Typography, Descriptions } from 'antd';
import dayjs from 'dayjs';
import { getConsultations } from '../api/client';
import type { ConsultationEntry, MedicineDto } from '../api/types';
import { useLang } from '../context/LanguageContext';

const { Title } = Typography;

const Consultations: React.FC = () => {
  const [data, setData] = useState<ConsultationEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [typeFilter, setTypeFilter] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const { t } = useLang();

  useEffect(() => {
    getConsultations().then(d => { setData(d); setLoading(false); }).catch(() => setLoading(false));
  }, []);

  const filtered = data.filter(d =>
    (!typeFilter || d.consultation.type === typeFilter) &&
    (!statusFilter || d.consultation.status === statusFilter)
  );

  const columns = [
    { title: 'ID', dataIndex: ['consultation', 'id'], key: 'id', width: 60 },
    {
      title: t('userName'), key: 'user',
      render: (_: unknown, r: ConsultationEntry) => r.user?.name || '-',
    },
    {
      title: t('type'), key: 'type',
      render: (_: unknown, r: ConsultationEntry) => (
        <Tag color={r.consultation.type === 'AI_CONSULTATION' ? 'purple' : 'green'}>
          {r.consultation.type === 'AI_CONSULTATION' ? t('AI_CONSULTATION_label') : t('DOCTOR_CONSULTATION_label')}
        </Tag>
      ),
    },
    {
      title: t('status'), key: 'status',
      render: (_: unknown, r: ConsultationEntry) => {
        const colors: Record<string, string> = { ACTIVE: 'blue', COMPLETED: 'green', CANCELLED: 'red' };
        return <Tag color={colors[r.consultation.status]}>{t(r.consultation.status as Parameters<typeof t>[0])}</Tag>;
      },
    },
    {
      title: 'Doctor', key: 'doctor',
      render: (_: unknown, r: ConsultationEntry) => r.doctor?.name || (r.consultation.type === 'AI_CONSULTATION' ? 'AI' : '-'),
    },
    {
      title: t('startTime'), key: 'startTime',
      render: (_: unknown, r: ConsultationEntry) =>
        dayjs(r.consultation.startTime).format('MM/DD HH:mm'),
    },
    {
      title: t('prescription'), key: 'prescription',
      render: (_: unknown, r: ConsultationEntry) =>
        r.prescription ? `${r.prescription.medicines.length} items` : '-',
    },
  ];

  return (
    <Card title={<Title level={5} style={{ margin: 0 }}>{t('consultations')}</Title>}>
      <Space style={{ marginBottom: 16 }}>
        <Select placeholder={t('type')} style={{ width: 180 }} allowClear
          onChange={v => setTypeFilter(v || '')}
          options={[
            { value: 'AI_CONSULTATION', label: t('AI_CONSULTATION') },
            { value: 'DOCTOR_CONSULTATION', label: t('DOCTOR_CONSULTATION') },
          ]} />
        <Select placeholder={t('status')} style={{ width: 160 }} allowClear
          onChange={v => setStatusFilter(v || '')}
          options={[
            { value: 'ACTIVE', label: t('ACTIVE') },
            { value: 'COMPLETED', label: t('COMPLETED') },
            { value: 'CANCELLED', label: t('CANCELLED') },
          ]} />
      </Space>
      <Table
        dataSource={filtered}
        columns={columns}
        rowKey={r => r.consultation.id}
        loading={loading}
        pagination={{ pageSize: 10 }}
        expandable={{
          expandedRowRender: (r: ConsultationEntry) => r.prescription ? (
            <Descriptions size="small" bordered column={2}>
              {r.prescription.medicines.map((m: MedicineDto, i: number) => (
                <Descriptions.Item key={i} label={m.name} span={2}>
                  {m.dosage} — {m.frequency} — {m.days} days
                </Descriptions.Item>
              ))}
            </Descriptions>
          ) : <span>No prescription</span>,
          rowExpandable: (r: ConsultationEntry) => !!r.prescription,
        }}
      />
    </Card>
  );
};

export default Consultations;
