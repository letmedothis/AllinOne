import { defineStore } from 'pinia'
import type { ReportConfig } from '@/types'
import { listConfig } from '@/api/report/config'

interface ReportState {
  configList: ReportConfig[]
  configTotal: number
}

const useReportStore = defineStore('report', {
  state: (): ReportState => ({
    configList: [],
    configTotal: 0
  }),

  actions: {
    /** 加载报表配置列表 */
    async loadConfigList(params?: any) {
      try {
        const res = await listConfig(params || { pageNum: 1, pageSize: 10 })
        this.configList = res.rows
        this.configTotal = res.total
      } catch (e) {
        console.error('加载报表配置列表失败:', e)
        this.configList = []
        this.configTotal = 0
      }
    },

    /** 清除配置列表缓存 */
    clearConfigList() {
      this.configList = []
      this.configTotal = 0
    }
  }
})

export default useReportStore
