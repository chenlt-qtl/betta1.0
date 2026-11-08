<template>
  <section class="closet">
    <div class="section-heading">
      <div><span class="label">YOUR COLLECTION</span><h2>衣橱</h2><p>把喜欢的衣服整理在一起。</p></div>
      <el-button type="primary" icon="el-icon-plus" @click="openAdd">添加衣服</el-button>
    </div>
    <div class="filters">
      <el-input v-model.trim="query.keyword" clearable placeholder="搜索名称" @keyup.enter.native="search" />
      <el-select v-model="query.category" clearable placeholder="全部分类"><el-option v-for="value in categories" :key="value" :label="value" :value="value" /></el-select>
      <el-input v-model.trim="query.color" clearable placeholder="颜色" @keyup.enter.native="search" />
      <el-select v-model="query.season" clearable placeholder="全部季节"><el-option v-for="value in seasons" :key="value" :label="value" :value="value" /></el-select>
      <el-select v-model="query.occasion" clearable filterable allow-create default-first-option placeholder="全部场合"><el-option v-for="value in occasions" :key="value" :label="value" :value="value" /></el-select>
      <el-button @click="search">筛选</el-button>
    </div>
    <div v-loading="loading" class="item-grid">
      <article v-for="item in items" :key="item.id" class="item-card">
        <button type="button" class="cover" :aria-label="`查看${item.name || item.category}`" @click="openDetail(item.id)">
          <wardrobe-image :key="`${item.id}-${imageVersion}`" :item-id="item.id" :alt="item.name || item.category" />
        </button>
        <div class="item-text"><strong>{{ item.name || item.category }}</strong><span>{{ [item.category, item.color].filter(Boolean).join(' · ') }}</span></div>
        <div class="item-actions"><el-button type="text" @click="openEdit(item.id)">编辑</el-button><el-button type="text" @click="remove(item.id)">删除</el-button></div>
      </article>
      <el-empty v-if="!loading && !items.length" description="衣橱里还没有符合条件的单品" />
    </div>
    <div v-if="total" class="pagination">
      <span>共 {{ total }} 件</span>
      <el-button size="small" :disabled="query.pageNum <= 1" @click="changePage(-1)">上一页</el-button>
      <span>{{ query.pageNum }} / {{ pageCount }}</span>
      <el-button size="small" :disabled="query.pageNum >= pageCount" @click="changePage(1)">下一页</el-button>
    </div>
    <wardrobe-item-form :visible.sync="formVisible" :item="editingItem" @saved="onSaved" />
    <el-dialog title="单品详情" :visible.sync="detailVisible" width="min(620px, 94vw)" append-to-body>
      <div v-if="detail" class="detail">
        <div class="detail-image"><wardrobe-image :item-id="detail.id" :alt="detail.name || detail.category" /></div>
        <div class="detail-text">
          <h3>{{ detail.name || detail.category }}</h3>
          <p><b>分类：</b>{{ detail.category }}</p>
          <p><b>颜色：</b>{{ detail.color || '未填写' }}</p>
          <p><b>季节：</b>{{ (detail.seasons || []).join('、') || '未填写' }}</p>
          <p><b>场合：</b>{{ (detail.occasions || []).join('、') || '未填写' }}</p>
          <p><b>备注：</b>{{ detail.note || '未填写' }}</p>
        </div>
      </div>
      <div slot="footer"><el-button @click="detailVisible = false">关闭</el-button><el-button v-if="detail" type="primary" @click="editDetail">编辑</el-button></div>
    </el-dialog>
  </section>
</template>

<script>
import { listItems, getItem, deleteItem } from '@/api/wardrobe'
import WardrobeImage from './WardrobeImage'
import WardrobeItemForm from './WardrobeItemForm'
import { CATEGORIES, SEASONS, OCCASIONS } from './constants'

export default {
  name: 'WardrobeCloset',
  components: { WardrobeImage, WardrobeItemForm },
  props: { openCreate: Boolean },
  data() {
    return { categories: CATEGORIES, seasons: SEASONS, occasions: OCCASIONS,
      query: { pageNum: 1, pageSize: 12, keyword: '', category: '', color: '', season: '', occasion: '' },
      loading: false, requestVersion: 0, items: [], total: 0, formVisible: false, editingItem: null, detailVisible: false, detail: null, imageVersion: 0 }
  },
  computed: { pageCount() { return Math.max(1, Math.ceil(this.total / this.query.pageSize)) } },
  created() { this.load() },
  mounted() { if (this.openCreate) this.openAdd() },
  methods: {
    async load() {
      const version = ++this.requestVersion
      this.loading = true
      try {
        const response = await listItems(this.query)
        if (version === this.requestVersion) {
          this.items = response.rows || []
          this.total = response.total || 0
          if (!this.items.length && this.query.pageNum > 1 && this.total > 0) {
            this.query.pageNum = this.pageCount
            await this.load()
          }
        }
      } catch (error) {
        // 请求层已显示错误；保留当前列表供用户重试。
      } finally {
        if (version === this.requestVersion) this.loading = false
      }
    },
    search() { this.query.pageNum = 1; this.load() },
    changePage(step) { this.query.pageNum += step; this.load() },
    openAdd() { this.editingItem = null; this.formVisible = true },
    async openDetail(id) {
      try {
        const response = await getItem(id)
        this.detail = response.data
        this.detailVisible = true
      } catch (error) { /* 请求层已显示错误。 */ }
    },
    async openEdit(id) {
      try {
        const response = await getItem(id)
        this.editingItem = response.data
        this.formVisible = true
      } catch (error) { /* 请求层已显示错误。 */ }
    },
    editDetail() { this.detailVisible = false; this.editingItem = this.detail; this.formVisible = true },
    onSaved() { this.imageVersion++; this.load() },
    async remove(id) {
      try {
        await this.$confirm('确定删除这件单品吗？已被搭配使用的单品需先解除关联。', '删除单品', { type: 'warning' })
        await deleteItem(id)
        this.$message.success('单品已删除')
        this.load()
      } catch (error) {
        // 取消或服务端拒绝删除时保留原数据。
      }
    }
  }
}
</script>

<style scoped>
.section-heading { display: flex; align-items: end; justify-content: space-between; gap: 16px; margin-bottom: 20px; }
.label { color: #a17e6a; font-size: 11px; font-weight: 700; letter-spacing: .16em; }
h2 { font-size: 23px; margin: 8px 0; }
p { margin: 0; color: #85766d; font-size: 13px; line-height: 1.7; }
.filters { display: flex; gap: 8px; flex-wrap: wrap; padding: 14px; border-radius: 12px; background: #fff; }
.filters .el-input, .filters .el-select { flex: 1 1 125px; min-width: 0; }
.item-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 15px; min-height: 120px; margin-top: 18px; }
.item-grid .el-empty { grid-column: 1 / -1; }
.item-card { min-width: 0; overflow: hidden; border: 1px solid #eee8e1; border-radius: 12px; background: #fff; }
.cover { display: block; width: 100%; height: 190px; padding: 0; border: 0; cursor: pointer; }
.item-text { padding: 12px 12px 0; }
.item-text strong, .item-text span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.item-text span { margin-top: 6px; color: #9c8d83; font-size: 12px; }
.item-actions { display: flex; gap: 14px; padding: 0 12px; }
.item-actions .el-button { margin: 0; }
.pagination { display: flex; justify-content: center; align-items: center; gap: 9px; margin-top: 20px; color: #85766d; font-size: 13px; }
.pagination .el-button { margin: 0; }
.detail { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 20px; }
.detail-image { height: 290px; overflow: hidden; border-radius: 10px; }
.detail-text h3 { margin: 0 0 10px; overflow-wrap: anywhere; }
.detail-text p { overflow-wrap: anywhere; }
@media (max-width: 600px) {
  .item-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
  .cover { height: 155px; }
  .detail { grid-template-columns: 1fr; }
  .detail-image { height: 200px; }
}
</style>
