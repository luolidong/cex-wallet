import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Form, Input, Modal, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createUser, listUsers, type CreateUserInput, type User } from '../api/users';

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
    }
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
      render: (value: string) => <Tag color={value === 'ACTIVE' ? 'green' : 'default'}>{value}</Tag>
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 210,
      render: (value: string) => new Date(value).toLocaleString()
    },
    {
      title: '操作',
      width: 100,
      render: (_, record) => (
        <Button type="link" onClick={() => navigate(`/users/${record.id}`)}>
          详情
        </Button>
      )
    }
  ];

  async function handleCreate() {
    const values = await form.validateFields();
    createMutation.mutate(values);
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

