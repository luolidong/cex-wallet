import { EditOutlined, ReloadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Form, Input, InputNumber, Modal, Select, Space, Switch, Table, Tabs, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useState } from 'react';
import { listChains, listTokens, updateChain, updateToken } from '../api/assets';
import type { ChainAsset, TokenAsset } from '../api/assets';

interface ChainFormValues {
  name: string;
  rpcUrl: string;
  explorerUrl?: string;
  confirmBlocks: number;
  scanEnabled: boolean;
  withdrawEnabled: boolean;
  status: string;
}

interface TokenFormValues {
  name: string;
  tokenAddress?: string;
  displayMinDepositAmount: string;
  displayMinWithdrawAmount: string;
  displayWithdrawFee: string;
  depositEnabled: boolean;
  withdrawEnabled: boolean;
  status: string;
}

export function AssetManagementPage() {
  const [editingChain, setEditingChain] = useState<ChainAsset>();
  const [editingToken, setEditingToken] = useState<TokenAsset>();
  const [chainForm] = Form.useForm<ChainFormValues>();
  const [tokenForm] = Form.useForm<TokenFormValues>();
  const queryClient = useQueryClient();

  const chainsQuery = useQuery({
    queryKey: ['assets', 'chains'],
    queryFn: listChains
  });
  const tokensQuery = useQuery({
    queryKey: ['assets', 'tokens'],
    queryFn: listTokens
  });

  const updateChainMutation = useMutation({
    mutationFn: ({ id, values }: { id: number; values: ChainFormValues }) => updateChain(id, values),
    onSuccess: async () => {
      message.success('链配置已保存');
      setEditingChain(undefined);
      chainForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['assets'] });
      await queryClient.invalidateQueries({ queryKey: ['risk', 'chains'] });
    },
    onError: (error) => showRequestError(error, '保存链配置失败')
  });

  const updateTokenMutation = useMutation({
    mutationFn: ({ id, values }: { id: number; values: TokenFormValues }) => {
      const token = tokensQuery.data?.find((item) => item.id === id);
      if (!token) {
        throw new Error('token not found');
      }
      return updateToken(id, {
        name: values.name,
        tokenAddress: values.tokenAddress,
        minDepositAmount: toBaseUnit(values.displayMinDepositAmount, token.decimals),
        minWithdrawAmount: toBaseUnit(values.displayMinWithdrawAmount, token.decimals),
        withdrawFee: toBaseUnit(values.displayWithdrawFee, token.decimals),
        depositEnabled: values.depositEnabled,
        withdrawEnabled: values.withdrawEnabled,
        status: values.status
      });
    },
    onSuccess: async () => {
      message.success('Token 配置已保存');
      setEditingToken(undefined);
      tokenForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['assets', 'tokens'] });
      await queryClient.invalidateQueries({ queryKey: ['risk', 'withdrawal-rules'] });
    },
    onError: (error) => showRequestError(error, '保存 Token 配置失败')
  });

  const chainColumns: ColumnsType<ChainAsset> = [
    { title: '链', dataIndex: 'name', width: 180 },
    { title: '类型', dataIndex: 'chainType', width: 110 },
    { title: '链 ID', dataIndex: 'chainId', width: 110 },
    { title: '确认块数', dataIndex: 'confirmBlocks', width: 110 },
    { title: 'RPC', dataIndex: 'rpcUrl', ellipsis: true },
    {
      title: '扫描',
      dataIndex: 'scanEnabled',
      width: 90,
      render: (value) => <Tag color={value ? 'green' : 'default'}>{value ? '启用' : '停用'}</Tag>
    },
    {
      title: '提现',
      dataIndex: 'withdrawEnabled',
      width: 90,
      render: (value) => <Tag color={value ? 'green' : 'default'}>{value ? '启用' : '停用'}</Tag>
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (value) => <Tag color={value === 'ACTIVE' ? 'green' : 'default'}>{value}</Tag>
    },
    {
      title: '操作',
      width: 100,
      render: (_, record) => (
        <Button size="small" icon={<EditOutlined />} onClick={() => openChainModal(record)}>
          编辑
        </Button>
      )
    }
  ];

  const tokenColumns: ColumnsType<TokenAsset> = [
    { title: 'Token', dataIndex: 'symbol', width: 100 },
    { title: '名称', dataIndex: 'name', width: 150 },
    { title: '链', dataIndex: 'chainName', width: 150 },
    { title: '类型', dataIndex: 'tokenType', width: 100 },
    { title: '精度', dataIndex: 'decimals', width: 80 },
    { title: '最小充值', dataIndex: 'displayMinDepositAmount', width: 120 },
    { title: '最小提现', dataIndex: 'displayMinWithdrawAmount', width: 120 },
    { title: '提现手续费', dataIndex: 'displayWithdrawFee', width: 130 },
    {
      title: '充值',
      dataIndex: 'depositEnabled',
      width: 90,
      render: (value) => <Tag color={value ? 'green' : 'default'}>{value ? '启用' : '停用'}</Tag>
    },
    {
      title: '提现',
      dataIndex: 'withdrawEnabled',
      width: 90,
      render: (value) => <Tag color={value ? 'green' : 'default'}>{value ? '启用' : '停用'}</Tag>
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (value) => <Tag color={value === 'ACTIVE' ? 'green' : 'default'}>{value}</Tag>
    },
    {
      title: '操作',
      width: 100,
      fixed: 'right',
      render: (_, record) => (
        <Button size="small" icon={<EditOutlined />} onClick={() => openTokenModal(record)}>
          编辑
        </Button>
      )
    }
  ];

  function openChainModal(chain: ChainAsset) {
    setEditingChain(chain);
    chainForm.setFieldsValue({
      name: chain.name,
      rpcUrl: chain.rpcUrl,
      explorerUrl: chain.explorerUrl,
      confirmBlocks: chain.confirmBlocks,
      scanEnabled: chain.scanEnabled,
      withdrawEnabled: chain.withdrawEnabled,
      status: chain.status
    });
  }

  function openTokenModal(token: TokenAsset) {
    setEditingToken(token);
    tokenForm.setFieldsValue({
      name: token.name,
      tokenAddress: token.tokenAddress,
      displayMinDepositAmount: token.displayMinDepositAmount,
      displayMinWithdrawAmount: token.displayMinWithdrawAmount,
      displayWithdrawFee: token.displayWithdrawFee,
      depositEnabled: token.depositEnabled,
      withdrawEnabled: token.withdrawEnabled,
      status: token.status
    });
  }

  async function handleSaveChain() {
    if (!editingChain) {
      return;
    }
    const values = await chainForm.validateFields();
    updateChainMutation.mutate({ id: editingChain.id, values });
  }

  async function handleSaveToken() {
    if (!editingToken) {
      return;
    }
    const values = await tokenForm.validateFields();
    updateTokenMutation.mutate({ id: editingToken.id, values });
  }

  return (
    <>
      <div className="page-toolbar">
        <div>
          <Typography.Title level={3}>资产管理</Typography.Title>
          <Typography.Text type="secondary">维护链、Token、充值和提现基础配置。</Typography.Text>
        </div>
        <Button
          icon={<ReloadOutlined />}
          onClick={() => {
            chainsQuery.refetch();
            tokensQuery.refetch();
          }}
        >
          刷新
        </Button>
      </div>

      <Tabs
        items={[
          {
            key: 'chains',
            label: '链配置',
            children: (
              <Table
                rowKey="id"
                columns={chainColumns}
                dataSource={chainsQuery.data || []}
                loading={chainsQuery.isLoading}
                pagination={false}
              />
            )
          },
          {
            key: 'tokens',
            label: 'Token 配置',
            children: (
              <Table
                rowKey="id"
                columns={tokenColumns}
                dataSource={tokensQuery.data || []}
                loading={tokensQuery.isLoading}
                pagination={false}
                scroll={{ x: 1300 }}
              />
            )
          }
        ]}
      />

      <Modal
        title={editingChain ? `编辑 ${editingChain.name}` : '编辑链配置'}
        open={Boolean(editingChain)}
        okText="保存"
        confirmLoading={updateChainMutation.isPending}
        onOk={handleSaveChain}
        onCancel={() => setEditingChain(undefined)}
      >
        <Form form={chainForm} layout="vertical">
          <Form.Item label="链名称" name="name" rules={[{ required: true, message: '请输入链名称' }]}>
            <Input placeholder="例如 Ethereum" />
          </Form.Item>
          <Form.Item label="RPC URL" name="rpcUrl" rules={[{ required: true, message: '请输入 RPC URL' }]}>
            <Input placeholder="例如 http://localhost:8545" />
          </Form.Item>
          <Form.Item label="浏览器 URL" name="explorerUrl">
            <Input placeholder="例如 https://etherscan.io" />
          </Form.Item>
          <Form.Item label="确认块数" name="confirmBlocks" rules={[{ required: true, message: '请输入确认块数' }]}>
            <InputNumber className="full-width" min={1} precision={0} />
          </Form.Item>
          <Space size="large">
            <Form.Item label="扫描" name="scanEnabled" valuePropName="checked">
              <Switch checkedChildren="启用" unCheckedChildren="停用" />
            </Form.Item>
            <Form.Item label="提现" name="withdrawEnabled" valuePropName="checked">
              <Switch checkedChildren="启用" unCheckedChildren="停用" />
            </Form.Item>
          </Space>
          <Form.Item label="状态" name="status" rules={[{ required: true, message: '请选择状态' }]}>
            <Select
              options={[
                { value: 'ACTIVE', label: 'ACTIVE' },
                { value: 'INACTIVE', label: 'INACTIVE' }
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={editingToken ? `编辑 ${editingToken.symbol}` : '编辑 Token 配置'}
        open={Boolean(editingToken)}
        okText="保存"
        confirmLoading={updateTokenMutation.isPending}
        onOk={handleSaveToken}
        onCancel={() => setEditingToken(undefined)}
      >
        <Form form={tokenForm} layout="vertical">
          <Form.Item label="名称" name="name" rules={[{ required: true, message: '请输入名称' }]}>
            <Input placeholder="例如 Tether USD" />
          </Form.Item>
          <Form.Item label="合约地址" name="tokenAddress">
            <Input disabled={editingToken?.isNative} placeholder={editingToken?.isNative ? '原生资产不需要合约地址' : '例如 0x...'} />
          </Form.Item>
          <Form.Item label="最小充值" name="displayMinDepositAmount" rules={[{ required: true, message: '请输入最小充值' }]}>
            <Input placeholder="例如 1" suffix={editingToken?.symbol} />
          </Form.Item>
          <Form.Item label="最小提现" name="displayMinWithdrawAmount" rules={[{ required: true, message: '请输入最小提现' }]}>
            <Input placeholder="例如 1" suffix={editingToken?.symbol} />
          </Form.Item>
          <Form.Item label="提现手续费" name="displayWithdrawFee" rules={[{ required: true, message: '请输入提现手续费' }]}>
            <Input placeholder="例如 0.1" suffix={editingToken?.symbol} />
          </Form.Item>
          <Space size="large">
            <Form.Item label="充值" name="depositEnabled" valuePropName="checked">
              <Switch checkedChildren="启用" unCheckedChildren="停用" />
            </Form.Item>
            <Form.Item label="提现" name="withdrawEnabled" valuePropName="checked">
              <Switch checkedChildren="启用" unCheckedChildren="停用" />
            </Form.Item>
          </Space>
          <Form.Item label="状态" name="status" rules={[{ required: true, message: '请选择状态' }]}>
            <Select
              options={[
                { value: 'ACTIVE', label: 'ACTIVE' },
                { value: 'INACTIVE', label: 'INACTIVE' }
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}

function toBaseUnit(value: string, decimals: number): string {
  const normalized = value.trim();
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
  const err = error as { response?: { data?: { error?: { code?: string; message?: string; details?: string } } }; message?: string };
  const apiError = err.response?.data?.error;
  const messageText = apiError?.details || apiError?.message || err.message || fallback;
  message.error(apiError?.code ? `${apiError.code}: ${messageText}` : messageText);
}
