const { request, verifyToken } = require('../../../utils/request');
Page({
  data: {
    detail: {},
    bookingDate: "",
    remark: "",
    bookingId: ""
  },

  onLoad(options) {
    verifyToken().then(() => {
    this.setData({
      detail: {
        id: options.roomTypeId,
        name: options.roomName,
        price: options.price,
        photo: options.photo
      },
      bookingDate: decodeURIComponent(options.bookingDate || "")
    })
    }).catch(() => {});
  },

  onRemarkInput(e) {
    this.setData({ remark: e.detail.value })
  },

  submitOrder() {
    const data = this.data
    if (!data.detail.id || !data.bookingDate) {
      wx.showToast({ title: "请完善预定信息", icon: "none" })
      return
    }

    wx.showLoading({ title: "提交中..." })
    request({
      url: "/wxLogin/createRoomBooking",
      method: "POST",
      data: {
        roomTypeId: Number(data.detail.id),
        bookingDate: data.bookingDate,
        remark: data.remark
      }
    }).then(res => {
      wx.hideLoading()
      if (res.data.code !== 200 || !res.data.data) {
        wx.showToast({ title: res.data.msg || "预定失败", icon: "none" })
        return
      }

      const bookingInfo = res.data.data
      this.setData({ bookingId: bookingInfo.id })

      wx.showToast({ title: "预定提交成功", icon: "success", duration: 1200 })

      setTimeout(() => {
        wx.navigateTo({
          url: `/servicePage/pages/payPage/payPage?sourceType=room` +
            `&orderId=${bookingInfo.id}` +
            `&serviceName=${encodeURIComponent(data.detail.name)}` +
            `&price=${data.detail.price}` +
            `&unit=月` +
            `&imageUrl=${encodeURIComponent(data.detail.photo || "")}` +
            `&familyName=房型预定` +
            `&serviceTime=${encodeURIComponent(data.bookingDate)}` +
            `&remark=${encodeURIComponent(data.remark || "")}` +
            `&totalPrice=${data.detail.price}`
        })
      }, 1200)
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: "网络异常，请稍后重试", icon: "none" })
    })
  }
})
