import { CheckOutlined, CloseOutlined, CloudUploadOutlined, ReloadOutlined, RollbackOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Form, Input, Modal, Select, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useState } from 'react';
import {
  approveWithdrawal,
  broadcastWithdrawal,
  confirmWithdrawal,
  failWithdrawal,
  listWithdrawals,
  rejectWithdrawal
} from '../api/withdrawals';
import type { Withdrawal } from '../api/users';

export function WithdrawalReviewPage() {
  const [rejecting, setRejecting] = useState<Withdrawal>();
  const [confirming, setConfirming] = useState<Withdrawal>();
  const [failing, setFailing] = useState<Withdrawal>();
  const [status, setStatus] = useState('ALL');
  const [form] = Form.useForm<{ reason: string }>();
  const [confirmForm] = Form.useForm<{ txHash: string }>();
  const [failForm] = Form.useForm<{ reason: string }>();
  const queryClient = useQueryClient();
  const withdrawalsQuery = useQuery({
    queryKey: ['withdrawals', status],
    queryFn: () => listWithdrawals(status)
  });

  const approveMutation = useMutation({
    mutationFn: approveWithdrawal,
    onSuccess: async () => {
      message.success('提现已批准');
      await queryClient.invalidateQueries({ queryKey: ['withdrawals'] });
      setStatus('APPROVED');
    },
    onError: (error) => showRequestError(error, '批准失败')
  });

  const rejectMutation = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason?: string }) => rejectWithdrawal(id, reason),
    onSuccess: async () => {
      message.success('提现已拒绝，冻结余额已退回');
      setRejecting(undefined);
      form.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['withdrawals'] });
    },
    onError: (error) => showRequestError(error, '拒绝失败')
  });

  const confirmMutation = useMutation({
    mutationFn: ({ id, txHash }: { id: number; txHash?: string }) => confirmWithdrawal(id, txHash),
    onSuccess: async () => {
      message.success('提现已确认，冻结余额已扣除');
      setConfirming(undefined);
      confirmForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['withdrawals'] });
    },
    onError: (error) => showRequestError(error, '确认失败')
  });

  const failMutation = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason?: string }) => failWithdrawal(id, reason),
    onSuccess: async () => {
      message.success('提现已标记失败，冻结余额已退回');
      setFailing(undefined);
      failForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['withdrawals'] });
    },
    onError: (error) => showRequestError(error, '标记失败失败')
  });

  const broadcastMutation = useMutation({
    mutationFn: broadcastWithdrawal,
    onSuccess: async () => {
      message.success('提现已广播');
      setStatus('BROADCASTED');
      await queryClient.invalidateQueries({ queryKey: ['withdrawals'] });
      await queryClient.refetchQueries({ queryKey: ['withdrawals', 'BROADCASTED'] });
    },
    onError: (error) => showRequestError(error, '广播失败，请确认 signer 服务已启动')
  });

  const columns: ColumnsType<Withdrawal> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '用户 ID', dataIndex: 'userId', width: 100 },
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
      width: 150,
      render: (value) => <Tag color={statusColor(value)}>{value}</Tag>
    },
    {
      title: '申请时间',
      dataIndex: 'requestedAt',
      width: 210,
      render: (value) => new Date(value).toLocaleString()
    },
    {
      title: '操作',
      width: 240,
      render: (_, record) => (
        <Space>
          {record.status === 'PENDING_APPROVAL' ? (
            <>
              <Button
                size="small"
                type="primary"
                icon={<CheckOutlined />}
                loading={approveMutation.isPending}
                onClick={() => approveMutation.mutate(record.id)}
              >
                批准
              </Button>
              <Button size="small" danger icon={<CloseOutlined />} onClick={() => setRejecting(record)}>
                拒绝
              </Button>
            </>
          ) : null}
          {record.status === 'APPROVED' ? (
            <>
              <Button
                size="small"
                type="primary"
                icon={<CloudUploadOutlined />}
                loading={broadcastMutation.isPending}
                onClick={() => broadcastMutation.mutate(record.id)}
              >
                广播
              </Button>
              <Button size="small" icon={<RollbackOutlined />} onClick={() => setFailing(record)}>
                失败退款
              </Button>
            </>
          ) : null}
          {record.status === 'BROADCASTED' ? (
            <>
              <Button size="small" type="primary" icon={<CheckOutlined />} onClick={() => setConfirming(record)}>
                确认成功
              </Button>
              <Button size="small" icon={<RollbackOutlined />} onClick={() => setFailing(record)}>
                失败退款
              </Button>
            </>
          ) : null}
          {!['PENDING_APPROVAL', 'APPROVED', 'BROADCASTED'].includes(record.status) ? (
            <Typography.Text type="secondary">无操作</Typography.Text>
          ) : null}
        </Space>
      )
    }
  ];

  async function handleReject() {
    if (!rejecting) {
      return;
    }
    const values = await form.validateFields();
    rejectMutation.mutate({ id: rejecting.id, reason: values.reason });
  }

  async function handleConfirm() {
    if (!confirming) {
      return;
    }
    const values = await confirmForm.validateFields();
    confirmMutation.mutate({ id: confirming.id, txHash: values.txHash });
  }

  async function handleFail() {
    if (!failing) {
      return;
    }
    const values = await failForm.validateFields();
    failMutation.mutate({ id: failing.id, reason: values.reason });
  }

  return (
    <>
      <div className="page-toolbar">
        <div>
          <Typography.Title level={3}>提现审核</Typography.Title>
          <Typography.Text type="secondary">处理待审核提现申请。</Typography.Text>
        </div>
        <Space>
          <Select
            value={status}
            onChange={setStatus}
            options={[
              { value: 'ALL', label: '全部' },
              { value: 'PENDING_APPROVAL', label: '待审核' },
              { value: 'APPROVED', label: '已批准' },
              { value: 'BROADCASTED', label: '已广播' },
              { value: 'CONFIRMED', label: '已确认' },
              { value: 'REJECTED', label: '已拒绝' },
              { value: 'FAILED', label: '失败已退款' }
            ]}
            style={{ width: 140 }}
          />
          <Button icon={<ReloadOutlined />} onClick={() => withdrawalsQuery.refetch()}>
            刷新
          </Button>
        </Space>
      </div>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={withdrawalsQuery.data || []}
        loading={withdrawalsQuery.isLoading}
        pagination={{ pageSize: 20 }}
      />

      <Modal
        title="拒绝提现"
        open={Boolean(rejecting)}
        okText="确认拒绝"
        okButtonProps={{ danger: true }}
        confirmLoading={rejectMutation.isPending}
        onOk={handleReject}
        onCancel={() => setRejecting(undefined)}
      >
        <Form form={form} layout="vertical">
          <Form.Item label="拒绝原因" name="reason" rules={[{ required: true, message: '请输入拒绝原因' }]}>
            <Input.TextArea rows={4} placeholder="例如：地址风险过高、用户资料待补充" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="确认链上成功"
        open={Boolean(confirming)}
        okText="确认"
        confirmLoading={confirmMutation.isPending}
        onOk={handleConfirm}
        onCancel={() => setConfirming(undefined)}
      >
        <Form form={confirmForm} layout="vertical">
          <Form.Item label="交易 Hash" name="txHash" rules={[{ required: true, message: '请输入交易 Hash' }]}>
            <Input placeholder="例如 0x..." />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="提现失败退款"
        open={Boolean(failing)}
        okText="确认退款"
        okButtonProps={{ danger: true }}
        confirmLoading={failMutation.isPending}
        onOk={handleFail}
        onCancel={() => setFailing(undefined)}
      >
        <Typography.Paragraph type="secondary">
          适用于链上广播失败、交易丢弃或人工确认不会成功的提现单。确认后会把冻结的提现金额和手续费退回用户可用余额。
        </Typography.Paragraph>
        <Form form={failForm} layout="vertical">
          <Form.Item label="失败原因" name="reason" rules={[{ required: true, message: '请输入失败原因' }]}>
            <Input.TextArea rows={4} placeholder="例如：链上交易失败、手续费不足、交易长时间未打包" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}

function statusColor(status: string) {
  if (status === 'PENDING_APPROVAL') {
    return 'gold';
  }
  if (status === 'APPROVED') {
    return 'blue';
  }
  if (status === 'BROADCASTED') {
    return 'processing';
  }
  if (status === 'CONFIRMED') {
    return 'green';
  }
  if (status === 'REJECTED') {
    return 'red';
  }
  if (status === 'FAILED') {
    return 'red';
  }
  return 'default';
}

function showRequestError(error: unknown, fallback: string) {
  if (typeof error === 'object' && error && 'response' in error) {
    const response = (error as { response?: { data?: { error?: { message?: string }; message?: string } } }).response;
    const apiMessage = response?.data?.error?.message || response?.data?.message;
    if (apiMessage) {
      message.error(`${fallback}：${apiMessage}`);
      return;
    }
  }
  if (error instanceof Error && error.message) {
    message.error(`${fallback}：${error.message}`);
    return;
  }
  message.error(fallback);
}
