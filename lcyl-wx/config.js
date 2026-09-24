/**
 * 环境配置：按微信运行环境自动选择后端地址
 *
 * envVersion 取值（来自 wx.getAccountInfoSync().miniProgram.envVersion）：
 *   develop —— 开发版（微信开发者工具 / 预览）
 *   trial   —— 体验版
 *   release —— 正式版
 *
 * ⚠️ 部署前必须做两件事：
 *   1. 把 trial / release 的 baseUrl 换成真实域名（微信要求 HTTPS）
 *   2. 在微信公众平台「开发管理 → 服务器域名」里配置为 request 合法域名
 */
const ENV_CONFIG = {
  develop: {
    baseUrl: 'http://localhost:8080'
  },
  trial: {
    // TODO 替换为测试环境域名（须 HTTPS）
    baseUrl: 'https://test.example.com'
  },
  release: {
    // TODO 替换为生产环境域名（须 HTTPS）
    baseUrl: 'https://api.example.com'
  }
}

/**
 * 取当前环境的后端地址。
 * 基础库过低不支持 getAccountInfoSync 时，降级为开发环境。
 */
function getBaseUrl() {
  let envVersion = 'develop'
  try {
    envVersion = wx.getAccountInfoSync().miniProgram.envVersion || 'develop'
  } catch (e) {
    // 忽略：降级为 develop
  }
  const conf = ENV_CONFIG[envVersion] || ENV_CONFIG.develop
  return conf.baseUrl
}

module.exports = { getBaseUrl, ENV_CONFIG }
