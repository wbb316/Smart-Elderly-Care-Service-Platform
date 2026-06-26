const app = getApp();

function getBaseUrl() {
  return (app && app.globalData && app.globalData.baseUrl) || 'http://localhost:8080';
}

function buildUrl(path) {
  const base = getBaseUrl();
  const cleanPath = path.startsWith('/') ? path : '/' + path;
  return base + cleanPath;
}

/**
 * 向后端验证 token 是否有效。有效则 resolve，无效/过期则跳转登录页并 reject
 */
function verifyToken() {
  return new Promise((resolve, reject) => {
    const token = app.globalData.token || wx.getStorageSync('token');
    if (!token) {
      handleTokenExpired();
      reject(new Error('未登录'));
      return;
    }
    wx.request({
      url: buildUrl('/wxLogin/checkToken'),
      method: 'GET',
      header: { 'Authorization': token },
      success: function (res) {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          resolve();
        } else {
          handleTokenExpired();
          reject(new Error('令牌无效'));
        }
      },
      fail: function () {
        handleTokenExpired();
        reject(new Error('网络错误'));
      }
    });
  });
}

/**
 * 封装 wx.request，自动注入 token 并处理登录过期
 */
function request(options) {
  return new Promise(function (resolve, reject) {
    var header = Object.assign(
      { 'content-type': 'application/json' },
      options.header || {}
    );

    var token = app.globalData.token || wx.getStorageSync('token');
    if (token) {
      header['Authorization'] = token;
    }

    var url = options.url && options.url.startsWith('http') ? options.url : buildUrl(options.url);

    wx.request({
      url: url,
      method: options.method,
      data: options.data,
      header: header,
      success: function (res) {
        if (isTokenExpired(res)) {
          handleTokenExpired();
          reject(new Error('登录已过期'));
          return;
        }
        resolve(res);
      },
      fail: function (err) {
        reject(err);
      }
    });
  });
}

function isTokenExpired(res) {
  if (res.statusCode === 401) return true;
  if (res.data) {
    if (res.data.code === 401) return true;
    if (res.data.code === 500 && res.data.msg && res.data.msg.indexOf('登录') !== -1) return true;
  }
  return false;
}

function handleTokenExpired() {
  app.globalData.token = '';
  wx.removeStorageSync('token');
  // 延迟一帧再跳转，避免与当前页面导航冲突导致白屏
  setTimeout(function () {
    wx.reLaunch({ url: '/pages/index/index' });
  }, 100);
}

function refreshSession() {
  var token = app.globalData.token || wx.getStorageSync('token');
  if (!token) return;
  wx.request({
    url: buildUrl('/wxLogin/refreshToken'),
    method: 'GET',
    header: { 'Authorization': token },
    success: function () {},
    fail: function () {}
  });
}

module.exports = { request, buildUrl, verifyToken, isTokenExpired, refreshSession };
