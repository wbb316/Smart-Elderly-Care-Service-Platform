const app = getApp();

Page({
  data: {
    sourceType: "order",
    orderId: "",
    billId: "",
    serviceName: "",
    price: "",
    unit: "",
    imageUrl: "",
    familyName: "",
    serviceTime: "",
    remark: "",
    totalPrice: "",
    countDown: "",
    isTimeout: false,
    timer: null
  },

  onLoad(options) {
    this.setData({
      sourceType: options.sourceType || "order",
      orderId: options.orderId || "",
      billId: options.billId || "",
      serviceName: decodeURIComponent(options.serviceName || ""),
      price: options.price || "",
      unit: options.unit || "",
      imageUrl: decodeURIComponent(options.imageUrl || ""),
      familyName: decodeURIComponent(options.familyName || ""),
      serviceTime: decodeURIComponent(options.serviceTime || ""),
      remark: decodeURIComponent(options.remark || ""),
      totalPrice: options.totalPrice || options.price || "0"
    });

    this.startCountDown(900);
  },

  startCountDown(seconds) {
    let total = seconds;
    this.updateTime(total);

    const timer = setInterval(() => {
      total -= 1;
      this.updateTime(total);

      if (total <= 0) {
        clearInterval(this.data.timer);
        this.setData({
          isTimeout: true,
          countDown: "00:00"
        });
      }
    }, 1000);

    this.setData({ timer });
  },

  updateTime(total) {
    const safeTotal = Math.max(total, 0);
    let minute = Math.floor(safeTotal / 60);
    let second = safeTotal % 60;
    minute = minute < 10 ? `0${minute}` : `${minute}`;
    second = second < 10 ? `0${second}` : `${second}`;
    this.setData({
      countDown: `${minute}:${second}`
    });
  },

  confirmPay() {
    if (this.data.isTimeout) {
      wx.showToast({ title: "订单已超时", icon: "none" });
      return;
    }

    const isBillPay = this.data.sourceType === "bill";
    const isRoomPay = this.data.sourceType === "room";
    const targetId = isBillPay ? this.data.billId : this.data.orderId;
    if (!targetId) {
      wx.showToast({
        title: isBillPay ? "账单信息不存在" : "订单信息不存在",
        icon: "none"
      });
      return;
    }

    wx.showLoading({ title: "支付中..." });

    let payUrl;
    if (isRoomPay) {
      payUrl = `http://localhost:8080/wxLogin/payRoomBooking/${targetId}`;
    } else if (isBillPay) {
      payUrl = `http://localhost:8080/wxLogin/payBill/${targetId}`;
    } else {
      payUrl = `http://localhost:8080/wxLogin/payOrder/${targetId}`;
    }

    wx.request({
      url: payUrl,
      method: "POST",
      header: {
        "content-type": "application/json",
        authorization: app.globalData.token || ""
      },
      success: (res) => {
        wx.hideLoading();
        if (!res.data || res.data.code !== 200) {
          wx.showToast({
            title: (res.data && res.data.msg) || "支付失败",
            icon: "none"
          });
          return;
        }

        clearInterval(this.data.timer);
        wx.showToast({ title: "支付成功", icon: "success" });

        setTimeout(() => {
          if (isBillPay) {
            wx.redirectTo({
              url: `/my/pages/myBillDetail/myBillDetail?id=${targetId}`
            });
            return;
          }

          if (isRoomPay) {
            // 用 globalData 传参，避免 URL 编码导致中文变乱码
            app.globalData.roomBookingResult = {
              bookingId: targetId,
              roomName: this.data.serviceName,
              price: this.data.price,
              bookingDate: this.data.serviceTime
            };
            wx.redirectTo({
              url: `/home/pages/roomSuccess/roomSuccess`
            });
            return;
          }

          const data = this.data;
          wx.navigateTo({
            url: `/servicePage/pages/successPage/successPage?` +
              `orderId=${data.orderId}` +
              `&serviceName=${encodeURIComponent(data.serviceName)}` +
              `&price=${data.price}` +
              `&unit=${data.unit}` +
              `&imageUrl=${encodeURIComponent(data.imageUrl || "")}` +
              `&familyName=${encodeURIComponent(data.familyName)}` +
              `&serviceTime=${encodeURIComponent(data.serviceTime)}` +
              `&remark=${encodeURIComponent(data.remark || "")}` +
              `&totalPrice=${data.totalPrice}`
          });
        }, 1000);
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: "网络异常，请稍后重试", icon: "none" });
      }
    });
  },

  onUnload() {
    clearInterval(this.data.timer);
  }
});
