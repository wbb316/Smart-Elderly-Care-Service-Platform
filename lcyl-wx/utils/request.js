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
 * 封装 wx.request，自动注入 token 并处理登录过期
 */
function request(options) {
  return new Promise((resolve, reject) => {
    const header = Object.assign(
      { "content-type": "application/json" },
      options.header || {}
    );

    // 自动注入 token
    const token = app.globalData.token || wx.getStorageSync("token");
    if (token) {
      header["Authorization"] = token;
    }

    // 自动补全 baseUrl（如果传入的 url 没有协议前缀）
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

/**
 * 判断 token 是否过期
 */
function isTokenExpired(res) {
  if (res.statusCode === 401) {
    return true;
  }
  if (res.data) {
    const code = res.data.code;
    const msg = res.data.msg || "";
    if (code === 401) {
      return true;
    }
    if (code === 500 && msg.indexOf("登录") !== -1) {
      return true;
    }
  }
  return false;
}

/**
 * 处理 token 过期：清除缓存，跳转登录页
 */
function handleTokenExpired() {
  app.globalData.token = "";
  wx.removeStorageSync("token");
  wx.showToast({
    title: "登录已过期，请重新登录",
    icon: "none",
    duration: 2000
  });
  setTimeout(() => {
    wx.reLaunch({
      url: "/pages/index/index"
    });
  }, 2000);
}

module.exports = { request, buildUrl, getBaseUrl, isTokenExpired };
