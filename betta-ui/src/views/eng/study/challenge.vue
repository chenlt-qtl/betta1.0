<template>
  <div class="app-container challenge-page" v-loading="loading">
    <div class="challenge-header">
      <el-button icon="el-icon-arrow-left" @click="backToStudy">返回学习中心</el-button>
      <div v-if="!result" class="progress-text">
        <span v-if="progress.bestTotalCount">历史最佳 {{ progress.bestScore || 0 }} 分</span>
        <span>已答 {{ answeredCount }}/{{ questions.length }}</span>
      </div>
    </div>

    <el-empty v-if="!loading && !result && questions.length === 0" description="该文章暂时没有可用题目">
      <el-button type="primary" @click="backToStudy">选择其他文章</el-button>
    </el-empty>

    <template v-else-if="!result">
      <el-card class="challenge-card" shadow="never">
        <div slot="header">
          <strong>{{ challenge.title || '文章闯关' }}</strong>
        </div>
        <el-progress :percentage="progressPercent" :show-text="false" />
        <el-alert
          v-if="hasUnsupportedQuestion"
          class="unsupported-alert"
          title="题目数据包含暂不支持的题型，本次闯关无法继续"
          type="error"
          :closable="false"
          show-icon
        />

        <div v-if="currentQuestion" class="question-box">
          <div class="question-index">第 {{ currentIndex + 1 }} 题</div>
          <div class="question-type">
            <el-tag size="small">{{ questionTypeLabel(currentQuestion.type) }}</el-tag>
            <el-button
              v-if="canPlayCurrentAudio"
              type="text"
              icon="el-icon-video-play"
              @click="playCurrentAudio"
            >
              重播发音
            </el-button>
          </div>
          <div class="question-prompt">{{ currentQuestion.prompt }}</div>

          <el-radio-group
            v-if="isChoiceQuestion(currentQuestion)"
            v-model="answers[currentQuestion.questionId]"
            class="option-list"
            @change="checkCurrentAnswer"
          >
            <el-radio
              v-for="option in currentQuestion.options"
              :key="optionValue(option)"
              :label="optionValue(option)"
              border
            >
              {{ optionLabel(option) }}
            </el-radio>
          </el-radio-group>
          <div v-else-if="currentQuestion.type === 'SENTENCE_FILL'" class="fill-answer-row">
            <div class="fill-letter-list">
              <input
                v-for="(letter, letterIndex) in currentFillLetters"
                :key="letterIndex"
                :ref="'fillInputs-' + currentQuestion.questionId"
                :value="letter"
                class="fill-letter-input"
                maxlength="1"
                autocomplete="off"
                :aria-label="`第 ${letterIndex + 1} 个字母`"
                @input="handleFillInput(letterIndex, $event)"
                @keydown.backspace="handleFillBackspace(letterIndex, $event)"
              >
            </div>
            <el-button
              v-if="currentQuestion.audioUrl"
              class="fill-audio-button"
              type="primary"
              size="mini"
              circle
              icon="el-icon-video-play"
              title="播放单词发音"
              @click="playQuestionAudio(currentQuestion)"
            />
          </div>
          <div v-else class="unsupported-question">
            <el-alert
              title="当前题型暂不支持，无法继续本次闯关"
              type="error"
              :closable="false"
              show-icon
            />
            <el-button type="primary" @click="backToStudy">返回学习中心</el-button>
          </div>

          <div v-if="currentCheckResult" class="check-result">
            <span v-if="currentCheckResult.loading" class="check-result-loading">
              <i class="el-icon-loading" /> 判题中...
            </span>
            <span v-else-if="currentCheckResult.correct" class="check-result-correct">
              <i class="el-icon-success" /> 回答正确
            </span>
            <span v-else-if="currentCheckResult.correct === false" class="check-result-error">
              <i class="el-icon-error" /> 回答错误，正确答案：{{ currentCheckResult.correctAnswer }}
            </span>
          </div>
        </div>

        <div v-if="!hasUnsupportedQuestion" class="challenge-actions">
          <el-button :disabled="currentIndex === 0" @click="goPrevious">上一题</el-button>
          <el-button
            v-if="currentIndex < questions.length - 1"
            type="primary"
            :disabled="isCurrentChecking"
            @click="goNext"
          >
            下一题
          </el-button>
          <el-button
            v-else
            type="success"
            :loading="submitting"
            :disabled="submitting || isCurrentChecking || hasUnsupportedQuestion || answeredCount !== questions.length"
            @click="submitChallenge"
          >
            提交答案
          </el-button>
        </div>
      </el-card>
    </template>

    <el-card v-else class="result-card" shadow="never">
      <el-result
        :icon="result.passed ? 'success' : 'warning'"
        :title="result.passed ? '闯关成功' : '继续加油'"
        :sub-title="resultSubtitle"
      >
        <template slot="extra">
          <el-button type="primary" @click="restart">再挑战一次</el-button>
          <el-button @click="backToStudy">继续学习</el-button>
          <el-button type="warning" plain @click="openWrongWords">查看错词本</el-button>
        </template>
      </el-result>

      <el-table v-if="resultRows.length" :data="resultRows" class="result-table">
        <el-table-column label="题目" min-width="240">
          <template slot-scope="scope">{{ scope.row.prompt }}</template>
        </el-table-column>
        <el-table-column label="结果" width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.correct ? 'success' : 'danger'">
              {{ scope.row.correct ? '正确' : '错误' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="正确答案" min-width="180">
          <template slot-scope="scope">{{ scope.row.correctAnswer || '-' }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import {
  checkArticleChallengeAnswer,
  getArticleChallenge,
  getArticleProgress,
  submitArticleChallenge
} from '@/api/eng/study'
import { play } from '@/utils/audio'

export default {
  name: 'EngStudyChallenge',
  data() {
    return {
      loading: false,
      submitting: false,
      challenge: {},
      progress: {},
      currentIndex: 0,
      answers: {},
      fillLetters: {},
      checkResults: {},
      checkRequestIds: {},
      checkSequence: 0,
      result: null
    }
  },
  computed: {
    articleId() {
      return this.$route.params.articleId
    },
    questions() {
      return Array.isArray(this.challenge.questions) ? this.challenge.questions : []
    },
    currentQuestion() {
      return this.questions[this.currentIndex]
    },
    currentFillLetters() {
      if (!this.currentQuestion || this.currentQuestion.type !== 'SENTENCE_FILL') return []
      return this.fillLetters[this.currentQuestion.questionId] || []
    },
    currentCheckResult() {
      if (!this.currentQuestion) return null
      return this.checkResults[this.currentQuestion.questionId] || null
    },
    isCurrentChecking() {
      return Boolean(this.currentCheckResult && this.currentCheckResult.loading)
    },
    canPlayCurrentAudio() {
      return this.currentQuestion &&
        this.currentQuestion.type === 'CN_TO_WORD' &&
        this.currentQuestion.audioUrl
    },
    hasUnsupportedQuestion() {
      return this.questions.some(question => !this.isSupportedQuestion(question))
    },
    answeredCount() {
      return this.questions.filter(question => this.isQuestionAnswered(question)).length
    },
    progressPercent() {
      if (!this.questions.length) return 0
      return Math.round((this.answeredCount / this.questions.length) * 100)
    },
    resultSubtitle() {
      return `本次得分 ${this.result.score || 0}，答对 ${this.result.correctCount || 0}/${this.result.totalCount || 0} 题`
    },
    resultRows() {
      const resultMap = (this.result.results || []).reduce((map, item) => {
        map[item.questionId] = item
        return map
      }, {})
      return this.questions.map(question => Object.assign({
        questionId: question.questionId,
        prompt: question.prompt
      }, resultMap[question.questionId] || {}))
    }
  },
  created() {
    this.loadChallenge()
  },
  watch: {
    currentQuestion(question) {
      // 首次加载、前后切题和重新挑战均通过当前题目变化统一触发自动发音。
      this.tryAutoPlay(question)
    }
  },
  methods: {
    loadChallenge() {
      this.loading = true
      Promise.all([
        getArticleChallenge(this.articleId),
        getArticleProgress(this.articleId)
      ]).then(([challengeResponse, progressResponse]) => {
        this.challenge = challengeResponse.data || {}
        this.progress = progressResponse.data || {}
        this.initializeFillAnswers()
      }).finally(() => {
        this.loading = false
      })
    },
    hasAnswer(answer) {
      return answer !== undefined && answer !== null && String(answer).trim() !== ''
    },
    isQuestionAnswered(question) {
      if (!question) return false
      if (question.type !== 'SENTENCE_FILL') {
        return this.hasAnswer(this.answers[question.questionId])
      }
      const letters = this.fillLetters[question.questionId] || []
      return letters.length > 0 && letters.every(letter => letter)
    },
    initializeFillAnswers() {
      this.questions.forEach(question => {
        if (question.type !== 'SENTENCE_FILL') return
        const answerLength = Number(question.answerLength)
        const letters = Number.isInteger(answerLength) && answerLength > 0
          ? Array(answerLength).fill('')
          : []
        this.$set(this.fillLetters, question.questionId, letters)
        this.$set(this.answers, question.questionId, '')
      })
    },
    optionValue(option) {
      return typeof option === 'object' ? (option.value !== undefined ? option.value : option.label) : option
    },
    optionLabel(option) {
      return typeof option === 'object' ? (option.label !== undefined ? option.label : option.value) : option
    },
    questionTypeLabel(type) {
      const labels = {
        WORD_TO_CN: '看词选中文',
        CN_TO_WORD: '看中文选英文',
        SENTENCE_CHOICE: '句子挖空选词',
        SENTENCE_FILL: '句子挖空填词'
      }
      return labels[type] || '英语闯关'
    },
    isChoiceQuestion(question) {
      return question && ['WORD_TO_CN', 'CN_TO_WORD', 'SENTENCE_CHOICE'].includes(question.type)
    },
    isSupportedQuestion(question) {
      return this.isChoiceQuestion(question) || (question && question.type === 'SENTENCE_FILL')
    },
    tryAutoPlay(question) {
      if (!question || question.type !== 'CN_TO_WORD' || !question.audioUrl) return
      this.$nextTick(() => {
        try {
          // 浏览器可能禁止无用户操作的播放；失败时保留重播入口，不影响继续答题。
          play(question.audioUrl, '', () => {})
        } catch (error) {
          // 音频资源或播放环境异常不应中断闯关流程。
        }
      })
    },
    playCurrentAudio() {
      if (!this.canPlayCurrentAudio) return
      this.playQuestionAudio(this.currentQuestion)
    },
    playQuestionAudio(question) {
      if (!question || !question.audioUrl) return
      try {
        play(question.audioUrl, '', () => {
          this.$modal.msgWarning('音频播放失败，请稍后重试')
        })
      } catch (error) {
        this.$modal.msgWarning('音频播放失败，请稍后重试')
      }
    },
    handleFillInput(letterIndex, event) {
      const question = this.currentQuestion
      if (!question || question.type !== 'SENTENCE_FILL') return

      const letters = this.fillLetters[question.questionId]
      if (!letters) return
      const value = String(event.target.value || '').replace(/[^a-zA-Z]/g, '').slice(-1)
      event.target.value = value
      this.$set(letters, letterIndex, value)
      this.$set(this.answers, question.questionId, letters.join(''))

      // 每次修改都使旧请求失效，只有全部字母填满后才发起新一轮判题。
      this.invalidateCheckResult(question.questionId)
      if (value && letterIndex < letters.length - 1) {
        this.focusFillInput(question.questionId, letterIndex + 1)
      }
      if (letters.length && letters.every(letter => letter)) {
        this.checkAnswer(question)
      }
    },
    handleFillBackspace(letterIndex, event) {
      if (event.target.value || letterIndex === 0 || !this.currentQuestion) return
      const questionId = this.currentQuestion.questionId
      const letters = this.fillLetters[questionId]
      if (!letters) return

      // 当前格为空时，一次退格直接清除前一格，并同步最终提交答案与即时反馈状态。
      event.preventDefault()
      this.$set(letters, letterIndex - 1, '')
      this.$set(this.answers, questionId, letters.join(''))
      this.invalidateCheckResult(questionId)
      this.focusFillInput(questionId, letterIndex - 1)
    },
    focusFillInput(questionId, letterIndex) {
      this.$nextTick(() => {
        const inputs = this.$refs['fillInputs-' + questionId]
        const input = Array.isArray(inputs) ? inputs[letterIndex] : inputs
        if (input) input.focus()
      })
    },
    invalidateCheckResult(questionId) {
      // 请求序号同步递增，确保答案修改后迟到的旧响应无法覆盖当前反馈。
      const requestId = ++this.checkSequence
      this.$set(this.checkRequestIds, questionId, requestId)
      this.$set(this.checkResults, questionId, null)
    },
    checkCurrentAnswer() {
      this.checkAnswer(this.currentQuestion)
    },
    checkAnswer(question) {
      if (!question) return
      const answer = this.answers[question.questionId]
      if (!this.hasAnswer(answer)) {
        this.invalidateCheckResult(question.questionId)
        return
      }

      const requestId = ++this.checkSequence
      this.$set(this.checkRequestIds, question.questionId, requestId)
      this.$set(this.checkResults, question.questionId, {
        loading: true,
        correct: null,
        correctAnswer: ''
      })
      checkArticleChallengeAnswer({
        articleId: this.articleId,
        questionId: question.questionId,
        answer
      }).then(response => {
        if (this.checkRequestIds[question.questionId] !== requestId) return
        const checkResult = response.data || {}
        this.$set(this.checkResults, question.questionId, {
          loading: false,
          correct: checkResult.correct,
          correctAnswer: checkResult.correctAnswer || ''
        })
      }).catch(() => {
        if (this.checkRequestIds[question.questionId] !== requestId) return
        this.$set(this.checkResults, question.questionId, null)
      })
    },
    goPrevious() {
      if (this.currentIndex > 0) this.currentIndex--
    },
    goNext() {
      // 未知题型没有合法作答方式，明确阻断前进，避免陷入要求答案但无法输入的状态。
      if (!this.isSupportedQuestion(this.currentQuestion)) {
        this.$modal.msgError('当前题型暂不支持，请返回学习中心')
        return
      }
      if (!this.isQuestionAnswered(this.currentQuestion)) {
        this.$modal.msgWarning('请先完成当前题目')
        return
      }
      if (this.isCurrentChecking) {
        this.$modal.msgWarning('正在判题，请稍候')
        return
      }
      if (this.currentIndex < this.questions.length - 1) this.currentIndex++
    },
    submitChallenge() {
      // 题目集合含未知类型时禁止提交，避免将不支持的题型错误地作为文本答案处理。
      if (this.submitting || this.isCurrentChecking || this.hasUnsupportedQuestion || this.answeredCount !== this.questions.length) return

      // 判题完全交给服务端，前端只提交题目编号与用户答案，避免泄露或推导正确答案。
      const data = {
        // 路由中的文章 ID 保持字符串传递，避免雪花 ID 转 Number 后精度丢失。
        articleId: this.articleId,
        answers: this.questions.map(question => ({
          questionId: question.questionId,
          answer: this.answers[question.questionId]
        }))
      }
      this.submitting = true
      submitArticleChallenge(data).then(response => {
        this.result = response.data || {}
        window.scrollTo(0, 0)
      }).finally(() => {
        this.submitting = false
      })
    },
    restart() {
      this.currentIndex = 0
      this.answers = {}
      this.fillLetters = {}
      this.checkResults = {}
      // 递增全局序号后再清理映射，使重启前仍在途的响应永久失效。
      this.checkSequence++
      this.checkRequestIds = {}
      this.result = null
      this.loadChallenge()
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

<style scoped lang="scss">
.challenge-page {
  max-width: 960px;
  margin: 0 auto;
  .challenge-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
  }
  .progress-text {
    display: flex;
    gap: 16px;
    color: #606266;
  }
  .question-box { min-height: 300px; padding: 32px 12px; }
  .unsupported-alert { margin-top: 20px; }
  .question-index { margin-bottom: 14px; color: #909399; }
  .question-type { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
  .question-prompt { margin-bottom: 28px; font-size: 20px; line-height: 1.6; }
  .unsupported-question .el-button { margin-top: 20px; }
  .option-list { display: flex; flex-direction: column; align-items: flex-start; }
  .option-list .el-radio { width: 100%; height: auto; margin: 0 0 12px; padding: 14px 20px; white-space: normal; }
  .fill-answer-row { display: flex; align-items: center; gap: 12px; }
  .fill-letter-list { display: flex; flex-wrap: wrap; gap: 8px; }
  .fill-letter-input {
    width: 32px;
    padding: 4px 2px;
    border: 0;
    border-bottom: 2px solid #c0c4cc;
    outline: none;
    color: #303133;
    font-size: 20px;
    text-align: center;
    text-transform: lowercase;
  }
  .fill-letter-input:focus { border-bottom-color: #409eff; }
  .fill-audio-button { flex: none; }
  .check-result { margin-top: 20px; font-size: 15px; }
  .check-result-loading { color: #909399; }
  .check-result-correct { color: #67c23a; }
  .check-result-error { color: #f56c6c; }
  .challenge-actions { display: flex; justify-content: flex-end; gap: 10px; }
  .result-table { margin-top: 20px; }
}
</style>
