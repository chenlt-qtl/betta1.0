<template>
  <section class="outfits">
    <div class="section-heading">
      <div><span class="label">YOUR LOOKS</span><h2>我的搭配</h2><p>把喜欢的组合和实际效果照保存下来。</p></div>
      <el-button type="primary" icon="el-icon-plus" :disabled="sorting" @click="openAdd">创建搭配</el-button>
    </div>
    <el-alert v-if="refreshRequired" title="搭配列表加载失败，请重新加载后继续排序" type="warning" :closable="false" show-icon>
      <el-button size="mini" :disabled="loading || sorting" @click="load">重新加载</el-button>
    </el-alert>
    <div v-loading="loading || sorting" class="outfit-grid">
      <article v-for="(outfit, index) in outfits" :key="outfit.id" class="outfit-card">
        <button type="button" class="cover" :aria-label="`查看${outfit.name}`" @click="openDetail(outfit.id)">
          <wardrobe-image :key="`${outfit.id}-${imageVersion}`" :outfit="outfit" :alt="outfit.name" />
        </button>
        <div class="outfit-text"><strong>{{ outfit.name }}</strong><span>{{ (outfit.occasions || []).join(' · ') || '适合多个场合' }}</span></div>
        <div class="outfit-actions">
          <el-button type="text" :disabled="!!favoriteBusy[outfit.id]" @click="toggleFavorite(outfit)"><i :class="outfit.favorite ? 'el-icon-star-on' : 'el-icon-star-off'" />{{ outfit.favorite ? '已收藏' : '收藏' }}</el-button>
          <el-button type="text" @click="openEdit(outfit.id)">编辑</el-button>
          <el-button type="text" @click="remove(outfit.id)">删除</el-button>
        </div>
        <div class="sort-actions">
          <el-button size="mini" icon="el-icon-top" :aria-label="`上移${outfit.name}`" :disabled="sorting || loading || refreshRequired || isFirst(index)" @click="move(outfit, index, 'up')">上移</el-button>
          <el-button size="mini" icon="el-icon-bottom" :aria-label="`下移${outfit.name}`" :disabled="sorting || loading || refreshRequired || isLast(index)" @click="move(outfit, index, 'down')">下移</el-button>
        </div>
      </article>
      <el-empty v-if="!loading && !refreshRequired && !outfits.length" description="还没有搭配，从衣橱选择单品来创建吧" />
    </div>
    <div v-if="total" class="pagination">
      <span>共 {{ total }} 套</span><el-button size="small" :disabled="loading || sorting || refreshRequired || pageNum <= 1" @click="changePage(-1)">上一页</el-button>
      <span>{{ pageNum }} / {{ pageCount }}</span><el-button size="small" :disabled="loading || sorting || refreshRequired || pageNum >= pageCount" @click="changePage(1)">下一页</el-button>
    </div>
    <wardrobe-outfit-editor :visible.sync="editorVisible" :outfit="editingOutfit" @saved="onSaved" />
    <el-dialog title="搭配详情" :visible.sync="detailVisible" width="min(680px, 94vw)" append-to-body>
      <div v-if="detail" class="detail">
        <div class="detail-cover"><wardrobe-image :outfit="detail" :alt="detail.name" /></div>
        <div class="detail-content"><h3>{{ detail.name }}</h3><p>{{ (detail.occasions || []).join(' · ') || '适合多个场合' }}</p>
          <p>{{ detail.favorite ? '已收藏' : '未收藏' }}</p><h4>搭配单品</h4>
          <div class="detail-items"><div v-for="item in detail.items || []" :key="item.id" class="detail-item"><div><wardrobe-image :item-id="item.id" /></div><span>{{ item.name || item.category }}</span></div></div>
        </div>
      </div>
      <div slot="footer"><el-button @click="detailVisible = false">关闭</el-button><el-button v-if="detail" type="primary" @click="editDetail">编辑</el-button></div>
    </el-dialog>
  </section>
</template>

<script>
import { listOutfits, getOutfit, deleteOutfit, setFavorite, moveOutfit } from '@/api/wardrobe'
import WardrobeImage from './WardrobeImage'
import WardrobeOutfitEditor from './WardrobeOutfitEditor'

export default {
  name: 'WardrobeOutfits',
  components: { WardrobeImage, WardrobeOutfitEditor },
  props: { openCreate: Boolean },
  data() {
    return { outfits: [], total: 0, pageNum: 1, pageSize: 12, loading: false, sorting: false, refreshRequired: false, alive: true, requestVersion: 0, editorVisible: false,
      editingOutfit: null, detailVisible: false, detail: null, favoriteBusy: {}, imageVersion: 0 }
  },
  computed: { pageCount() { return Math.max(1, Math.ceil(this.total / this.pageSize)) } },
  created() { this.load() },
  mounted() { if (this.openCreate) this.openAdd() },
  beforeDestroy() { this.alive = false; this.requestVersion++ },
  methods: {
    async load() {
      const version = ++this.requestVersion
      this.loading = true
      try {
        const response = await listOutfits({ pageNum: this.pageNum, pageSize: this.pageSize })
        if (this.alive && version === this.requestVersion) {
          this.total = response.total || 0
          if (this.pageNum > this.pageCount) {
            // PageHelper 可能已返回末页数据，需按纠正后的页码重新加载。
            this.pageNum = this.pageCount
            this.outfits = []
            return await this.load()
          }
          this.outfits = response.rows || []
          this.refreshRequired = false
          return true
        }
        return false
      } catch (error) {
        // 失败时保留旧列表，但禁用排序，避免以过期顺序执行操作。
        if (this.alive && version === this.requestVersion) this.refreshRequired = true
        return false
      } finally { if (this.alive && version === this.requestVersion) this.loading = false }
    },
    changePage(step) {
      if (this.loading || this.sorting || this.refreshRequired) return
      const page = this.pageNum + step
      if (page < 1 || page > this.pageCount) return
      this.pageNum = page
      this.load()
    },
    isFirst(index) { return this.pageNum === 1 && index === 0 },
    isLast(index) { return (this.pageNum - 1) * this.pageSize + index >= this.total - 1 },
    showSortMessage(type, message) { this.$message[type]({ message, customClass: 'wardrobe-sort-message' }) },
    async move(outfit, index, direction) {
      if (this.sorting || this.loading || this.refreshRequired || !outfit || !['up', 'down'].includes(direction) || index < 0 || index >= this.outfits.length) return
      if ((direction === 'up' && this.isFirst(index)) || (direction === 'down' && this.isLast(index))) return
      const previousPage = this.pageNum
      const crossesPage = (direction === 'up' && index === 0) || (direction === 'down' && index === this.outfits.length - 1)
      this.sorting = true
      try {
        const response = await moveOutfit(outfit.id, direction)
        if (!this.alive) return
        const refreshed = await this.load()
        if (!this.alive) return
        if (!refreshed) {
          this.showSortMessage('warning', '列表刷新失败，请重新加载后查看排序结果')
        } else if (!response.data) {
          this.showSortMessage('info', '该搭配已在排序边界，无需调整')
        } else if (crossesPage && this.pageNum === previousPage) {
          this.showSortMessage('success', `顺序已调整，若此搭配不在当前页，请查看${direction === 'up' ? '上一页' : '下一页'}`)
        } else {
          this.showSortMessage('success', '搭配顺序已调整')
        }
      } catch (error) {
        if (this.alive) await this.load()
      } finally { if (this.alive) this.sorting = false }
    },
    openAdd() { if (!this.sorting) { this.editingOutfit = null; this.editorVisible = true } },
    async openDetail(id) {
      try { const response = await getOutfit(id); this.detail = response.data; this.detailVisible = true } catch (error) { /* 请求层已显示错误。 */ }
    },
    async openEdit(id) {
      try { const response = await getOutfit(id); this.editingOutfit = response.data; this.editorVisible = true } catch (error) { /* 请求层已显示错误。 */ }
    },
    editDetail() { this.detailVisible = false; this.editingOutfit = this.detail; this.editorVisible = true },
    onSaved() { this.imageVersion++; this.load() },
    async toggleFavorite(outfit) {
      this.$set(this.favoriteBusy, outfit.id, true)
      try { await setFavorite(outfit.id, !outfit.favorite); outfit.favorite = !outfit.favorite } catch (error) { /* 保留原收藏状态。 */ }
      finally { this.$delete(this.favoriteBusy, outfit.id) }
    },
    async remove(id) {
      try {
        await this.$confirm('确定删除这套搭配吗？', '删除搭配', { type: 'warning' })
        await deleteOutfit(id)
        this.$message.success('搭配已删除')
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
.outfit-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(155px, 1fr)); gap: 15px; min-height: 120px; }
.outfit-grid .el-empty { grid-column: 1 / -1; }
.outfit-card { min-width: 0; overflow: hidden; border: 1px solid #eee8e1; border-radius: 12px; background: #fff; }
.cover { display: block; width: 100%; height: 200px; padding: 0; border: 0; cursor: pointer; }
.outfit-text { padding: 12px 12px 0; }
.outfit-text strong, .outfit-text span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.outfit-text span { margin-top: 6px; color: #9c8d83; font-size: 12px; }
.outfit-actions { display: flex; gap: 10px; padding: 0 12px; flex-wrap: wrap; }
.outfit-actions .el-button { margin: 0; }
.sort-actions { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 6px; padding: 0 12px 12px; }
.sort-actions .el-button { width: 100%; min-width: 0; min-height: 40px; margin: 0; padding: 8px 2px; }
.pagination { display: flex; justify-content: center; align-items: center; gap: 9px; margin-top: 20px; color: #85766d; font-size: 13px; }
.pagination .el-button { margin: 0; }
.detail { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 20px; }
.detail-cover { height: 300px; overflow: hidden; border-radius: 10px; }
.detail-content { min-width: 0; }
.detail-content h3 { margin: 0 0 10px; overflow-wrap: anywhere; }
.detail-items { display: flex; flex-wrap: wrap; gap: 8px; }
.detail-item { width: 70px; font-size: 12px; }
.detail-item > div { height: 70px; overflow: hidden; border-radius: 6px; }
.detail-item span { display: block; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
@media (max-width: 600px) { .outfit-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; } .cover { height: 160px; } .detail { grid-template-columns: 1fr; } .detail-cover { height: 200px; } }
</style>

<style>
@media (max-width: 480px) {
  .el-message.wardrobe-sort-message { width: calc(100% - 32px); min-width: 0; }
  .wardrobe-sort-message .el-message__content { min-width: 0; line-height: 1.5; overflow-wrap: anywhere; }
}
</style>
