import request from '@/utils/request'
import { saveAs } from 'file-saver'
import { blobValidate } from '@/utils/ruoyi'
import type { AjaxResult, TableDataInfo } from '@/types'

/** 异步导出任务 */
export interface CollectExportTask {
  taskId: number
  taskName: string
  status: 'pending' | 'running' | 'success' | 'failed'
  fileName?: string
  errorMsg?: string
  createTime?: string
  finishTime?: string
}

/** 创建异步导出任务（后端落库并交线程池后台生成文件），返回任务 ID */
export function createExportTask(query: Record<string, any>): Promise<AjaxResult<number>> {
  return request({ url: '/collect/data/export/tasks', method: 'post', params: query })
}

/** 查询导出任务列表（后端已按当前用户过滤） */
export function listExportTask(query?: { status?: string; pageNum?: number; pageSize?: number }): Promise<TableDataInfo<CollectExportTask[]>> {
  return request({ url: '/collect/data/export/tasks', method: 'get', params: query })
}

/** 按任务下载生成的文件（后端校验任务属主） */
export function downloadExportTask(task: CollectExportTask) {
  return request({
    url: `/collect/data/export/tasks/${task.taskId}/download`,
    method: 'get',
    responseType: 'blob',
    timeout: 120000
  }).then(async (data: any) => {
    const isBlob = blobValidate(data)
    if (isBlob) {
      saveAs(new Blob([data]), task.fileName || '填报数据导出.xlsx')
    } else {
      // 后端以 JSON 返回业务错误（如文件已失效）
      const resText = await data.text()
      const rspObj = JSON.parse(resText)
      throw new Error(rspObj.msg || '下载失败')
    }
  })
}
