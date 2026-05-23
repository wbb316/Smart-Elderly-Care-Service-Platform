const app = getApp()
Page({
  data: {
    roomInfo: {},
    showDatePopup: false,
    bookingDate: "",
    today: ""
  },

  onLoad(options) {
    const id = options.id
    console.log("房型ID：", id)
    const now = new Date()
    const today = now.getFullYear() + '-' +
      String(now.getMonth() + 1).padStart(2, '0') + '-' +
      String(now.getDate()).padStart(2, '0')
    this.setData({ today })
    this.getRoomDetail(id)
  },

  getRoomDetail(id) {
    wx.request({
      url: 'http://localhost:8080/wxLogin/roomType/' + id,
      method: 'GET',
      header: {
        'content-type': 'application/json',
        'authorization': app.globalData.token || ''
      },
      success: (res) => {
        console.log("房型详情返回：", res.data)
        if (res.data.code === 200) {
          this.setData({ roomInfo: res.data.data })
          wx.setNavigationBarTitle({ title: res.data.data.name })
        }
      },
      fail(err) {
        console.log("请求失败", err)
      }
    })
  },

  showBooking() {
    this.setData({ showDatePopup: true })
  },

  closePopup() {
    this.setData({ showDatePopup: false })
  },

  onDateChange(e) {
    this.setData({ bookingDate: e.detail.value })
  },

  confirmBooking() {
    const { roomInfo, bookingDate } = this.data
    if (!bookingDate) {
      wx.showToast({ title: '请选择预定日期', icon: 'none' })
      return
    }
    this.setData({ showDatePopup: false })
    wx.navigateTo({
      url: `/home/pages/roomConfirm/roomConfirm?` +
        `roomTypeId=${roomInfo.id}` +
        `&roomName=${encodeURIComponent(roomInfo.name)}` +
        `&price=${roomInfo.price}` +
        `&photo=${encodeURIComponent(roomInfo.photo || '')}` +
        `&bookingDate=${encodeURIComponent(bookingDate)}`
    })
  }
})
