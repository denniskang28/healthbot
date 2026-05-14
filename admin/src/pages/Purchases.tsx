import React, { useEffect, useState } from 'react';
import { Table, Tag, Card, Typography, Descriptions } from 'antd';
import dayjs from 'dayjs';
import { getPurchases } from '../api/client';
import type { PurchaseEntry, MedicineDto } from '../api/types';
import { useLang } from '../context/LanguageContext';

const { Title } = Typography;

const Purchases: React.FC = () => {
  const [data, setData] = useState<PurchaseEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const { t } = useLang();

  useEffect(() => {
    getPurchases().then(d => { setData(d); setLoading(false); }).catch(() => setLoading(false));
  }, []);

  const columns = [
    { title: 'ID', dataIndex: ['purchase', 'id'], key: 'id', width: 60 },
    {
      title: t('userName'), key: 'user',
      render: (_: unknown, r: PurchaseEntry) => r.user?.name || '-',
    },
    {
      title: 'Rx ID', key: 'rxId',
      render: (_: unknown, r: PurchaseEntry) => `#${r.purchase.prescriptionId}`,
    },
    {
      title: t('status'), key: 'status',
      render: (_: unknown, r: PurchaseEntry) => (
        <Tag color={r.purchase.status === 'COMPLETED' ? 'green' : 'orange'}>
          {t(r.purchase.status as Parameters<typeof t>[0])}
        </Tag>
      ),
    },
    {
      title: t('amount'), key: 'amount',
      render: (_: unknown, r: PurchaseEntry) => `$${r.purchase.totalAmount?.toFixed(2) || '0.00'}`,
    },
    {
      title: t('medicines'), key: 'medicines',
      render: (_: unknown, r: PurchaseEntry) =>
        r.prescription ? `${r.prescription.medicines.length} items` : '-',
    },
    {
      title: t('purchasedAt'), key: 'purchasedAt',
      render: (_: unknown, r: PurchaseEntry) =>
        r.purchase.purchasedAt ? dayjs(r.purchase.purchasedAt).format('MM/DD HH:mm') : '-',
    },
  ];

  return (
    <Card title={<Title level={5} style={{ margin: 0 }}>{t('purchases')}</Title>}>
      <Table
        dataSource={data}
        columns={columns}
        rowKey={r => r.purchase.id}
        loading={loading}
        pagination={{ pageSize: 10 }}
        expandable={{
          expandedRowRender: (r: PurchaseEntry) => r.prescription ? (
            <Descriptions size="small" bordered column={2}>
              {r.prescription.medicines.map((m: MedicineDto, i: number) => (
                <Descriptions.Item key={i} label={m.name} span={2}>
                  {m.dosage} — {m.frequency} — {m.days} days
                </Descriptions.Item>
              ))}
            </Descriptions>
          ) : <span>No prescription data</span>,
          rowExpandable: (r: PurchaseEntry) => !!r.prescription,
        }}
      />
    </Card>
  );
};

export default Purchases;
