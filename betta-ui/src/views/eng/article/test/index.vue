<template>
  <div class="test-article" v-loading="loading || submitting">
    <section :class="['test-container', { 'test-container--questions': step === 2 }]">
      <div v-if="challenge.title" class="test-title">
        <span>{{ challenge.title }}</span>
        <span v-if="progress.bestTotalCount" class="best-score">
          历史最佳 {{ progress.bestCorrectCount || 0 }}/{{ progress.bestTotalCount }} 题
        </span>
      </div>

      <el-empty
        v-if="!loading && questionList.length === 0"
        description="该文章暂时没有可用题目"
      >
        <el-button type="primary" @click="backToStudy">选择其他文章</el-button>
      </el-empty>

      <step1
        v-else-if="step === 1"
        :word-list="wordList"
        @next="toStep2"
      />
      <step2
        v-else-if="step === 2"
        :key="roundKey"
        :article-id="articleId"
        :question-list="questionList"
        @complete="submitAnswers"
      />
      <step3
        v-else-if="step === 3"
        :result="result"
        @restart="restart"
        @study="backToStudy"
        @wrong="openWrongWords"
      />
    </section>
  </div>
</template>

<script>
import { getArticleChallenge, submitArticleChallenge } from '@/api/eng/study'
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
      progress: {},
      wordList: [],
      questionList: [],
      result: null,
      step: 0,
      roundKey: 0
    }
  },
  computed: {
    articleId() {
      return this.$route.params && this.$route.params.articleId
    }
  },
  created() {
    this.loadTest()
  },
  methods: {
    /** 加载服务端本轮选中的单词及对应题目。 */
    loadTest() {
      if (!this.articleId) {
        this.$modal.msgError('请指定文章ID')
        this.loading = false
        return
      }
      this.loading = true
      getArticleChallenge(this.articleId).then(response => {
        this.challenge = response.data || {}
        this.wordList = Array.isArray(this.challenge.words)
          ? this.challenge.words.filter(word => word.wordName)
          : []
        this.progress = this.challenge.progress || {}
        this.questionList = Array.isArray(this.challenge.questions)
          ? this.challenge.questions
          : []
        this.step = this.questionList.length ? 1 : 0
      }).finally(() => {
        this.loading = false
      })
    },
    toStep2() {
      this.step = 2
    },
    /** 统一交由服务端判分并维护学习记录、文章进度和错词。 */
    submitAnswers(answers) {
      if (this.submitting) return
      this.submitting = true
      submitArticleChallenge({
        // 路由参数保持字符串传递，避免雪花 ID 转 Number 后精度丢失。
        articleId: this.articleId,
        answers
      }).then(response => {
        this.result = response.data || {}
        this.step = 3
      }).finally(() => {
        this.submitting = false
      })
    },
    restart() {
      this.loading = true
      getArticleChallenge(this.articleId).then(response => {
        this.challenge = response.data || {}
        this.wordList = Array.isArray(this.challenge.words)
          ? this.challenge.words.filter(word => word.wordName)
          : []
        this.progress = this.challenge.progress || {}
        this.questionList = Array.isArray(this.challenge.questions)
          ? this.challenge.questions
          : []
        this.result = null
        this.roundKey++
        this.step = this.questionList.length ? 2 : 0
      }).finally(() => {
        this.loading = false
      })
    },
    backToStudy() {
      this.$router.push('/eng/study/index')
    },
    openWrongWords() {
      this.$router.push('/eng/study/wrong')
    }
  }
}
</script>

<style lang="scss">
.test-article {
  min-height: calc(100vh - 84px);
  padding: 24px;
  color: #333;
  background: linear-gradient(135deg, #e8f3ff 0%, #f4ecff 55%, #fff6e5 100%);

  .test-container {
    position: relative;
    width: 460px;
    height: calc(100vh - 164px);
    min-height: 560px;
    margin: 16px auto;
    padding: 20px;
    overflow: hidden;
    border: 1px solid rgba(255, 255, 255, 0.3);
    border-right-color: rgba(255, 255, 255, 0.2);
    border-bottom-color: rgba(255, 255, 255, 0.2);
    border-radius: 10px;
    background: rgba(255, 255, 255, 0.18);
    box-shadow: 0 25px 45px rgba(0, 0, 0, 0.1);
    backdrop-filter: blur(18px);
  }

  .test-container--questions {
    display: flex;
    flex-direction: column;
    height: auto;
    min-height: max(560px, calc(100vh - 164px));
    overflow: visible;
  }

  .test-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    min-height: 28px;
    margin-bottom: 12px;
    font-weight: 600;
  }

  .best-score {
    color: #909399;
    font-size: 12px;
    font-weight: normal;
  }

  .box-block {
    width: 100%;
    margin-top: 14px;
    padding: 13px 18px;
    border: 1px solid rgba(255, 255, 255, 0.3);
    border-right-color: rgba(255, 255, 255, 0.2);
    border-bottom-color: rgba(255, 255, 255, 0.2);
    border-radius: 28px;
    background: rgba(255, 255, 255, 0.3);
    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.05);
    font-size: 14px;
  }

  .block-button {
    min-width: 96px;
    padding: 12px 18px;
    border: 1px solid rgba(255, 255, 255, 0.3);
    border-radius: 28px;
    color: #333;
    background: #fff;
    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.05);
    font-weight: 600;
    cursor: pointer;
  }

  .block-button:disabled {
    color: #c0c4cc;
    cursor: not-allowed;
  }

  .toolbar {
    display: flex;
    justify-content: center;
    gap: 12px;
    padding: 10px 0;
  }
}

@media (max-width: 600px) {
  .test-article {
    padding: 12px;

    .test-container {
      width: 100%;
      min-height: calc(100vh - 132px);
      margin: 0;
    }
  }
}
</style>
