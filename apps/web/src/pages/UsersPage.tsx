import { IdcardOutlined, LockOutlined, PlusOutlined, ReloadOutlined, UnlockOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Form, Input, Modal, Select, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  createUser,
  listUsers,
  updateUserKycLevel,
  updateUserStatus,
  type CreateUserInput,
  type ListUsersParams,
  type User
} from '../api/users';

interface FilterValues {
  keyword?: string;
  status?: string;
  kycLevel?: number;
}

export function UsersPage() {
  const [filters, setFilters] = useState<ListUsersParams>({ page: 1, pageSize: 20 });
  const [createOpen, setCreateOpen] = useState(false);
  const [kycUser, setKycUser] = useState<User>();
  const [filterForm] = Form.useForm<FilterValues>();
  const [form] = Form.useForm<CreateUserInput>();
  const [kycForm] = Form.useForm<{ kycLevel: number }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const usersQuery = useQuery({
    queryKey: ['users', filters],
    queryFn: () => listUsers(filters)
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

  const kycMutation = useMutation({
    mutationFn: ({ id, kycLevel }: { id: number; kycLevel: number }) => updateUserKycLevel(id, kycLevel),
    onSuccess: async () => {
      message.success('KYC 等级已更新');
      setKycUser(undefined);
      kycForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['users'] });
    },
    onError: (error) => showRequestError(error, 'KYC 更新失败')
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
          <Button size="small" icon={<IdcardOutlined />} onClick={() => openKycModal(record)}>
            KYC
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

  function handleSearch(values: FilterValues) {
    setFilters({
      keyword: values.keyword?.trim() || undefined,
      status: values.status,
      kycLevel: values.kycLevel,
      page: 1,
      pageSize: filters.pageSize
    });
  }

  function handleReset() {
    filterForm.resetFields();
    setFilters({ page: 1, pageSize: filters.pageSize });
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

  function openKycModal(user: User) {
    setKycUser(user);
    kycForm.setFieldsValue({ kycLevel: user.kycLevel });
  }

  async function handleUpdateKycLevel() {
    if (!kycUser) {
      return;
    }
    const values = await kycForm.validateFields();
    kycMutation.mutate({ id: kycUser.id, kycLevel: values.kycLevel });
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

      <Space direction="vertical" className="full-width" size={16}>
        <Form form={filterForm} layout="inline" onFinish={handleSearch}>
          <Form.Item name="keyword">
            <Input allowClear placeholder="用户 ID / 用户名 / 邮箱 / 手机" />
          </Form.Item>
          <Form.Item name="status">
            <Select
              allowClear
              className="filter-select"
              placeholder="状态"
              options={[
                { value: 'ACTIVE', label: 'ACTIVE' },
                { value: 'FROZEN', label: 'FROZEN' }
              ]}
            />
          </Form.Item>
          <Form.Item name="kycLevel">
            <Select
              allowClear
              className="filter-select"
              placeholder="KYC"
              options={[
                { value: 0, label: 'L0' },
                { value: 1, label: 'L1' },
                { value: 2, label: 'L2' },
                { value: 3, label: 'L3' }
              ]}
            />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                查询
              </Button>
              <Button onClick={handleReset}>重置</Button>
            </Space>
          </Form.Item>
        </Form>

        <Table
          rowKey="id"
          columns={columns}
          dataSource={usersQuery.data?.items || []}
          loading={usersQuery.isLoading}
          pagination={{
            current: usersQuery.data?.page || filters.page || 1,
            pageSize: usersQuery.data?.pageSize || filters.pageSize || 20,
            total: usersQuery.data?.total || 0,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 个用户`,
            onChange: (page, pageSize) => setFilters((current) => ({ ...current, page, pageSize }))
          }}
          scroll={{ x: 1100 }}
        />
      </Space>

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

      <Modal
        title="调整 KYC 等级"
        open={Boolean(kycUser)}
        okText="保存"
        confirmLoading={kycMutation.isPending}
        onOk={handleUpdateKycLevel}
        onCancel={() => setKycUser(undefined)}
      >
        <Form form={kycForm} layout="vertical">
          <Form.Item label="用户">{kycUser?.username}</Form.Item>
          <Form.Item label="KYC 等级" name="kycLevel" rules={[{ required: true, message: '请选择 KYC 等级' }]}>
            <Select
              options={[
                { value: 0, label: 'L0 未认证' },
                { value: 1, label: 'L1 基础认证' },
                { value: 2, label: 'L2 高级认证' },
                { value: 3, label: 'L3 机构/增强认证' }
              ]}
            />
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
