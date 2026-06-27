const { request, verifyToken } = require('../../../utils/request');
const app = getApp();

Page({
  data: {
    sessionId: '',
    messages: [
      { role: 'assistant', content: '你好呀！我是小乐 😊\n你可以问我关于账单、订单、合同或请假的问题，比如：\n- "我妈妈这个月花了多少钱"\n- "爸爸请假的申请通过了吗"\n- "帮我看看合同信息"' }
    ],
    inputText: '',
    loading: false
  },

  onLoad() {
    const memberId = wx.getStorageSync('memberId') || '';
    const today = new Date().toISOString().slice(0, 10).replace(/-/g, '');
    this.setData({
      sessionId: `member_${memberId}_${today}`
    });
  },

  onInput(e) {
    this.setData({ inputText: e.detail.value });
  },

  sendMessage() {
    const { inputText, loading } = this.data;
    if (!inputText.trim() || loading) return;

    const userMsg = { role: 'user', content: inputText.trim() };
    const messages = [...this.data.messages, userMsg];
    this.setData({
      messages,
      inputText: '',
      loading: true
    });

    this.scrollToBottom();

    verifyToken().then(() => {
      request({
        url: '/wxLogin/ai/ask',
        method: 'POST',
        data: {
          question: userMsg.content,
          sessionId: this.data.sessionId
        }
      }).then((res) => {
        if (res.data && res.data.code === 200) {
          const data = res.data.data || '';
          if (typeof data === 'string' && data.indexOf('[CONFIRM]') === 0) {
            const confirmInfo = data.replace('[CONFIRM]', '');
            this.setData({
              messages: [...this.data.messages, {
                role: 'assistant',
                content: confirmInfo,
                needsConfirm: true
              }]
            });
          } else {
            console.log('AI 回复内容:', data);
            console.log('消息列表长度:', this.data.messages.length);
            this.setData({
              messages: [...this.data.messages, { role: 'assistant', content: data }]
            }, () => {
              console.log('setData 完成, 消息数:', this.data.messages.length);
            });
          }
        } else {
          this.setData({
            messages: [...this.data.messages, { role: 'assistant', content: res.data.msg || '服务出错了，请稍后再试' }]
          });
        }
      }).catch(() => {
        this.setData({
          messages: [...this.data.messages, { role: 'assistant', content: '网络异常，请稍后重试' }]
        });
      }).finally(() => {
        this.setData({ loading: false });
        this.scrollToBottom();
      });
    }).catch(() => {});
  },

  confirmAction() {
    const msgs = this.data.messages;
    const lastMsg = msgs[msgs.length - 1];
    if (!lastMsg || !lastMsg.needsConfirm) return;

    this.setData({ loading: true });

    const app = getApp();
    wx.request({
      url: 'http://localhost:8080/wxLogin/ai/confirm',
      method: 'POST',
      data: { sessionId: this.data.sessionId },
      header: { 'Authorization': app.globalData.token || wx.getStorageSync('token') },
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const messages = [...this.data.messages];
          messages[messages.length - 1] = {
            role: 'assistant',
            content: res.data.data || '操作完成'
          };
          this.setData({ messages });
        } else {
          wx.showToast({ title: res.data.msg || '操作失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.showToast({ title: '网络异常', icon: 'none' });
      },
      complete: () => {
        this.setData({ loading: false });
        this.scrollToBottom();
      }
    });
  },

  cancelAction() {
    const app = getApp();
    wx.request({
      url: 'http://localhost:8080/wxLogin/ai/cancel',
      method: 'POST',
      data: { sessionId: this.data.sessionId },
      header: { 'Authorization': app.globalData.token || wx.getStorageSync('token') },
      success: () => {
        const messages = [...this.data.messages];
        const lastMsg = messages[messages.length - 1];
        messages[messages.length - 1] = {
          role: 'assistant',
          content: (lastMsg ? lastMsg.content : '') + '\n\n（已取消）'
        };
        this.setData({ messages });
      }
    });
  },

  scrollToBottom() {
    setTimeout(() => {
      wx.pageScrollTo({ scrollTop: 99999 });
    }, 100);
  }
});
