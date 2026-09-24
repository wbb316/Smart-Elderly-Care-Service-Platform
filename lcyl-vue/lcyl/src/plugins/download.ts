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
      // 同上：res 已是 Blob；Blob 上没有 headers，文件名用入参
      const isBlob = blobValidate(res)
      if (isBlob) {
        const blob = new Blob([res])
        this.saveAs(blob, decodeURIComponent(name))
      } else {
        this.printErrMsg(res)
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
      // 同上：res 已是 Blob；文件名取资源路径的最后一段
      const isBlob = blobValidate(res)
      if (isBlob) {
        const blob = new Blob([res])
        this.saveAs(blob, decodeURIComponent(resource.split('/').pop() || 'download'))
      } else {
        this.printErrMsg(res)
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
      // 响应拦截器已把 blob 解包返回（utils/request.ts 对 responseType=blob 直接 return res.data），
      // 因此这里的 res 本身就是 Blob，不能再取 res.data（否则 blobValidate(undefined) 抛 TypeError）
      const isBlob = blobValidate(res)
      if (isBlob) {
        const blob = new Blob([res], { type: 'application/zip' })
        this.saveAs(blob, name)
      } else {
        this.printErrMsg(res)
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
