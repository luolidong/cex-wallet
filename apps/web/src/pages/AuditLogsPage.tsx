import { EyeOutlined, ReloadOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { Button, Descriptions, Drawer, Empty, Form, Input, Select, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useState } from 'react';
import { listAuditLogs } from '../api/audit';
import type { AuditLog, ListAuditLogsParams } from '../api/audit';

interface FilterValues {
  keyword?: string;
  action?: string;
  targetType?: string;
}

export function AuditLogsPage() {
  const [filters, setFilters] = useState<ListAuditLogsParams>({ page: 1, pageSize: 20 });
  const [selectedLog, setSelectedLog] = useState<AuditLog>();
  const [form] = Form.useForm<FilterValues>();
  const logsQuery = useQuery({
    queryKey: ['audit-logs', filters],
    queryFn: () => listAuditLogs(filters),
    refetchInterval: 10000
  });

  const columns: ColumnsType<AuditLog> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '管理员', dataIndex: 'adminUsername', width: 130, render: (value) => value || '-' },
    {
      title: '动作',
      dataIndex: 'action',
      width: 220,
      render: (value) => <Tag color={actionColor(value)}>{actionLabel(value)}</Tag>
    },
    { title: '对象类型', dataIndex: 'targetType', width: 150, render: targetTypeLabel },
    { title: '对象 ID', dataIndex: 'targetId', width: 120, render: (value) => value || '-' },
    { title: '摘要', dataIndex: 'summary', ellipsis: true, render: (value) => value || '-' },
    {
      title: '时间',
      dataIndex: 'createdAt',
      width: 210,
      render: (value) => new Date(value).toLocaleString()
    },
    {
      title: '详情',
      width: 90,
      render: (_, record) => (
        <Button size="small" icon={<EyeOutlined />} onClick={() => setSelectedLog(record)}>
          查看
        </Button>
      )
    }
  ];

  function handleSearch(values: FilterValues) {
    setFilters({
      keyword: values.keyword?.trim() || undefined,
      action: values.action,
      targetType: values.targetType,
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
          <Typography.Title level={3}>审计日志</Typography.Title>
          <Typography.Text type="secondary">查看关键后台操作记录。</Typography.Text>
        </div>
        <Space>
          <Typography.Text type="secondary">每 10 秒自动刷新</Typography.Text>
          <Button icon={<ReloadOutlined />} onClick={() => logsQuery.refetch()}>
            刷新
          </Button>
        </Space>
      </div>

      <Space direction="vertical" className="full-width" size={16}>
        <Form form={form} layout="inline" onFinish={handleSearch}>
          <Form.Item name="keyword">
            <Input allowClear placeholder="管理员 / 对象 ID / 摘要 / 详情" />
          </Form.Item>
          <Form.Item name="action">
            <Select allowClear className="filter-select-wide" placeholder="动作" options={auditActionOptions()} />
          </Form.Item>
          <Form.Item name="targetType">
            <Select allowClear className="filter-select" placeholder="对象类型" options={targetTypeOptions()} />
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
          dataSource={logsQuery.data?.items || []}
          loading={logsQuery.isLoading}
          pagination={{
            current: logsQuery.data?.page || filters.page || 1,
            pageSize: logsQuery.data?.pageSize || filters.pageSize || 20,
            total: logsQuery.data?.total || 0,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条日志`,
            onChange: (page, pageSize) => setFilters((current) => ({ ...current, page, pageSize }))
          }}
          locale={{ emptyText: <Empty description="暂无审计日志" /> }}
          scroll={{ x: 1300 }}
        />
      </Space>

      <Drawer
        title="审计详情"
        open={Boolean(selectedLog)}
        width={620}
        onClose={() => setSelectedLog(undefined)}
      >
        {selectedLog ? (
          <Space direction="vertical" className="full-width" size={16}>
            <Descriptions bordered column={1} size="small">
              <Descriptions.Item label="日志 ID">{selectedLog.id}</Descriptions.Item>
              <Descriptions.Item label="管理员">{selectedLog.adminUsername || '-'}</Descriptions.Item>
              <Descriptions.Item label="动作">{actionLabel(selectedLog.action)}</Descriptions.Item>
              <Descriptions.Item label="对象">
                {targetTypeLabel(selectedLog.targetType)} {selectedLog.targetId || ''}
              </Descriptions.Item>
              <Descriptions.Item label="摘要">{selectedLog.summary || '-'}</Descriptions.Item>
              <Descriptions.Item label="时间">{new Date(selectedLog.createdAt).toLocaleString()}</Descriptions.Item>
            </Descriptions>
            <div>
              <Typography.Title level={5}>详细信息</Typography.Title>
              <pre className="json-panel">{formatDetail(selectedLog.detailJson)}</pre>
            </div>
          </Space>
        ) : null}
      </Drawer>
    </>
  );
}

function actionLabel(action: string) {
  const labels: Record<string, string> = {
    CHAIN_UPDATE: '修改链配置',
    TOKEN_UPDATE: '修改 Token 配置',
    WITHDRAWAL_RULE_UPDATE: '修改提现规则',
    BLACKLIST_ADDRESS_ADD: '添加黑名单地址',
    BLACKLIST_ADDRESS_DISABLE: '停用黑名单地址',
    BLACKLIST_ADDRESS_ENABLE: '启用黑名单地址',
    KYC_WITHDRAWAL_LIMIT_UPDATE: '修改 KYC 提现限额',
    PLATFORM_WALLET_CREATE: '新增平台钱包',
    PLATFORM_WALLET_UPDATE: '修改平台钱包',
    PLATFORM_WALLET_DISABLE: '停用平台钱包',
    USER_STATUS_UPDATE: '修改用户状态',
    USER_KYC_UPDATE: '修改用户 KYC',
    LEDGER_MANUAL_ADJUSTMENT: '人工调账',
    ADMIN_ACCOUNT_CREATE: '新增后台账号',
    ADMIN_ACCOUNT_STATUS_UPDATE: '修改后台账号状态',
    ADMIN_ACCOUNT_ROLES_UPDATE: '修改后台账号角色',
    ROLE_PERMISSIONS_UPDATE: '修改角色权限',
    WALLET_ENABLE: '启用充值地址',
    WALLET_DISABLE: '停用充值地址',
    WITHDRAWAL_APPROVE: '批准提现',
    WITHDRAWAL_REJECT: '拒绝提现',
    WITHDRAWAL_FAIL: '提现失败退款',
    WITHDRAWAL_BROADCAST: '广播提现',
    WITHDRAWAL_CONFIRM: '确认提现'
  };
  return labels[action] || action;
}

function auditActionOptions() {
  return [
    'CHAIN_UPDATE',
    'TOKEN_UPDATE',
    'WITHDRAWAL_RULE_UPDATE',
    'BLACKLIST_ADDRESS_ADD',
    'BLACKLIST_ADDRESS_DISABLE',
    'BLACKLIST_ADDRESS_ENABLE',
    'KYC_WITHDRAWAL_LIMIT_UPDATE',
    'PLATFORM_WALLET_CREATE',
    'PLATFORM_WALLET_UPDATE',
    'PLATFORM_WALLET_DISABLE',
    'USER_STATUS_UPDATE',
    'USER_KYC_UPDATE',
    'LEDGER_MANUAL_ADJUSTMENT',
    'ADMIN_ACCOUNT_CREATE',
    'ADMIN_ACCOUNT_STATUS_UPDATE',
    'ADMIN_ACCOUNT_ROLES_UPDATE',
    'ROLE_PERMISSIONS_UPDATE',
    'WALLET_ENABLE',
    'WALLET_DISABLE',
    'WITHDRAWAL_APPROVE',
    'WITHDRAWAL_REJECT',
    'WITHDRAWAL_FAIL',
    'WITHDRAWAL_BROADCAST',
    'WITHDRAWAL_CONFIRM'
  ].map((action) => ({ value: action, label: actionLabel(action) }));
}

function targetTypeLabel(targetType: string) {
  const labels: Record<string, string> = {
    CHAIN: '链',
    TOKEN: 'Token',
    PLATFORM_WALLET: '平台钱包',
    ADMIN_USER: '后台账号',
    ROLE: '角色',
    USER: '用户',
    KYC_WITHDRAWAL_LIMIT: 'KYC 提现限额',
    LEDGER_JOURNAL: '账务流水',
    WALLET: '充值地址',
    WITHDRAWAL: '提现单',
    WITHDRAWAL_ADDRESS: '提现地址'
  };
  return labels[targetType] || targetType;
}

function targetTypeOptions() {
  return [
    'CHAIN',
    'TOKEN',
    'PLATFORM_WALLET',
    'ADMIN_USER',
    'ROLE',
    'USER',
    'KYC_WITHDRAWAL_LIMIT',
    'LEDGER_JOURNAL',
    'WALLET',
    'WITHDRAWAL',
    'WITHDRAWAL_ADDRESS'
  ].map((targetType) => ({ value: targetType, label: targetTypeLabel(targetType) }));
}

function formatDetail(detailJson?: string) {
  if (!detailJson) {
    return '无详情';
  }
  try {
    return JSON.stringify(JSON.parse(detailJson), null, 2);
  } catch {
    return detailJson;
  }
}

function actionColor(action: string) {
  if (action.includes('REJECT') || action.includes('FAIL') || action.includes('DISABLE')) {
    return 'red';
  }
  if (action.includes('APPROVE') || action.includes('ENABLE')) {
    return 'green';
  }
  if (action.includes('BROADCAST') || action.includes('CONFIRM')) {
    return 'blue';
  }
  return 'default';
}
