// pages/home/home.js
const { request, refreshSession } = require('../../utils/request');
const app = getApp();
Page({

  data: {
    list: []
  },

  getBedType() {
    request({
      url: '/wxLogin/getRoomType',
      method: 'GET'
    }).then((res) => {
      if (res.data.code == 200) {
        const list = (res.data.data || []).map(item => ({
          ...item,
          photo: item.photo || '/images/default-room.png'
        }));
        this.setData({ list });
      }
    }).catch(() => {
      // 请求失败静默处理
    });
  },

  onLoad(options) {
    this.getBedType();
  },

  onImageError() {
    // 图片加载失败使用默认样式
  },

  goRoomDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: '/home/pages/roomDetail/roomDetail?id=' + id
    })
  },

  onReady() { },

  onShow() {
    refreshSession();
    this.getBedType();
  },

  onHide() { },

  onUnload() { },

  onPullDownRefresh() { },

  onReachBottom() { },

  onShareAppMessage() { }
})
