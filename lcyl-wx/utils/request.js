const app = getApp();
const envConfig = require('../config');

function getBaseUrl() {
  // 优先用 app.globalData（启动时按环境写入），取不到时回退到环境配置
  return (app && app.globalData && app.globalData.baseUrl) || envConfig.getBaseUrl();
}

function buildUrl(path) {
  const base = getBaseUrl();
  const cleanPath = path.startsWith('/') ? path : '/' + path;
  return base + cleanPath;
}

/**
 * 解码 JWT payload（仅读取过期时间等声明，不做签名校验）。
 * 失败返回 null，调用方会回退到后端校验，不影响原有逻辑。
 */
function decodeJwtPayload(token) {
  try {
    var parts = token.split('.');
    if (parts.length !== 3) return null;
    var b64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    while (b64.length % 4 !== 0) b64 += '=';
    var table = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
    var binary = '';
    for (var i = 0; i < b64.length; i += 4) {
      var a = table.indexOf(b64.charAt(i));
      var b = table.indexOf(b64.charAt(i + 1));
      var c = table.indexOf(b64.charAt(i + 2));
      var d = table.indexOf(b64.charAt(i + 3));
      if (a === -1 || b === -1) return null;
      binary += String.fromCharCode((a << 2) | (b >> 4));
      if (c !== -1 && c !== 64) binary += String.fromCharCode(((b & 15) << 4) | (c >> 2));
      if (d !== -1 && d !== 64) binary += String.fromCharCode(((c & 3) << 6) | d);
    }
    return JSON.parse(binary);
  } catch (e) {
    return null;
  }
}

// 本地快路径的提前量（秒）：exp 剩余大于该值视为"安全未过期"，直接放行。
// 小于该值时仍走后端校验，保证与原来"过期必跳登录"的逻辑完全一致。
var LOCAL_VERIFY_GRACE_SECONDS = 10 * 60;

/**
 * 向后端验证 token 是否有效。有效则 resolve，无效/过期则跳转登录页并 reject。
 *
 * 快路径：token 为 JWT 且本地 exp 未过期 → 直接 resolve（0 次网络往返）。
 *         不在这里调 refreshSession：页面本来就会显式刷新，重复调用会翻倍。
 * 慢路径：token 快过期 / 无法本地解析 → 请求后端 checkToken 校验，过期则跳登录页。
 *
 * 网络异常与服务端错误**不再**判定为登录过期：
 *   原实现里 wx.request 的 fail（断网、超时）也会清 token 并强制跳登录页，
 *   弱网抖动就把用户踢下线。现在只有明确的 401/令牌无效才清会话。
 *   此外后端每次请求仍会校验 token（401 兜底跳登录），语义不变。
 */
function verifyToken() {
  return new Promise(function (resolve, reject) {
    var token = app.globalData.token || wx.getStorageSync('token');
    if (!token) {
      handleTokenExpired();
      reject(new Error('未登录'));
      return;
    }

    // 本地快路径：JWT exp 未过期则直接放行，跳过网络往返
    var payload = decodeJwtPayload(token);
    if (payload && payload.exp) {
      var nowSec = Math.floor(Date.now() / 1000);
      if (payload.exp - nowSec > LOCAL_VERIFY_GRACE_SECONDS) {
        resolve();
        return;
      }
    }

    // 慢路径：快过期或无法本地判断 → 走后端校验（与原逻辑一致）
    wx.request({
      url: buildUrl('/wxLogin/checkToken'),
      method: 'GET',
      header: { 'Authorization': token },
      success: function (res) {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          resolve();
          return;
        }
        // 只有明确的"未授权/令牌无效"才算过期；服务端 5xx 等异常不应清会话
        if (isTokenExpired(res)) {
          handleTokenExpired();
          reject(new Error('令牌无效'));
        } else {
          reject(new Error('校验失败'));
        }
      },
      fail: function () {
        // 网络异常 ≠ 登录过期：不清 token、不跳登录页，交由调用方决定是否重试
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
