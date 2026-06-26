# 头像上传修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 myProfile.js 头像上传报错 `TypeError: getBaseUrl is not a function`

**Architecture:** 微信小程序前端修复。`request.js` 导出了 `buildUrl` 但未导出 `getBaseUrl`，`myProfile.js` 导入不存在的 `getBaseUrl` 导致报错。改用已导出的 `buildUrl` 替代。

**Tech Stack:** 微信小程序原生开发

## Global Constraints

- 不改动 `request.js` 的导出
- 只修改 `myProfile.js` 一个文件
- 使用已有的 `buildUrl` 函数做 URL 拼接

---

### Task 1: 修复 myProfile.js 头像上传 URL 拼接

**Files:**
- Modify: `lcyl-wx/my/pages/myProfile/myProfile.js:2,120`

**Root cause:** `request.js` 第 111 行 `module.exports = { request, buildUrl, verifyToken, isTokenExpired, refreshSession }` 没有导出 `getBaseUrl`。但 `myProfile.js` 第 2 行 `const { request, getBaseUrl } = require('../../../utils/request')` 试图解构 `getBaseUrl`，得到 `undefined`，第 120 行调用时报错。

- [ ] **Step 1: 修改导入语句**

将第 2 行从：
```javascript
const { request, getBaseUrl } = require('../../../utils/request');
```
改为：
```javascript
const { request, buildUrl } = require('../../../utils/request');
```

- [ ] **Step 2: 修改 uploadFile URL**

将第 120 行从：
```javascript
url: getBaseUrl() + '/upload/image',
```
改为：
```javascript
url: buildUrl('/upload/image'),
```

- [ ] **Step 3: 验证修改**

检查修改后的文件第 2 行和第 120 行是否匹配：
- 导入的是 `buildUrl` 而非 `getBaseUrl`
- `uploadFile` 调用中使用的是 `buildUrl('/upload/image')`
- 确认无其他位置使用 `getBaseUrl`

- [ ] **Step 4: 提交**

```bash
cd D:/WBB_JAVA/team
git add lcyl-wx/my/pages/myProfile/myProfile.js
git commit -m "fix: 修复头像上传 getBaseUrl is not a function 错误，改用 buildUrl"
```
