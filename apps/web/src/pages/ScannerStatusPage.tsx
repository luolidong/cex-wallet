import { ReloadOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { Button, Empty, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { listScannerStatuses } from '../api/scanner';
import type { ScannerStatus } from '../api/scanner';

export function ScannerStatusPage() {
  const statusesQuery = useQuery({
    queryKey: ['scanner', 'statuses'],
    queryFn: listScannerStatuses,
    refetchInterval: 10000
  });

  const columns: ColumnsType<ScannerStatus> = [
    { title: '链', dataIndex: 'chainName', width: 160 },
    { title: '类型', dataIndex: 'chainType', width: 100 },
    { title: '网络 ID', dataIndex: 'networkChainId', width: 100 },
    {
      title: '扫描',
      dataIndex: 'scanEnabled',
      width: 90,
      render: (value) => <Tag color={value ? 'green' : 'default'}>{value ? '启用' : '停用'}</Tag>
    },
    { title: '确认块数', dataIndex: 'confirmBlocks', width: 100 },
    { title: '扫描器', dataIndex: 'scannerName', width: 220, render: (value) => value || '暂无游标' },
    { title: '最后扫描', dataIndex: 'lastScannedBlock', width: 120, render: nullableNumber },
    { title: '最后确认', dataIndex: 'lastFinalizedBlock', width: 120, render: nullableNumber },
    {
      title: '滞后',
      dataIndex: 'lagBlocks',
      width: 90,
      render: (value) => <Tag color={Number(value || 0) > 0 ? 'gold' : 'green'}>{value ?? '-'}</Tag>
    },
    {
      title: '游标状态',
      dataIndex: 'cursorStatus',
      width: 110,
      render: (value) => <Tag color={value === 'ACTIVE' ? 'green' : 'default'}>{value || '-'}</Tag>
    },
    { title: '充值地址数', dataIndex: 'depositAddressCount', width: 120 },
    { title: '扫描器充值数', dataIndex: 'scannerDepositCount', width: 130 },
    { title: '链充值总数', dataIndex: 'depositCount', width: 120 },
    {
      title: '更新时间',
      dataIndex: 'cursorUpdatedAt',
      width: 210,
      render: (value) => (value ? new Date(value).toLocaleString() : '-')
    }
  ];

  return (
    <>
      <div className="page-toolbar">
        <div>
          <Typography.Title level={3}>扫描状态</Typography.Title>
          <Typography.Text type="secondary">查看链扫描游标、确认进度和充值同步情况。</Typography.Text>
        </div>
        <Space>
          <Typography.Text type="secondary">每 10 秒自动刷新</Typography.Text>
          <Button icon={<ReloadOutlined />} onClick={() => statusesQuery.refetch()}>
            刷新
          </Button>
        </Space>
      </div>

      <Table
        rowKey={(record) => `${record.chainId}-${record.scannerName || 'none'}`}
        columns={columns}
        dataSource={statusesQuery.data || []}
        loading={statusesQuery.isLoading}
        pagination={false}
        locale={{ emptyText: <Empty description="暂无扫描状态" /> }}
      />
    </>
  );
}

function nullableNumber(value?: number) {
  return value ?? '-';
}
