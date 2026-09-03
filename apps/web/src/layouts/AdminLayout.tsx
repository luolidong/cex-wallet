import { AuditOutlined, DashboardOutlined, FileSearchOutlined, IdcardOutlined, ImportOutlined, MonitorOutlined, ProfileOutlined, RadarChartOutlined, ReconciliationOutlined, SafetyCertificateOutlined, UserOutlined, WalletOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { Button, Layout, Menu, Space, Typography } from 'antd';
import { useEffect } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { getAdminProfile } from '../api/auth';
import { clearSession, getStoredUser, updateStoredUser } from '../auth/session';

const { Header, Sider, Content } = Layout;

export function AdminLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const storedUser = getStoredUser();
  const profileQuery = useQuery({
    queryKey: ['admin-profile'],
    queryFn: getAdminProfile,
    staleTime: 30000
  });
  const user = profileQuery.data || storedUser;
  const permissions = new Set(user?.permissions || []);
  useEffect(() => {
    if (profileQuery.data) {
      updateStoredUser(profileQuery.data);
    }
  }, [profileQuery.data]);
  const selectedKey = resolveSelectedKey(location.pathname);

  function handleLogout() {
    clearSession();
    navigate('/login', { replace: true });
  }

  function can(permission: string) {
    return permissions.has(permission);
  }

  const menuItems = [
    { key: 'dashboard', icon: <DashboardOutlined />, label: '仪表盘' },
    can('user:read') ? { key: 'users', icon: <UserOutlined />, label: '用户管理' } : null,
    can('wallet:read') ? { key: 'wallets', icon: <WalletOutlined />, label: '地址管理' } : null,
    can('wallet:read') ? { key: 'deposits', icon: <ImportOutlined />, label: '充值记录' } : null,
    can('withdrawal:review') ? { key: 'withdrawal-records', icon: <FileSearchOutlined />, label: '提现记录' } : null,
    can('withdrawal:review') ? { key: 'withdrawals/review', icon: <AuditOutlined />, label: '提现审核' } : null,
    can('ledger:read') ? { key: 'ledger/journals', icon: <ProfileOutlined />, label: '账务流水' } : null,
    can('scanner:read') ? { key: 'scanner/status', icon: <RadarChartOutlined />, label: '扫描状态' } : null,
    can('risk:manage') ? { key: 'risk/settings', icon: <SafetyCertificateOutlined />, label: '风控配置' } : null,
    can('asset:manage') ? { key: 'assets', icon: <WalletOutlined />, label: '资产管理' } : null,
    can('admin:manage') ? { key: 'admin-management', icon: <IdcardOutlined />, label: '权限管理' } : null,
    can('system:read') ? { key: 'system/status', icon: <MonitorOutlined />, label: '系统状态' } : null,
    can('audit:read') ? { key: 'audit-logs', icon: <FileSearchOutlined />, label: '审计日志' } : null,
    can('reconciliation:read') ? { key: 'reconciliation', icon: <ReconciliationOutlined />, label: '账务对账' } : null
  ];

  return (
    <Layout className="app-shell">
      <Sider width={232} theme="dark">
        <div className="brand">CEX Wallet</div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[selectedKey]}
          onClick={({ key }) => navigate(key === 'dashboard' ? '/' : `/${key}`)}
          items={menuItems}
        />
      </Sider>
      <Layout>
        <Header className="app-header">
          <Typography.Text strong>钱包后台管理台</Typography.Text>
          <Space>
            <Typography.Text type="secondary">{user?.displayName || user?.username}</Typography.Text>
            <Button size="small" onClick={handleLogout}>
              退出
            </Button>
          </Space>
        </Header>
        <Content className="app-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}

function resolveSelectedKey(pathname: string) {
  if (pathname.startsWith('/users')) return 'users';
  if (pathname.startsWith('/wallets')) return 'wallets';
  if (pathname.startsWith('/deposits')) return 'deposits';
  if (pathname.startsWith('/withdrawal-records')) return 'withdrawal-records';
  if (pathname.startsWith('/withdrawals/review')) return 'withdrawals/review';
  if (pathname.startsWith('/ledger/journals')) return 'ledger/journals';
  if (pathname.startsWith('/scanner/status')) return 'scanner/status';
  if (pathname.startsWith('/risk/settings')) return 'risk/settings';
  if (pathname.startsWith('/assets')) return 'assets';
  if (pathname.startsWith('/admin-management')) return 'admin-management';
  if (pathname.startsWith('/system/status')) return 'system/status';
  if (pathname.startsWith('/audit-logs')) return 'audit-logs';
  if (pathname.startsWith('/reconciliation')) return 'reconciliation';
  return 'dashboard';
}
