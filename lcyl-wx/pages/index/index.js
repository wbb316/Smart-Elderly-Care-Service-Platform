// index.js
const { request } = require('../../utils/request');
const app = getApp();
Page({

  data: {},

  // 一键登录：获取手机号 + wx.login
  getPhoneNumber(e) {
    if (!e.detail.code) {
      wx.showToast({
        title: '已取消授权',
        icon: 'none'
      })
      return;
    }

    let phoneCode = e.detail.code;

    wx.login({
      success: (res) => {
        let loginCode = res.code;

        request({
          url: '/wxLogin/phoneLogin',
          method: 'POST',
          data: {
            code: loginCode,
            phoneCode: phoneCode
          }
        }).then((res) => {
          if (res.data && res.data.code === 200) {
            const resultData = res.data.data || {};
            app.globalData.token = resultData.token;
            app.globalData.userName = resultData.name;
            wx.setStorageSync('token', resultData.token);

            wx.showToast({
              title: '登录成功',
              icon: 'success'
            });
            wx.switchTab({ url: '/pages/home/home' });
          } else {
            wx.showToast({
              title: res.data.msg || '登录失败',
              icon: 'none',
              duration: 3000
            });
          }
        }).catch((err) => {
          wx.showToast({
            title: '网络请求失败',
            icon: 'none'
          });
        });
      },
      fail: (err) => {
        wx.showToast({
          title: '微信登录失败',
          icon: 'none'
        });
      }
    });
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad: function (options) {
    // 必须在当前会话授权登录，不走自动跳转
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