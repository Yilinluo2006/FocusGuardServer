# 手机号登录与绑定

采用阿里云 PNVS `SendSmsVerifyCode` / `CheckSmsVerifyCode`，只接受 `VerifyResult=PASS`。邮箱登录保留，账号与备份始终以 users.id 为归属。

后端 `.env` 配置 `ALIBABA_CLOUD_ACCESS_KEY_ID`、`ALIBABA_CLOUD_ACCESS_KEY_SECRET`，签名默认 `恒创联众`，可用 `ALIYUN_SMS_SIGN_NAME` 覆盖。RAM 只需 dypns:SendSmsVerifyCode、dypns:CheckSmsVerifyCode。密钥不进入 APK 或 Git。

IDE 或脚本启动均自动加载本地外部 `.env`：支持 Maven 模块目录和上层 E:\FocusGuardServer 目录。真实环境变量优先；启动日志仅显示 `PNVS SMS credentials configured: true/false`，不打印密钥。修改配置后需重新启动后端进程。

- POST /api/auth/phone/request：phone，返回 challengeId。
- POST /api/auth/phone/confirm：challengeId、code；首次认证自动注册。
- POST /api/account/bindings/phone/proof/request：向当前已绑定手机号发送身份验证码，需要登录。
- POST /api/account/bindings/phone/request：首次绑定仅需 phone；更换已绑定手机号需 phone、password，无密码账号用 proofChallengeId、proofCode 验证原手机号。
- POST /api/account/bindings/phone/confirm：challengeId、code，绑定新手机号。
- 无密码账号添加邮箱：邮件绑定请求的 password 为新邮箱登录密码，还需 phoneChallengeId、phoneCode。

验证码 6 位、5 分钟有效，5 次错误后失效，用途和账号隔离，发送重新生成 challengeId。数据库锁保护消费；后端按手机号限制 60 秒一次、24 小时 10 次，按来源 IP 限制每小时 20 次。提供商生成验证码，后端不保存明文短信验证码，不自动重试付费发送。

启动时兼容迁移 users.email 与 password_hash 的旧 NOT NULL 约束，不删除或改变已有账号资料。测试使用独立 focusguard_backup_test 数据库、模拟短信和邮件，不发送真实短信。

阿里云控制台测试号和平台赠送签名/模板的可用范围仍受账户服务配置限制；正式对外开放前需验证非测试手机号可用范围。

## 发送失败排查

- `isv.OUT_OF_SERVICE`：阿里云短信业务停用，检查账户余额与欠费。即使有套餐包，账户欠费仍可能无法抵扣。2026-10-01 本地诊断复现该返回码；诊断使用无效号码，没有发送短信。
- `isv.ValidateFail`：验证码错误或过期，按认证失败处理，不再误报服务不可用。
- 后端错误提示和日志仅保留安全的返回码，不输出 SDK 异常消息、密钥或完整手机号。
