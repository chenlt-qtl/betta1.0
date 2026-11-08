<template>
  <div class="wardrobe-app">
    <header class="wardrobe-header">
      <div><span class="eyebrow">MY WARDROBE</span><h1>我的衣橱</h1><p>整理衣服，记录每天喜欢的搭配。</p></div>
    </header>
    <main class="wardrobe-main">
      <wardrobe-home v-if="active === 'home'" @go="navigate" />
      <wardrobe-closet v-else-if="active === 'closet'" :open-create="openCreate" @go="navigate" />
      <wardrobe-outfits v-else-if="active === 'outfits'" :open-create="openCreate" @go="navigate" />
    </main>
    <nav class="wardrobe-nav" aria-label="衣橱导航">
      <button v-for="tab in tabs" :key="tab.name" type="button" :class="{ active: active === tab.name }" @click="navigate(tab.name)">
        <i :class="tab.icon" /><span>{{ tab.label }}</span>
      </button>
      <button type="button" @click="$router.push('/user/profile')"><i class="el-icon-user" /><span>我的</span></button>
    </nav>
  </div>
</template>

<script>
import WardrobeHome from './WardrobeHome'
import WardrobeCloset from './WardrobeCloset'
import WardrobeOutfits from './WardrobeOutfits'

export default {
  name: 'Wardrobe',
  components: { WardrobeHome, WardrobeCloset, WardrobeOutfits },
  data() {
    return {
      active: 'home',
      openCreate: false,
      tabs: [
        { name: 'home', label: '首页', icon: 'el-icon-house' },
        { name: 'closet', label: '衣橱', icon: 'el-icon-menu' },
        { name: 'outfits', label: '搭配', icon: 'el-icon-collection' }
      ]
    }
  },
  methods: {
    navigate(name, action) {
      if (this.tabs.some(tab => tab.name === name)) {
        this.openCreate = action === 'create'
        this.active = name
      }
    }
  }
}
</script>

<style scoped>
.wardrobe-app { max-width: 1320px; min-height: calc(100vh - 110px); margin: 0 auto; padding: 28px 32px 45px; color: #392f2a; background: #faf8f5; }
.wardrobe-header { margin-bottom: 23px; }
.wardrobe-header h1 { font-size: 28px; margin: 5px 0; letter-spacing: .08em; }
.wardrobe-header p { margin: 0; color: #8f8075; font-size: 14px; }
.eyebrow { color: #b8957d; font-size: 11px; letter-spacing: .18em; font-weight: bold; }
.wardrobe-nav { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 30px; }
.wardrobe-nav button { display: flex; align-items: center; gap: 6px; border: 0; border-radius: 20px; background: #fff; color: #77675c; padding: 10px 18px; cursor: pointer; }
.wardrobe-nav button.active { color: white; background: #997563; }
@media (max-width: 767px) {
  .wardrobe-app { padding: 20px 14px 12px; min-height: calc(100vh - 80px); }
  .wardrobe-header h1 { font-size: 23px; }
  .wardrobe-nav { position: sticky; bottom: 0; z-index: 9; display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 0; padding: 6px 2px calc(6px + env(safe-area-inset-bottom)); margin: 20px -14px -12px; background: #fff; box-shadow: 0 -3px 18px rgba(59, 43, 33, .08); }
  .wardrobe-nav button { flex-direction: column; gap: 3px; border-radius: 10px; padding: 7px 1px; font-size: 12px; }
  .wardrobe-nav button i { font-size: 18px; }
}
</style>
