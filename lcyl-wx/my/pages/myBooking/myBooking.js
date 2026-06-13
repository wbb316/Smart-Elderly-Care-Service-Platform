const app = getApp()
const { request } = require('../../../utils/request');

Page({
  data: {
    bookingList: [],
    loading: false
  },

  onShow() {
    this.getMyBookings()
  },

  getMyBookings() {
    this.setData({ loading: true })
    request({
      url: '/wxLogin/myRoomBookings',
      method: 'GET'
    }).then((res) => {
      if (res.data.code === 200) {
        const statusMap = { '0': '待支付', '1': '已支付', '5': '已关闭' }
        const list = (res.data.data || []).map(item => ({
          id: item.id,
          bookingNo: item.bookingNo,
          roomName: item.roomTypeName,
          photo: item.roomTypePhoto,
          price: item.price ? Number(item.price).toFixed(2) : '0.00',
          bookingDate: item.bookingDate,
          status: item.status,
          statusText: statusMap[item.status] || '未知',
          remark: item.remark || '-',
          createTime: item.createTime ? item.createTime.replace('T', ' ').substring(0, 16) : '-'
        }))
        this.setData({ bookingList: list })
      }
    }).catch(() => {
      wx.showToast({ title: '查询失败', icon: 'none' })
    }).finally(() => {
      this.setData({ loading: false })
    })
  },

  // 去支付待支付的预定
  handlePay(e) {
    const id = e.currentTarget.dataset.id
    const booking = this.data.bookingList.find(item => item.id == id)
    if (!booking) {
      wx.showToast({ title: '预定不存在', icon: 'none' })
      return
    }
    app.globalData.roomBookingResult = {
      bookingId: booking.id,
      roomName: booking.roomName,
      price: booking.price,
      bookingDate: booking.bookingDate
    }
    wx.navigateTo({
      url: `/servicePage/pages/payPage/payPage?sourceType=room` +
        `&orderId=${booking.id}` +
        `&serviceName=${encodeURIComponent(booking.roomName)}` +
        `&price=${booking.price}` +
        `&totalPrice=${booking.price}` +
        `&serviceTime=${encodeURIComponent(booking.bookingDate)}`
    })
  }
})
