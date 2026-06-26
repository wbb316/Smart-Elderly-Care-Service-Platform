# 头像上传 URL 修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复小程序头像上传 URL 错误：`/upload/image` 不存在，应改为 `/upload`

**Architecture:** 微信小程序单行修复。小程序调用 `buildUrl('/upload/image')` 生成 `http://localhost:8080/upload/image`，但后端只有 `/upload`（FileController）。Spring Security 拦截不存在的 `/upload/image` 路径返回 401。改为 `/upload` 即可命中已有端点。

**Tech Stack:** 微信小程序原生 JavaScript

## Global Constraints

- 只修改 `myProfile.js` 一个文件
- 不改动后端任何代码
- `/upload` 已在 SecurityConfig permitAll 白名单中

---

### Task 1: 修正头像上传 URL

**Files:**
- Modify: `lcyl-wx/my/pages/myProfile/myProfile.js:120`

**Interfaces:**
- Consumes: `buildUrl()` from `utils/request.js`
- Produces: None

- [ ] **Step 1: 修改 uploadFile 的 URL**

将 `myProfile.js` 第 120 行从：

```javascript
url: buildUrl('/upload/image'),
```

改为：

```javascript
url: buildUrl('/upload'),
```

- [ ] **Step 2: 验证**

在修改后的文件中 grep 确认：
```bash
grep "upload" lcyl-wx/my/pages/myProfile/myProfile.js
```
应只出现 `buildUrl('/upload')`，不出现 `buildUrl('/upload/image')`。

- [ ] **Step 3: 提交**

```bash
cd D:/WBB_JAVA/team
git add lcyl-wx/my/pages/myProfile/myProfile.js
git commit -m "fix: 修正头像上传 URL /upload/image 改为 /upload，后端无 /upload/image 端点"
```
