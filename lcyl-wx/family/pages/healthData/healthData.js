const { request, verifyToken } = require('../../../utils/request');
Page({
  data: {
    elderId: '',
    elderName: '',
    activeTab: 'health',
    loading: true,
    noData: false,
    healthData: null,
    abilityData: null
  },

  onLoad(options) {
    this.setData({ elderId: options.elderId || '' });
    this.loadHealthData();
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({ activeTab: tab });
  },

  loadHealthData() {
    const elderId = this.data.elderId;
    if (!elderId) {
      this.setData({ loading: false, noData: true });
      return;
    }

    verifyToken().then(() => {
      request({
        url: '/wxLogin/healthData/' + elderId,
        method: 'GET'
      }).then((res) => {
        if (res.data && res.data.code === 200) {
          const data = res.data.data || {};
          const health = data.healthEvaluate;
          const ability = data.abilityEvaluate;

          this.setData({
            healthData: health || null,
            abilityData: ability || null,
            noData: !health && !ability
          });
        } else {
          wx.showToast({ title: res.data.msg || '暂无数据', icon: 'none' });
          this.setData({ noData: true });
        }
      }).catch(() => {
        this.setData({ noData: true });
      }).finally(() => {
        this.setData({ loading: false });
      });
    }).catch(() => {
      this.setData({ loading: false, noData: true });
    });
  },

  goBack() {
    wx.navigateBack({ delta: 1 });
  }
});
