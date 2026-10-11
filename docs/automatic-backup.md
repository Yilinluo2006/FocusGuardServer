# 自动备份 API

JWT 认证，所有查询均限定当前用户，不接受客户端提供 owner/userId。

| 方法 | 地址 | 功能 |
| --- | --- | --- |
| GET | `/api/backups` | 当前用户各设备的最近备份元数据，按更新时间降序 |
| GET | `/api/backups/{deviceId}` | 当前用户指定设备的完整快照 |
| PUT | `/api/backups/{deviceId}` | 上传/更新指定设备快照 |

deviceId 为规范 UUID。请求字段：deviceName（1–80 字符）、expectedRevision（初次为 0）、payload（JSON 字符串）。
payload schemaVersion=1，至少包含 limits 数组、tasks 数组和 focus 对象；Android 端还校验具体便携配置和历史。
上限为 2,000,000 字符且 UTF-8 不超过 4,000,000 字节；设备名和请求字段由 Bean Validation 校验。
响应包含 deviceId/deviceName/revision/updatedAt/hash，读取单个备份还包含 payload。

cloud_backups 与 users 关联，用户+设备唯一。事务先锁用户记录，将同一账号写入串行化，包括首条插入。
SHA-256 相同则返回现有版本，允许丢失响应后用旧 expectedRevision 重试。
内容不同必须匹配 expectedRevision，否则 409；成功后版本+1。
不同账号访问相同 deviceId 得到各自独立数据；查询他人备份返回 404。
每台设备保存最近一份快照，不提供历史版本或后台全量双向合并。

## 启动与测试

将 `.env.example` 复制成 `.env`，填写数据库/JWT/邮件参数；`.env` 已被 Git 忽略。
执行 `./start-local.ps1`。脚本读取环境后构建，使用 Java 21 启动 8080。
测试需先创建独立的 `focusguard_backup_test` MySQL 数据库，通过 DB_URL 指向它再执行 `./mvnw.cmd test`。
CloudBackupIntegrationTests 在事务中创建测试账号、验证真实 JWT/filter/controller/JPA/MySQL 链路后回滚。

本地 HTTP 仅供开发。公网发布需配置 HTTPS、服务环境及长效会话/刷新令牌；Android 端现有 JWT 过期后需要重新登录。
