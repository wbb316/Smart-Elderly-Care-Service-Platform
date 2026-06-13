const app = getApp()
const { request } = require('../../../utils/request');
Page({
  data: {
    leave: null,
    records: [],
    showBackModal: false,
    actualReturnTime: ''
  },

  onLoad(options) {
    const id = options.id
    if (id) {
      this.loadDetail(id)
      this.loadRecords(id)
    }
  },

  // 加载请假详情
  loadDetail(id) {
    request({
      url: `/wxLogin/leave/${id}`,
      method: 'GET'
    }).then((res) => {
      if (res.data.code === 200) {
        const item = res.data.data
        this.setData({
          leave: {
            ...item,
            startTime: this.formatTime(item.leaveStartTime),
            endTime: this.formatTime(item.plannedReturnTime),
            actualReturn: this.formatTime(item.actualReturnTime),
            statusLabel: this.getStatusLabel(item.status)
          }
        })
      }
    })
  },

  // 加载审批记录
  loadRecords(id) {
    request({
      url: `/wxLogin/leave/records/${id}`,
      method: 'GET'
    }).then((res) => {
      if (res.data.code === 200) {
        this.setData({ records: res.data.data || [] })
      }
    })
  },

  formatTime(str) {
    if (!str) return ''
    return str.replace('T', ' ').substring(0, 16)
  },

  getStatusLabel(status) {
    const map = {
      'draft': '草稿', 'approving': '审批中', 'approved': '已通过',
      'rejected': '已拒绝', 'leaving': '请假中', 'returned': '已返回',
      'timeout': '超时未归'
    }
    return map[status] || status
  },

  // 销假弹窗
  showBack() {
    this.setData({ showBackModal: true })
  },

  hideBack() {
    this.setData({ showBackModal: false })
  },

  onReturnTimeChange(e) {
    this.setData({ actualReturnTime: e.detail.value })
  },

  // 确认销假
  confirmBack() {
    const { actualReturnTime, leave } = this.data
    if (!actualReturnTime) {
      wx.showToast({ title: '请选择实际返回时间', icon: 'none' })
      return
    }
    wx.showLoading({ title: '处理中...' })
    request({
      url: '/wxLogin/leave/return',
      method: 'PUT',
      data: {
        id: leave.id,
        actualReturnTime: actualReturnTime + ' 00:00:00',
        isReturned: 1
      }
    }).then((res) => {
      wx.hideLoading()
      if (res.data.code === 200) {
        wx.showToast({ title: '销假成功', icon: 'success' })
        this.setData({ showBackModal: false })
        this.loadDetail(leave.id)
      } else {
        wx.showToast({ title: res.data.msg || '操作失败', icon: 'none' })
      }
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '网络请求失败', icon: 'error' })
    })
  }
})
