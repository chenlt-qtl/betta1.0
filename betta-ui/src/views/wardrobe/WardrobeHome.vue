<template>
  <section class="home">
    <div class="today-card">
      <div class="today-intro">
        <span class="label">TODAY · {{ today }}</span>
        <h2>今天穿什么？</h2>
        <p>从自己的衣橱里，找到今天喜欢的搭配。</p>
        <div class="actions">
          <el-button type="primary" size="small" @click="$emit('go', 'closet', 'create')">添加衣服</el-button>
          <el-button size="small" @click="$emit('go', 'outfits', 'create')">创建搭配</el-button>
        </div>
      </div>
    </div>

    <div class="recommend-heading">
      <div><span class="label">FOR YOU</span><h2>今日搭配灵感</h2><p>可手动输入温度和场合，查看衣橱中适合的搭配。</p></div>
      <div class="filters">
        <el-input v-model.trim="temperature" type="number" placeholder="温度（℃）" clearable aria-label="温度" />
        <el-select v-model="occasion" placeholder="全部场合" clearable filterable allow-create default-first-option aria-label="场合">
          <el-option v-for="value in occasions" :key="value" :label="value" :value="value" />
        </el-select>
        <el-button @click="loadRecommendations">查看推荐</el-button>
      </div>
    </div>
    <div v-loading="loading" class="recommendations">
      <div v-for="outfit in recommendations" :key="outfit.id" class="recommend-card">
        <div class="cover"><wardrobe-image :outfit="outfit" :alt="outfit.name" /></div>
        <div class="card-text"><strong>{{ outfit.name }}</strong><span>{{ (outfit.occasions || []).join(' · ') || '适合多个场合' }}</span></div>
      </div>
      <el-empty v-if="!loading && !recommendations.length" description="还没有符合条件的搭配，先去创建一套吧" />
    </div>
  </section>
</template>

<script>
import { getRecommendations } from '@/api/wardrobe'
import WardrobeImage from './WardrobeImage'
import { dateKey, OCCASIONS } from './constants'

export default {
  name: 'WardrobeHome',
  components: { WardrobeImage },
  data() {
    return { today: dateKey(new Date()), loading: false, temperature: '', occasion: '', occasions: OCCASIONS, recommendations: [], recommendationRequestVersion: 0 }
  },
  created() {
    this.loadRecommendations()
  },
  beforeDestroy() {
    this.recommendationRequestVersion++
  },
  methods: {
    async loadRecommendations() {
      // 后发请求才代表当前筛选，旧响应和卸载后的响应不能覆盖页面。
      const version = ++this.recommendationRequestVersion
      const params = {}
      if (this.temperature !== '') {
        const temperature = Number(this.temperature)
        if (!Number.isFinite(temperature)) {
          this.loading = false
          this.$message.warning('请输入有效温度')
          return
        }
        params.temperature = temperature
      }
      if (this.occasion) params.occasion = this.occasion
      this.loading = true
      try {
        const response = await getRecommendations(params)
        if (version === this.recommendationRequestVersion) this.recommendations = response.data || []
      } catch (error) {
        if (version === this.recommendationRequestVersion) this.recommendations = []
      } finally {
        if (version === this.recommendationRequestVersion) this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.today-card { padding: 30px; border-radius: 18px; background: #eaded2; }
.label { color: #a17e6a; font-size: 11px; font-weight: 700; letter-spacing: .16em; }
h2 { margin: 8px 0; font-size: 23px; }
p { margin: 0; color: #85766d; font-size: 13px; line-height: 1.7; }
.today-intro { align-self: center; }
.actions { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 22px; }
.actions .el-button { margin-left: 0; }
.recommend-heading { display: flex; justify-content: space-between; align-items: flex-end; gap: 15px; margin: 34px 0 17px; }
.filters { display: flex; gap: 8px; flex-wrap: wrap; }
.filters .el-input { width: 120px; }
.filters .el-select { width: 150px; }
.recommendations { display: grid; grid-template-columns: repeat(auto-fill, minmax(155px, 1fr)); gap: 15px; min-height: 120px; }
.recommendations .el-empty { grid-column: 1 / -1; }
.recommend-card { overflow: hidden; border: 1px solid #eee8e1; border-radius: 12px; background: #fff; }
.cover { height: 190px; }
.card-text { padding: 12px; }
.card-text strong, .card-text span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-text span { margin-top: 6px; color: #9c8d83; font-size: 12px; }
@media (max-width: 767px) {
  .today-card { padding: 20px; }
  .recommend-heading { align-items: stretch; flex-direction: column; }
  .filters .el-input, .filters .el-select { flex: 1 1 120px; width: auto; }
  .recommendations { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
  .cover { height: 160px; }
}
</style>
