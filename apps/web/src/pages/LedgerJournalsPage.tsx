import { EyeOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Descriptions, Drawer, Form, Input, Modal, Select, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useState } from 'react';
import { listTokens } from '../api/assets';
import { createManualAdjustment, listLedgerEntries, listLedgerJournals } from '../api/ledger';
import type { LedgerEntry, LedgerJournal, ListLedgerJournalsParams } from '../api/ledger';

interface FilterValues {
  keyword?: string;
  businessType?: string;
  status?: string;
}

interface AdjustmentFormValues {
  userId: number;
  tokenId: number;
  direction: string;
  displayAmount: string;
  reason: string;
  idempotencyKey?: string;
}

export function LedgerJournalsPage() {
  const [filters, setFilters] = useState<ListLedgerJournalsParams>({ page: 1, pageSize: 20 });
  const [selectedJournal, setSelectedJournal] = useState<LedgerJournal>();
  const [adjustmentOpen, setAdjustmentOpen] = useState(false);
  const [form] = Form.useForm<FilterValues>();
  const [adjustmentForm] = Form.useForm<AdjustmentFormValues>();
  const selectedTokenId = Form.useWatch('tokenId', adjustmentForm);
  const queryClient = useQueryClient();

  const journalsQuery = useQuery({
    queryKey: ['ledger-journals', filters],
    queryFn: () => listLedgerJournals(filters)
  });
  const entriesQuery = useQuery({
    queryKey: ['ledger-entries', selectedJournal?.id],
    queryFn: () => listLedgerEntries(selectedJournal!.id),
    enabled: Boolean(selectedJournal)
  });
  const tokensQuery = useQuery({
    queryKey: ['assets', 'tokens'],
    queryFn: listTokens
  });
  const selectedToken = tokensQuery.data?.find((token) => token.id === selectedTokenId);

  const adjustmentMutation = useMutation({
    mutationFn: (values: AdjustmentFormValues) => {
      const token = tokensQuery.data?.find((item) => item.id === values.tokenId);
      if (!token) {
        throw new Error('请选择 Token');
      }
      return createManualAdjustment({
        userId: Number(values.userId),
        tokenId: Number(values.tokenId),
        direction: values.direction,
        amount: toBaseUnit(values.displayAmount, token.decimals),
        reason: values.reason,
        idempotencyKey: values.idempotencyKey?.trim() || undefined
      });
    },
    onSuccess: async () => {
      message.success('人工调账已入账');
      setAdjustmentOpen(false);
      adjustmentForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['ledger-journals'] });
    },
    onError: (error) => showRequestError(error, '人工调账失败')
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
        <Space>
          <Button icon={<ReloadOutlined />} onClick={() => journalsQuery.refetch()}>
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setAdjustmentOpen(true)}>
            人工调账
          </Button>
        </Space>
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
                { value: 'WITHDRAWAL_FAIL_REFUND', label: 'WITHDRAWAL_FAIL_REFUND' },
                { value: 'WITHDRAWAL_SETTLE', label: 'WITHDRAWAL_SETTLE' },
                { value: 'MANUAL_ADJUSTMENT', label: 'MANUAL_ADJUSTMENT' }
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

      <Modal
        title="人工调账"
        open={adjustmentOpen}
        okText="确认入账"
        okButtonProps={{ danger: true }}
        confirmLoading={adjustmentMutation.isPending}
        onOk={() => adjustmentForm.validateFields().then((values) => adjustmentMutation.mutate(values))}
        onCancel={() => setAdjustmentOpen(false)}
      >
        <Form form={adjustmentForm} layout="vertical" initialValues={{ direction: 'CREDIT' }}>
          <Form.Item label="用户 ID" name="userId" rules={[{ required: true, message: '请输入用户 ID' }]}>
            <Input placeholder="例如 1" />
          </Form.Item>
          <Form.Item label="Token" name="tokenId" rules={[{ required: true, message: '请选择 Token' }]}>
            <Select
              placeholder="选择调账资产"
              loading={tokensQuery.isLoading}
              options={(tokensQuery.data || []).map((token) => ({
                value: token.id,
                label: `${token.symbol} / ${token.chainName}`
              }))}
            />
          </Form.Item>
          <Form.Item label="方向" name="direction" rules={[{ required: true, message: '请选择方向' }]}>
            <Select
              options={[
                { value: 'CREDIT', label: '增加用户可用余额' },
                { value: 'DEBIT', label: '减少用户可用余额' }
              ]}
            />
          </Form.Item>
          <Form.Item label="数量" name="displayAmount" rules={[{ required: true, message: '请输入数量' }]}>
            <Input placeholder="例如 0.1" suffix={selectedToken?.symbol} />
          </Form.Item>
          <Form.Item label="原因" name="reason" rules={[{ required: true, message: '请输入调账原因' }]}>
            <Input.TextArea rows={3} placeholder="例如：客服补偿、异常订单修正、测试环境修正" />
          </Form.Item>
          <Form.Item label="幂等键" name="idempotencyKey">
            <Input placeholder="可选；重复提交保护，例如 ticket-10001" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}

function toBaseUnit(value: string, decimals: number): string {
  const normalized = value.trim();
  if (!/^\d+(\.\d+)?$/.test(normalized)) {
    throw new Error('amount format is invalid');
  }
  const [integerPart, decimalPart = ''] = normalized.split('.');
  if (decimalPart.length > decimals) {
    throw new Error('too many decimal places');
  }
  return `${integerPart}${decimalPart.padEnd(decimals, '0')}`.replace(/^0+(?=\d)/, '') || '0';
}

function showRequestError(error: unknown, fallback: string) {
  const err = error as { response?: { data?: { error?: { message?: string; details?: string } } }; message?: string };
  message.error(err.response?.data?.error?.details || err.response?.data?.error?.message || err.message || fallback);
}
