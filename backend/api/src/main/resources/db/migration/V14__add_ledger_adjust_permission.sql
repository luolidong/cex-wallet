INSERT INTO permissions (code, name)
VALUES ('ledger:adjust', '人工账务调账')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'ledger:adjust'
WHERE r.code = 'admin'
ON CONFLICT DO NOTHING;
