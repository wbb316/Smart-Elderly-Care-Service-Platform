const app = getApp()
Page({
  data: {
    tabs: [
      { label: '全部', value: '' },
      { label: '审批中', value: 'approving' },
      { label: '请假中', value: 'leaving' },
      { label: '已完成', value: 'returned' }
    ],
    currentTab: '',
    leaveList: [],
    loading: false
  },

  onLoad() {
    this.loadList()
  },

  onShow() {
    this.loadList()
  },

  // 切换tab
  switchTab(e) {
    const value = e.currentTarget.dataset.value
    this.setData({ currentTab: value })
    this.loadList()
  },

  // 加载请假列表
  loadList() {
    this.setData({ loading: true })
    const params = {}
    if (this.data.currentTab) {
      params.status = this.data.currentTab
    }
    wx.request({
      url: 'http://localhost:8080/wxLogin/leave/list',
      method: 'GET',
      data: params,
      header: { 'authorization': app.globalData.token },
      success: (res) => {
        if (res.data.code === 200) {
          const rows = (res.data.rows || []).map(item => ({
            ...item,
            startTime: this.formatTime(item.leaveStartTime),
            endTime: this.formatTime(item.plannedReturnTime),
            statusLabel: this.getStatusLabel(item.status),
            statusClass: this.getStatusClass(item.status)
          }))
          this.setData({ leaveList: rows })
        }
      },
      fail: () => wx.showToast({ title: '加载失败', icon: 'error' }),
      complete: () => this.setData({ loading: false })
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
      'timeout': '超时未归', 'cancelled': '已作废'
    }
    return map[status] || status
  },

  getStatusClass(status) {
    if (status === 'approving') return 'status-approving'
    if (status === 'leaving' || status === 'approved') return 'status-leaving'
    if (status === 'returned') return 'status-done'
    if (status === 'rejected') return 'status-reject'
    return 'status-default'
  },

  // 跳转详情
  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/family/pages/leave/detail/detail?id=${id}` })
  },

  // 发起请假
  goApply() {
    wx.navigateTo({ url: '/family/pages/leave/apply/apply' })
  }
})
