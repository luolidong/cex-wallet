import { Card, Col, Row, Statistic, Typography } from 'antd';

export function DashboardPage() {
  return (
    <>
      <Typography.Title level={3}>仪表盘</Typography.Title>
      <Row gutter={16}>
        <Col span={6}>
          <Card>
            <Statistic title="待审核提现" value={0} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="今日充值" value={0} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="今日提现" value={0} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="服务状态" value="UP" />
          </Card>
        </Col>
      </Row>
    </>
  );
}

