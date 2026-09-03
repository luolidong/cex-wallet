import { ReloadOutlined, StopOutlined, CheckCircleOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Form, Input, Select, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useState } from 'react';
import { listChains } from '../api/assets';
import { disableWallet, enableWallet, listWallets } from '../api/wallets';
import type { AdminWallet, ListWalletsParams } from '../api/wallets';
import { getStoredUser } from '../auth/session';

interface FilterValues {
  keyword?: string;
  chainId?: number;
  status?: string;
}

export function WalletManagementPage() {
  const [filters, setFilters] = useState<ListWalletsParams>({ page: 1, pageSize: 20 });
  const [form] = Form.useForm<FilterValues>();
  const queryClient = useQueryClient();
  const canManageWallet = Boolean(getStoredUser()?.permissions.includes('wallet:manage'));

  const chainsQuery = useQuery({
    queryKey: ['assets', 'chains'],
    queryFn: listChains
  });
  const walletsQuery = useQuery({
    queryKey: ['wallets', filters],
    queryFn: () => listWallets(filters)
  });

  const enableMutation = useMutation({
    mutationFn: enableWallet,
    onSuccess: async () => {
      message.success('充值地址已启用');
      await queryClient.invalidateQueries({ queryKey: ['wallets'] });
      await queryClient.invalidateQueries({ queryKey: ['audit-logs'] });
    },
    onError: (error) => showRequestError(error, '启用充值地址失败')
  });

  const disableMutation = useMutation({
    mutationFn: disableWallet,
    onSuccess: async () => {
      message.success('充值地址已停用');
      await queryClient.invalidateQueries({ queryKey: ['wallets'] });
      await queryClient.invalidateQueries({ queryKey: ['audit-logs'] });
    },
    onError: (error) => showRequestError(error, '停用充值地址失败')
  });

  const columns: ColumnsType<AdminWallet> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '用户 ID', dataIndex: 'userId', width: 100 },
    { title: '用户名', dataIndex: 'username', width: 140 },
    { title: '链', dataIndex: 'chainName', width: 140 },
    { title: '地址', dataIndex: 'address', ellipsis: true },
    {
      title: '类型',
      dataIndex: 'addressType',
      width: 120,
      render: (value) => <Tag>{value}</Tag>
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 110,
      render: (value) => <Tag color={value === 'ACTIVE' ? 'green' : 'default'}>{value}</Tag>
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 200,
      render: (value) => new Date(value).toLocaleString()
    },
    {
      title: '操作',
      width: 120,
      fixed: 'right',
      render: (_, record) => {
        if (!canManageWallet) {
          return '-';
        }
        return record.status === 'ACTIVE' ? (
          <Button
            danger
            size="small"
            icon={<StopOutlined />}
            loading={disableMutation.isPending}
            onClick={() => disableMutation.mutate(record.id)}
          >
            停用
          </Button>
        ) : (
          <Button
            size="small"
            icon={<CheckCircleOutlined />}
            loading={enableMutation.isPending}
            onClick={() => enableMutation.mutate(record.id)}
          >
            启用
          </Button>
        );
      }
    }
  ];

  function handleSearch(values: FilterValues) {
    setFilters({
      keyword: values.keyword?.trim() || undefined,
      chainId: values.chainId,
      status: values.status,
      page: 1,
      pageSize: filters.pageSize
    });
  }

  function handleReset() {
    form.resetFields();
    setFilters({ page: 1, pageSize: filters.pageSize });
  }

  return (
    <>
      <div className="page-toolbar">
        <div>
          <Typography.Title level={3}>地址管理</Typography.Title>
          <Typography.Text type="secondary">查询用户充值地址，处理异常地址启停。</Typography.Text>
        </div>
        <Button icon={<ReloadOutlined />} onClick={() => walletsQuery.refetch()}>
          刷新
        </Button>
      </div>

      <Space direction="vertical" className="full-width" size={16}>
        <Form form={form} layout="inline" onFinish={handleSearch}>
          <Form.Item name="keyword">
            <Input allowClear placeholder="地址 / 用户名 / 用户 ID" />
          </Form.Item>
          <Form.Item name="chainId">
            <Select
              allowClear
              className="filter-select"
              placeholder="链"
              options={(chainsQuery.data || []).map((chain) => ({ value: chain.id, label: chain.name }))}
            />
          </Form.Item>
          <Form.Item name="status">
            <Select
              allowClear
              className="filter-select"
              placeholder="状态"
              options={[
                { value: 'ACTIVE', label: 'ACTIVE' },
                { value: 'INACTIVE', label: 'INACTIVE' }
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
          dataSource={walletsQuery.data?.items || []}
          loading={walletsQuery.isLoading}
          pagination={{
            current: walletsQuery.data?.page || filters.page || 1,
            pageSize: walletsQuery.data?.pageSize || filters.pageSize || 20,
            total: walletsQuery.data?.total || 0,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => setFilters((current) => ({ ...current, page, pageSize }))
          }}
          scroll={{ x: 1200 }}
        />
      </Space>
    </>
  );
}

function showRequestError(error: unknown, fallback: string) {
  const err = error as { response?: { data?: { error?: { code?: string; message?: string; details?: string } } }; message?: string };
  const apiError = err.response?.data?.error;
  const messageText = apiError?.details || apiError?.message || err.message || fallback;
  message.error(apiError?.code ? `${apiError.code}: ${messageText}` : messageText);
}
