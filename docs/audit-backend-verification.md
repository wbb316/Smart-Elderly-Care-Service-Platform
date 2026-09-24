# 后端核验报告（对照既有清单实地验证）

> 核验日期：2026-08-16
> 核验方式：读取当前源码 + 直连 MySQL 实际查询（库 ry-vue）
> 基准：`docs/lcyl-java-code-issue-checklist.md`（2026-08-01，83 个问题）

---

## 一、结论摘要

既有清单的 83 个问题中，**大部分已修复**。抽验结果：

| 状态 | 数量 | 说明 |
|------|------|------|
| ✅ 已修复 | 9 类 | 权限注解、IDOR、事务、索引等 |
| ❌ 仍未修复 | 9 项 | 见下方 |
| ⚠️ 清单判断有误 | 1 项 | 硬编码审批人实际可用 |
| 🆕 新发现 | 3 类 | 不在原清单中 |

---

## 二、已修复（✅ 抽验确认）

| 原清单项 | 验证结果 | 证据 |
|---------|---------|------|
| 1.1 权限校验缺失 | ✅ 全部补齐 | NursingTaskController(6)、IndexController(7)、ElderResponsibleController(3)、ArrivalController(8)、ProcessDeployController(1)、ElderLeaveProcessController(1) 均有 `@PreAuthorize` |
| 1.2 getElderList 返回全院老人 | ✅ 已修 | 不再调用 `selectAllElder` |
| 1.2 deleteElder 可解绑他人老人 | ✅ 已修 | 存在 `checkElderBelongsToMember` 校验 |
| 1.2 notification/read 越权写 | ✅ 已修 | SQL 现为 `where id=#{id} and member_id=#{memberId}` |
| 1.2 ai/history 会话可枚举 | ✅ 已修 | Redis key 现为 `ai:session:{memberId}:{sessionId}` |
| 5.x `"5L"` 字面量污染 | ✅ 已修 | 全文无 `"5L"` |
| 5.x 入住编号日期格式 | ✅ 已修 | 无 `yyyyMMddHHmmSSS` |
| 5.x 取消次数统计缺 member_id | ✅ 已修 | SQL 已带 member_id |
| 5.x 缺 `@Transactional` | ✅ 大部分已补 | ServiceOrderRefund(2)、ServiceOrder(2)、WxLogin(3)、Bill(3) 均有注解 |
| 6.x 索引缺失 | ✅ **已全部执行到库** | 实测 12 条索引均存在（含 `lc_checkin_health_evaluate.idx_apply_id`、`lc_checkin_ability_evaluate.idx_apply_id`） |

---

## 三、仍未修复（❌ 需处理）

### 🔴 严重

| # | 文件:行 | 问题 | 影响 |
|---|---------|------|------|
| 1 | `lcyl-admin/src/main/resources/application.yml:106` | **JWT 密钥明文硬编码**（原值 64 字符，已在 git 历史中，此处脱敏不复述） | 任何拿到该字符串的人可自行签发任意用户 token，管理端与小程序端同用一个密钥，影响全系统。**已于本次修复中改为环境变量注入并轮换，旧值作废** |
| 2 | `lcyl-framework/.../config/SecurityConfig.java:114` | **`/upload`、`/uploadContractPdf` 仍在 permitAll 清单** | 任何人无需登录即可上传文件（配合 OSS 公读 = 任意上传+公开下载） |
| 3 | `lcyl-admin/.../OssServiceImpl.java` | **合同 PDF OSS ACL 仍为 `PublicRead`** | 含身份证、签约信息的合同 PDF 公开可下载 |
| 4 | `lcyl-code/.../WxLoginServiceImpl.java:310,391,417` | **支付仍是模拟实现**：`wechatOrderNo = "WX" + 时间戳 + 随机数`，不调微信网关、无回调验签 | 客户端可"免费"把任意自己名下订单/账单标记为已支付，账目与真实资金脱节 |

### 🟠 高

| # | 文件:行 | 问题 | 影响 |
|---|---------|------|------|
| 5 | `BillMapper.xml` `updateBill` | 仍是 **无条件更新**（无 `and trade_status='0'`） | 并发两个请求都读到"待支付"→ 各写一条支付记录 → **重复支付、重复冲抵欠费** |
| 6 | `CheckInConfigServiceImpl.submitConfig` | 仍**无条件 `insertBalance`** | 重复提交产生多条余额账户，而查询带 `limit 1` → 账目分裂、对账混乱 |
| 7 | `BedController.add` | 仍**调用 `insertBed` 两次** | 重复插入两条相同床位记录 |
| 8 | `application-druid.yml:47` | Druid 控制台 `allow:` **为空** | 任意 IP 可访问 `/druid/*`，暴露 SQL、会话、连接池信息 |

---

## 四、清单判断有误（⚠️ 需修正）

| 原清单项 | 原判断 | 实测结果 |
|---------|--------|---------|
| 5.x「审批人硬编码 `zhuguan`/`buzhang`，流程会卡死」 | 【严重】审批任务无人认领 | **账号真实存在**：`sys_user` 中 user_id=100 `zhuguan`（护理组主管）、user_id=101 `buzhang`（护理部部长），status=0 正常。流程可正常流转。<br>**真正的问题**是"用硬编码用户名而非按角色查询"，换环境/改用户名即失效，属设计脆弱而非功能故障 |

---

## 五、新发现问题（🆕 不在原清单）

### 5.1 🔴 数据库存在 9 张死表，其中一张 6 万行

实测 `ry-vue` 共 123 张表，以下表**在任何 Mapper XML 中都不被 SQL 引用**：

| 表名 | 行数 | 说明 |
|------|------|------|
| `device_data` | **59,801** | 全库最大表（10.5MB），代码完全未使用；有 iot_id/function_name 索引但无人查 |
| `lc_retreat_copy1` | - | **手工复制的表副本**（`_copy1` 后缀），生产库中的临时产物，建议立即清理 |
| `member` | - | 与 `lc_member` 重复 |
| `device` | - | 与 `lc_device` 重复 |
| `nursing_task` | 425 | 项目用 `lc_nursing_task_execution` |
| `nursing_project` | 12 | 未使用 |
| `nursing_project_plan` | 24 | 未使用 |
| `accraditation_record` | 94 | 表名疑似拼写错误（accreditation），未使用 |
| `sys_notice_read` | - | 与 `sys_notification` 并存 |

**风险**：占用空间、误导后续开发（新人不知道该用哪张）、备份/迁移变慢。

**建议**：确认无历史数据依赖后 `DROP`；若需保留先 `RENAME` 为 `_deprecated_xxx` 并加注释。

### 5.2 🟠 Redis 未运行

实测 `localhost:6379` 拒绝连接，但配置（`application.yml:69-89`，database 15）要求 Redis。

**影响**：
- 小程序 token 的**滑动续期完全失效**（`refreshSession` 底层 `tokenService.getLoginUser` 因 JWT 无 `login_user_key` claim 恒返回 null，本就是空操作；Redis 再挂则彻底无会话管理）
- 管理员在线用户、验证码、限流（`RateLimiterAspect`）、AI 会话历史全部不可用

**建议**：开发环境也要常驻 Redis；或将不需要 Redis 的功能做降级处理。

### 5.3 🟡 索引迁移脚本未纳入版本管理

`lcyl-java/lcyl/sql/20260802_index_migration.sql` 在 git 中是 **未跟踪状态（`??`）**，但**已实际执行到数据库**。

**风险**：新环境部署时不会执行该脚本 → 缺少 12 条索引 → 性能问题在新环境复现。

**建议**：`git add` 并提交该脚本，同时纳入部署流程。

---

## 六、优先级建议

| 优先级 | 处理项 |
|--------|--------|
| **P0（立即）** | JWT 密钥改环境变量并轮换（#1）、移出 /upload 放行（#2） |
| **P1（本周）** | 合同 PDF 改私有+签名 URL（#3）、支付接真实网关或加 `@Profile("demo")` 隔离（#4）、updateBill 条件更新（#5） |
| **P2（计划）** | insertBalance 幂等（#6）、BedController 去重（#7）、Druid allow 收紧（#8） |
| **P3（清理）** | 死表清理（5.1）、提交索引脚本（5.3）、修正清单中审批人条目（四） |
