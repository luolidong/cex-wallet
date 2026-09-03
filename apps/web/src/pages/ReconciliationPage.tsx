import { ReloadOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { Alert, Button, Empty, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { listTokenReconciliations } from '../api/reconciliation';
import type { TokenReconciliation } from '../api/reconciliation';

export function ReconciliationPage() {
  const reconciliationQuery = useQuery({
    queryKey: ['reconciliation', 'tokens'],
    queryFn: listTokenReconciliations,
    refetchInterval: 10000
  });
  const rows = reconciliationQuery.data || [];
  const mismatchCount = rows.filter((item) => item.status !== 'MATCHED').length;

  const columns: ColumnsType<TokenReconciliation> = [
    { title: 'Token', dataIndex: 'symbol', width: 100 },
    { title: '类型', dataIndex: 'tokenType', width: 100 },
    { title: '用户可用', dataIndex: 'displayUserAvailable', width: 130 },
    { title: '用户冻结', dataIndex: 'displayUserFrozen', width: 130 },
    { title: '账本总额', dataIndex: 'displayLedgerTotal', width: 130 },
    { title: '确认充值', dataIndex: 'displayConfirmedDeposits', width: 130 },
    { title: '确认提现', dataIndex: 'displayConfirmedWithdrawals', width: 130 },
    { title: '待完成提现', dataIndex: 'displayPendingWithdrawals', width: 140 },
    { title: '期望账本', dataIndex: 'displayExpectedLedgerTotal', width: 130 },
    { title: '热钱包地址', dataIndex: 'hotWalletAddress', width: 180, ellipsis: true, render: (value) => value || '未配置' },
    { title: '热钱包链上余额', dataIndex: 'displayHotWalletBalance', width: 160, render: (value) => value || '未配置' },
    {
      title: '资金覆盖差额',
      dataIndex: 'displayCoverageDifference',
      width: 140,
      render: (value, record) => (
        <Typography.Text type={record.coverageDifference && BigInt(record.coverageDifference) < 0n ? 'danger' : undefined}>
          {value || '-'}
        </Typography.Text>
      )
    },
    {
      title: '差额',
      dataIndex: 'displayDifference',
      width: 120,
      render: (value, record) => <Typography.Text type={record.status === 'MATCHED' ? undefined : 'danger'}>{value}</Typography.Text>
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 120,
      render: (value) => <Tag color={value === 'MATCHED' ? 'green' : 'red'}>{value}</Tag>
    }
  ];

  return (
    <>
      <div className="page-toolbar">
        <div>
          <Typography.Title level={3}>账务对账</Typography.Title>
          <Typography.Text type="secondary">按 Token 核对用户账本、充值和提现记录。</Typography.Text>
        </div>
        <Space>
          <Typography.Text type="secondary">每 10 秒自动刷新</Typography.Text>
          <Button icon={<ReloadOutlined />} onClick={() => reconciliationQuery.refetch()}>
            刷新
          </Button>
        </Space>
      </div>

      <Space direction="vertical" className="full-width" size={16}>
        <Alert
          type={mismatchCount === 0 ? 'success' : 'error'}
          showIcon
          message={mismatchCount === 0 ? '账务检查通过' : `发现 ${mismatchCount} 个 Token 账务不一致`}
          description="当前检查口径：账本总额 = 确认充值 - 确认提现；热钱包链上余额需要覆盖账本总额。mock 入账不纳入实际账本总额。"
        />
        <Table
          rowKey="tokenId"
          columns={columns}
          dataSource={rows}
          loading={reconciliationQuery.isLoading}
          pagination={false}
          scroll={{ x: 1300 }}
          locale={{ emptyText: <Empty description="暂无对账数据" /> }}
        />
      </Space>
    </>
  );
}
