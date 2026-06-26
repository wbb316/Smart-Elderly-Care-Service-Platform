const { request } = require('../../../utils/request');
Page({
  data: {
    elderId: '',
    elderName: '',
    loading: true
  },

  onLoad(options) {
    this.setData({ elderId: options.elderId || '' });
    this.loadElderInfo();
  },

  loadElderInfo() {
    request({
      url: '/wxLogin/getElderBedList',
      method: 'POST'
    }).then((res) => {
      if (res.data && res.data.code === 200) {
        const list = res.data.data || [];
        const elder = list.find(item => String(item.id) === this.data.elderId);
        if (elder) {
          this.setData({ elderName: elder.name || '' });
        }
      }
    }).finally(() => {
      this.setData({ loading: false });
    });
  },

  goBack() {
    wx.navigateBack({ delta: 1 });
  }
});
