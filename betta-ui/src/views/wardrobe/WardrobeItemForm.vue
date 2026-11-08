<template>
  <el-dialog :title="editing ? '编辑单品' : '添加衣服'" :visible="visible" width="min(680px, 94vw)" append-to-body :close-on-click-modal="!saving" :close-on-press-escape="!saving" :show-close="!saving" @close="close">
    <el-form ref="itemForm" :model="form" label-position="top" :rules="rules" :disabled="saving" class="item-form">
      <el-form-item label="衣服照片" required>
        <div class="photo-list">
          <div v-if="editing && !files.length" class="photo"><wardrobe-image :item-id="item.id" /></div>
          <div v-for="(entry, index) in files" :key="entry.url" class="photo">
            <img :src="entry.url" :alt="entry.file.name" />
            <button type="button" class="photo-remove" :disabled="saving" aria-label="移除照片" @click="removeFile(index)"><i class="el-icon-close" /></button>
          </div>
          <label v-if="!editing || !files.length" class="photo-picker" :class="{ disabled: saving }">
            <i class="el-icon-plus" /><span>{{ editing ? '更换照片' : '选择照片' }}</span>
            <input type="file" accept="image/jpeg,image/png,.jpg,.jpeg,.png" :multiple="!editing" :disabled="saving" @change="chooseFiles" />
          </label>
        </div>
        <div class="tip">JPG 或 PNG，每张不超过 5 MB。{{ editing ? '' : '可一次选择多张，使用相同的分类和季节。' }}</div>
      </el-form-item>
      <div class="fields">
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="选择分类" style="width:100%">
            <el-option v-for="value in categories" :key="value" :label="value" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称"><el-input v-model.trim="form.name" maxlength="100" placeholder="例如：白色衬衫" /></el-form-item>
        <el-form-item label="颜色"><el-input v-model.trim="form.color" maxlength="40" placeholder="例如：白色" /></el-form-item>
      </div>
      <el-form-item label="适合季节">
        <el-checkbox-group v-model="form.seasons"><el-checkbox v-for="season in seasons" :key="season" :label="season">{{ season }}</el-checkbox></el-checkbox-group>
      </el-form-item>
      <el-form-item label="适合场合">
        <el-select v-model="form.occasions" multiple filterable allow-create default-first-option placeholder="选择或输入场合" style="width:100%">
          <el-option v-for="value in occasions" :key="value" :label="value" :value="value" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注"><el-input v-model.trim="form.note" type="textarea" :rows="2" maxlength="500" /></el-form-item>
    </el-form>
    <div slot="footer">
      <el-button :disabled="saving" @click="close">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">{{ editing ? '保存修改' : '保存到衣橱' }}</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { addItem, updateItem } from '@/api/wardrobe'
import WardrobeImage from './WardrobeImage'
import { CATEGORIES, SEASONS, OCCASIONS, validImage } from './constants'

function emptyForm() {
  return { category: '', name: '', color: '', seasons: [], occasions: [], note: '' }
}

export default {
  name: 'WardrobeItemForm',
  components: { WardrobeImage },
  props: { visible: Boolean, item: { type: Object, default: null } },
  data() {
    return { form: emptyForm(), files: [], saving: false, categories: CATEGORIES, seasons: SEASONS, occasions: OCCASIONS,
      rules: { category: [{ required: true, message: '请选择分类', trigger: 'change' }] } }
  },
  computed: { editing() { return !!(this.item && this.item.id) } },
  watch: {
    visible(value) {
      if (value) {
        this.releaseFiles()
        const source = this.item || {}
        const fields = emptyForm()
        Object.keys(fields).forEach(key => {
          if (source[key] != null) fields[key] = source[key]
        })
        this.form = Object.assign(fields, {
          seasons: Array.isArray(source.seasons) ? source.seasons.slice() : [],
          occasions: Array.isArray(source.occasions) ? source.occasions.slice() : []
        })
        this.$nextTick(() => { if (this.$refs.itemForm) this.$refs.itemForm.clearValidate() })
      }
    }
  },
  beforeDestroy() { this.releaseFiles() },
  methods: {
    releaseFiles() {
      this.files.forEach(entry => URL.revokeObjectURL(entry.url))
      this.files = []
    },
    removeFile(index) {
      if (this.saving) return
      URL.revokeObjectURL(this.files[index].url)
      this.files.splice(index, 1)
    },
    chooseFiles(event) {
      if (this.saving) return
      const selected = Array.from(event.target.files || [])
      event.target.value = ''
      if (this.editing && selected.some(validImage)) this.releaseFiles()
      let invalid = false
      selected.forEach(file => {
        if (!validImage(file)) invalid = true
        else this.files.push({ file, url: URL.createObjectURL(file) })
      })
      if (invalid) this.$message.warning('请选择不超过 5 MB 的 JPG 或 PNG 图片')
    },
    close() {
      if (this.saving) return
      this.releaseFiles()
      this.$emit('update:visible', false)
    },
    save() {
      if (this.saving) return
      this.$refs.itemForm.validate(async valid => {
        if (!valid || this.saving) return
        if (!this.editing && !this.files.length) {
          this.$message.warning('请先选择衣服照片')
          return
        }
        // 批量提交使用独立快照，确保每件单品的属性一致。
        const payload = {
          category: this.form.category,
          name: this.form.name,
          color: this.form.color,
          seasons: this.form.seasons.slice(),
          occasions: this.form.occasions.slice(),
          note: this.form.note
        }
        this.saving = true
        try {
          if (this.editing) {
            await updateItem(this.item.id, payload, this.files.length ? this.files[0].file : null)
            this.$message.success('单品已保存')
            this.$emit('saved')
            this.saving = false
            this.close()
            return
          }
          let saved = 0
          const failed = []
          // 每张照片单独保存；失败项留在表单中，重试不会重复新增已保存项。
          for (const entry of this.files) {
            try {
              await addItem(payload, entry.file)
              URL.revokeObjectURL(entry.url)
              saved++
            } catch (error) {
              failed.push(entry)
            }
          }
          this.files = failed
          if (saved) this.$emit('saved')
          if (failed.length) this.$message.warning(`已保存 ${saved} 件，${failed.length} 张照片未保存，请重试`)
          else {
            this.$message.success(`已添加 ${saved} 件衣服`)
            this.saving = false
            this.close()
          }
        } catch (error) {
          // 修改失败时保留表单和图片，便于重试。
        } finally {
          this.saving = false
        }
      })
    }
  }
}
</script>

<style scoped>
.fields { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 16px; }
.photo-list { display: flex; flex-wrap: wrap; gap: 10px; }
.photo, .photo-picker { position: relative; width: 105px; height: 105px; overflow: hidden; border-radius: 8px; background: #f5f1ec; }
.photo img { display: block; width: 100%; height: 100%; object-fit: cover; }
.photo-remove { position: absolute; top: 4px; right: 4px; width: 25px; height: 25px; border: 0; border-radius: 50%; color: white; background: rgba(0,0,0,.55); cursor: pointer; }
.photo-picker { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 5px; border: 1px dashed #cbb8a8; color: #9c8070; font-size: 12px; cursor: pointer; }
.photo-picker.disabled { cursor: not-allowed; opacity: .55; }
.photo-picker i { font-size: 22px; }
.photo-picker input { position: absolute; width: 1px; height: 1px; opacity: 0; }
.tip { color: #9d8e83; font-size: 12px; line-height: 1.6; margin-top: 7px; }
@media (max-width: 500px) { .fields { grid-template-columns: 1fr; gap: 0; } .photo, .photo-picker { width: 88px; height: 88px; } }
</style>
