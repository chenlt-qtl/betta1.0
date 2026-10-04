<template>
  <div class="test-article" v-loading="loading || submitting">
    <section :class="['test-container', { 'test-container--questions': step === 2 }]">
      <div v-if="challenge.title" class="test-title">
        <span>{{ challenge.title }}</span>
        <span class="challenge-mode">{{ isReview ? '单词复习' : `第 ${challenge.levelNo || levelNo} 关` }}</span>
      </div>

      <el-empty v-if="!loading && questionList.length === 0" :description="emptyDescription">
        <el-button type="primary" @click="backToLearning">返回学习</el-button>
      </el-empty>
      <step1 v-else-if="step === 1" :word-list="wordList" @back="backToLearning" @next="toStep2" />
      <step2
        v-else-if="step === 2"
        :key="roundKey"
        :attempt-id="challenge.attemptId"
        :mode="mode"
        :article-id="articleId"
        :level-no="levelNo"
        :question-list="questionList"
        @complete="submitAnswers"
      />
      <step3
        v-else-if="step === 3"
        :result="result"
        @restart="restart"
        @next="goNextLevel"
        @study="backToLearning"
        @wrong="openWrongWords"
        @review="openReview"
      />
    </section>

  </div>
</template>

<script>
import { getStudyChallenge, submitArticleChallenge } from '@/api/eng/study'
import Step1 from './step1.vue'
import Step2 from './step2.vue'
import Step3 from './step3.vue'

export default {
  name: 'EngArticleTest',
  components: { Step1, Step2, Step3 },
  data() {
    return {
      loading: true,
      submitting: false,
      challenge: {},
      wordList: [],
      questionList: [],
      result: null,
      step: 0,
      roundKey: 0
    }
  },
  computed: {
    mode() { return String(this.$route.query.mode || '').toUpperCase() },
    isReview() { return this.mode === 'REVIEW' },
    articleId() {
      if (this.isReview) return null
      return this.$route.params && this.$route.params.articleId
    },
    levelNo() {
      if (this.isReview) return null
      const value = Number(this.$route.query.levelNo)
      return Number.isInteger(value) && value > 0 ? value : null
    },
    wordIds() { return this.isReview ? String(this.$route.query.wordIds || '') : '' },
    emptyDescription() {
      return this.isReview ? '当前没有可复习的单词' : '本关没有需要学习的新词，请返回地图继续'
    }
  },
  created() {
    if (!['NEW', 'REVIEW'].includes(this.mode)) {
      const articleId = this.$route.params && this.$route.params.articleId
      this.$router.replace(articleId ? '/eng/study/levels/' + articleId : '/eng/study/index')
      return
    }
    this.loadTest()
  },
  methods: {
    challengeQuery() {
      const query = { mode: this.mode }
      if (this.isReview) {
        if (this.wordIds) query.wordIds = this.wordIds
      } else {
        query.articleId = this.articleId
        query.levelNo = this.levelNo
      }
      return query
    },
    loadTest() {
      if (!this.isReview && (!this.articleId || !this.levelNo)) {
        this.$modal.msgError('请指定文章和关卡')
        this.loading = false
        return
      }
      this.loading = true
      this.step = 0
      getStudyChallenge(this.challengeQuery()).then(response => {
        this.challenge = response.data || {}
        this.wordList = Array.isArray(this.challenge.words) ? this.challenge.words.filter(word => word.wordName) : []
        this.questionList = Array.isArray(this.challenge.questions) ? this.challenge.questions : []
        this.result = null
        this.roundKey++
        if (!this.questionList.length) return
        this.step = !this.isReview && this.wordList.length ? 1 : 2
      }).finally(() => { this.loading = false })
    },
    toStep2() { this.step = 2 },
    submitAnswers(answers) {
      if (this.submitting) return
      this.submitting = true
      submitArticleChallenge({
        attemptId: this.challenge.attemptId,
        mode: this.mode,
        articleId: this.articleId,
        levelNo: this.levelNo,
        answers
      }).then(response => {
        this.result = response.data || {}
        this.step = 3
      }).finally(() => { this.submitting = false })
    },
    restart() { this.loadTest() },
    goNextLevel() {
      if (this.isReview || !this.result || this.result.nextLevelUnlocked !== true || this.result.nextLevelNo == null) {
        this.backToLearning()
        return
      }
      this.$router.replace({
        path: '/eng/study/challenge/' + this.articleId,
        query: { mode: 'NEW', levelNo: String(this.result.nextLevelNo) }
      }).then(() => this.loadTest())
    },
    backToLearning() {
      this.$router.push(this.isReview ? '/eng/study/review' : '/eng/study/levels/' + this.articleId)
    },
    openReview() { this.$router.push('/eng/study/review') },
    openWrongWords() { this.$router.push('/eng/study/wrong') }
  }
}
</script>

<style lang="scss">
.test-article {
  min-height: calc(100vh - 84px);
  padding: 24px;
  color: #333;
  background: linear-gradient(135deg, #e4f6dc 0%, #eef8e8 50%, #fff7df 100%);

  .test-container { position: relative; width: 460px; height: calc(100vh - 164px); min-height: 560px; margin: 16px auto; padding: 20px; overflow: hidden; border: 1px solid rgba(255, 255, 255, .7); border-radius: 12px; background: rgba(255, 255, 255, .55); box-shadow: 0 25px 45px rgba(53, 112, 52, .12); backdrop-filter: blur(18px); }
  .test-container--questions { display: flex; flex-direction: column; height: auto; min-height: max(560px, calc(100vh - 164px)); overflow: visible; }
  .test-title { display: flex; align-items: center; justify-content: space-between; gap: 10px; min-height: 28px; margin-bottom: 12px; font-weight: 600; }
  .challenge-mode { color: #67a85d; font-size: 12px; font-weight: normal; }
  .box-block { width: 100%; margin-top: 14px; padding: 13px 18px; border: 1px solid rgba(255, 255, 255, .7); border-radius: 28px; background: rgba(255, 255, 255, .7); box-shadow: 0 5px 15px rgba(0, 0, 0, .05); font-size: 14px; }
  .block-button { min-width: 96px; padding: 12px 18px; border: 1px solid rgba(255, 255, 255, .7); border-radius: 28px; color: #333; background: #fff; box-shadow: 0 5px 15px rgba(0, 0, 0, .05); font-weight: 600; cursor: pointer; }
  .block-button:disabled { color: #c0c4cc; cursor: not-allowed; }
  .toolbar { display: flex; justify-content: center; gap: 12px; padding: 10px 0; }
}
@media (max-width: 600px) {
  .test-article { min-height: calc(100vh - 50px); padding: 12px; overflow-x: hidden; }
  .test-article .test-container { width: 100%; min-height: calc(100vh - 96px); margin: 0; padding: 16px; }
}
</style>
