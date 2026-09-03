import { ReloadOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { Button, Form, Input, Select, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useState } from 'react';
import { listChains, listTokens } from '../api/assets';
import { listWithdrawalRecords } from '../api/withdrawalRecords';
import type { AdminWithdrawalRecord, ListWithdrawalRecordsParams } from '../api/withdrawalRecords';

interface FilterValues {
  keyword?: string;
  chainId?: number;
  tokenId?: number;
  status?: string;
}

export function WithdrawalRecordsPage() {
  const [filters, setFilters] = useState<ListWithdrawalRecordsParams>({ page: 1, pageSize: 20 });
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
  const recordsQuery = useQuery({
    queryKey: ['withdrawal-records', filters],
    queryFn: () => listWithdrawalRecords(filters),
    refetchInterval: 10000
  });

  const tokenOptions = (tokensQuery.data || [])
    .filter((token) => !selectedChainId || token.chainId === selectedChainId)
    .map((token) => ({ value: token.id, label: `${token.symbol} / ${token.chainName}` }));

  const columns: ColumnsType<AdminWithdrawalRecord> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '用户 ID', dataIndex: 'userId', width: 100 },
    { title: '用户名', dataIndex: 'username', width: 140 },
    { title: '链', dataIndex: 'chainName', width: 130 },
    { title: 'Token', dataIndex: 'symbol', width: 100 },
    { title: '数量', dataIndex: 'displayAmount', width: 130 },
    { title: '手续费', dataIndex: 'displayFee', width: 120 },
    { title: '目标地址', dataIndex: 'toAddress', ellipsis: true },
    { title: '交易 Hash', dataIndex: 'txHash', ellipsis: true, render: (value) => value || '-' },
    {
      title: '状态',
      dataIndex: 'status',
      width: 150,
      render: (value) => <Tag color={statusColor(value)}>{value}</Tag>
    },
    { title: '拒绝原因', dataIndex: 'rejectReason', ellipsis: true, render: (value) => value || '-' },
    {
      title: '申请时间',
      dataIndex: 'requestedAt',
      width: 200,
      render: (value) => new Date(value).toLocaleString()
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
          <Typography.Title level={3}>提现记录</Typography.Title>
          <Typography.Text type="secondary">全局查询提现申请、审核、广播和确认记录。</Typography.Text>
        </div>
        <Space>
          <Typography.Text type="secondary">每 10 秒自动刷新</Typography.Text>
          <Button icon={<ReloadOutlined />} onClick={() => recordsQuery.refetch()}>
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
                { value: 'PENDING_APPROVAL', label: 'PENDING_APPROVAL' },
                { value: 'APPROVED', label: 'APPROVED' },
                { value: 'BROADCASTED', label: 'BROADCASTED' },
                { value: 'CONFIRMED', label: 'CONFIRMED' },
                { value: 'REJECTED', label: 'REJECTED' },
                { value: 'FAILED', label: 'FAILED' }
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
          dataSource={recordsQuery.data?.items || []}
          loading={recordsQuery.isLoading}
          pagination={{
            current: recordsQuery.data?.page || filters.page || 1,
            pageSize: recordsQuery.data?.pageSize || filters.pageSize || 20,
            total: recordsQuery.data?.total || 0,
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

function statusColor(status: string) {
  if (status === 'PENDING_APPROVAL') {
    return 'gold';
  }
  if (status === 'APPROVED') {
    return 'blue';
  }
  if (status === 'BROADCASTED') {
    return 'processing';
  }
  if (status === 'CONFIRMED') {
    return 'green';
  }
  if (status === 'REJECTED') {
    return 'red';
  }
  if (status === 'FAILED') {
    return 'red';
  }
  return 'default';
}
