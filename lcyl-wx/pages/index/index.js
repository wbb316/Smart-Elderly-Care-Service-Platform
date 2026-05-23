// index.js
const app = getApp();
Page({

  /**
   * 页面的初始数据
   */
  data: {},

  // 一键登录：获取手机号 + wx.login
  getPhoneNumber(e) {
    // 1. 先判断用户是否允许授权
    if (!e.detail.code) {
      wx.showToast({
        title: '已取消授权',
        icon: 'none'
      })
      return;
    }

    // 2. 获取手机号专用 code（必须用这个）
    let phoneCode = e.detail.code;

    // 3. 执行 wx.login 获取登录 code
    wx.login({
      success: (res) => {
        let loginCode = res.code;

        console.log("登录code：", loginCode);
        console.log("手机号code：", phoneCode);

        // 4. 两个 code 一起传给后端
        wx.request({
          url: "http://localhost:8080/wxLogin/phoneLogin",
          method: "POST",
          data: {
            code: loginCode, // 登录用
            phoneCode: phoneCode // 拿手机号
          },
          header: {
            'content-type': 'application/json'
          },
            success: (res) => {
                console.log("后端完整返回:", res.data); // 打印完整数据方便调试

                // 1. 先判断后端是否处理成功 (code === 200)
                if (res.data && res.data.code === 200) {
                    // 2. 安全地获取 data 对象，防止 undefined
                    const resultData = res.data.data || {};

                    app.globalData.token = resultData.token;
                    app.globalData.userName = resultData.name;

                    // 缓存 token 到本地，下次自动登录
                    wx.setStorageSync('token', resultData.token);

                    console.log("登录成功，Token:", app.globalData.token);

                    wx.showToast({
                        title: '登录成功',
                        icon: 'success'
                    });

                    wx.switchTab({
                        url: "/pages/home/home"
                    });
                } else {
                    // 3. 如果后端报错（如 500），提示错误信息而不是崩溃
                    wx.showToast({
                        title: res.data.msg || '登录失败',
                        icon: 'none',
                        duration: 3000
                    });
                    console.error("登录失败详情:", res.data.msg);
                }
            },
          fail: (err) => {
            console.log("请求失败：", err);
          }
        });
      },
      fail: (err) => {
        console.log("wx.login 失败：", err);
      }
    });
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad: function (options) {
    // 检查是否有缓存的 token，有则直接跳转首页
    const token = wx.getStorageSync('token')
    if (token) {
      app.globalData.token = token
      wx.switchTab({ url: '/pages/home/home' })
    }
  },

  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady: function () {

  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow: function () {

  },

  /**
   * 生命周期函数--监听页面隐藏
   */
  onHide: function () {

  },

  /**
   * 生命周期函数--监听页面卸载
   */
  onUnload: function () {

  },

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh: function () {

  },

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom: function () {

  },

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage: function () {

  }
})