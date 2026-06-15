const app = getApp();
const { request, hasToken, refreshSession } = require('../../utils/request');
Page({
  data: {
    familyList: [],
    showFamilyModal: false,
    inputFamilyName: ''
  },

  handleUnbind(e) {
    if (!hasToken()) return wx.reLaunch({ url: '/pages/index/index' });
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: "提示",
      content: "确定要解绑该家人吗？",
      success: (res) => {
        if (res.confirm) {
         request({
          url: `/wxLogin/deleteElder/${id}`,
          method:'DELETE'
         }).then((res) =>{
            if(res.data.code == 200){
              wx.showToast({ title: '解绑成功', icon: 'success', duration: 2000 });
              this.getElderList();
            }
          })
        }
      }
    });
  },

  goToBill(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/family/pages/mybill/mybill?id=${id}` });
  },

  goToLeave(e) {
    wx.navigateTo({ url: `/family/pages/leave/list/list` });
  },

  goToBind() {
    wx.navigateTo({ url: "/family/pages/addfamily/addfamily" });
  },

  getElderList() {
    request({
      url: '/wxLogin/getElderBedList',
      method: "POST"
    }).then((resp) => {
      if (resp.data.code == 200) {
        this.setData({ familyList: resp.data.data })
      }
    }).catch(() => {})
  },

  onShow() {
    if (!hasToken()) {
      wx.reLaunch({ url: '/pages/index/index' });
      return;
    }
    refreshSession();
    this.getElderList();
  },
});