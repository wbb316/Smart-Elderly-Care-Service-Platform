const app = getApp();

/**
 * 获取 API 基础地址
 */
function getBaseUrl() {
  return (app && app.globalData && app.globalData.baseUrl) || 'http://localhost:8080';
}

/**
 * 拼接完整 URL
 */
function buildUrl(path) {
  const base = getBaseUrl();
  const cleanPath = path.startsWith('/') ? path : '/' + path;
  return base + cleanPath;
}

/**
 * 判断当前是否有有效 token
 */
function hasToken() {
  return !!(app.globalData.token || wx.getStorageSync("token"));
}

/**
 * 封装 wx.request，自动注入 token 并处理登录过期
 */
function request(options) {
  return new Promise((resolve, reject) => {
    const header = Object.assign(
      { "content-type": "application/json" },
      options.header || {}
    );

    const token = app.globalData.token || wx.getStorageSync("token");
    if (token) {
      header["Authorization"] = token;
    }

    const url = options.url && options.url.startsWith('http') ? options.url : buildUrl(options.url);

    wx.request({
      ...options,
      url,
      header,
      success: (res) => {
        if (isTokenExpired(res)) {
          handleTokenExpired();
          reject(new Error("登录已过期"));
          return;
        }
        resolve(res);
      },
      fail: (err) => {
        reject(err);
      }
    });
  });
}

function isTokenExpired(res) {
  if (res.statusCode === 401) return true;
  if (res.data) {
    const code = res.data.code;
    if (code === 401) return true;
    if (code === 500 && res.data.msg && res.data.msg.indexOf("登录") !== -1) return true;
  }
  return false;
}

/**
 * 处理 token 过期：清缓存，立即跳转登录页
 */
function handleTokenExpired() {
  app.globalData.token = "";
  wx.removeStorageSync("token");
  wx.reLaunch({ url: "/pages/index/index" });
}

module.exports = { request, buildUrl, getBaseUrl, hasToken, isTokenExpired };
