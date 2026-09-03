import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Form, Input, InputNumber, Modal, Select, Space, Switch, Table, Tabs, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useState } from 'react';
import {
  createPlatformWallet,
  disablePlatformWallet,
  listChains,
  listPlatformWallets,
  listTokens,
  updateChain,
  updatePlatformWallet,
  updateToken
} from '../api/assets';
import type { ChainAsset, PlatformWallet, TokenAsset } from '../api/assets';

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

interface PlatformWalletFormValues {
  chainId: number;
  tokenId?: number;
  address: string;
  walletRole: string;
  status: string;
  remark?: string;
}

export function AssetManagementPage() {
  const [editingChain, setEditingChain] = useState<ChainAsset>();
  const [editingToken, setEditingToken] = useState<TokenAsset>();
  const [editingWallet, setEditingWallet] = useState<PlatformWallet>();
  const [walletOpen, setWalletOpen] = useState(false);
  const [chainForm] = Form.useForm<ChainFormValues>();
  const [tokenForm] = Form.useForm<TokenFormValues>();
  const [walletForm] = Form.useForm<PlatformWalletFormValues>();
  const selectedWalletChainId = Form.useWatch('chainId', walletForm);
  const queryClient = useQueryClient();

  const chainsQuery = useQuery({
    queryKey: ['assets', 'chains'],
    queryFn: listChains
  });
  const tokensQuery = useQuery({
    queryKey: ['assets', 'tokens'],
    queryFn: listTokens
  });
  const platformWalletsQuery = useQuery({
    queryKey: ['assets', 'platform-wallets'],
    queryFn: listPlatformWallets
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

  const createWalletMutation = useMutation({
    mutationFn: (values: PlatformWalletFormValues) => createPlatformWallet(toCreateWalletInput(values)),
    onSuccess: async () => {
      message.success('平台钱包已新增');
      closeWalletModal();
      await queryClient.invalidateQueries({ queryKey: ['assets', 'platform-wallets'] });
      await queryClient.invalidateQueries({ queryKey: ['reconciliation', 'tokens'] });
      await queryClient.invalidateQueries({ queryKey: ['audit-logs'] });
    },
    onError: (error) => showRequestError(error, '新增平台钱包失败')
  });

  const updateWalletMutation = useMutation({
    mutationFn: ({ id, values }: { id: number; values: PlatformWalletFormValues }) =>
      updatePlatformWallet(id, toUpdateWalletInput(values)),
    onSuccess: async () => {
      message.success('平台钱包已保存');
      closeWalletModal();
      await queryClient.invalidateQueries({ queryKey: ['assets', 'platform-wallets'] });
      await queryClient.invalidateQueries({ queryKey: ['reconciliation', 'tokens'] });
      await queryClient.invalidateQueries({ queryKey: ['audit-logs'] });
    },
    onError: (error) => showRequestError(error, '保存平台钱包失败')
  });

  const disableWalletMutation = useMutation({
    mutationFn: disablePlatformWallet,
    onSuccess: async () => {
      message.success('平台钱包已停用');
      await queryClient.invalidateQueries({ queryKey: ['assets', 'platform-wallets'] });
      await queryClient.invalidateQueries({ queryKey: ['reconciliation', 'tokens'] });
      await queryClient.invalidateQueries({ queryKey: ['audit-logs'] });
    },
    onError: (error) => showRequestError(error, '停用平台钱包失败')
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

  const walletColumns: ColumnsType<PlatformWallet> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '链', dataIndex: 'chainName', width: 140 },
    { title: 'Token', dataIndex: 'tokenSymbol', width: 120, render: (value) => value || '链级钱包' },
    {
      title: '角色',
      dataIndex: 'walletRole',
      width: 120,
      render: (value) => <Tag color={value === 'HOT' ? 'orange' : 'blue'}>{walletRoleLabel(value)}</Tag>
    },
    { title: '地址', dataIndex: 'address', ellipsis: true },
    {
      title: '状态',
      dataIndex: 'status',
      width: 110,
      render: (value) => <Tag color={value === 'ACTIVE' ? 'green' : 'default'}>{value}</Tag>
    },
    { title: '备注', dataIndex: 'remark', ellipsis: true, render: (value) => value || '-' },
    {
      title: '操作',
      width: 170,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => openWalletModal(record)}>
            编辑
          </Button>
          <Button
            danger
            size="small"
            icon={<DeleteOutlined />}
            disabled={record.status !== 'ACTIVE'}
            loading={disableWalletMutation.isPending}
            onClick={() => disableWalletMutation.mutate(record.id)}
          >
            停用
          </Button>
        </Space>
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

  function openWalletModal(wallet?: PlatformWallet) {
    setEditingWallet(wallet);
    setWalletOpen(true);
    walletForm.setFieldsValue(
      wallet
        ? {
            chainId: wallet.chainId,
            tokenId: wallet.tokenId,
            address: wallet.address,
            walletRole: wallet.walletRole,
            status: wallet.status,
            remark: wallet.remark
          }
        : {
            chainId: chainsQuery.data?.[0]?.id,
            tokenId: undefined,
            address: '',
            walletRole: 'HOT',
            status: 'ACTIVE',
            remark: ''
          }
    );
  }

  function closeWalletModal() {
    setEditingWallet(undefined);
    setWalletOpen(false);
    walletForm.resetFields();
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

  async function handleSaveWallet() {
    const values = await walletForm.validateFields();
    if (editingWallet) {
      updateWalletMutation.mutate({ id: editingWallet.id, values });
      return;
    }
    createWalletMutation.mutate(values);
  }

  const walletTokenOptions = (tokensQuery.data || [])
    .filter((token) => !selectedWalletChainId || token.chainId === selectedWalletChainId)
    .map((token) => ({ value: token.id, label: `${token.symbol} / ${token.chainName}` }));

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
            platformWalletsQuery.refetch();
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
          },
          {
            key: 'platform-wallets',
            label: '平台钱包',
            children: (
              <Space direction="vertical" className="full-width" size={16}>
                <div className="table-toolbar">
                  <Typography.Text type="secondary">配置平台热钱包、冷钱包和归集钱包。账务对账会优先读取 ACTIVE 的 HOT 钱包。</Typography.Text>
                  <Button type="primary" icon={<PlusOutlined />} onClick={() => openWalletModal()}>
                    新增钱包
                  </Button>
                </div>
                <Table
                  rowKey="id"
                  columns={walletColumns}
                  dataSource={platformWalletsQuery.data || []}
                  loading={platformWalletsQuery.isLoading}
                  pagination={false}
                  scroll={{ x: 1200 }}
                />
              </Space>
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

      <Modal
        title={editingWallet ? `编辑平台钱包 #${editingWallet.id}` : '新增平台钱包'}
        open={walletOpen}
        okText="保存"
        confirmLoading={createWalletMutation.isPending || updateWalletMutation.isPending}
        onOk={handleSaveWallet}
        onCancel={closeWalletModal}
      >
        <Form form={walletForm} layout="vertical">
          <Form.Item label="链" name="chainId" rules={[{ required: true, message: '请选择链' }]}>
            <Select
              disabled={Boolean(editingWallet)}
              options={(chainsQuery.data || []).map((chain) => ({ value: chain.id, label: `${chain.name} / ${chain.chainId}` }))}
            />
          </Form.Item>
          <Form.Item label="Token" name="tokenId">
            <Select
              allowClear
              disabled={Boolean(editingWallet)}
              placeholder="不选表示链级钱包"
              options={walletTokenOptions}
            />
          </Form.Item>
          <Form.Item label="钱包角色" name="walletRole" rules={[{ required: true, message: '请选择钱包角色' }]}>
            <Select
              options={[
                { value: 'HOT', label: 'HOT / 热钱包' },
                { value: 'COLD', label: 'COLD / 冷钱包' },
                { value: 'COLLECTION', label: 'COLLECTION / 归集钱包' },
                { value: 'FEE', label: 'FEE / 手续费钱包' }
              ]}
            />
          </Form.Item>
          <Form.Item label="钱包地址" name="address" rules={[{ required: true, message: '请输入钱包地址' }]}>
            <Input placeholder="例如 0x..." />
          </Form.Item>
          <Form.Item label="状态" name="status" rules={[{ required: true, message: '请选择状态' }]}>
            <Select
              options={[
                { value: 'ACTIVE', label: 'ACTIVE' },
                { value: 'INACTIVE', label: 'INACTIVE' }
              ]}
            />
          </Form.Item>
          <Form.Item label="备注" name="remark">
            <Input.TextArea rows={3} placeholder="例如 Anvil 开发热钱包" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}

function toCreateWalletInput(values: PlatformWalletFormValues) {
  return {
    chainId: values.chainId,
    tokenId: values.tokenId || undefined,
    address: values.address.trim(),
    walletRole: values.walletRole,
    status: values.status,
    remark: values.remark?.trim()
  };
}

function toUpdateWalletInput(values: PlatformWalletFormValues) {
  return {
    address: values.address.trim(),
    walletRole: values.walletRole,
    status: values.status,
    remark: values.remark?.trim()
  };
}

function walletRoleLabel(role: string) {
  const labels: Record<string, string> = {
    HOT: '热钱包',
    COLD: '冷钱包',
    COLLECTION: '归集钱包',
    FEE: '手续费钱包'
  };
  return labels[role] || role;
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
