import request from '@/utils/request'

/**
 * 填报表格图片上传：走通用上传接口，返回可访问 URL。
 * CollectSheet 以 uploadImage 选项接入 Luckysheet fork 的图片上传流程，
 * 工作簿中仅保存 URL 而非 base64（设计 02 §1.9）。
 *
 * @param file 用户插入/粘贴的图片文件
 * @returns 图片访问 URL
 */
export function uploadSheetImage(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/common/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    // 大图上传可能较慢，放宽超时
    timeout: 60000
  }).then((res: any) => {
    const url: string | undefined = res?.url
    if (!url) {
      return Promise.reject(new Error('上传接口未返回图片地址'))
    }
    return url
  })
}
