import { AuditOutlined, DashboardOutlined, FileSearchOutlined, MonitorOutlined, RadarChartOutlined, SafetyCertificateOutlined, UserOutlined, WalletOutlined } from '@ant-design/icons';
import { Button, Layout, Menu, Space, Typography } from 'antd';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { clearSession, getStoredUser } from '../auth/session';

const { Header, Sider, Content } = Layout;

export function AdminLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const user = getStoredUser();
  const selectedKey = location.pathname.startsWith('/users')
      ? 'users'
      : location.pathname.startsWith('/withdrawals/review')
        ? 'withdrawals/review'
        : location.pathname.startsWith('/scanner/status')
          ? 'scanner/status'
          : location.pathname.startsWith('/risk/settings')
            ? 'risk/settings'
            : location.pathname.startsWith('/assets')
              ? 'assets'
              : location.pathname.startsWith('/system/status')
                ? 'system/status'
                : location.pathname.startsWith('/audit-logs')
                  ? 'audit-logs'
                  : 'dashboard';

  function handleLogout() {
    clearSession();
    navigate('/login', { replace: true });
  }

  return (
    <Layout className="app-shell">
      <Sider width={232} theme="dark">
        <div className="brand">CEX Wallet</div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[selectedKey]}
          onClick={({ key }) => navigate(key === 'dashboard' ? '/' : `/${key}`)}
          items={[
            { key: 'dashboard', icon: <DashboardOutlined />, label: '仪表盘' },
            { key: 'users', icon: <UserOutlined />, label: '用户管理' },
            { key: 'withdrawals/review', icon: <AuditOutlined />, label: '提现审核' },
            { key: 'scanner/status', icon: <RadarChartOutlined />, label: '扫描状态' },
            { key: 'risk/settings', icon: <SafetyCertificateOutlined />, label: '风控配置' },
            { key: 'assets', icon: <WalletOutlined />, label: '资产管理' },
            { key: 'system/status', icon: <MonitorOutlined />, label: '系统状态' },
            { key: 'audit-logs', icon: <FileSearchOutlined />, label: '审计日志' }
          ]}
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
