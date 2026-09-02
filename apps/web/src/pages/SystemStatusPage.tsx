import { ReloadOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { Button, Empty, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { listSystemStatuses } from '../api/system';
import type { ServiceStatus } from '../api/system';

export function SystemStatusPage() {
  const statusesQuery = useQuery({
    queryKey: ['system', 'statuses'],
    queryFn: listSystemStatuses,
    refetchInterval: 10000
  });

  const columns: ColumnsType<ServiceStatus> = [
    { title: '服务', dataIndex: 'name', width: 140 },
    { title: '类型', dataIndex: 'type', width: 120 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (value) => <Tag color={value === 'UP' ? 'green' : 'red'}>{value}</Tag>
    },
    { title: '端点', dataIndex: 'endpoint', ellipsis: true },
    { title: '耗时', dataIndex: 'latencyMs', width: 100, render: (value) => `${value ?? '-'} ms` },
    { title: '信息', dataIndex: 'message', width: 260, ellipsis: true, render: (value) => value || '-' },
    {
      title: '检查时间',
      dataIndex: 'checkedAt',
      width: 210,
      render: (value) => new Date(value).toLocaleString()
    }
  ];

  return (
    <>
      <div className="page-toolbar">
        <div>
          <Typography.Title level={3}>系统状态</Typography.Title>
          <Typography.Text type="secondary">集中查看 API、scanner、signer、Postgres 和 Redis 的健康状态。</Typography.Text>
        </div>
        <Space>
          <Typography.Text type="secondary">每 10 秒自动刷新</Typography.Text>
          <Button icon={<ReloadOutlined />} onClick={() => statusesQuery.refetch()}>
            刷新
          </Button>
        </Space>
      </div>

      <Table
        rowKey="name"
        columns={columns}
        dataSource={statusesQuery.data || []}
        loading={statusesQuery.isLoading}
        pagination={false}
        locale={{ emptyText: <Empty description="暂无系统状态" /> }}
      />
    </>
  );
}
