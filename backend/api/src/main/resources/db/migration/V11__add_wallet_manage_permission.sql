INSERT INTO permissions (code, name)
VALUES ('wallet:manage', '管理充值地址')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'wallet:manage'
WHERE r.code = 'admin'
ON CONFLICT DO NOTHING;
