import { CheckOutlined, DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Form, Input, Modal, Select, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useState } from 'react';
import {
  addBlacklistAddress,
  disableBlacklistAddress,
  enableBlacklistAddress,
  listBlacklistAddresses,
  listRiskChains,
  listWithdrawalRules,
  updateWithdrawalRule
} from '../api/risk';
import type { BlacklistAddress, WithdrawalRule } from '../api/risk';

interface RuleFormValues {
  maxWithdrawAmount?: string;
  dailyWithdrawLimit?: string;
}

interface BlacklistFormValues {
  chainId: number;
  address: string;
  reason?: string;
}

export function RiskSettingsPage() {
  const [editingRule, setEditingRule] = useState<WithdrawalRule>();
  const [blacklistOpen, setBlacklistOpen] = useState(false);
  const [ruleForm] = Form.useForm<RuleFormValues>();
  const [blacklistForm] = Form.useForm<BlacklistFormValues>();
  const queryClient = useQueryClient();

  const rulesQuery = useQuery({
    queryKey: ['risk', 'withdrawal-rules'],
    queryFn: listWithdrawalRules
  });
  const blacklistQuery = useQuery({
    queryKey: ['risk', 'withdrawal-address-blacklist'],
    queryFn: listBlacklistAddresses
  });
  const chainsQuery = useQuery({
    queryKey: ['risk', 'chains'],
    queryFn: listRiskChains
  });

  const updateRuleMutation = useMutation({
    mutationFn: ({ tokenId, values }: { tokenId: number; values: RuleFormValues }) => {
      const rule = rulesQuery.data?.find((item) => item.tokenId === tokenId);
      if (!rule) {
        throw new Error('token not found');
      }
      return updateWithdrawalRule(tokenId, {
        maxWithdrawAmount: toBaseUnit(values.maxWithdrawAmount || '', rule.decimals),
        dailyWithdrawLimit: toBaseUnit(values.dailyWithdrawLimit || '', rule.decimals)
      });
    },
    onSuccess: async () => {
      message.success('提现规则已保存');
      setEditingRule(undefined);
      ruleForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['risk', 'withdrawal-rules'] });
    },
    onError: (error) => showRequestError(error, '保存提现规则失败')
  });

  const addBlacklistMutation = useMutation({
    mutationFn: addBlacklistAddress,
    onSuccess: async () => {
      message.success('黑名单地址已添加');
      setBlacklistOpen(false);
      blacklistForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['risk', 'withdrawal-address-blacklist'] });
    },
    onError: (error) => showRequestError(error, '添加黑名单失败')
  });

  const disableBlacklistMutation = useMutation({
    mutationFn: disableBlacklistAddress,
    onSuccess: async () => {
      message.success('黑名单地址已停用');
      await queryClient.invalidateQueries({ queryKey: ['risk', 'withdrawal-address-blacklist'] });
    },
    onError: (error) => showRequestError(error, '停用黑名单失败')
  });

  const enableBlacklistMutation = useMutation({
    mutationFn: enableBlacklistAddress,
    onSuccess: async () => {
      message.success('黑名单地址已启用');
      await queryClient.invalidateQueries({ queryKey: ['risk', 'withdrawal-address-blacklist'] });
    },
    onError: (error) => showRequestError(error, '启用黑名单失败')
  });

  const ruleColumns: ColumnsType<WithdrawalRule> = [
    { title: 'Token', dataIndex: 'symbol', width: 100 },
    { title: '类型', dataIndex: 'tokenType', width: 110 },
    { title: '最小提现', dataIndex: 'displayMinWithdrawAmount', width: 130 },
    { title: '手续费', dataIndex: 'displayWithdrawFee', width: 120 },
    { title: '单笔上限', dataIndex: 'displayMaxWithdrawAmount', width: 140, render: (value) => value || '不限' },
    { title: '每日上限', dataIndex: 'displayDailyWithdrawLimit', width: 140, render: (value) => value || '不限' },
    {
      title: '提现',
      dataIndex: 'withdrawEnabled',
      width: 90,
      render: (value) => <Tag color={value ? 'green' : 'default'}>{value ? '启用' : '停用'}</Tag>
    },
    {
      title: '操作',
      width: 100,
      render: (_, record) => (
        <Button size="small" icon={<EditOutlined />} onClick={() => openRuleModal(record)}>
          编辑
        </Button>
      )
    }
  ];

  const blacklistColumns: ColumnsType<BlacklistAddress> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '链', dataIndex: 'chainName', width: 140 },
    { title: '地址', dataIndex: 'address', ellipsis: true },
    { title: '原因', dataIndex: 'reason', ellipsis: true, render: (value) => value || '-' },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (value) => <Tag color={value === 'ACTIVE' ? 'red' : 'default'}>{value}</Tag>
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 210,
      render: (value) => new Date(value).toLocaleString()
    },
    {
      title: '操作',
      width: 110,
      render: (_, record) => (
        <Space>
          {record.status === 'ACTIVE' ? (
          <Button
            size="small"
            danger
            icon={<DeleteOutlined />}
            loading={disableBlacklistMutation.isPending}
            onClick={() => disableBlacklistMutation.mutate(record.id)}
          >
            停用
          </Button>
          ) : (
            <Button
              size="small"
              type="primary"
              icon={<CheckOutlined />}
              loading={enableBlacklistMutation.isPending}
              onClick={() => enableBlacklistMutation.mutate(record.id)}
            >
              启用
            </Button>
          )}
        </Space>
      )
    }
  ];

  function openRuleModal(rule: WithdrawalRule) {
    setEditingRule(rule);
    ruleForm.setFieldsValue({
      maxWithdrawAmount: rule.displayMaxWithdrawAmount,
      dailyWithdrawLimit: rule.displayDailyWithdrawLimit
    });
  }

  async function handleSaveRule() {
    if (!editingRule) {
      return;
    }
    const values = await ruleForm.validateFields();
    updateRuleMutation.mutate({ tokenId: editingRule.tokenId, values });
  }

  async function handleAddBlacklist() {
    const values = await blacklistForm.validateFields();
    addBlacklistMutation.mutate(values);
  }

  return (
    <>
      <div className="page-toolbar">
        <div>
          <Typography.Title level={3}>风控配置</Typography.Title>
          <Typography.Text type="secondary">维护提现限额和地址黑名单。</Typography.Text>
        </div>
        <Space>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => {
              rulesQuery.refetch();
              blacklistQuery.refetch();
            }}
          >
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setBlacklistOpen(true)}>
            添加黑名单
          </Button>
        </Space>
      </div>

      <div className="content-stack">
        <section>
          <Typography.Title level={4}>提现限额</Typography.Title>
          <Table
            rowKey="tokenId"
            columns={ruleColumns}
            dataSource={rulesQuery.data || []}
            loading={rulesQuery.isLoading}
            pagination={false}
          />
        </section>

        <section>
          <Typography.Title level={4}>地址黑名单</Typography.Title>
          <Table
            rowKey="id"
            columns={blacklistColumns}
            dataSource={blacklistQuery.data || []}
            loading={blacklistQuery.isLoading}
            pagination={{ pageSize: 10 }}
          />
        </section>
      </div>

      <Modal
        title={editingRule ? `编辑 ${editingRule.symbol} 提现限额` : '编辑提现限额'}
        open={Boolean(editingRule)}
        okText="保存"
        confirmLoading={updateRuleMutation.isPending}
        onOk={handleSaveRule}
        onCancel={() => setEditingRule(undefined)}
      >
        <Form form={ruleForm} layout="vertical">
          <Form.Item label="单笔上限" name="maxWithdrawAmount" rules={[{ required: true, message: '请输入单笔上限' }]}>
            <Input placeholder="例如 100000" suffix={editingRule?.symbol} />
          </Form.Item>
          <Form.Item label="每日上限" name="dailyWithdrawLimit" rules={[{ required: true, message: '请输入每日上限' }]}>
            <Input placeholder="例如 500000" suffix={editingRule?.symbol} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="添加黑名单地址"
        open={blacklistOpen}
        okText="添加"
        confirmLoading={addBlacklistMutation.isPending}
        onOk={handleAddBlacklist}
        onCancel={() => setBlacklistOpen(false)}
      >
        <Form form={blacklistForm} layout="vertical" initialValues={{ chainId: 1 }}>
          <Form.Item label="链" name="chainId" rules={[{ required: true, message: '请选择链' }]}>
            <Select
              placeholder="选择地址所在链"
              loading={chainsQuery.isLoading}
              options={(chainsQuery.data || []).map((chain) => ({
                value: chain.id,
                label: `${chain.name}，${chain.chainType}-${chain.chainId}`
              }))}
            />
          </Form.Item>
          <Form.Item label="地址" name="address" rules={[{ required: true, message: '请输入地址' }]}>
            <Input placeholder="例如 0x..." />
          </Form.Item>
          <Form.Item label="原因" name="reason">
            <Input.TextArea rows={3} placeholder="例如：测试黑名单、风险地址" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}

function toBaseUnit(value: string, decimals: number): string {
  const normalized = value.trim();
  if (!normalized) {
    return '0';
  }
  if (!/^\d+(\.\d+)?$/.test(normalized)) {
    throw new Error('invalid amount');
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
