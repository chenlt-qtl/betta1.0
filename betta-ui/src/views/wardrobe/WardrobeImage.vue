<template>
  <div class="wardrobe-image">
    <div v-if="showMosaic" class="wardrobe-image__mosaic">
      <wardrobe-image v-for="item in mosaicItems" :key="item.id" :item-id="item.id" />
      <span v-if="!mosaicItems.length" class="wardrobe-image__placeholder"><i class="el-icon-picture-outline" /></span>
    </div>
    <img v-else-if="source" :src="source" :alt="alt" />
    <span v-else class="wardrobe-image__placeholder"><i :class="loading ? 'el-icon-loading' : 'el-icon-picture-outline'" /></span>
  </div>
</template>

<script>
import { getWardrobeImage } from '@/api/wardrobe'

export default {
  name: 'WardrobeImage',
  props: {
    itemId: { type: [Number, String], default: null },
    outfit: { type: Object, default: null },
    alt: { type: String, default: '衣橱图片' }
  },
  data() {
    return { source: '', loading: false, requestVersion: 0 }
  },
  computed: {
    showMosaic() { return !!this.outfit && !this.outfit.hasImage },
    mosaicItems() {
      if (!this.outfit) return []
      if (this.outfit.items && this.outfit.items.length) return this.outfit.items.slice(0, 4)
      return (this.outfit.itemIds || []).slice(0, 4).map(id => ({ id }))
    }
  },
  watch: {
    itemId: { immediate: true, handler() { this.loadImage() } },
    outfit() { this.loadImage() }
  },
  beforeDestroy() {
    this.requestVersion++
    this.releaseImage()
  },
  methods: {
    releaseImage() {
      if (this.source) URL.revokeObjectURL(this.source)
      this.source = ''
    },
    async loadImage() {
      const version = ++this.requestVersion
      this.releaseImage()
      const kind = this.outfit ? 'outfits' : 'items'
      const id = this.outfit ? this.outfit.id : this.itemId
      if (!id || (this.outfit && !this.outfit.hasImage)) {
        this.loading = false
        return
      }
      this.loading = true
      try {
        const blob = await getWardrobeImage(kind, id)
        // 列表切换或组件销毁后到达的旧响应不能覆盖新图片。
        if (version === this.requestVersion) this.source = URL.createObjectURL(blob)
      } catch (error) {
        // 私有图片读取失败时保留占位，避免把错误响应当作图片。
      } finally {
        if (version === this.requestVersion) this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.wardrobe-image { width: 100%; height: 100%; min-height: 0; overflow: hidden; background: #f4f0ea; }
.wardrobe-image > img { width: 100%; height: 100%; object-fit: cover; display: block; }
.wardrobe-image__placeholder { display: flex; width: 100%; height: 100%; min-height: 70px; align-items: center; justify-content: center; color: #aa9b8d; font-size: 26px; }
.wardrobe-image__mosaic { width: 100%; height: 100%; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); grid-auto-rows: minmax(0, 1fr); gap: 2px; }
</style>
