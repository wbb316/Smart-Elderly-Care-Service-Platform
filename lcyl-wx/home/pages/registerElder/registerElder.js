const { request, verifyToken } = require('../../../utils/request');
const app = getApp();

Page({
  data: {
    name: '',
    idCardNo: '',
    age: '',
    sex: '',
    phone: '',
    relation: '',
    remark: '',
    submitting: false
  },

  onNameInput(e) { this.setData({ name: e.detail.value }); },
  onIdCardInput(e) { this.setData({ idCardNo: e.detail.value }); },
  onAgeInput(e) { this.setData({ age: e.detail.value }); },
  onPhoneInput(e) { this.setData({ phone: e.detail.value }); },
  onRelationInput(e) { this.setData({ relation: e.detail.value }); },
  onRemarkInput(e) { this.setData({ remark: e.detail.value }); },

  onSexChange(e) {
    const index = e.detail.value;
    const sexList = ['男', '女'];
    this.setData({ sex: sexList[index] });
  },

  submitForm() {
    const { name, idCardNo, sex, relation } = this.data;
    if (!name) {
      wx.showToast({ title: '请输入老人姓名', icon: 'none' });
      return;
    }
    if (!idCardNo) {
      wx.showToast({ title: '请输入身份证号', icon: 'none' });
      return;
    }
    if (!sex) {
      wx.showToast({ title: '请选择性别', icon: 'none' });
      return;
    }
    if (!relation) {
      wx.showToast({ title: '请输入与老人关系', icon: 'none' });
      return;
    }

    this.setData({ submitting: true });

    verifyToken().then(() => {
      request({
        url: '/wxLogin/registerElder',
        method: 'POST',
        data: {
          name: this.data.name,
          idCardNo: this.data.idCardNo,
          age: this.data.age,
          sex: this.data.sex,
          phone: this.data.phone,
          relation: this.data.relation,
          remark: this.data.remark
        }
      }).then((res) => {
        if (res.data && res.data.code === 200) {
          wx.showToast({ title: '提交成功，等待审核', icon: 'success', duration: 2000 });
          setTimeout(() => { wx.navigateBack({ delta: 1 }); }, 2000);
        } else {
          wx.showToast({ title: res.data.msg || '提交失败', icon: 'none' });
        }
      }).catch(() => {
        wx.showToast({ title: '网络异常', icon: 'none' });
      }).finally(() => {
        this.setData({ submitting: false });
      });
    });
  }
});
