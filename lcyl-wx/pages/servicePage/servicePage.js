const app = getApp()
const { request, verifyToken, refreshSession } = require('../../utils/request');
Page({
  data: {
    serviceList: [],
    originalList: [],
    searchKey: "",
    showOrderPopup: false,
    selectedService: null,
    familyName: "",
    familyId: "",
    serviceDate: "",
    serviceTime: "",
    showFamilyList: false,
    familyList: [],
    nowDate: "",
    showNoFamilyTip: false // 无家人弹窗控制变量
  },

  onLoad(options) {
    verifyToken().then(() => {
    this.getServiceList()
    this.getFamilyList()
    this.setData({ nowDate: new Date().toISOString().slice(0, 10) })
    }).catch(() => {});
  },

  onShow() {
    verifyToken().then(() => {
    refreshSession();
    if (this.data.originalList.length === 0) {
      this.getServiceList()
      this.getFamilyList()
    }
    }).catch(() => {});
  },

  onSearchInput(e) {
    this.setData({ searchKey: e.detail.value.trim() })
    this.doSearch()
  },

  doSearch() {
    const { originalList, searchKey } = this.data
    if (!searchKey) {
      this.setData({ serviceList: originalList });
      return
    }
    const filtered = originalList.filter(i => i.itemName.includes(searchKey))
    this.setData({ serviceList: filtered })
  },

  getServiceList() {
    request({
      url: "/wxLogin/item/list"
    }).then(res => {
      if (res.data.code === 200) {
        this.setData({ serviceList: res.data.rows, originalList: res.data.rows })
      } else {
        wx.showToast({ title: res.data.msg || "加载服务列表失败", icon: "none" })
      }
    }).catch(err => {
      wx.showToast({ title: "网络请求失败，请检查后端是否启动", icon: "none" })
    })
  },

  goServiceDetail(e) {
    wx.navigateTo({ url: '/servicePage/pages/serviceDetail/serviceDetail?id=' + e.currentTarget.dataset.id })
  },

  showOrderPopup(e) {
    const id = e.currentTarget.dataset.id
    const service = this.data.serviceList.find(i => i.id == id)
    this.setData({
      showOrderPopup: true,
      selectedService: service,
      familyName: "",
      serviceDate: "",
      serviceTime: ""
    })
  },

  hideOrderPopup() {
    this.setData({ showOrderPopup: false })
  },

  getFamilyList() {
    request({
      url: "/wxLogin/getElderBedList",
      method: "POST"
    }).then(res => {
      if (res.data.code === 200) {
        this.setData({ familyList: res.data.data || [] })
      } else {
        this.setData({ familyList: [] })
      }
    }).catch(() => {
      this.setData({ familyList: [] })
    })
  },

  // 检查家人列表，控制弹窗显示（先请求后端，再判断）
  checkFamilyAndShowPicker() {
    request({
      url: "/wxLogin/getElderBedList",
      method: "POST"
    }).then(res => {
      if (res.data.code === 200 && res.data.data && res.data.data.length > 0) {
        this.setData({ familyList: res.data.data })
        this.setData({ showFamilyList: true })
      } else {
        this.setData({ familyList: [] })
        this.setData({ showNoFamilyTip: true })
      }
    }).catch(() => {
      this.setData({ familyList: [] })
      this.setData({ showNoFamilyTip: true })
    })
  },

  showFamilyPicker() {
    this.setData({ showFamilyList: true })
  },

  hideFamilyPicker() {
    this.setData({ showFamilyList: false })
  },

  selectFamilyItem(e) {
    this.setData({
      familyId: e.currentTarget.dataset.id,
      familyName: e.currentTarget.dataset.name,
      showFamilyList: false
    })
  },

  onDateChange(e) {
    this.setData({ serviceDate: e.detail.value })
  },

  onTimeChange(e) {
    this.setData({ serviceTime: e.detail.value })
  },

  // 关闭无家人弹窗
  hideNoFamilyTip() {
    this.setData({ showNoFamilyTip: false })
  },

  // 跳转绑定家人页面（替换为你的实际路径）
  goToBindFamily() {
    this.setData({ showNoFamilyTip: false })
    wx.navigateTo({
      url: '/family/pages/addfamily/addfamily'
    })
  },

  submitOrder() {
    const { familyName, serviceDate, serviceTime, selectedService, familyId } = this.data

    if (!familyName || !serviceDate || !serviceTime) {
      wx.showToast({ title: '请完善信息', icon: 'none' })
      return
    }

    const fullTime = serviceDate + ' ' + serviceTime
    wx.navigateTo({
      url: `/servicePage/pages/orderConfirm/orderConfirm?serviceId=${selectedService.id}&serviceName=${selectedService.itemName}&price=${selectedService.price}&unit=${selectedService.unit}&familyId=${familyId}&familyName=${familyName}&serviceTime=${encodeURIComponent(fullTime)}&imageUrl=${selectedService.imageUrl}`
    })
  },

  onPullDownRefresh() {
    this.getServiceList()
    setTimeout(() => wx.stopPullDownRefresh(), 1000)
  }
})