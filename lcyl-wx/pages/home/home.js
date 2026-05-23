// pages/home/home.js
const app =getApp();
Page({

  /**
   * 页面的初始数据
   */
  data: {
    list: [
      // {
      //   id: 1,
      //   imageUrl: '/images/arrows.png',
      //   title: '标题1',
      //   description: '描述信息1'
      // },
      // {
      //   id: 2,
      //   imageUrl: '/images/arrows.png',
      //   title: '标题2',
      //   description: '描述信息2'
      // },
      // {
      //   id: 3,
      //   imageUrl: '/images/arrows.png',
      //   title: '标题3',
      //   description: '描述信息3'
      // }
    ]
  },


 getBedType(){
  wx.request({
    url:'http://localhost:8080/wxLogin/getRoomType',
    method:'GET',
    header:{
      'content-type': 'application/json',
      'authorization': app.globalData.token|| ''
    },
    success:(res)=>{
        console.log('getRoomType返回:', res.data);
        if(res.data.code == 200){
          const list = (res.data.data || []).map(item => ({
            ...item,
            photo: item.photo || '/images/default-room.png'
          }));
          console.log('处理后的list:', list);
          this.setData({ list });
        } else {
          console.error('getRoomType失败:', res.data.msg);
        }
    },
    fail:(err)=>{
        console.error('getRoomType请求失败:', err);
    }
  })
},
  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    this.getBedType();
  },
  onImageError(e) {
    const index = e.currentTarget.dataset.index;
    const item = this.data.list[index];
    console.error('图片加载失败 index:', index, 'src:', item ? item.photo : 'unknown');
  },

  goRoomDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: '/home/pages/roomDetail/roomDetail?id=' + id
    })
  },
  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady() {

  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    this.getBedType();
  },

  /**
   * 生命周期函数--监听页面隐藏
   */
  onHide() {

  },

  /**
   * 生命周期函数--监听页面卸载
   */
  onUnload() {

  },

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh() {

  },

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom() {

  },

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage() {

  }
})