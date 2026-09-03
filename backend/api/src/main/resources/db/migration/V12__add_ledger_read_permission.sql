INSERT INTO permissions (code, name)
VALUES ('ledger:read', '查看账务流水')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'ledger:read'
WHERE r.code IN ('admin', 'operator', 'viewer')
ON CONFLICT DO NOTHING;
