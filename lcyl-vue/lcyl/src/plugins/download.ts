import request from '@/utils/request'
import { ElLoading, ElMessage } from 'element-plus'
import { saveAs } from 'file-saver'
import errorCode from '@/utils/errorCode'
import { blobValidate } from '@/utils/ruoyi'

let downloadLoadingInstance: ReturnType<typeof ElLoading.service>

export default {
  name(name: string, isDelete = true) {
    request({
      method: 'get',
      url: "/common/download",
      params: { fileName: name, delete: isDelete },
      responseType: 'blob'
    }).then((res: any) => {
      const isBlob = blobValidate(res.data)
      if (isBlob) {
        const blob = new Blob([res.data])
        this.saveAs(blob, decodeURIComponent(res.headers['download-filename']))
      } else {
        this.printErrMsg(res.data)
      }
    }).catch(() => {
      ElMessage.error('下载文件失败，请重试')
    })
  },
  resource(resource: string) {
    request({
      method: 'get',
      url: "/common/download/resource",
      params: { resource },
      responseType: 'blob'
    }).then((res: any) => {
      const isBlob = blobValidate(res.data)
      if (isBlob) {
        const blob = new Blob([res.data])
        this.saveAs(blob, decodeURIComponent(res.headers['download-filename']))
      } else {
        this.printErrMsg(res.data)
      }
    }).catch(() => {
      ElMessage.error('下载资源失败，请重试')
    })
  },
  zip(url: string, name: string) {
    downloadLoadingInstance = ElLoading.service({ text: "正在下载数据，请稍候", background: "rgba(0, 0, 0, 0.7)", })
    request({
      method: 'get',
      url: url,
      responseType: 'blob'
    }).then((res: any) => {
      const isBlob = blobValidate(res.data)
      if (isBlob) {
        const blob = new Blob([res.data], { type: 'application/zip' })
        this.saveAs(blob, name)
      } else {
        this.printErrMsg(res.data)
      }
      downloadLoadingInstance.close()
    }).catch((r: any) => {
      console.error(r)
      ElMessage.error('下载文件出现错误，请联系管理员！')
      downloadLoadingInstance.close()
    })
  },
  saveAs(text: Blob, name: string, opts?: any) {
    saveAs(text, name, opts)
  },
  async printErrMsg(data: any) {
    try {
      const resText = await data.text()
      const rspObj = JSON.parse(resText)
      const errMsg = errorCode[rspObj.code] || rspObj.msg || errorCode['default']
      ElMessage.error(errMsg)
    } catch {
      ElMessage.error('下载文件失败，请重试')
    }
  }
}
