INSERT INTO roles (code, name)
VALUES
  ('operator', '运营人员'),
  ('viewer', '只读人员')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('system:read', 'user:read', 'wallet:read', 'scanner:read', 'reconciliation:read')
WHERE r.code = 'viewer'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
  'system:read',
  'user:read',
  'wallet:read',
  'scanner:read',
  'reconciliation:read',
  'withdrawal:review'
)
WHERE r.code = 'operator'
ON CONFLICT DO NOTHING;
