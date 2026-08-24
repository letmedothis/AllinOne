import { defineStore } from 'pinia'
import type { CollectTemplate, CollectData, CollectCategory } from '@/types'
import { listTemplate } from '@/api/collect/template'
import { listData } from '@/api/collect/data'
import { listCategory } from '@/api/collect/category'

interface CollectState {
  templates: CollectTemplate[]
  templateTotal: number
  dataList: CollectData[]
  dataTotal: number
  categoryTree: CollectCategory[]
}

const useCollectStore = defineStore('collect', {
  state: (): CollectState => ({
    templates: [],
    templateTotal: 0,
    dataList: [],
    dataTotal: 0,
    categoryTree: []
  }),

  actions: {
    /** 加载模板列表 */
    async loadTemplates(params?: any) {
      try {
        const res = await listTemplate(params || { pageNum: 1, pageSize: 10 })
        this.templates = res.rows
        this.templateTotal = res.total
      } catch (e) {
        console.error('加载模板列表失败:', e)
        this.templates = []
        this.templateTotal = 0
      }
    },

    /** 加载填报数据列表 */
    async loadDataList(params?: any) {
      try {
        const res = await listData(params || { pageNum: 1, pageSize: 10 })
        this.dataList = res.rows
        this.dataTotal = res.total
      } catch (e) {
        console.error('加载填报数据列表失败:', e)
        this.dataList = []
        this.dataTotal = 0
      }
    },

    /** 加载分类树 */
    async loadCategoryTree() {
      try {
        const res = await listCategory()
        this.categoryTree = res.data || []
      } catch (e) {
        console.error('加载分类树失败:', e)
        this.categoryTree = []
      }
    },

    /** 清除模板列表缓存 */
    clearTemplates() {
      this.templates = []
      this.templateTotal = 0
    },

    /** 清除数据列表缓存 */
    clearDataList() {
      this.dataList = []
      this.dataTotal = 0
    }
  }
})

export default useCollectStore
