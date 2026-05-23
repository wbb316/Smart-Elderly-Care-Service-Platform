const app = getApp()

Page({
  data: {
    bookingId: "",
    roomName: "",
    price: "",
    bookingDate: ""
  },

  onLoad() {
    const result = app.globalData.roomBookingResult || {};
    this.setData({
      bookingId: result.bookingId || "",
      roomName: result.roomName || "",
      price: result.price || "",
      bookingDate: result.bookingDate || ""
    });
    // 用完清掉，避免残留
    app.globalData.roomBookingResult = null;
  },

  backToHome() {
    wx.switchTab({ url: '/pages/home/home' })
  }
})
