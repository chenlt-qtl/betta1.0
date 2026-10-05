<template>
  <div class="app-container review-page" v-loading="loading">
    <header class="review-header">
      <div>
        <h2>单词复习</h2>
        <p>当前星低于最高星的单词会优先推荐，也可以主动选择其他已学单词。</p>
      </div>
      <el-button icon="el-icon-arrow-left" @click="backToStudy">学习中心</el-button>
    </header>

    <el-card shadow="never" class="review-summary">
      <div><strong>{{ overview.learnedWordCount || words.length }}</strong><span>已学单词</span></div>
      <div><strong>{{ overview.recommendedCount || recommendedWords.length }}</strong><span>建议复习</span></div>
      <div><strong>{{ selectedWordIds.length }}/5</strong><span>本轮已选</span></div>
    </el-card>

    <div class="review-toolbar">
      <el-radio-group v-model="viewMode" size="small">
        <el-radio-button label="recommended">推荐复习</el-radio-button>
        <el-radio-button label="all">全部已学</el-radio-button>
      </el-radio-group>
      <div class="review-actions">
        <el-button type="primary" :disabled="selectedWordIds.length === 0" @click="startReview">
          普通复习（{{ selectedWordIds.length }}）
        </el-button>
        <el-button type="warning" :disabled="selectedWordIds.length === 0" @click="startSpelling">
          拼写测试（{{ selectedWordIds.length }}）
        </el-button>
      </div>
    </div>

    <el-empty v-if="!loading && visibleWords.length === 0" :description="emptyDescription" />
    <div v-else class="review-grid">
      <button
        v-for="word in visibleWords"
        :key="word.wordId"
        type="button"
        :class="['review-card', { 'is-selected': isSelected(word.wordId) }]"
        @click="toggleWord(word.wordId)"
      >
        <div class="word-heading">
          <strong>{{ word.wordName }}</strong>
          <div class="word-tags">
            <el-tag v-if="word.recommended" type="warning" size="mini">建议复习</el-tag>
            <el-tag v-if="word.spellingEligible === false" type="info" size="mini">不可拼写</el-tag>
          </div>
        </div>
        <div class="star-row">
          <span>当前 <b>{{ word.currentStars || 0 }}★</b></span>
          <span>最新 {{ word.latestStars || 0 }}★</span>
          <span>最高 {{ word.highestStars || 0 }}★</span>
        </div>
        <div class="word-meta">最近测试：{{ formatTime(word.latestTestTime) }}</div>
        <div class="word-meta sources">来源：{{ sourceText(word.sourceArticles) }}</div>
        <i :class="isSelected(word.wordId) ? 'el-icon-success selected-icon' : 'el-icon-circle-check select-icon'" />
      </button>
    </div>
  </div>
</template>

<script>
import { getReviewOverview } from '@/api/eng/study'

export default {
  name: 'EngStudyReview',
  data() {
    return {
      loading: false,
      overview: {},
      words: [],
      selectedWordIds: [],
      viewMode: 'recommended'
    }
  },
  computed: {
    recommendedWords() {
      return this.words.filter(word => word.recommended)
    },
    visibleWords() {
      return this.viewMode === 'recommended' ? this.recommendedWords : this.words
    },
    emptyDescription() {
      return this.viewMode === 'recommended' ? '当前没有建议复习的单词' : '还没有已学单词'
    }
  },
  created() {
    this.loadReview()
  },
  methods: {
    loadReview() {
      this.loading = true
      getReviewOverview().then(response => {
        this.overview = response.data || {}
        this.words = Array.isArray(this.overview.words) ? this.overview.words : []
        const requestedIds = String(this.$route.query.wordIds || '').split(',').filter(Boolean)
        const defaultIds = requestedIds.length
          ? requestedIds
          : this.recommendedWords.slice(0, 5).map(word => word.wordId)
        this.selectedWordIds = defaultIds
          .filter(id => this.words.some(word => String(word.wordId) === String(id)))
          .slice(0, 5)
        if (requestedIds.length) this.viewMode = 'all'
      }).finally(() => {
        this.loading = false
      })
    },
    isSelected(wordId) {
      return this.selectedWordIds.some(id => String(id) === String(wordId))
    },
    toggleWord(wordId) {
      const index = this.selectedWordIds.findIndex(id => String(id) === String(wordId))
      if (index >= 0) {
        this.selectedWordIds.splice(index, 1)
        return
      }
      if (this.selectedWordIds.length >= 5) {
        this.$modal.msgWarning('每轮最多选择 5 个不同单词')
        return
      }
      this.selectedWordIds.push(wordId)
    },
    startReview() {
      this.startChallenge('REVIEW')
    },
    startSpelling() {
      if (!this.selectedWordIds.length) return
      const ineligibleWords = this.words.filter(word =>
        this.isSelected(word.wordId) && word.spellingEligible !== true
      )
      if (ineligibleWords.length) {
        this.$modal.msgWarning('拼写测试只能选择不少于 4 个字符且全部为英文字母的单词')
        return
      }
      this.startChallenge('SPELLING')
    },
    /** 普通复习与拼写测试复用挑战页，通过模式隔离出题和奖励规则。 */
    startChallenge(mode) {
      if (!this.selectedWordIds.length) return
      this.$router.push({
        path: '/eng/study/challenge/review',
        query: {
          mode,
          wordIds: this.selectedWordIds.map(String).join(',')
        }
      })
    },
    formatTime(value) {
      return this.parseTime(value) || '尚未测试'
    },
    sourceText(sources) {
      if (!Array.isArray(sources) || !sources.length) return '-'
      return sources.map(source => {
        if (typeof source === 'string') return source
        return source.title || source.articleTitle || ('文章 #' + source.articleId)
      }).join('、')
    },
    backToStudy() {
      this.$router.push('/eng/study/index')
    }
  }
}
</script>

<style scoped lang="scss">
.review-page { min-height: calc(100vh - 84px); background: #f5f7f2; }
.review-header, .review-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.review-header { margin-bottom: 18px; }
.review-header h2 { margin: 0 0 8px; color: #2e6b35; }
.review-header p { margin: 0; color: #71806e; }
.review-summary { margin-bottom: 18px; }
.review-summary ::v-deep .el-card__body { display: flex; justify-content: space-around; text-align: center; }
.review-summary div { display: flex; flex-direction: column; gap: 5px; }
.review-summary strong { color: #3b8b43; font-size: 26px; }
.review-summary span { color: #71806e; font-size: 13px; }
.review-toolbar { margin-bottom: 18px; }
.review-actions, .word-tags { display: flex; align-items: center; gap: 8px; }
.review-actions .el-button, .word-tags .el-tag { margin: 0; }
.review-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.review-card {
  position: relative;
  min-width: 0;
  padding: 16px 42px 16px 16px;
  overflow: hidden;
  border: 2px solid transparent;
  border-radius: 12px;
  color: #303133;
  background: #fff;
  box-shadow: 0 5px 18px rgba(46, 107, 53, 0.08);
  text-align: left;
  cursor: pointer;
}
.review-card.is-selected { border-color: #67c23a; background: #f0f9eb; }
.word-heading { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.word-heading strong { min-width: 0; overflow-wrap: anywhere; font-size: 20px; }
.star-row { display: flex; flex-wrap: wrap; gap: 10px; margin: 13px 0; color: #606266; font-size: 13px; }
.star-row b { color: #e6a23c; }
.word-meta { overflow: hidden; color: #909399; font-size: 12px; line-height: 1.7; text-overflow: ellipsis; white-space: nowrap; }
.selected-icon, .select-icon { position: absolute; right: 14px; bottom: 14px; font-size: 22px; }
.selected-icon { color: #67c23a; }
.select-icon { color: #c0c4cc; }

@media (max-width: 900px) { .review-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 600px) {
  .review-page { min-height: calc(100vh - 50px); padding: 12px; overflow-x: hidden; }
  .review-header { align-items: flex-start; }
  .review-header p { font-size: 12px; }
  .review-summary ::v-deep .el-card__body { padding: 12px 6px; }
  .review-toolbar { align-items: stretch; flex-direction: column; }
  .review-actions { width: 100%; }
  .review-actions .el-button { flex: 1; min-width: 0; }
  .review-grid { grid-template-columns: 1fr; }
}
</style>
