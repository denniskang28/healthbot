import React, { useEffect, useState, useRef } from 'react';
import { Card, Table, Tag, Row, Col, Statistic, Badge, Typography, Space } from 'antd';
import { UserOutlined, RocketOutlined, MedicineBoxOutlined, ShoppingCartOutlined } from '@ant-design/icons';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import dayjs from 'dayjs';
import { getActiveUsers } from '../api/client';
import type { ActiveUserEntry, UserSessionDto } from '../api/types';
import { useLang } from '../context/LanguageContext';

const { Title } = Typography;

const STATE_COLORS: Record<string, string> = {
  IDLE: 'default', CHATTING: 'blue', AI_CONSULTATION: 'purple',
  DOCTOR_CONSULTATION: 'green', APPOINTMENT: 'orange', PHARMACY: 'red',
};

const Dashboard: React.FC = () => {
  const [users, setUsers] = useState<ActiveUserEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const stompRef = useRef<Client | null>(null);
  const { t } = useLang();

  const fetchUsers = async () => {
    try {
      const data = await getActiveUsers();
      setUsers(data);
    } catch {
      // backend may not be running yet
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
    const interval = setInterval(fetchUsers, 10000);

    const socket = new SockJS('/ws');
    const client = new Client({
      webSocketFactory: () => socket as WebSocket,
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe('/topic/user-status', (msg) => {
          const updated: UserSessionDto = JSON.parse(msg.body);
          setUsers(prev => prev.map(u =>
            u.session.userId === updated.userId
              ? { ...u, session: updated }
              : u
          ));
        });
      },
    });
    client.activate();
    stompRef.current = client;

    return () => {
      clearInterval(interval);
      client.deactivate();
    };
  }, []);

  const activeSessions = users.filter(u => u.session.currentState !== 'IDLE').length;
  const today = dayjs().format('YYYY-MM-DD');
  const todayConsultations = users.filter(u => u.latestConsultation &&
    dayjs(u.latestConsultation.startTime).format('YYYY-MM-DD') === today).length;
  const todayPurchases = users.filter(u => u.latestPurchase &&
    dayjs(u.latestPurchase.createdAt).format('YYYY-MM-DD') === today).length;

  const columns = [
    { title: t('userName'), dataIndex: ['session', 'userName'], key: 'name' },
    {
      title: t('phone'),
      render: (_: unknown, r: ActiveUserEntry) => r.user?.phone,
      key: 'phone',
    },
    {
      title: t('state'), key: 'state',
      render: (_: unknown, r: ActiveUserEntry) => (
        <Badge status={r.session.currentState === 'IDLE' ? 'default' : 'processing'}>
          <Tag color={STATE_COLORS[r.session.currentState] || 'default'}>
            {t(r.session.currentState as Parameters<typeof t>[0])}
          </Tag>
        </Badge>
      ),
    },
    {
      title: t('lastActive'), key: 'lastActive',
      render: (_: unknown, r: ActiveUserEntry) =>
        r.session.lastActive ? dayjs(r.session.lastActive).format('HH:mm:ss') : '-',
    },
    {
      title: t('prescription'), key: 'prescription',
      render: (_: unknown, r: ActiveUserEntry) =>
        r.latestPrescription ? `${r.latestPrescription.medicines.length} items` : '-',
    },
  ];

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="large">
      <Row gutter={16}>
        <Col span={6}>
          <Card>
            <Statistic title={t('totalUsers')} value={users.length}
              prefix={<UserOutlined />} valueStyle={{ color: '#1890ff' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title={t('activeSessions')} value={activeSessions}
              prefix={<RocketOutlined />} valueStyle={{ color: '#52c41a' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title={t('todayConsultations')} value={todayConsultations}
              prefix={<MedicineBoxOutlined />} valueStyle={{ color: '#722ed1' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title={t('todayPurchases')} value={todayPurchases}
              prefix={<ShoppingCartOutlined />} valueStyle={{ color: '#fa8c16' }} />
          </Card>
        </Col>
      </Row>

      <Card title={<Title level={5} style={{ margin: 0 }}>{t('userStatus')}</Title>}>
        <Table
          dataSource={users}
          columns={columns}
          rowKey={r => r.user?.id?.toString() || r.session.userId.toString()}
          loading={loading}
          pagination={{ pageSize: 10 }}
          size="middle"
        />
      </Card>
    </Space>
  );
};

export default Dashboard;
