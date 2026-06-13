const app =getApp();
const { request } = require('../../utils/request');
Page({
  data: {
    familyList: [

    ],
    showFamilyModal: false, // 控制弹窗显隐
    inputFamilyName: ''     // 弹窗输入内容
  },

  // 解绑操作
  handleUnbind(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: "提示",
      content: "确定要解绑该家人吗？",
      success: (res) => {
        if (res.confirm) {
          // 执行解绑逻辑，如更新数据
         request({
          url: `/wxLogin/deleteElder/${id}`,
          method:'DELETE'
         }).then((res) =>{
            if(res.data.code == 200){
              wx.showToast({
                title: '解绑成功',
                icon: 'success',
                duration: 2000
              });
              this.getElderList();
            }
          })
        }
      }
    });
  },

  // 跳转我的账单
  goToBill(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/family/pages/mybill/mybill?id=${id}`
    });
  },

  // 跳转请假列表
  goToLeave(e) {
    wx.navigateTo({
      url: `/family/pages/leave/list/list`
    });
  },

  // 跳转绑定家人
  goToBind() {
    wx.navigateTo({
      url: "/family/pages/addfamily/addfamily"
    });
  },

  getElderList() {
    request({
      url: '/wxLogin/getElderBedList',
      method: "POST"
    }).then((resp) => {
      if (resp.data.code == 200) {
        this.setData({
          familyList: resp.data.data
        })
      }
    })
  },

  onLoad(options) {
    this.getElderList()
  },
  onShow() {
    this.getElderList()
  },
});