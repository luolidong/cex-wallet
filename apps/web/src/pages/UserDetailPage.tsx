import { ArrowLeftOutlined, ExportOutlined, ReloadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Descriptions, Empty, Form, Input, Modal, Select, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  createWithdrawal,
  createDepositAddress,
  getUser,
  getUserBalances,
  listUserDeposits,
  listUserWallets,
  listUserWithdrawals,
  type Balance,
  type CreateWithdrawalInput,
  type Deposit,
  type Wallet,
  type Withdrawal
} from '../api/users';

interface WithdrawalFormValues {
  tokenId: number;
  toAddress: string;
  displayAmount: string;
}

export function UserDetailPage() {
  const [withdrawOpen, setWithdrawOpen] = useState(false);
  const [withdrawForm] = Form.useForm<WithdrawalFormValues>();
  const selectedTokenId = Form.useWatch('tokenId', withdrawForm);
  const displayAmount = Form.useWatch('displayAmount', withdrawForm);
  const navigate = useNavigate();
  const params = useParams();
  const userId = Number(params.userId);
  const queryClient = useQueryClient();

  const userQuery = useQuery({
    queryKey: ['user', userId],
    queryFn: () => getUser(userId),
    enabled: Number.isFinite(userId)
  });

  const balancesQuery = useQuery({
    queryKey: ['user-balances', userId],
    queryFn: () => getUserBalances(userId),
    enabled: Number.isFinite(userId)
  });

  const walletsQuery = useQuery({
    queryKey: ['user-wallets', userId],
    queryFn: () => listUserWallets(userId),
    enabled: Number.isFinite(userId)
  });

  const depositsQuery = useQuery({
    queryKey: ['user-deposits', userId],
    queryFn: () => listUserDeposits(userId),
    enabled: Number.isFinite(userId)
  });

  const withdrawalsQuery = useQuery({
    queryKey: ['user-withdrawals', userId],
    queryFn: () => listUserWithdrawals(userId),
    enabled: Number.isFinite(userId)
  });

  const createAddressMutation = useMutation({
    mutationFn: () => createDepositAddress(userId, 1),
    onSuccess: async () => {
      message.success('充值地址已生成');
      await queryClient.invalidateQueries({ queryKey: ['user-wallets', userId] });
    }
  });

  const createWithdrawalMutation = useMutation({
    mutationFn: (values: CreateWithdrawalInput) => createWithdrawal(userId, values),
    onSuccess: async () => {
      message.success('提现申请已创建');
      setWithdrawOpen(false);
      withdrawForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['user-balances', userId] });
      await queryClient.invalidateQueries({ queryKey: ['user-withdrawals', userId] });
    }
  });

  const columns: ColumnsType<Balance> = [
    {
      title: 'Token',
      dataIndex: 'symbol'
    },
    {
      title: '可用余额',
      dataIndex: 'displayAvailable'
    },
    {
      title: '冻结余额',
      dataIndex: 'displayFrozen'
    },
    {
      title: '精度',
      dataIndex: 'decimals',
      width: 100
    }
  ];

  const walletColumns: ColumnsType<Wallet> = [
    { title: '链', dataIndex: 'chainName', width: 160 },
    { title: '地址', dataIndex: 'address' },
    {
      title: '类型',
      dataIndex: 'addressType',
      width: 120,
      render: (value) => <Tag>{value}</Tag>
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (value) => <Tag color={value === 'ACTIVE' ? 'green' : 'default'}>{value}</Tag>
    }
  ];

  const depositColumns: ColumnsType<Deposit> = [
    { title: 'Token', dataIndex: 'symbol', width: 100 },
    { title: '数量', dataIndex: 'displayAmount', width: 140 },
    {
      title: '交易 Hash',
      dataIndex: 'txHash',
      ellipsis: true
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 120,
      render: (value) => <Tag color={value === 'CONFIRMED' ? 'green' : 'processing'}>{value}</Tag>
    },
    { title: '确认数', dataIndex: 'confirmationCount', width: 100 },
    {
      title: '发现时间',
      dataIndex: 'detectedAt',
      width: 210,
      render: (value) => new Date(value).toLocaleString()
    }
  ];

  const withdrawalColumns: ColumnsType<Withdrawal> = [
    { title: 'Token', dataIndex: 'symbol', width: 100 },
    { title: '数量', dataIndex: 'displayAmount', width: 140 },
    { title: '手续费', dataIndex: 'displayFee', width: 120 },
    {
      title: '目标地址',
      dataIndex: 'toAddress',
      ellipsis: true
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 160,
      render: (value) => <Tag color={value === 'PENDING_APPROVAL' ? 'gold' : 'processing'}>{value}</Tag>
    },
    {
      title: '申请时间',
      dataIndex: 'requestedAt',
      width: 210,
      render: (value) => new Date(value).toLocaleString()
    }
  ];

  const user = userQuery.data;
  const balances = balancesQuery.data || [];
  const selectedBalance = balances.find((item) => item.tokenId === selectedTokenId);
  const withdrawalAmountPreview = selectedBalance ? safeToBaseUnit(displayAmount || '', selectedBalance.decimals) : '';

  function openWithdrawModal() {
    const firstBalance = balances.find((item) => BigInt(item.available) > 0n) || balances[0];
    withdrawForm.setFieldsValue({
      tokenId: firstBalance?.tokenId,
      displayAmount: '',
      toAddress: ''
    });
    setWithdrawOpen(true);
  }

  async function handleCreateWithdrawal() {
    const values = await withdrawForm.validateFields();
    const balance = balances.find((item) => item.tokenId === values.tokenId);
    if (!balance) {
      message.error('请选择有余额的币种');
      return;
    }
    const amount = toBaseUnit(values.displayAmount, balance.decimals);
    createWithdrawalMutation.mutate({
      tokenId: Number(values.tokenId),
      toAddress: values.toAddress,
      amount
    });
  }

  return (
    <>
      <div className="page-toolbar">
        <Space>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/users')} />
          <div>
            <Typography.Title level={3}>用户详情</Typography.Title>
            <Typography.Text type="secondary">用户资料、余额和后续钱包操作入口。</Typography.Text>
          </div>
        </Space>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={() => balancesQuery.refetch()}>
            刷新余额
          </Button>
          <Button loading={createAddressMutation.isPending} onClick={() => createAddressMutation.mutate()}>
            生成充值地址
          </Button>
          <Button type="primary" icon={<ExportOutlined />} onClick={openWithdrawModal}>
            申请提现
          </Button>
        </Space>
      </div>

      <div className="content-stack">
        <Descriptions bordered column={3}>
          <Descriptions.Item label="用户 ID">{user?.id}</Descriptions.Item>
          <Descriptions.Item label="用户名">{user?.username}</Descriptions.Item>
          <Descriptions.Item label="状态">
            {user?.status ? <Tag color={user.status === 'ACTIVE' ? 'green' : 'default'}>{user.status}</Tag> : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="邮箱">{user?.email || '-'}</Descriptions.Item>
          <Descriptions.Item label="手机">{user?.phone || '-'}</Descriptions.Item>
          <Descriptions.Item label="KYC">L{user?.kycLevel ?? 0}</Descriptions.Item>
        </Descriptions>

        <div>
          <Typography.Title level={4}>资产余额</Typography.Title>
          <Table
            rowKey="tokenId"
            columns={columns}
            dataSource={balancesQuery.data || []}
            loading={balancesQuery.isLoading}
            pagination={false}
            locale={{ emptyText: <Empty description="暂无余额" /> }}
          />
        </div>

        <div>
          <Typography.Title level={4}>充值地址</Typography.Title>
          <Table
            rowKey="id"
            columns={walletColumns}
            dataSource={walletsQuery.data || []}
            loading={walletsQuery.isLoading}
            pagination={false}
            locale={{ emptyText: <Empty description="暂无充值地址" /> }}
          />
        </div>

        <div>
          <Typography.Title level={4}>充值记录</Typography.Title>
          <Table
            rowKey="id"
            columns={depositColumns}
            dataSource={depositsQuery.data || []}
            loading={depositsQuery.isLoading}
            pagination={false}
            locale={{ emptyText: <Empty description="暂无充值记录" /> }}
          />
        </div>

        <div>
          <Typography.Title level={4}>提现记录</Typography.Title>
          <Table
            rowKey="id"
            columns={withdrawalColumns}
            dataSource={withdrawalsQuery.data || []}
            loading={withdrawalsQuery.isLoading}
            pagination={false}
            locale={{ emptyText: <Empty description="暂无提现记录" /> }}
          />
        </div>
      </div>

      <Modal
        title="申请提现"
        open={withdrawOpen}
        okText="提交"
        confirmLoading={createWithdrawalMutation.isPending}
        onOk={handleCreateWithdrawal}
        onCancel={() => setWithdrawOpen(false)}
      >
        <Form form={withdrawForm} layout="vertical">
          <Form.Item label="币种" name="tokenId" rules={[{ required: true, message: '请选择币种' }]}>
            <Select
              placeholder="选择可提现资产"
              options={balances.map((balance) => ({
                value: balance.tokenId,
                label: `${balance.symbol}，可用 ${balance.displayAvailable}`
              }))}
            />
          </Form.Item>
          <Form.Item label="目标地址" name="toAddress" rules={[{ required: true, message: '请输入目标地址' }]}>
            <Input placeholder="输入链上提现地址" />
          </Form.Item>
          <Form.Item
            label="提现数量"
            name="displayAmount"
            rules={[
              { required: true, message: '请输入提现数量' },
              {
                validator: (_, value) => {
                  if (!value) {
                    return Promise.resolve();
                  }
                  if (!selectedBalance) {
                    return Promise.reject(new Error('请选择币种'));
                  }
                  try {
                    const amount = BigInt(toBaseUnit(value, selectedBalance.decimals));
                    const available = BigInt(selectedBalance.available);
                    if (amount <= 0n) {
                      return Promise.reject(new Error('提现数量必须大于 0'));
                    }
                    if (amount > available) {
                      return Promise.reject(new Error('提现数量超过可用余额'));
                    }
                    return Promise.resolve();
                  } catch {
                    return Promise.reject(new Error('提现数量格式不正确'));
                  }
                }
              }
            ]}
          >
            <Input placeholder="例如 0.1" suffix={selectedBalance?.symbol} />
          </Form.Item>
          {selectedBalance ? (
            <Alert
              type="info"
              showIcon
              message={`可用 ${selectedBalance.displayAvailable} ${selectedBalance.symbol}，冻结 ${selectedBalance.displayFrozen} ${selectedBalance.symbol}`}
              description={withdrawalAmountPreview ? `提交金额最小单位：${withdrawalAmountPreview}` : '输入提现数量后会自动换算为链上最小单位'}
            />
          ) : (
            <Alert type="warning" showIcon message="当前用户暂无可提现资产" />
          )}
        </Form>
      </Modal>
    </>
  );
}

function toBaseUnit(value: string, decimals: number): string {
  const normalized = value.trim();
  if (!/^\d+(\.\d+)?$/.test(normalized)) {
    throw new Error('invalid decimal amount');
  }
  const [integerPart, decimalPart = ''] = normalized.split('.');
  if (decimalPart.length > decimals) {
    throw new Error('too many decimal places');
  }
  const paddedDecimal = decimalPart.padEnd(decimals, '0');
  const combined = `${integerPart}${paddedDecimal}`.replace(/^0+(?=\d)/, '');
  return combined || '0';
}

function safeToBaseUnit(value: string, decimals: number): string {
  if (!value.trim()) {
    return '';
  }
  try {
    return toBaseUnit(value, decimals);
  } catch {
    return '';
  }
}
