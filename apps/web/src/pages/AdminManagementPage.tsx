import { EditOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Form, Input, Modal, Select, Space, Table, Tabs, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useState } from 'react';
import {
  createAdminAccount,
  listAdminAccounts,
  listPermissions,
  listRoles,
  updateAdminAccountRoles,
  updateAdminAccountStatus,
  updateRolePermissions
} from '../api/adminManagement';
import type { AdminAccount, Role } from '../api/adminManagement';

interface AdminFormValues {
  username: string;
  password: string;
  displayName?: string;
  roles: string[];
}

interface AdminRolesFormValues {
  roles: string[];
}

interface AdminStatusFormValues {
  status: string;
}

interface RolePermissionsFormValues {
  permissions: string[];
}

export function AdminManagementPage() {
  const [createAdminOpen, setCreateAdminOpen] = useState(false);
  const [editingAdminRoles, setEditingAdminRoles] = useState<AdminAccount>();
  const [editingAdminStatus, setEditingAdminStatus] = useState<AdminAccount>();
  const [editingRole, setEditingRole] = useState<Role>();
  const [adminForm] = Form.useForm<AdminFormValues>();
  const [adminRolesForm] = Form.useForm<AdminRolesFormValues>();
  const [adminStatusForm] = Form.useForm<AdminStatusFormValues>();
  const [rolePermissionsForm] = Form.useForm<RolePermissionsFormValues>();
  const queryClient = useQueryClient();

  const permissionsQuery = useQuery({
    queryKey: ['admin-management', 'permissions'],
    queryFn: listPermissions
  });
  const rolesQuery = useQuery({
    queryKey: ['admin-management', 'roles'],
    queryFn: listRoles
  });
  const adminsQuery = useQuery({
    queryKey: ['admin-management', 'admins'],
    queryFn: listAdminAccounts
  });

  const createAdminMutation = useMutation({
    mutationFn: createAdminAccount,
    onSuccess: async () => {
      message.success('后台账号已创建');
      setCreateAdminOpen(false);
      adminForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['admin-management', 'admins'] });
      await queryClient.invalidateQueries({ queryKey: ['audit-logs'] });
    },
    onError: (error) => showRequestError(error, '创建后台账号失败')
  });

  const updateAdminRolesMutation = useMutation({
    mutationFn: ({ id, roles }: { id: number; roles: string[] }) => updateAdminAccountRoles(id, roles),
    onSuccess: async () => {
      message.success('账号角色已保存');
      setEditingAdminRoles(undefined);
      adminRolesForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['admin-management', 'admins'] });
      await queryClient.invalidateQueries({ queryKey: ['audit-logs'] });
    },
    onError: (error) => showRequestError(error, '保存账号角色失败')
  });

  const updateAdminStatusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) => updateAdminAccountStatus(id, status),
    onSuccess: async () => {
      message.success('账号状态已保存');
      setEditingAdminStatus(undefined);
      adminStatusForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['admin-management', 'admins'] });
      await queryClient.invalidateQueries({ queryKey: ['audit-logs'] });
    },
    onError: (error) => showRequestError(error, '保存账号状态失败')
  });

  const updateRolePermissionsMutation = useMutation({
    mutationFn: ({ roleCode, permissions }: { roleCode: string; permissions: string[] }) =>
      updateRolePermissions(roleCode, permissions),
    onSuccess: async () => {
      message.success('角色权限已保存');
      setEditingRole(undefined);
      rolePermissionsForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['admin-management'] });
      await queryClient.invalidateQueries({ queryKey: ['audit-logs'] });
    },
    onError: (error) => showRequestError(error, '保存角色权限失败')
  });

  const roleOptions = (rolesQuery.data || []).map((role) => ({ value: role.code, label: `${role.name} / ${role.code}` }));
  const permissionOptions = (permissionsQuery.data || []).map((permission) => ({
    value: permission.code,
    label: `${permission.name} / ${permission.code}`
  }));

  const adminColumns: ColumnsType<AdminAccount> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '用户名', dataIndex: 'username', width: 150 },
    { title: '显示名', dataIndex: 'displayName', width: 150, render: (value) => value || '-' },
    {
      title: '状态',
      dataIndex: 'status',
      width: 110,
      render: (value) => <Tag color={value === 'ACTIVE' ? 'green' : 'default'}>{value}</Tag>
    },
    {
      title: '角色',
      dataIndex: 'roles',
      width: 220,
      render: (roles: string[]) => renderTags(roles, 'blue')
    },
    {
      title: '权限',
      dataIndex: 'permissions',
      render: (permissions: string[]) => renderTags(permissions, 'default', 6)
    },
    {
      title: '最近登录',
      dataIndex: 'lastLoginAt',
      width: 190,
      render: (value) => (value ? new Date(value).toLocaleString() : '-')
    },
    {
      title: '操作',
      width: 180,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => openAdminRolesModal(record)}>
            角色
          </Button>
          <Button size="small" onClick={() => openAdminStatusModal(record)}>
            状态
          </Button>
        </Space>
      )
    }
  ];

  const roleColumns: ColumnsType<Role> = [
    { title: '角色', dataIndex: 'name', width: 160 },
    { title: 'Code', dataIndex: 'code', width: 160 },
    {
      title: '权限',
      dataIndex: 'permissions',
      render: (permissions: string[]) => renderTags(permissions, 'default')
    },
    {
      title: '操作',
      width: 110,
      render: (_, record) => (
        <Button size="small" icon={<EditOutlined />} onClick={() => openRolePermissionsModal(record)}>
          权限
        </Button>
      )
    }
  ];

  function openAdminRolesModal(admin: AdminAccount) {
    setEditingAdminRoles(admin);
    adminRolesForm.setFieldsValue({ roles: admin.roles });
  }

  function openAdminStatusModal(admin: AdminAccount) {
    setEditingAdminStatus(admin);
    adminStatusForm.setFieldsValue({ status: admin.status });
  }

  function openRolePermissionsModal(role: Role) {
    setEditingRole(role);
    rolePermissionsForm.setFieldsValue({ permissions: role.permissions });
  }

  async function handleCreateAdmin() {
    const values = await adminForm.validateFields();
    createAdminMutation.mutate(values);
  }

  async function handleSaveAdminRoles() {
    if (!editingAdminRoles) {
      return;
    }
    const values = await adminRolesForm.validateFields();
    updateAdminRolesMutation.mutate({ id: editingAdminRoles.id, roles: values.roles });
  }

  async function handleSaveAdminStatus() {
    if (!editingAdminStatus) {
      return;
    }
    const values = await adminStatusForm.validateFields();
    updateAdminStatusMutation.mutate({ id: editingAdminStatus.id, status: values.status });
  }

  async function handleSaveRolePermissions() {
    if (!editingRole) {
      return;
    }
    const values = await rolePermissionsForm.validateFields();
    updateRolePermissionsMutation.mutate({ roleCode: editingRole.code, permissions: values.permissions || [] });
  }

  return (
    <>
      <div className="page-toolbar">
        <div>
          <Typography.Title level={3}>权限管理</Typography.Title>
          <Typography.Text type="secondary">管理后台账号、角色和权限分配。</Typography.Text>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={() => queryClient.invalidateQueries({ queryKey: ['admin-management'] })}>
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateAdminOpen(true)}>
            新增账号
          </Button>
        </Space>
      </div>

      <Tabs
        items={[
          {
            key: 'admins',
            label: '管理员账号',
            children: (
              <Table
                rowKey="id"
                columns={adminColumns}
                dataSource={adminsQuery.data || []}
                loading={adminsQuery.isLoading}
                pagination={false}
                scroll={{ x: 1300 }}
              />
            )
          },
          {
            key: 'roles',
            label: '角色权限',
            children: (
              <Table
                rowKey="id"
                columns={roleColumns}
                dataSource={rolesQuery.data || []}
                loading={rolesQuery.isLoading}
                pagination={false}
              />
            )
          }
        ]}
      />

      <Modal
        title="新增后台账号"
        open={createAdminOpen}
        okText="创建"
        confirmLoading={createAdminMutation.isPending}
        onOk={handleCreateAdmin}
        onCancel={() => setCreateAdminOpen(false)}
      >
        <Form form={adminForm} layout="vertical">
          <Form.Item label="用户名" name="username" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input placeholder="例如 operator001" />
          </Form.Item>
          <Form.Item label="密码" name="password" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password placeholder="至少使用开发环境可记住的测试密码" />
          </Form.Item>
          <Form.Item label="显示名" name="displayName">
            <Input placeholder="例如 运营一号" />
          </Form.Item>
          <Form.Item label="角色" name="roles" rules={[{ required: true, message: '请选择角色' }]}>
            <Select mode="multiple" options={roleOptions} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={editingAdminRoles ? `修改 ${editingAdminRoles.username} 的角色` : '修改账号角色'}
        open={Boolean(editingAdminRoles)}
        okText="保存"
        confirmLoading={updateAdminRolesMutation.isPending}
        onOk={handleSaveAdminRoles}
        onCancel={() => setEditingAdminRoles(undefined)}
      >
        <Form form={adminRolesForm} layout="vertical">
          <Form.Item label="角色" name="roles" rules={[{ required: true, message: '请选择角色' }]}>
            <Select mode="multiple" options={roleOptions} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={editingAdminStatus ? `修改 ${editingAdminStatus.username} 的状态` : '修改账号状态'}
        open={Boolean(editingAdminStatus)}
        okText="保存"
        confirmLoading={updateAdminStatusMutation.isPending}
        onOk={handleSaveAdminStatus}
        onCancel={() => setEditingAdminStatus(undefined)}
      >
        <Form form={adminStatusForm} layout="vertical">
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
        title={editingRole ? `修改 ${editingRole.name} 权限` : '修改角色权限'}
        open={Boolean(editingRole)}
        okText="保存"
        confirmLoading={updateRolePermissionsMutation.isPending}
        onOk={handleSaveRolePermissions}
        onCancel={() => setEditingRole(undefined)}
        width={760}
      >
        <Form form={rolePermissionsForm} layout="vertical">
          <Form.Item label="权限" name="permissions">
            <Select mode="multiple" options={permissionOptions} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}

function renderTags(values: string[], color: string, max = 20) {
  if (!values.length) {
    return '-';
  }
  const visible = values.slice(0, max);
  return (
    <Space size={[4, 4]} wrap>
      {visible.map((value) => (
        <Tag key={value} color={color}>
          {value}
        </Tag>
      ))}
      {values.length > max ? <Tag>+{values.length - max}</Tag> : null}
    </Space>
  );
}

function showRequestError(error: unknown, fallback: string) {
  const err = error as { response?: { data?: { error?: { code?: string; message?: string; details?: string } } }; message?: string };
  const apiError = err.response?.data?.error;
  const messageText = apiError?.details || apiError?.message || err.message || fallback;
  message.error(apiError?.code ? `${apiError.code}: ${messageText}` : messageText);
}
