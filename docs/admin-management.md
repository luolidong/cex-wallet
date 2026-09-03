# 权限管理页面验证

本文档验证后台 `权限管理` 页面。

## 1. 页面入口

启动 Java API 和前端后，进入后台左侧菜单：

```text
权限管理
```

页面包含两个页签：

- `管理员账号`
- `角色权限`

## 2. 验证管理员账号

在 `管理员账号` 页签点击 `新增账号`。

建议填写：

```text
用户名：operator001
密码：123456
显示名：运营一号
角色：admin
```

保存后预期：

- 表格出现 `operator001`。
- 状态为 `ACTIVE`。
- 角色显示 `admin`。
- 权限来自角色绑定。

可以继续点击该账号的 `角色` 或 `状态` 按钮，验证角色和状态能正常修改。

当前登录账号不能把自己改成 `INACTIVE`，后端会返回：

```text
CANNOT_DISABLE_SELF
```

## 3. 验证角色权限

进入 `角色权限` 页签，点击 `系统管理员` 的 `权限` 按钮。

可以看到所有权限 code，例如：

```text
system:read
user:read
wallet:read
withdrawal:review
asset:manage
risk:manage
audit:read
reconciliation:read
scanner:read
admin:manage
```

角色权限保存后会影响后台接口访问。用户进入后台时会自动刷新当前账号权限；如果登录态较旧，也可以退出后重新登录。

## 4. 验证审计日志

完成以下任意操作后进入 `审计日志` 页面：

- 新增后台账号
- 修改后台账号状态
- 修改后台账号角色
- 修改角色权限

预期可以看到对应动作：

```text
新增后台账号
修改后台账号状态
修改后台账号角色
修改角色权限
```

## 5. 验证权限拦截

建议创建一个低权限角色测试账号：

1. 重启 Java API，让 `V10__init_default_admin_roles.sql` 初始化 `operator` 和 `viewer` 角色。
2. 在 `管理员账号` 页签创建账号，例如 `viewer001`，角色选择 `viewer`。
3. 退出当前 admin 账号，使用 `viewer001` 登录。
4. 当前系统已经对以下接口启用权限校验：

```text
admin:manage          权限管理
audit:read            审计日志
reconciliation:read   账务对账
scanner:read          扫描状态
system:read           系统状态
asset:manage          资产管理
risk:manage           风控配置
withdrawal:review     提现审核和后台代提现
user:read             用户、余额、充值记录查询
wallet:read           地址管理查询
wallet:manage         启用、停用充值地址
```

如果账号缺少对应权限：

- 左侧菜单不会显示对应入口。
- 直接访问接口会返回 `FORBIDDEN`。
- 前端会提示 `当前账号没有权限执行此操作。`
