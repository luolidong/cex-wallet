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

第一版先支持配置角色权限关系。下一阶段再把关键接口接入权限拦截。

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
