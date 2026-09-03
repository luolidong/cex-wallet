INSERT INTO permissions (code, name)
VALUES
  ('asset:manage', '管理资产配置'),
  ('risk:manage', '管理风控配置'),
  ('audit:read', '查看审计日志'),
  ('reconciliation:read', '查看账务对账'),
  ('scanner:read', '查看扫描状态'),
  ('admin:manage', '管理后台账号和权限')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'admin'
ON CONFLICT DO NOTHING;
