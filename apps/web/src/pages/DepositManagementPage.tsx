import { ReloadOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { Button, Form, Input, Select, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useState } from 'react';
import { listChains, listTokens } from '../api/assets';
import { listDeposits } from '../api/deposits';
import type { ListDepositsParams } from '../api/deposits';
import type { Deposit } from '../api/users';

interface FilterValues {
  keyword?: string;
  chainId?: number;
  tokenId?: number;
  status?: string;
}

export function DepositManagementPage() {
  const [filters, setFilters] = useState<ListDepositsParams>({ page: 1, pageSize: 20 });
  const [form] = Form.useForm<FilterValues>();
  const selectedChainId = Form.useWatch('chainId', form);

  const chainsQuery = useQuery({
    queryKey: ['assets', 'chains'],
    queryFn: listChains
  });
  const tokensQuery = useQuery({
    queryKey: ['assets', 'tokens'],
    queryFn: listTokens
  });
  const depositsQuery = useQuery({
    queryKey: ['deposits', filters],
    queryFn: () => listDeposits(filters),
    refetchInterval: 10000
  });

  const tokenOptions = (tokensQuery.data || [])
    .filter((token) => !selectedChainId || token.chainId === selectedChainId)
    .map((token) => ({ value: token.id, label: `${token.symbol} / ${token.chainName}` }));

  const columns: ColumnsType<Deposit> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '用户 ID', dataIndex: 'userId', width: 100 },
    { title: '链', dataIndex: 'chainName', width: 130 },
    { title: 'Token', dataIndex: 'symbol', width: 100 },
    { title: '数量', dataIndex: 'displayAmount', width: 140 },
    { title: '交易 Hash', dataIndex: 'txHash', ellipsis: true },
    { title: 'From', dataIndex: 'fromAddress', ellipsis: true, render: (value) => value || '-' },
    { title: 'To', dataIndex: 'toAddress', ellipsis: true },
    {
      title: '状态',
      dataIndex: 'status',
      width: 120,
      render: (value) => <Tag color={value === 'CONFIRMED' ? 'green' : 'processing'}>{value}</Tag>
    },
    { title: '区块', dataIndex: 'blockNumber', width: 110, render: (value) => value || '-' },
    { title: '确认数', dataIndex: 'confirmationCount', width: 100 },
    {
      title: '发现时间',
      dataIndex: 'detectedAt',
      width: 200,
      render: (value) => new Date(value).toLocaleString()
    },
    {
      title: '确认时间',
      dataIndex: 'confirmedAt',
      width: 200,
      render: (value) => (value ? new Date(value).toLocaleString() : '-')
    }
  ];

  function handleSearch(values: FilterValues) {
    setFilters({
      keyword: values.keyword?.trim() || undefined,
      chainId: values.chainId,
      tokenId: values.tokenId,
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
          <Typography.Title level={3}>充值记录</Typography.Title>
          <Typography.Text type="secondary">全局查询链上充值入账记录，支持按交易、地址和用户排查。</Typography.Text>
        </div>
        <Space>
          <Typography.Text type="secondary">每 10 秒自动刷新</Typography.Text>
          <Button icon={<ReloadOutlined />} onClick={() => depositsQuery.refetch()}>
            刷新
          </Button>
        </Space>
      </div>

      <Space direction="vertical" className="full-width" size={16}>
        <Form form={form} layout="inline" onFinish={handleSearch}>
          <Form.Item name="keyword">
            <Input allowClear placeholder="txHash / 地址 / 用户名 / 用户 ID" />
          </Form.Item>
          <Form.Item name="chainId">
            <Select
              allowClear
              className="filter-select"
              placeholder="链"
              options={(chainsQuery.data || []).map((chain) => ({ value: chain.id, label: chain.name }))}
              onChange={() => form.setFieldValue('tokenId', undefined)}
            />
          </Form.Item>
          <Form.Item name="tokenId">
            <Select allowClear className="filter-select" placeholder="Token" options={tokenOptions} />
          </Form.Item>
          <Form.Item name="status">
            <Select
              allowClear
              className="filter-select"
              placeholder="状态"
              options={[
                { value: 'DETECTED', label: 'DETECTED' },
                { value: 'CONFIRMED', label: 'CONFIRMED' }
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
          dataSource={depositsQuery.data?.items || []}
          loading={depositsQuery.isLoading}
          pagination={{
            current: depositsQuery.data?.page || filters.page || 1,
            pageSize: depositsQuery.data?.pageSize || filters.pageSize || 20,
            total: depositsQuery.data?.total || 0,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => setFilters((current) => ({ ...current, page, pageSize }))
          }}
          scroll={{ x: 1700 }}
        />
      </Space>
    </>
  );
}
