import React, { useEffect, useRef, useState } from 'react';
import { Avatar, Button, Card, Empty, List, Popconfirm, Spin, Typography, message } from 'antd';
import { DeleteOutlined, RobotOutlined, UserOutlined } from '@ant-design/icons';
import { deleteChatHistory, getChatHistory, getUsers } from '../api/client';
import type { ChatMessageDto, UserDto } from '../api/types';
import { useLang } from '../context/LanguageContext';

const { Text } = Typography;

const ChatHistory: React.FC = () => {
  const [users, setUsers] = useState<UserDto[]>([]);
  const [selectedUser, setSelectedUser] = useState<UserDto | null>(null);
  const [msgs, setMsgs] = useState<ChatMessageDto[]>([]);
  const [loadingUsers, setLoadingUsers] = useState(true);
  const [loadingMsgs, setLoadingMsgs] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);
  const { t } = useLang();

  useEffect(() => {
    getUsers()
      .then(setUsers)
      .catch(() => {})
      .finally(() => setLoadingUsers(false));
  }, []);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [msgs]);

  const loadHistory = (user: UserDto) => {
    setSelectedUser(user);
    setLoadingMsgs(true);
    getChatHistory(user.id)
      .then(setMsgs)
      .catch(() => {})
      .finally(() => setLoadingMsgs(false));
  };

  const handleDelete = async () => {
    if (!selectedUser) return;
    setDeleting(true);
    try {
      await deleteChatHistory(selectedUser.id);
      setMsgs([]);
      message.success(t('historyCleared'));
    } catch {
      message.error('Failed to clear history');
    } finally {
      setDeleting(false);
    }
  };

  return (
    <Card
      title={<Typography.Title level={5} style={{ margin: 0 }}>{t('chatHistory')}</Typography.Title>}
      styles={{ body: { padding: 0 } }}
    >
      <div style={{ display: 'flex', height: 640 }}>

        {/* User list */}
        <div style={{ width: 220, borderRight: '1px solid #f0f0f0', overflowY: 'auto', flexShrink: 0 }}>
          <List
            loading={loadingUsers}
            dataSource={users}
            renderItem={u => (
              <List.Item
                onClick={() => loadHistory(u)}
                style={{
                  padding: '12px 16px',
                  cursor: 'pointer',
                  background: selectedUser?.id === u.id ? '#e6f4ff' : undefined,
                  borderLeft: `3px solid ${selectedUser?.id === u.id ? '#1677ff' : 'transparent'}`,
                }}
              >
                <List.Item.Meta
                  avatar={<Avatar icon={<UserOutlined />} size="small" />}
                  title={<Text strong style={{ fontSize: 13 }}>{u.name}</Text>}
                  description={<Text type="secondary" style={{ fontSize: 11 }}>{u.phone}</Text>}
                />
              </List.Item>
            )}
          />
        </div>

        {/* Message panel */}
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0 }}>
          {selectedUser ? (
            <>
              {/* Toolbar */}
              <div style={{ padding: '10px 16px', borderBottom: '1px solid #f0f0f0', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexShrink: 0 }}>
                <Text strong>{selectedUser.name}</Text>
                <Popconfirm
                  title={t('confirmClearHistory')}
                  onConfirm={handleDelete}
                  okText={t('confirm')}
                  cancelText={t('cancel')}
                  disabled={msgs.length === 0}
                >
                  <Button danger icon={<DeleteOutlined />} size="small" loading={deleting} disabled={msgs.length === 0}>
                    {t('clearHistory')}
                  </Button>
                </Popconfirm>
              </div>

              {/* Bubbles */}
              <div style={{ flex: 1, overflowY: 'auto', padding: 16 }}>
                {loadingMsgs
                  ? <Spin style={{ display: 'block', margin: '60px auto' }} />
                  : msgs.length === 0
                    ? <Empty description={t('noHistory')} style={{ marginTop: 80 }} />
                    : msgs.map(msg => {
                        const isUser = msg.role === 'USER';
                        return (
                          <div key={msg.id} style={{ display: 'flex', justifyContent: isUser ? 'flex-end' : 'flex-start', marginBottom: 12, alignItems: 'flex-start' }}>
                            {!isUser && (
                              <Avatar icon={<RobotOutlined />} size="small"
                                style={{ background: '#722ed1', marginRight: 8, flexShrink: 0, marginTop: 2 }} />
                            )}
                            <div style={{
                              maxWidth: '70%',
                              background: isUser ? '#1677ff' : '#f5f5f5',
                              color: isUser ? '#fff' : '#000',
                              borderRadius: isUser ? '12px 12px 2px 12px' : '12px 12px 12px 2px',
                              padding: '8px 12px',
                              fontSize: 13,
                              lineHeight: 1.6,
                              wordBreak: 'break-word',
                            }}>
                              <div style={{ whiteSpace: 'pre-wrap' }}>{msg.content}</div>
                              <div style={{ fontSize: 10, opacity: 0.6, marginTop: 4, textAlign: isUser ? 'right' : 'left' }}>
                                {new Date(msg.timestamp).toLocaleTimeString()}
                              </div>
                            </div>
                            {isUser && (
                              <Avatar icon={<UserOutlined />} size="small"
                                style={{ background: '#1677ff', marginLeft: 8, flexShrink: 0, marginTop: 2 }} />
                            )}
                          </div>
                        );
                      })
                }
                <div ref={bottomRef} />
              </div>
            </>
          ) : (
            <Empty description={t('selectUser')} style={{ margin: 'auto' }} />
          )}
        </div>
      </div>
    </Card>
  );
};

export default ChatHistory;
