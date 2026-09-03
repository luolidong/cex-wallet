import { LockOutlined, PlusOutlined, ReloadOutlined, UnlockOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Form, Input, Modal, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createUser, listUsers, updateUserStatus, type CreateUserInput, type User } from '../api/users';

export function UsersPage() {
  const [createOpen, setCreateOpen] = useState(false);
  const [form] = Form.useForm<CreateUserInput>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const usersQuery = useQuery({
    queryKey: ['users'],
    queryFn: listUsers
  });

  const createMutation = useMutation({
    mutationFn: createUser,
    onSuccess: async (user) => {
      message.success('用户已创建');
      setCreateOpen(false);
      form.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['users'] });
      navigate(`/users/${user.id}`);
    },
    onError: (error) => showRequestError(error, '创建失败')
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) => updateUserStatus(id, status),
    onSuccess: async (user) => {
      message.success(user.status === 'ACTIVE' ? '用户已恢复' : '用户已冻结');
      await queryClient.invalidateQueries({ queryKey: ['users'] });
    },
    onError: (error) => showRequestError(error, '状态更新失败')
  });

  const columns: ColumnsType<User> = [
    {
      title: '用户 ID',
      dataIndex: 'id',
      width: 100
    },
    {
      title: '用户名',
      dataIndex: 'username'
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      render: (value?: string) => value || '-'
    },
    {
      title: '手机',
      dataIndex: 'phone',
      render: (value?: string) => value || '-'
    },
    {
      title: 'KYC',
      dataIndex: 'kycLevel',
      width: 90,
      render: (value: number) => `L${value}`
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (value: string) => <Tag color={value === 'ACTIVE' ? 'green' : 'red'}>{value}</Tag>
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 210,
      render: (value: string) => new Date(value).toLocaleString()
    },
    {
      title: '操作',
      width: 190,
      render: (_, record) => (
        <Space>
          <Button type="link" onClick={() => navigate(`/users/${record.id}`)}>
            详情
          </Button>
          {record.status === 'ACTIVE' ? (
            <Button
              size="small"
              danger
              icon={<LockOutlined />}
              loading={statusMutation.isPending}
              onClick={() => confirmStatusChange(record, 'FROZEN')}
            >
              冻结
            </Button>
          ) : (
            <Button
              size="small"
              icon={<UnlockOutlined />}
              loading={statusMutation.isPending}
              onClick={() => confirmStatusChange(record, 'ACTIVE')}
            >
              恢复
            </Button>
          )}
        </Space>
      )
    }
  ];

  async function handleCreate() {
    const values = await form.validateFields();
    createMutation.mutate(values);
  }

  function confirmStatusChange(user: User, status: string) {
    const freeze = status === 'FROZEN';
    Modal.confirm({
      title: freeze ? '冻结用户' : '恢复用户',
      content: freeze
        ? `冻结 ${user.username} 后，该用户不能生成充值地址，也不能申请提现。`
        : `恢复 ${user.username} 后，该用户可以继续生成充值地址和申请提现。`,
      okText: freeze ? '确认冻结' : '确认恢复',
      okButtonProps: { danger: freeze },
      onOk: () => statusMutation.mutateAsync({ id: user.id, status })
    });
  }

  return (
    <>
      <div className="page-toolbar">
        <div>
          <Typography.Title level={3}>用户管理</Typography.Title>
          <Typography.Text type="secondary">查看用户资料、资产余额和钱包业务状态。</Typography.Text>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={() => usersQuery.refetch()}>
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
            创建用户
          </Button>
        </Space>
      </div>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={usersQuery.data || []}
        loading={usersQuery.isLoading}
        pagination={{ pageSize: 20 }}
      />

      <Modal
        title="创建用户"
        open={createOpen}
        okText="创建"
        confirmLoading={createMutation.isPending}
        onOk={handleCreate}
        onCancel={() => setCreateOpen(false)}
      >
        <Form form={form} layout="vertical">
          <Form.Item label="用户名" name="username" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input />
          </Form.Item>
          <Form.Item label="邮箱" name="email" rules={[{ type: 'email', message: '邮箱格式不正确' }]}>
            <Input />
          </Form.Item>
          <Form.Item label="手机" name="phone">
            <Input />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}

function showRequestError(error: unknown, fallback: string) {
  if (typeof error === 'object' && error && 'response' in error) {
    const response = (error as { response?: { data?: { error?: { message?: string }; message?: string } } }).response;
    const apiMessage = response?.data?.error?.message || response?.data?.message;
    if (apiMessage) {
      message.error(`${fallback}：${apiMessage}`);
      return;
    }
  }
  if (error instanceof Error && error.message) {
    message.error(`${fallback}：${error.message}`);
    return;
  }
  message.error(fallback);
}
