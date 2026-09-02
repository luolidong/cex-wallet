import { CheckOutlined, CloseOutlined, ReloadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Form, Input, Modal, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useState } from 'react';
import { approveWithdrawal, listWithdrawals, rejectWithdrawal } from '../api/withdrawals';
import type { Withdrawal } from '../api/users';

export function WithdrawalReviewPage() {
  const [rejecting, setRejecting] = useState<Withdrawal>();
  const [form] = Form.useForm<{ reason: string }>();
  const queryClient = useQueryClient();
  const withdrawalsQuery = useQuery({
    queryKey: ['withdrawals', 'PENDING_APPROVAL'],
    queryFn: () => listWithdrawals('PENDING_APPROVAL')
  });

  const approveMutation = useMutation({
    mutationFn: approveWithdrawal,
    onSuccess: async () => {
      message.success('提现已批准');
      await queryClient.invalidateQueries({ queryKey: ['withdrawals'] });
    }
  });

  const rejectMutation = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason?: string }) => rejectWithdrawal(id, reason),
    onSuccess: async () => {
      message.success('提现已拒绝，冻结余额已退回');
      setRejecting(undefined);
      form.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['withdrawals'] });
    }
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
      render: (value) => <Tag color="gold">{value}</Tag>
    },
    {
      title: '申请时间',
      dataIndex: 'requestedAt',
      width: 210,
      render: (value) => new Date(value).toLocaleString()
    },
    {
      title: '操作',
      width: 180,
      render: (_, record) => (
        <Space>
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

  return (
    <>
      <div className="page-toolbar">
        <div>
          <Typography.Title level={3}>提现审核</Typography.Title>
          <Typography.Text type="secondary">处理待审核提现申请。</Typography.Text>
        </div>
        <Button icon={<ReloadOutlined />} onClick={() => withdrawalsQuery.refetch()}>
          刷新
        </Button>
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
    </>
  );
}
