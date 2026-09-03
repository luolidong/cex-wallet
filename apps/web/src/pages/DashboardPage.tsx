import {
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloudUploadOutlined,
  ExclamationCircleOutlined,
  ReloadOutlined,
  SwapOutlined,
  WalletOutlined
} from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { Button, Card, Col, Empty, Row, Space, Statistic, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useNavigate } from 'react-router-dom';
import { getDashboardSummary } from '../api/dashboard';
import type { DashboardTokenBalance, DashboardWithdrawal } from '../api/dashboard';

export function DashboardPage() {
  const navigate = useNavigate();
  const summaryQuery = useQuery({
    queryKey: ['dashboard', 'summary'],
    queryFn: getDashboardSummary,
    refetchInterval: 10000
  });
  const summary = summaryQuery.data;

  const withdrawalColumns: ColumnsType<DashboardWithdrawal> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '用户', dataIndex: 'username', width: 140 },
    { title: 'Token', dataIndex: 'symbol', width: 90 },
    { title: '数量', dataIndex: 'displayAmount', width: 120 },
    { title: '手续费', dataIndex: 'displayFee', width: 110 },
    { title: '目标地址', dataIndex: 'toAddress', ellipsis: true },
    {
      title: '状态',
      dataIndex: 'status',
      width: 150,
      render: (value) => <Tag color={withdrawalStatusColor(value)}>{value}</Tag>
    },
    {
      title: '申请时间',
      dataIndex: 'requestedAt',
      width: 190,
      render: (value) => new Date(value).toLocaleString()
    }
  ];

  const balanceColumns: ColumnsType<DashboardTokenBalance> = [
    { title: 'Token', dataIndex: 'symbol', width: 100 },
    { title: '用户可用', dataIndex: 'displayUserAvailable', width: 140 },
    { title: '用户冻结', dataIndex: 'displayUserFrozen', width: 140 },
    { title: '热钱包余额', dataIndex: 'displayHotWalletBalance', width: 150, render: (value) => value || '-' },
    { title: '覆盖差额', dataIndex: 'displayCoverageDifference', width: 150, render: (value) => value || '-' },
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
          <Typography.Title level={3}>仪表盘</Typography.Title>
          <Typography.Text type="secondary">运营待办、资金状态和服务健康总览。</Typography.Text>
        </div>
        <Space>
          <Typography.Text type="secondary">每 10 秒自动刷新</Typography.Text>
          <Button icon={<ReloadOutlined />} onClick={() => summaryQuery.refetch()}>
            刷新
          </Button>
        </Space>
      </div>

      <Space direction="vertical" className="full-width" size={16}>
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={12} lg={6}>
            <Card hoverable onClick={() => navigate('/withdrawals/review')}>
              <Statistic
                title="待审核提现"
                value={summary?.pendingWithdrawalCount || 0}
                prefix={<ClockCircleOutlined />}
                loading={summaryQuery.isLoading}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card hoverable onClick={() => navigate('/withdrawals/review')}>
              <Statistic
                title="已广播待确认"
                value={summary?.broadcastedWithdrawalCount || 0}
                prefix={<CloudUploadOutlined />}
                loading={summaryQuery.isLoading}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card hoverable onClick={() => navigate('/deposits')}>
              <Statistic
                title="今日充值笔数"
                value={summary?.todayDepositCount || 0}
                prefix={<WalletOutlined />}
                loading={summaryQuery.isLoading}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card hoverable onClick={() => navigate('/withdrawal-records')}>
              <Statistic
                title="今日确认提现"
                value={summary?.todayWithdrawalCount || 0}
                prefix={<SwapOutlined />}
                loading={summaryQuery.isLoading}
              />
            </Card>
          </Col>
        </Row>

        <Row gutter={[16, 16]}>
          <Col xs={24} sm={12}>
            <Card hoverable onClick={() => navigate('/reconciliation')}>
              <Statistic
                title="账务异常 Token"
                value={summary?.reconciliationMismatchCount || 0}
                prefix={summary?.reconciliationMismatchCount ? <ExclamationCircleOutlined /> : <CheckCircleOutlined />}
                valueStyle={{ color: summary?.reconciliationMismatchCount ? '#cf1322' : '#3f8600' }}
                loading={summaryQuery.isLoading}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12}>
            <Card hoverable onClick={() => navigate('/system/status')}>
              <Statistic
                title="异常服务"
                value={summary?.serviceDownCount || 0}
                prefix={summary?.serviceDownCount ? <ExclamationCircleOutlined /> : <CheckCircleOutlined />}
                valueStyle={{ color: summary?.serviceDownCount ? '#cf1322' : '#3f8600' }}
                loading={summaryQuery.isLoading}
              />
            </Card>
          </Col>
        </Row>

        <Card title="Token 资金覆盖">
          <Table
            rowKey="tokenId"
            columns={balanceColumns}
            dataSource={summary?.tokenBalances || []}
            loading={summaryQuery.isLoading}
            pagination={false}
            scroll={{ x: 820 }}
            locale={{ emptyText: <Empty description="暂无 Token 余额" /> }}
          />
        </Card>

        <Card title="最近待处理提现">
          <Table
            rowKey="id"
            columns={withdrawalColumns}
            dataSource={summary?.recentPendingWithdrawals || []}
            loading={summaryQuery.isLoading}
            pagination={false}
            scroll={{ x: 1120 }}
            locale={{ emptyText: <Empty description="暂无待处理提现" /> }}
          />
        </Card>
      </Space>
    </>
  );
}

function withdrawalStatusColor(status: string) {
  if (status === 'PENDING_APPROVAL') {
    return 'gold';
  }
  if (status === 'APPROVED') {
    return 'blue';
  }
  if (status === 'BROADCASTED') {
    return 'processing';
  }
  return 'default';
}
