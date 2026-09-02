import { Button, Card, Form, Input, Typography } from 'antd';
import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { login } from '../api/auth';
import { setSession } from '../auth/session';

interface LoginFormValues {
  username: string;
  password: string;
}

export function LoginPage() {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  async function handleFinish(values: LoginFormValues) {
    setLoading(true);
    try {
      const result = await login(values.username, values.password);
      setSession(result.accessToken, result.adminUser);
      const from = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname || '/';
      navigate(from, { replace: true });
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="login-page">
      <Card className="login-card">
        <Typography.Title level={3}>CEX Wallet</Typography.Title>
        <Form layout="vertical" onFinish={handleFinish} initialValues={{ username: 'admin' }}>
          <Form.Item label="用户名" name="username" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input />
          </Form.Item>
          <Form.Item label="密码" name="password" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={loading} block>
            登录
          </Button>
        </Form>
      </Card>
    </div>
  );
}
