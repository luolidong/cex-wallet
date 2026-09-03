import { EyeOutlined, ReloadOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { Button, Descriptions, Drawer, Form, Input, Select, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useState } from 'react';
import { listLedgerEntries, listLedgerJournals } from '../api/ledger';
import type { LedgerEntry, LedgerJournal, ListLedgerJournalsParams } from '../api/ledger';

interface FilterValues {
  keyword?: string;
  businessType?: string;
  status?: string;
}

export function LedgerJournalsPage() {
  const [filters, setFilters] = useState<ListLedgerJournalsParams>({ page: 1, pageSize: 20 });
  const [selectedJournal, setSelectedJournal] = useState<LedgerJournal>();
  const [form] = Form.useForm<FilterValues>();

  const journalsQuery = useQuery({
    queryKey: ['ledger-journals', filters],
    queryFn: () => listLedgerJournals(filters)
  });
  const entriesQuery = useQuery({
    queryKey: ['ledger-entries', selectedJournal?.id],
    queryFn: () => listLedgerEntries(selectedJournal!.id),
    enabled: Boolean(selectedJournal)
  });

  const journalColumns: ColumnsType<LedgerJournal> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '流水号', dataIndex: 'journalNo', width: 230 },
    { title: '业务类型', dataIndex: 'businessType', width: 180, render: (value) => <Tag color="blue">{value}</Tag> },
    { title: '业务 ID', dataIndex: 'businessId', width: 180 },
    { title: '幂等键', dataIndex: 'idempotencyKey', ellipsis: true },
    { title: '状态', dataIndex: 'status', width: 110, render: (value) => <Tag color={value === 'POSTED' ? 'green' : 'default'}>{value}</Tag> },
    { title: '描述', dataIndex: 'description', ellipsis: true, render: (value) => value || '-' },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 200,
      render: (value) => new Date(value).toLocaleString()
    },
    {
      title: '分录',
      width: 90,
      fixed: 'right',
      render: (_, record) => (
        <Button size="small" icon={<EyeOutlined />} onClick={() => setSelectedJournal(record)}>
          查看
        </Button>
      )
    }
  ];

  const entryColumns: ColumnsType<LedgerEntry> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '账户 ID', dataIndex: 'accountId', width: 100 },
    { title: '归属', dataIndex: 'ownerType', width: 100 },
    { title: '归属 ID', dataIndex: 'ownerId', width: 100, render: (value) => value || '-' },
    { title: '账户类型', dataIndex: 'accountType', width: 170 },
    { title: 'Token', dataIndex: 'symbol', width: 100 },
    {
      title: '方向',
      dataIndex: 'direction',
      width: 100,
      render: (value) => <Tag color={value === 'CREDIT' ? 'green' : 'red'}>{value}</Tag>
    },
    { title: '数量', dataIndex: 'displayAmount', width: 130 },
    { title: '最小单位', dataIndex: 'amount', ellipsis: true }
  ];

  function handleSearch(values: FilterValues) {
    setFilters({
      keyword: values.keyword?.trim() || undefined,
      businessType: values.businessType,
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
          <Typography.Title level={3}>账务流水</Typography.Title>
          <Typography.Text type="secondary">查询 Ledger 流水和借贷分录，用于排查余额和对账差异。</Typography.Text>
        </div>
        <Button icon={<ReloadOutlined />} onClick={() => journalsQuery.refetch()}>
          刷新
        </Button>
      </div>

      <Space direction="vertical" className="full-width" size={16}>
        <Form form={form} layout="inline" onFinish={handleSearch}>
          <Form.Item name="keyword">
            <Input allowClear placeholder="流水号 / 业务 ID / 幂等键 / 描述" />
          </Form.Item>
          <Form.Item name="businessType">
            <Select
              allowClear
              className="filter-select-wide"
              placeholder="业务类型"
              options={[
                { value: 'DEPOSIT_CONFIRMED', label: 'DEPOSIT_CONFIRMED' },
                { value: 'MOCK_DEPOSIT', label: 'MOCK_DEPOSIT' },
                { value: 'WITHDRAWAL_FREEZE', label: 'WITHDRAWAL_FREEZE' },
                { value: 'WITHDRAWAL_REJECT', label: 'WITHDRAWAL_REJECT' },
                { value: 'WITHDRAWAL_SETTLE', label: 'WITHDRAWAL_SETTLE' }
              ]}
            />
          </Form.Item>
          <Form.Item name="status">
            <Select allowClear className="filter-select" placeholder="状态" options={[{ value: 'POSTED', label: 'POSTED' }]} />
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
          columns={journalColumns}
          dataSource={journalsQuery.data?.items || []}
          loading={journalsQuery.isLoading}
          pagination={{
            current: journalsQuery.data?.page || filters.page || 1,
            pageSize: journalsQuery.data?.pageSize || filters.pageSize || 20,
            total: journalsQuery.data?.total || 0,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => setFilters((current) => ({ ...current, page, pageSize }))
          }}
          scroll={{ x: 1500 }}
        />
      </Space>

      <Drawer title="账务分录" open={Boolean(selectedJournal)} width={900} onClose={() => setSelectedJournal(undefined)}>
        {selectedJournal ? (
          <Space direction="vertical" className="full-width" size={16}>
            <Descriptions bordered size="small" column={1}>
              <Descriptions.Item label="流水号">{selectedJournal.journalNo}</Descriptions.Item>
              <Descriptions.Item label="业务">{selectedJournal.businessType} / {selectedJournal.businessId}</Descriptions.Item>
              <Descriptions.Item label="幂等键">{selectedJournal.idempotencyKey}</Descriptions.Item>
              <Descriptions.Item label="描述">{selectedJournal.description || '-'}</Descriptions.Item>
            </Descriptions>
            <Table
              rowKey="id"
              columns={entryColumns}
              dataSource={entriesQuery.data || []}
              loading={entriesQuery.isLoading}
              pagination={false}
              scroll={{ x: 1000 }}
            />
          </Space>
        ) : null}
      </Drawer>
    </>
  );
}
