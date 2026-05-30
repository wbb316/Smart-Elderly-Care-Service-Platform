const app = getApp();

/**
 * 封装 wx.request，自动注入 token 并处理登录过期
 * 用法与原 wx.request 完全一致
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

    wx.request({
      ...options,
      header,
      success: (res) => {
        // 检查是否需要登录
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
  // HTTP 401
  if (res.statusCode === 401) {
    return true;
  }
  // 后端返回的 code 为 401 或 500 且消息中包含"登录"关键字
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

module.exports = { request, isTokenExpired };
