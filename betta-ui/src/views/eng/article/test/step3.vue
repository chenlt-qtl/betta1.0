<template>
  <div class="test-step3">
    <section class="step3-result">
      <i :class="passed ? 'el-icon-success passed' : 'el-icon-warning-outline pending'" />
      <h2>{{ resultTitle }}</h2>
      <div class="result-stars" :aria-label="`本轮 ${stars} 星`">
        <i v-for="star in 3" :key="star" :class="star <= stars ? 'el-icon-star-on' : 'el-icon-star-off'" />
      </div>
      <p class="score">得分 {{ result.score || 0 }} · 答对 {{ correctCount }}/{{ totalCount }} 题</p>

      <div v-if="hasPronunciation" class="pronunciation-summary">
        <strong>跟读评分</strong>
        <span>合格 {{ pronunciationPassedCount }}/{{ pronunciationTotalCount }} 个</span>
        <span>平均 {{ pronunciationAverageScore }} 分</span>
      </div>

      <div class="coin-reward">本轮获得 {{ coinReward }} 金币</div>
      <div class="reward-detail">
        <span>{{ isSpelling ? '拼写里程碑' : '星级里程碑' }} +{{ milestoneCoin }}</span>
        <span v-if="!isSpelling">复习奖励 +{{ reviewCoin }}</span>
      </div>
      <div class="coin-balance"><i class="el-icon-coin" /> 金币余额 {{ result.coinBalance || 0 }}</div>

      <div v-if="wordResults.length" class="word-results">
        <div v-for="word in wordResults" :key="word.wordId" class="word-result-row">
          <strong>{{ word.wordName || ('单词 #' + word.wordId) }}</strong>
          <span>{{ word.correctCount || 0 }}/{{ word.totalCount || 0 }} 题</span>
          <span v-if="hasWordPronunciation(word)" :class="word.pronunciationPassed ? 'pronunciation-passed' : 'pronunciation-pending'">
            跟读 {{ word.pronunciationScore == null ? '--' : Math.round(Number(word.pronunciationScore)) }} 分
          </span>
          <span class="word-stars">{{ displayWordStars(word) }}★</span>
          <span v-if="isReview" :class="{ rewarded: word.allCorrect }">{{ word.allCorrect ? '+1 复习币' : '本轮未全对' }}</span>
        </div>
      </div>
    </section>
    <div class="toolbar result-actions">
      <button class="block-button" @click="$emit('restart')">再测一次</button>
      <button v-if="canOpenNextLevel" class="block-button primary-action" @click="$emit('next')">下一关</button>
      <button v-if="isWordSelectionMode" class="block-button primary-action" @click="$emit('review')">选择其他单词</button>
      <button class="block-button" @click="$emit('study')">{{ isWordSelectionMode ? '返回选词' : '返回地图' }}</button>
      <button class="block-button" @click="$emit('wrong')">错词本</button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'EngArticleTestResult',
  props: {
    result: { type: Object, default: () => ({}) }
  },
  computed: {
    isReview() { return String(this.result.mode || '').toUpperCase() === 'REVIEW' },
    isSpelling() { return String(this.result.mode || '').toUpperCase() === 'SPELLING' },
    isWordSelectionMode() { return this.isReview || this.isSpelling },
    stars() { return Number(this.result.stars) || 0 },
    passed() { return this.isWordSelectionMode || this.stars >= 1 },
    canOpenNextLevel() {
      return !this.isWordSelectionMode && this.result.nextLevelUnlocked === true && this.result.nextLevelNo != null
    },
    resultTitle() {
      if (this.isSpelling) return '拼写测试完成！'
      if (this.isReview) return '复习完成！'
      return this.passed ? '闯关成功！' : '再试一次，就能解锁下一关'
    },
    coinReward() { return Number(this.result.coinReward) || 0 },
    milestoneCoin() { return Number(this.result.milestoneCoin) || 0 },
    reviewCoin() { return Number(this.result.reviewCoin) || 0 },
    wordResults() { return Array.isArray(this.result.wordResults) ? this.result.wordResults : [] },
    questionResults() { return Array.isArray(this.result.results) ? this.result.results : [] },
    correctCount() {
      if (this.result.correctCount != null) return this.result.correctCount
      return this.questionResults.filter(item => item.correct).length
    },
    totalCount() {
      return this.result.totalCount == null ? this.questionResults.length : this.result.totalCount
    },
    pronunciationTotalCount() { return Number(this.result.pronunciationTotalCount) || 0 },
    pronunciationPassedCount() { return Number(this.result.pronunciationPassedCount) || 0 },
    pronunciationAverageScore() {
      const score = Number(this.result.pronunciationAverageScore)
      return Number.isFinite(score) ? Math.round(score) : 0
    },
    hasPronunciation() {
      return this.pronunciationTotalCount > 0
    }
  },
  methods: {
    displayWordStars(word) {
      // 拼写星级与普通学习星级独立，结算时应展示本轮拼写结果。
      if (this.isSpelling && word.stars != null) return word.stars
      if (word.currentStars != null) return word.currentStars
      if (word.latestStars != null) return word.latestStars
      return word.stars || 0
    },
    hasWordPronunciation(word) {
      return word && (word.pronunciationScore != null || typeof word.pronunciationPassed === 'boolean')
    }
  }
}
</script>

<style scoped lang="scss">
.test-step3 { display: flex; flex-direction: column; height: calc(100% - 40px); }
.step3-result { display: flex; flex: 1; flex-direction: column; align-items: center; min-height: 0; overflow-y: auto; text-align: center; }
.step3-result > i { margin-top: 18px; font-size: 52px; }
.passed { color: #67c23a; }
.pending { color: #e6a23c; }
h2 { margin: 14px 0 4px; }
.result-stars { color: #f5b51b; font-size: 42px; line-height: 1; }
.score { margin: 8px 0; color: #606266; }
.pronunciation-summary { display: flex; flex-wrap: wrap; justify-content: center; gap: 8px 14px; margin: 2px 0 12px; padding: 8px 14px; border-radius: 18px; color: #606266; background: rgba(236, 245, 255, .85); font-size: 13px; }
.pronunciation-summary strong { color: #409eff; }
.coin-reward { color: #e6a23c; font-size: 30px; font-weight: 600; }
.reward-detail { display: flex; flex-wrap: wrap; justify-content: center; gap: 14px; margin: 10px 0; color: #606266; font-size: 13px; }
.coin-balance { padding: 7px 15px; border-radius: 18px; color: #b88230; background: rgba(253, 246, 236, .9); font-weight: 600; }
.word-results { width: 100%; max-height: 190px; margin-top: 14px; overflow-y: auto; border-radius: 8px; background: rgba(255, 255, 255, .6); }
.word-result-row { display: grid; grid-template-columns: minmax(90px, 1fr) repeat(4, auto); align-items: center; gap: 10px; padding: 9px 12px; border-bottom: 1px solid rgba(220, 223, 230, .7); color: #606266; font-size: 12px; text-align: left; }
.word-result-row strong { min-width: 0; overflow-wrap: anywhere; color: #303133; font-size: 14px; }
.word-stars, .rewarded { color: #e6a23c; }
.pronunciation-passed { color: #67c23a; }
.pronunciation-pending { color: #e6a23c; }
.result-actions { flex-wrap: wrap; }
.primary-action { color: #fff !important; border-color: #67c23a !important; background: #67c23a !important; }

@media (max-width: 600px) {
  .step3-result > i { margin-top: 6px; font-size: 44px; }
  h2 { font-size: 21px; }
  .result-stars { font-size: 34px; }
  .coin-reward { font-size: 25px; }
  .word-result-row { grid-template-columns: minmax(72px, 1fr) auto auto; }
  .word-result-row span:nth-of-type(n+3) { grid-column: auto; }
  .word-result-row span:last-child { grid-column: 1 / -1; }
  .result-actions .block-button { flex: 1 1 calc(50% - 12px); min-width: 0; }
}
</style>
