<template>
  <div class="test-step2">
    <div class="question-meta">
      <el-tag size="small">{{ questionTypeLabel(currentQuestion.type) }}</el-tag>
      <span>{{ index + 1 }}/{{ questionList.length }}</span>
    </div>

    <section class="question">
      <div class="question-prompt">
        <span>{{ displayPrompt }}</span>
        <el-popover
          v-if="sentenceMeaning"
          placement="top"
          trigger="click"
          :content="sentenceMeaning"
        >
          <el-button
            slot="reference"
            class="sentence-meaning-button"
            type="text"
            icon="el-icon-question"
            aria-label="查看句子中文意思"
            title="查看句子中文意思"
          />
        </el-popover>
      </div>
      <el-button
        v-if="canPlayAudio"
        type="text"
        icon="el-icon-video-play"
        aria-label="重新发音"
        title="重新发音"
        @click="playCurrentAudio"
      />
    </section>

    <section v-if="isChoiceQuestion(currentQuestion)" class="answer">
      <ul>
        <li
          v-for="option in currentQuestion.options"
          :key="optionValue(option)"
          :class="['box-block', answerClass(optionValue(option))]"
          @click="chooseAnswer(optionValue(option))"
        >
          {{ optionLabel(option) }}
        </li>
      </ul>
    </section>

    <section v-else-if="currentQuestion.type === 'SENTENCE_FILL'" class="fill-answer">
      <div class="fill-letter-list">
        <button
          v-for="(letter, letterIndex) in currentFillLetters"
          :key="letterIndex"
          type="button"
          :class="['fill-letter-slot', { selected: letter }]"
          :aria-label="letter ? `撤回第 ${letterIndex + 1} 个字母 ${letter}` : `第 ${letterIndex + 1} 个字母待选择`"
          @click="removeFillLetter(letterIndex)"
        >
          {{ letter }}
        </button>
      </div>
      <div class="fill-option-list" aria-label="候选字母">
        <button
          v-for="(option, optionIndex) in currentQuestion.options"
          :key="optionIndex"
          type="button"
          :class="['fill-option-button', { selected: currentFillOptionIndexes.includes(optionIndex) }]"
          :disabled="isFillOptionDisabled(optionIndex)"
          :aria-label="`选择字母 ${optionLabel(option)}`"
          @click="chooseFillOption(optionIndex)"
        >
          {{ optionLabel(option) }}
        </button>
      </div>
    </section>

    <el-alert
      v-else
      title="当前题型暂不支持"
      type="error"
      :closable="false"
      show-icon
    />
  </div>
</template>

<script>
import { checkArticleChallengeAnswer } from '@/api/eng/study'
import { play, playAnswerFeedback, prepareAnswerFeedback } from '@/utils/audio'

const AUTO_ADVANCE_DELAY = 1000

export default {
  name: 'EngArticleTestQuestions',
  props: {
    articleId: {
      type: [String, Number],
      required: true
    },
    questionList: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      index: 0,
      answers: {},
      fillLetters: {},
      fillOptionIndexes: {},
      checkResults: {},
      checkRequestIds: {},
      checkSequence: 0,
      autoAdvanceTimer: null,
      completionEmitted: false
    }
  },
  computed: {
    currentQuestion() {
      return this.questionList[this.index] || {}
    },
    currentFillLetters() {
      return this.fillLetters[this.currentQuestion.questionId] || []
    },
    currentFillOptionIndexes() {
      return this.fillOptionIndexes[this.currentQuestion.questionId] || []
    },
    currentCheckResult() {
      return this.checkResults[this.currentQuestion.questionId] || null
    },
    /** 按题型精简操作说明，仅保留用户作答所需的单词、释义或句子。 */
    displayPrompt() {
      const prompt = String(this.currentQuestion.prompt || '')
      if (this.currentQuestion.type === 'WORD_TO_CN') {
        const match = prompt.match(/^请选择单词\s*[“"](.+)[”"]\s*的正确释义$/)
        return match ? match[1] : prompt
      }
      if (this.currentQuestion.type === 'CN_TO_WORD') {
        const match = prompt.match(/^请选择释义\s*[“"](.+)[”"]\s*对应的英文单词$/)
        return match ? match[1] : prompt
      }
      const sentencePrompt = prompt.split('；中文提示：')[0]
      if (this.currentQuestion.type !== 'SENTENCE_CHOICE') return sentencePrompt
      return sentencePrompt
        .replace(/^请选择句子中的空缺单词：\s*[“"]?/, '')
        .replace(/[”"]$/, '')
    },
    sentenceMeaning() {
      if (!['SENTENCE_CHOICE', 'SENTENCE_FILL'].includes(this.currentQuestion.type)) return ''
      const separator = '；中文提示：'
      const prompt = String(this.currentQuestion.prompt || '')
      const separatorIndex = prompt.indexOf(separator)
      return separatorIndex === -1 ? '' : prompt.slice(separatorIndex + separator.length).trim()
    },
    canPlayAudio() {
      // 看中文选英文题在作答前不提供发音提示，避免直接暴露答案。
      return this.currentQuestion.type !== 'CN_TO_WORD' &&
        Boolean(this.currentQuestion.audioUrl)
    }
  },
  created() {
    this.initializeAnswers()
    this.tryAutoPlay(this.currentQuestion)
  },
  watch: {
    currentQuestion(question) {
      this.tryAutoPlay(question)
    }
  },
  beforeDestroy() {
    this.completionEmitted = true
    this.clearAutoAdvance()
  },
  methods: {
    /** 为所有题目预建响应式答案，填词题同时记录各答案格占用的候选项索引。 */
    initializeAnswers() {
      this.questionList.forEach(question => {
        this.$set(this.answers, question.questionId, '')
        if (question.type !== 'SENTENCE_FILL') return
        const answerLength = Number(question.answerLength)
        const letters = Number.isInteger(answerLength) && answerLength > 0
          ? Array(answerLength).fill('')
          : []
        this.$set(this.fillLetters, question.questionId, letters)
        this.$set(this.fillOptionIndexes, question.questionId, Array(letters.length).fill(null))
      })
    },
    questionTypeLabel(type) {
      const labels = {
        WORD_TO_CN: '看词选中文',
        CN_TO_WORD: '看中文选英文',
        SENTENCE_CHOICE: '句子挖空选词',
        SENTENCE_FILL: '句子挖空填词'
      }
      return labels[type] || '英语测试'
    },
    isChoiceQuestion(question) {
      return question && ['WORD_TO_CN', 'CN_TO_WORD', 'SENTENCE_CHOICE'].includes(question.type)
    },
    optionValue(option) {
      return typeof option === 'object'
        ? (option.value !== undefined ? option.value : option.label)
        : option
    },
    optionLabel(option) {
      return typeof option === 'object'
        ? (option.label !== undefined ? option.label : option.value)
        : option
    },
    chooseAnswer(value) {
      if (!this.isChoiceQuestion(this.currentQuestion)) return
      prepareAnswerFeedback()
      this.$set(this.answers, this.currentQuestion.questionId, value)
      this.checkAnswer(this.currentQuestion)
    },
    answerClass(value) {
      if (!this.currentCheckResult || this.currentCheckResult.loading) return ''
      if (String(value) === String(this.currentCheckResult.correctAnswer)) return 'right'
      if (String(value) === String(this.answers[this.currentQuestion.questionId])) return 'wrong'
      return ''
    },
    hasAnswer(answer) {
      return answer !== undefined && answer !== null && String(answer).trim() !== ''
    },
    checkAnswer(question) {
      if (!question || this.completionEmitted) return
      this.clearAutoAdvance()
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
        if (this.completionEmitted || this.checkRequestIds[question.questionId] !== requestId) return
        const result = response.data || {}
        this.$set(this.checkResults, question.questionId, {
          loading: false,
          correct: result.correct,
          correctAnswer: result.correctAnswer || ''
        })
        if (typeof result.correct === 'boolean') {
          playAnswerFeedback(result.correct)
          this.playCorrectAnswerAudio(question)
          this.scheduleAutoAdvance(question.questionId, requestId)
        }
      }).catch(() => {
        if (this.checkRequestIds[question.questionId] !== requestId) return
        this.$set(this.checkResults, question.questionId, null)
      })
    },
    invalidateCheckResult(questionId) {
      this.clearAutoAdvance()
      this.$set(this.checkRequestIds, questionId, ++this.checkSequence)
      this.$set(this.checkResults, questionId, null)
    },
    chooseFillOption(optionIndex) {
      const question = this.currentQuestion
      const letters = this.fillLetters[question.questionId]
      const optionIndexes = this.fillOptionIndexes[question.questionId]
      const letterIndex = letters ? letters.indexOf('') : -1
      if (letterIndex === -1 || !optionIndexes || optionIndexes.includes(optionIndex)) return
      this.$set(letters, letterIndex, String(this.optionLabel(question.options[optionIndex]) || ''))
      this.$set(optionIndexes, letterIndex, optionIndex)
      this.updateFillAnswer(question)
      if (letters.length && letters.every(letter => letter)) {
        prepareAnswerFeedback()
        this.checkAnswer(question)
      }
    },
    removeFillLetter(letterIndex) {
      const question = this.currentQuestion
      const letters = this.fillLetters[question.questionId]
      const optionIndexes = this.fillOptionIndexes[question.questionId]
      if (!letters || !letters[letterIndex] || !optionIndexes) return
      this.$set(letters, letterIndex, '')
      this.$set(optionIndexes, letterIndex, null)
      this.updateFillAnswer(question)
    },
    updateFillAnswer(question) {
      const letters = this.fillLetters[question.questionId] || []
      this.$set(this.answers, question.questionId, letters.join(''))
      this.invalidateCheckResult(question.questionId)
    },
    isFillOptionDisabled(optionIndex) {
      return this.currentFillOptionIndexes.includes(optionIndex) || !this.currentFillLetters.includes('')
    },
    /** 判题反馈短暂停留后自动进入下一题，最后一题则提交整轮答案。 */
    scheduleAutoAdvance(questionId, requestId) {
      this.clearAutoAdvance()
      this.autoAdvanceTimer = setTimeout(() => {
        this.autoAdvanceTimer = null
        if (this.checkRequestIds[questionId] !== requestId || this.currentQuestion.questionId !== questionId) return
        this.advanceOrComplete()
      }, AUTO_ADVANCE_DELAY)
    },
    clearAutoAdvance() {
      if (this.autoAdvanceTimer === null) return
      clearTimeout(this.autoAdvanceTimer)
      this.autoAdvanceTimer = null
    },
    advanceOrComplete() {
      if (this.index < this.questionList.length - 1) {
        this.index++
        return
      }
      if (this.completionEmitted) return
      this.completionEmitted = true
      this.$emit('complete', this.questionList.map(question => ({
        questionId: question.questionId,
        answer: this.answers[question.questionId]
      })))
    },
    /** 看词选中文题进入时自动播放单词发音。 */
    tryAutoPlay(question) {
      if (!question || question.type !== 'WORD_TO_CN' || !question.audioUrl) return
      this.$nextTick(() => {
        try {
          play(question.audioUrl, '', () => {})
        } catch (error) {
          // 浏览器禁止自动播放或音频异常时保留手动发音入口。
        }
      })
    },
    /** 判题完成后播放目标单词发音，音频异常不得阻断自动切题。 */
    playCorrectAnswerAudio(question) {
      if (!question || !question.audioUrl) return
      try {
        play(question.audioUrl, '', () => {})
      } catch (error) {
        // 音频缺失、浏览器限制或播放异常时继续原有答题流程。
      }
    },
    playCurrentAudio() {
      if (!this.currentQuestion.audioUrl) return
      try {
        play(this.currentQuestion.audioUrl, '', () => {
          this.$modal.msgWarning('音频播放失败，请稍后重试')
        })
      } catch (error) {
        this.$modal.msgWarning('音频播放失败，请稍后重试')
      }
    }
  }
}
</script>

<style scoped lang="scss">
.test-step2 {
  display: flex;
  flex: 1;
  flex-direction: column;

  .question-meta {
    display: flex;
    flex: none;
    align-items: center;
    justify-content: space-between;
    color: #909399;
  }

  .question {
    display: flex;
    flex: 1;
    flex-direction: row;
    align-items: center;
    justify-content: center;
    gap: 6px;
    min-height: 80px;
    padding: 12px 0;
    text-align: center;
  }

  .question-prompt {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    min-width: 0;
    font-size: 18px;
    font-weight: 600;
    line-height: 1.6;
  }

  .question > .el-button {
    flex: none;
    padding: 0;
    font-size: 18px;
  }

  .sentence-meaning-button {
    padding: 0;
    font-size: 18px;
  }

  .answer {
    flex: none;

    ul {
      margin: 0;
      padding: 0;
    }

    li {
      list-style: none;
      cursor: pointer;
    }
  }

  .right {
    background: rgba(46, 204, 113, 0.25);
  }

  .wrong {
    background: rgba(231, 76, 60, 0.2);
  }

  .fill-answer,
  .fill-letter-list,
  .fill-option-list {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
  }

  .fill-answer {
    flex: none;
    flex-direction: column;
  }

  .fill-letter-list {
    flex-wrap: wrap;
  }

  .fill-letter-slot {
    width: 34px;
    height: 38px;
    padding: 4px 2px;
    border: 0;
    border-bottom: 2px solid #909399;
    outline: none;
    color: #303133;
    background: transparent;
    font-size: 20px;
    line-height: 28px;
    text-align: center;
    text-transform: lowercase;
    cursor: default;
  }

  .fill-letter-slot.selected {
    border-bottom-color: #409eff;
    color: #409eff;
    cursor: pointer;
  }

  .fill-option-list {
    display: grid;
    grid-template-columns: repeat(5, 36px);
  }

  .fill-option-button {
    width: 36px;
    height: 36px;
    padding: 0;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    color: #303133;
    background: #fff;
    font-size: 18px;
    text-transform: lowercase;
    cursor: pointer;
  }

  .fill-option-button:hover:not(:disabled) {
    border-color: #409eff;
    color: #409eff;
  }

  .fill-option-button:disabled {
    border-color: #ebeef5;
    color: #c0c4cc;
    background: #f5f7fa;
    cursor: not-allowed;
  }

  .fill-option-button.selected:disabled {
    border-color: #a0cfff;
    color: #409eff;
    background: #ecf5ff;
  }

  @media (max-width: 480px) {
    .fill-option-list {
      gap: 8px;
    }
  }
}
</style>
