# 统一账号与绑定

## 当前实现

- users.id 是不可变的账号 ID。昵称、头像和登录邮箱变化不改变账号、应用设置或云端备份的归属。
- 邮箱注册只要求邮箱与密码，验证邮箱后登录；默认昵称“小奶娃”，默认头像 default。昵称可重复，1–24 个 Unicode 字符；禁止空白昵称和控制字符。
- 头像使用 Android 内置资源，服务端存 avatarKey：default、sad、strong、thinking、thumbs_up。第一张为默认，其余四张供选择。
- 保留 users.username 的唯一约束和旧账号名称。新账号使用随机内部标识；旧账号未设置 display_name 时显示原名称，未设置 avatar_key 时显示默认头像。
- JWT 验证签名后使用 userId claim 查找账号；旧版以邮箱为 subject 的令牌已携带 userId，同样兼容。令牌仍按原设置过期。
- account_identities 保存已验证身份，provider + issuer + subject 唯一，账号 + provider + issuer 唯一。现有邮箱身份在个人资料首次读取或更新时补齐。

## API（需要当前账号 Bearer JWT）

| 方法 | 路径 | 请求/响应 |
| --- | --- | --- |
| GET | /api/account/profile | userId、displayName、avatarKey、email、emailVerified、linkedMethods |
| PUT | /api/account/profile | displayName、avatarKey；返回最新资料 |
| POST | /api/account/bindings/email/request | email（新邮箱）、password（当前密码）；提交邮件发送 |
| POST | /api/account/bindings/email/confirm | email（同一新邮箱）、code（六位）；返回最新资料 |

换绑需要当前密码与新邮箱验证码。验证码仅存 BCrypt 哈希，10 分钟有效，每账号 60 秒发送间隔，最多 5 次校验，成功后不可再次使用。密码重置后之前的换绑验证码失效。
换绑成功删除发送至旧邮箱的密码重置/注册验证码；密码重置与换绑共用账号行锁，避免旧邮箱验证码竞态。
占用邮箱返回 409，不合并已有账号。当前唯一邮箱登录身份不提供解除绑定入口。

## 后续手机/微信

模型预留 PHONE、WECHAT 和 issuer 命名空间，绑定钩子只允许服务端验证短信/OAuth 凭证后调用。首版没有短信/微信登录界面或公开绑定接口。
后续接入需供应商资质、SDK、服务端凭证校验；手机号独立注册还需调整现有邮箱/密码非空规则，不能用虚构邮箱替代。绑定到已有邮箱账号时沿用同一 users.id。

## 验证

AccountIntegrationTests 使用独立 focusguard_backup_test 数据库，在事务内验证注册默认资料、认证、旧资料兼容、资料校验、换绑、备份保留、验证码耗尽/重放/过期/密码变化和邮箱冲突，测试结束回滚。EmailService 被 mock，不发送真实邮件。
JwtServiceTest 使用随机测试密钥验证旧令牌兼容，不使用生产密钥。
本地后端由 start-local.ps1 加载 .env；公开发布需 HTTPS。昵称与头像属于服务端账号资料，不进入设备备份快照。
