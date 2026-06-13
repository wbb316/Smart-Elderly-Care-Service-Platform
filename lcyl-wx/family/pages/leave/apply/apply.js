const app = getApp()
const { request } = require('../../../utils/request');
Page({
  data: {
    elderList: [],
    selectedElder: null,
    elderInfo: null,

    companionType: '',           // 家属/护理人员/其他/无
    companionTypeOptions: ['家属', '护理人员', '其他', '无'],
    companionName: '',
    companionPhone: '',
    leaveStartTime: '',
    plannedReturnTime: '',
    leaveDays: '',
    leaveReason: '',

    submitting: false
  },

  onLoad() {
    this.loadElderOptions()
  },

  // 加载可请假老人列表（家属绑定的老人）
  loadElderOptions() {
    request({
      url: '/wxLogin/getElderBedList',
      method: 'POST'
    }).then((res) => {
      if (res.data.code === 200) {
        this.setData({ elderList: res.data.data || [] })
      }
    })
  },

  // 选择老人
  onElderChange(e) {
    const index = e.detail.value
    const elder = this.data.elderList[index]
    if (!elder) return
    this.setData({ selectedElder: elder })
    this.loadElderFormInfo(elder.id)
  },

  // 加载老人基本信息
  loadElderFormInfo(elderId) {
    request({
      url: `/wxLogin/leave/formInfo/${elderId}`,
      method: 'GET'
    }).then((res) => {
      if (res.data.code === 200) {
        this.setData({ elderInfo: res.data.data })
      }
    })
  },

  // 选择陪同人类型
  onCompanionTypeChange(e) {
    const index = e.detail.value
    this.setData({ companionType: this.data.companionTypeOptions[index] })
  },

  // 选择开始时间
  onStartTimeChange(e) {
    this.setData({ leaveStartTime: e.detail.value })
    this.calcLeaveDays()
  },

  // 选择结束时间
  onEndTimeChange(e) {
    this.setData({ plannedReturnTime: e.detail.value })
    this.calcLeaveDays()
  },

  // 输入框同步
  onInputChange(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [field]: e.detail.value })
  },

  // 计算请假天数
  calcLeaveDays() {
    const { leaveStartTime, plannedReturnTime } = this.data
    if (!leaveStartTime || !plannedReturnTime) return
    const start = new Date(leaveStartTime.replace(/-/g, '/'))
    const end = new Date(plannedReturnTime.replace(/-/g, '/'))
    const diff = Math.ceil((end - start) / (1000 * 60 * 60 * 24))
    this.setData({ leaveDays: diff > 0 ? diff : 0 })
  },

  // 提交请假
  submitLeave() {
    const { selectedElder, companionType, companionName, companionPhone,
            leaveStartTime, plannedReturnTime, leaveReason, submitting } = this.data

    if (submitting) return
    if (!selectedElder) { wx.showToast({ title: '请选择老人', icon: 'none' }); return }
    if (!companionType) { wx.showToast({ title: '请选择陪同人类型', icon: 'none' }); return }
    if (!leaveStartTime) { wx.showToast({ title: '请选择开始时间', icon: 'none' }); return }
    if (!plannedReturnTime) { wx.showToast({ title: '请选择预计返回时间', icon: 'none' }); return }
    if (!leaveReason.trim()) { wx.showToast({ title: '请输入请假原因', icon: 'none' }); return }

    this.setData({ submitting: true })
    wx.showLoading({ title: '提交中...' })

    request({
      url: '/wxLogin/leave/submit',
      method: 'POST',
      data: {
        elderId: selectedElder.id,
        companionType: companionType,
        companionName: companionName || undefined,
        companionPhone: companionPhone || undefined,
        leaveStartTime: leaveStartTime + ' 00:00:00',
        plannedReturnTime: plannedReturnTime + ' 00:00:00',
        leaveReason: leaveReason.trim()
      }
    }).then((res) => {
      wx.hideLoading()
      if (res.data.code === 200) {
        wx.showToast({ title: '提交成功', icon: 'success' })
        setTimeout(() => wx.navigateBack(), 1500)
      } else {
        wx.showToast({ title: res.data.msg || '提交失败', icon: 'none' })
      }
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '网络请求失败', icon: 'error' })
    }).finally(() => {
      this.setData({ submitting: false })
    })
  }
})
