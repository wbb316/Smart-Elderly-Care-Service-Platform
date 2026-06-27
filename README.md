# 乐康养老智慧服务平台

基于 **RuoYi v3.8** 二次开发的智慧养老综合管理平台，提供管理后台（Vue 3）、护理端小程序和家属端小程序的完整解决方案。

## 项目结构

```
lcyl-java/          # Java 后端（Spring Boot）
  ├── lcyl-admin       # 系统/业务控制器、配置文件
  ├── lcyl-code        # 核心业务逻辑（Service/Domain/Mapper）
  ├── lcyl-common      # 通用工具类（XSS 过滤、SQL 注入防护、Http 工具等）
  ├── lcyl-framework   # 框架核心（Spring Security、JWT、拦截器、异常处理）
  ├── lcyl-generator   # 代码生成器
  ├── lcyl-quartz      # 定时任务
  └── lcyl-system      # 系统管理（用户/角色/菜单/部门等）

lcyl-vue/lcyl/      # Vue 3 管理后台前端
  └── src/views/      # 页面（入住管理、护理管理、账单、退住等）

lcyl-wx/            # 微信小程序（家属端）
  ├── pages/           # 首页、家人、服务、我的（Tab 主页面）
  ├── home/            # 房型浏览、参观预约、探访、注册老人、订单
  ├── family/          # 家属绑定、健康数据、账单、请假
  ├── my/              # AI 智能助手、订单、账单、合同、预约、个人资料
  └── servicePage/     # 服务详情、下单、支付
```

## 功能模块

### 管理后台（Vue 3）

| 模块 | 功能 |
|------|------|
| 入住管理 | 入住申请、评估、审批、签约、床位分配 |
| 护理管理 | 护理项目、护理等级、护理计划、护理任务 |
| 老人管理 | 老人档案、入退住管理 |
| 账单管理 | 月度账单、费用明细、支付记录 |
| 退住管理 | 退住申请法务/结算/审批工作流 |
| 预约来访 | 来访登记、预约管理 |
| 请假管理 | 请假申请、审批、销假 |
| 系统管理 | 用户、角色、部门、菜单、字典、参数配置 |
| 监控管理 | 在线用户、操作日志、缓存监控、定时任务 |

### 微信小程序（家属端）

| 页面 | 功能 |
|------|------|
| 首页 | 房型浏览、参观预约、探访预约 |
| 家人 | 绑定/解绑老人、健康数据、账单、请假 |
| 服务 | 护理服务浏览、下单、支付 |
| 我的 | AI 智能助手、订单、账单、合同、预约、预定、个人资料 |

### 微信小程序自动实现的功能

| 功能 | 说明 |
|------|------|
| Token 校验 | 每次页面切换自动校验 token，过期跳转登录页 |
| Token 刷新 | 页面切换时自动延长 Redis 中的 token 有效期（2 小时）|
| 老人注册 | 家属可自助填写老人信息提交审核，后台审核通过后自动绑定 |
| 请假流程 | 家属在线申请请假，后台 Activiti 工作流审批 |

### AI 智能助手

接入 DeepSeek API，家属在"我的"页面进入「智能助手」，可通过自然语言查询：

- **账单查询**："我妈妈上个月花了多少钱"、"这个月账单出来了吗"
- **订单查询**："爸爸的护理订单执行了吗"、"帮我看看订单状态"
- **合同查询**："合同什么时候到期"、"帮我看看合同信息"
- **请假查询**："请假申请通过了吗"、"销假了没有"

后端自动识别提问意图，查询绑定的老人的对应数据，结合 DeepSeek 生成自然语言回答。支持多轮对话（Redis 缓存最近6轮对话）。

## 技术栈

### 后端
- JDK 17
- Spring Boot 3.x
- Spring Security + JWT（双认证：管理后台 JWT + 小程序 token）
- MyBatis + PageHelper
- Activiti 7（工作流引擎：入住申请、请假审批、退住流程）
- Redis（缓存 + Session 管理）
- Quartz（定时任务）
- Alibaba Cloud OSS（文件存储）
- MySQL + Druid

### 前端
- **管理后台**：Vue 3 + TypeScript + Element Plus + Axios
- **微信小程序**：原生微信小程序 + 手动封装 `request` 请求库（自动注入 token、拦截过期）

## 环境要求

| 依赖 | 版本/说明 |
|------|----------|
| JDK | 17+ |
| Maven | 3.6+ |
| MySQL | 8.0+ |
| Redis | 6.x+ |
| Node.js | 16+（Vue 前端）|

## 本地运行

### 1. 数据库
```sql
CREATE DATABASE lcyl DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```
执行 `lcyl-java/sql/` 目录下的初始化脚本。

### 2. 后端配置
修改 `lcyl-java/lcyl/lcyl-admin/src/main/resources/application.yml`：
- MySQL 连接信息
- Redis 连接信息
- OSS 配置（可通过环境变量 `OSS_KEY`/`OSS_SECRET` 注入）

### 3. 启动后端
```bash
cd lcyl-java/lcyl
mvn compile
mvn spring-boot:run
```

### 4. 启动管理后台
```bash
cd lcyl-vue/lcyl
npm install
npm run dev
```

### 5. 微信小程序
用微信开发者工具打开 `lcyl-wx/` 目录，修改 `utils/request.js` 中的 API 地址。

## 环境变量配置

| 变量名 | 用途 | 必填 |
|--------|------|------|
| `OSS_KEY` | 阿里云 OSS 的 AccessKeyId | 上传图片需要 |
| `OSS_SECRET` | 阿里云 OSS 的 AccessKeySecret | 上传图片需要 |
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥 | AI 智能助手需要 |

```bash
# Windows 命令行
set OSS_KEY=your-access-key-id
set OSS_SECRET=your-access-key-secret
set DEEPSEEK_API_KEY=your-deepseek-api-key

# IDE Run Configuration
-DOSS_KEY=your-access-key-id -DOSS_SECRET=your-access-key-secret -DDEEPSEEK_API_KEY=your-deepseek-api-key
```

## 相关文档

- [技术设计文档](docs/superpowers/specs/) — 功能设计规格说明
- [实现计划](docs/superpowers/plans/) — 开发实施计划
