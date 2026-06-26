const { request, verifyToken, refreshSession } = require('../../utils/request');
const app = getApp()
Page({

  data: {
    defaultAvatar: '/images/head.png',
    defaultName: '用户名',
    form: {
      name: '',
      avatar: '',
      gender: ''
    }
  },

  goProfile() {
    wx.navigateTo({
      url: '/my/pages/myProfile/myProfile'
    })
  },

  logout() {
    app.globalData.token = ''
        wx.removeStorageSync('token')
    wx.reLaunch({
      url: "/pages/index/index"
    })
  },

  loadUserInfo() {
    request({
      url: '/wxLogin/get/profile',
      method: 'GET'
    }).then((res) => {
      const result = res.data || {}
      if (result.code === 200) {
        const user = result.data || {}
        this.setData({
          'form.name': user.name || '',
          'form.avatar': user.avatar || '',
          'form.gender': user.gender || ''
        })
      } else {
        wx.showToast({
          title: result.msg || '获取失败',
          icon: 'none'
        })
      }
    }).catch(() => {
      wx.showToast({
        title: '网络异常',
        icon: 'none'
      })
    })
  },

  onShow() {
    verifyToken().then(() => {
    refreshSession();
    this.loadUserInfo();
    }).catch(() => {});
  }
})
