<template>
  <el-dialog :title="editing ? '编辑搭配' : '创建搭配'" :visible="visible" width="min(860px, 94vw)" append-to-body :close-on-click-modal="!saving" :close-on-press-escape="!saving" :show-close="!saving" @close="close">
    <el-form ref="outfitForm" :model="form" :rules="rules" :disabled="saving" label-position="top" class="outfit-form">
      <div class="fields">
        <el-form-item label="搭配名称" prop="name"><el-input v-model.trim="form.name" maxlength="100" placeholder="例如：周末休闲" /></el-form-item>
        <el-form-item label="适合场合">
          <el-select v-model="form.occasions" multiple filterable allow-create default-first-option placeholder="选择或输入场合" style="width:100%">
            <el-option v-for="value in occasions" :key="value" :label="value" :value="value" />
          </el-select>
        </el-form-item>
      </div>
      <el-form-item label="实际搭配效果照（选填）">
        <div class="effect-row">
          <div v-if="photoUrl" class="effect-photo"><img :src="photoUrl" alt="搭配效果照预览" /></div>
          <div v-else-if="outfit && outfit.hasImage && imageAction === 'KEEP'" class="effect-photo"><wardrobe-image :outfit="outfit" /></div>
          <div v-else class="effect-empty"><i class="el-icon-picture-outline" /><span>暂无效果照</span></div>
          <div class="photo-controls">
            <label class="photo-picker" :class="{ disabled: saving }">{{ photoUrl || (outfit && outfit.hasImage && imageAction === 'KEEP') ? '更换照片' : '选择照片' }}<input type="file" accept="image/jpeg,image/png,.jpg,.jpeg,.png" :disabled="saving" @change="choosePhoto" /></label>
            <el-button v-if="photoUrl || (outfit && outfit.hasImage && imageAction === 'KEEP')" type="text" :disabled="saving" @click="removePhoto">移除照片</el-button>
            <el-button v-if="outfit && outfit.hasImage && imageAction === 'REMOVE'" type="text" :disabled="saving" @click="restorePhoto">恢复原照片</el-button>
            <p>JPG 或 PNG，不超过 5 MB。</p>
          </div>
        </div>
      </el-form-item>
      <el-form-item label="已选单品（至少一件）" required>
        <div v-if="selected.length" class="selected-list">
          <div v-for="(item, index) in selected" :key="item.id" class="selected-item">
            <div class="selected-cover"><wardrobe-image :item-id="item.id" /></div>
            <span class="selected-name">{{ item.name || item.category }}</span>
            <div class="selected-actions">
              <el-button type="text" :disabled="saving || index === 0" @click="move(index, -1)">上移</el-button>
              <el-button type="text" :disabled="saving || index === selected.length - 1" @click="move(index, 1)">下移</el-button>
              <el-button type="text" :disabled="saving" @click="removeSelected(item.id)">移除</el-button>
            </div>
          </div>
        </div>
        <div v-else class="empty-selected">从下方衣橱中选择单品</div>
      </el-form-item>
      <el-form-item label="选择衣橱单品">
        <div class="item-search"><el-input v-model.trim="keyword" clearable placeholder="搜索单品名称" @keyup.enter.native="searchItems" /><el-button :disabled="saving" @click="searchItems">搜索</el-button></div>
        <div v-loading="loadingItems" class="item-picker">
          <button v-for="item in items" :key="item.id" type="button" class="pick-item" :class="{ selected: isSelected(item.id) }" :disabled="saving" @click="toggle(item)">
            <div class="pick-cover"><wardrobe-image :item-id="item.id" /></div><span>{{ item.name || item.category }}</span>
            <i v-if="isSelected(item.id)" class="el-icon-circle-check" />
          </button>
          <el-empty v-if="!loadingItems && !items.length" description="没有符合条件的单品" />
        </div>
        <el-button v-if="items.length < itemTotal" size="small" :loading="loadingItems" :disabled="saving" @click="loadMoreItems">加载更多</el-button>
      </el-form-item>
      <el-checkbox v-model="form.favorite">收藏这套搭配</el-checkbox>
    </el-form>
    <div slot="footer"><el-button :disabled="saving" @click="close">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存搭配</el-button></div>
  </el-dialog>
</template>

<script>
import { listItems, getItem, addOutfit, updateOutfit } from '@/api/wardrobe'
import WardrobeImage from './WardrobeImage'
import { OCCASIONS, validImage } from './constants'

export default {
  name: 'WardrobeOutfitEditor',
  components: { WardrobeImage },
  props: { visible: Boolean, outfit: { type: Object, default: null } },
  data() {
    return { form: { name: '', occasions: [], favorite: false }, rules: { name: [{ required: true, message: '请输入搭配名称', trigger: 'blur' }] },
      occasions: OCCASIONS, selected: [], items: [], itemTotal: 0, itemPage: 0, keyword: '', loadingItems: false,
      editorVersion: 0, itemRequestVersion: 0, selectionError: false, photo: null, photoUrl: '', imageAction: 'KEEP', saving: false }
  },
  computed: { editing() { return !!(this.outfit && this.outfit.id) } },
  watch: {
    visible(value) {
      if (value) this.initialize()
    }
  },
  beforeDestroy() { this.editorVersion++; this.itemRequestVersion++; this.releasePhoto() },
  methods: {
    async initialize() {
      const version = ++this.editorVersion
      this.itemRequestVersion++
      this.releasePhoto()
      const source = this.outfit || {}
      this.form = { name: source.name || '', occasions: Array.isArray(source.occasions) ? source.occasions.slice() : [], favorite: !!source.favorite }
      this.selected = Array.isArray(source.items) ? source.items.slice() : []
      this.imageAction = 'KEEP'
      this.keyword = ''
      this.itemPage = 0
      this.items = []
      this.itemTotal = 0
      this.selectionError = false
      this.$nextTick(() => { if (this.$refs.outfitForm) this.$refs.outfitForm.clearValidate() })
      if (!this.selected.length && source.itemIds && source.itemIds.length) {
        try {
          const results = await Promise.all(source.itemIds.map(id => getItem(id)))
          if (version !== this.editorVersion) return
          this.selected = results.map(result => result.data).filter(Boolean)
        } catch (error) {
          if (version !== this.editorVersion) return
          this.selectionError = true
          this.$message.error('无法加载已选单品，请重试')
        }
      }
      if (version !== this.editorVersion) return
      this.loadItems(true)
    },
    async loadItems(reset) {
      const version = ++this.itemRequestVersion
      const page = reset ? 1 : this.itemPage + 1
      this.loadingItems = true
      try {
        const result = await listItems({ pageNum: page, pageSize: 24, keyword: this.keyword })
        if (version === this.itemRequestVersion) {
          this.items = reset ? (result.rows || []) : this.items.concat(result.rows || [])
          this.itemTotal = result.total || 0
          this.itemPage = page
        }
      } catch (error) {
        // 全局请求提示已展示，保留当前已加载数据以便重试。
      } finally { if (version === this.itemRequestVersion) this.loadingItems = false }
    },
    searchItems() { if (!this.saving) this.loadItems(true) },
    loadMoreItems() { if (!this.saving) this.loadItems(false) },
    isSelected(id) { return this.selected.some(item => item.id === id) },
    toggle(item) {
      if (this.saving) return
      if (this.isSelected(item.id)) this.removeSelected(item.id)
      else this.selected.push(item)
    },
    removeSelected(id) { if (!this.saving) this.selected = this.selected.filter(item => item.id !== id) },
    move(index, step) {
      if (this.saving) return
      const target = index + step
      if (target < 0 || target >= this.selected.length) return
      const item = this.selected.splice(index, 1)[0]
      this.selected.splice(target, 0, item)
    },
    releasePhoto() {
      if (this.photoUrl) URL.revokeObjectURL(this.photoUrl)
      this.photoUrl = ''
      this.photo = null
    },
    choosePhoto(event) {
      if (this.saving) return
      const file = (event.target.files || [])[0]
      event.target.value = ''
      if (!file) return
      if (!validImage(file)) { this.$message.warning('请选择不超过 5 MB 的 JPG 或 PNG 图片'); return }
      this.releasePhoto()
      this.photo = file
      this.photoUrl = URL.createObjectURL(file)
      this.imageAction = 'REPLACE'
    },
    removePhoto() {
      if (this.saving) return
      this.releasePhoto()
      this.imageAction = this.outfit && this.outfit.hasImage ? 'REMOVE' : 'KEEP'
    },
    restorePhoto() {
      if (this.saving) return
      this.releasePhoto()
      this.imageAction = 'KEEP'
    },
    close() {
      if (this.saving) return
      this.editorVersion++
      this.itemRequestVersion++
      this.releasePhoto()
      this.$emit('update:visible', false)
    },
    save() {
      if (this.saving) return
      this.$refs.outfitForm.validate(async valid => {
        if (!valid || this.saving) return
        if (this.selectionError) { this.$message.warning('已选单品加载失败，请关闭后重试'); return }
        if (!this.selected.length) { this.$message.warning('请至少选择一件单品'); return }
        const payload = { name: this.form.name, occasions: this.form.occasions, favorite: this.form.favorite,
          itemIds: this.selected.map(item => item.id), imageAction: this.imageAction }
        this.saving = true
        try {
          if (this.editing) await updateOutfit(this.outfit.id, payload, this.photo)
          else await addOutfit(payload, this.photo)
          this.$message.success('搭配已保存')
          this.$emit('saved')
          this.saving = false
          this.close()
        } catch (error) {
          // 保存失败时保留已选单品和照片，便于重试。
        } finally { this.saving = false }
      })
    }
  }
}
</script>

<style scoped>
.fields { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 16px; }
.effect-row { display: flex; gap: 16px; align-items: center; }
.effect-photo, .effect-empty { width: 110px; height: 110px; flex: 0 0 110px; border-radius: 8px; overflow: hidden; background: #f5f1ec; }
.effect-photo img { width: 100%; height: 100%; object-fit: cover; }
.effect-empty { display: flex; flex-direction: column; justify-content: center; align-items: center; color: #b49c8c; font-size: 12px; }
.effect-empty i { font-size: 25px; }
.photo-controls p { margin: 0; color: #9d8e83; font-size: 12px; line-height: 1.5; }
.photo-picker { position: relative; display: inline-block; color: #409eff; cursor: pointer; font-size: 14px; }
.photo-picker.disabled { color: #c0c4cc; cursor: not-allowed; }
.photo-picker input { position: absolute; width: 1px; height: 1px; opacity: 0; }
.selected-list { display: flex; flex-direction: column; gap: 7px; }
.selected-item { display: flex; align-items: center; gap: 10px; min-width: 0; padding: 6px; background: #faf8f5; border-radius: 7px; }
.selected-cover { width: 48px; height: 48px; flex: 0 0 48px; border-radius: 5px; overflow: hidden; }
.selected-name { flex: 1; min-width: 0; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.selected-actions { display: flex; flex-wrap: wrap; }
.selected-actions .el-button { margin-left: 8px; }
.empty-selected { padding: 12px; background: #faf8f5; color: #a39388; border-radius: 7px; }
.item-search { display: flex; gap: 8px; margin-bottom: 12px; }
.item-picker { display: grid; grid-template-columns: repeat(auto-fill, minmax(90px, 1fr)); gap: 8px; max-height: 270px; overflow-y: auto; margin-bottom: 8px; }
.item-picker .el-empty { grid-column: 1 / -1; }
.pick-item { position: relative; min-width: 0; padding: 4px; border: 1px solid #eee7e1; border-radius: 7px; text-align: left; background: #fff; cursor: pointer; }
.pick-item.selected { border-color: #ad8672; }
.pick-item:disabled { cursor: not-allowed; opacity: .65; }
.pick-cover { height: 78px; overflow: hidden; border-radius: 4px; }
.pick-item span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; }
.pick-item i { position: absolute; top: 8px; right: 8px; color: #7c5a4b; background: #fff; border-radius: 50%; }
@media (max-width: 600px) { .fields { grid-template-columns: 1fr; gap: 0; } .selected-actions .el-button { margin-left: 5px; font-size: 12px; } }
</style>
