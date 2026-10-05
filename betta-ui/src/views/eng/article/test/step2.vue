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

    <section v-if="!isSpelling && currentQuestion.type === 'PRONUNCIATION'" class="pronunciation-answer">
      <p class="pronunciation-tip">请先听标准发音，再清晰朗读单词。</p>

      <div v-if="currentPronunciationState.status === 'recording'" class="recording-status" role="status">
        <span class="recording-dot" />
        正在录音 {{ recordingSeconds }} 秒（最长 5 秒）
      </div>
      <div v-else-if="currentPronunciationState.status === 'assessing'" class="assessing-status" role="status">
        <i class="el-icon-loading" /> 正在评分，请稍候…
      </div>

      <el-alert
        v-if="currentPronunciationState.error"
        :title="currentPronunciationState.error"
        type="error"
        :closable="false"
        show-icon
      />

      <div v-if="currentPronunciationResult" class="pronunciation-result" aria-live="polite">
        <div class="pronunciation-score">
          <strong>{{ scoreText(currentPronunciationResult.score) }}</strong>
          <span>跟读得分</span>
          <el-tag :type="currentPronunciationResult.passed ? 'success' : 'warning'" size="small">
            {{ currentPronunciationResult.passed ? '发音合格' : '继续加油' }}
          </el-tag>
        </div>
        <div class="score-details">
          <span>准确度 {{ scoreText(currentPronunciationResult.accuracy) }}</span>
          <span>流利度 {{ scoreText(currentPronunciationResult.fluency) }}</span>
          <span>完整度 {{ scoreText(currentPronunciationResult.completeness) }}</span>
        </div>
        <div v-if="currentPronunciationPhones.length" class="phone-feedback">
          <span
            v-for="(phone, phoneIndex) in currentPronunciationPhones"
            :key="phoneIndex"
            :class="{ 'phone-feedback--weak': phoneScore(phone) < 60 }"
          >
            {{ phoneLabel(phone) }} {{ scoreText(phoneScore(phone)) }}
          </span>
        </div>
      </div>

      <p class="pronunciation-attempts">
        剩余 {{ currentRemainingAttempts }}/3 次
      </p>

      <div class="pronunciation-actions">
        <el-button
          v-if="currentPronunciationState.status !== 'recording' && currentRemainingAttempts > 0"
          type="primary"
          round
          :disabled="pronunciationBusy"
          icon="el-icon-microphone"
          @click="startPronunciationRecording"
        >
          {{ currentPronunciationResult ? '重新录制' : '开始录音' }}
        </el-button>
        <el-button
          v-if="currentPronunciationState.status === 'recording'"
          type="danger"
          round
          icon="el-icon-video-pause"
          @click="finishPronunciationRecording"
        >
          停止并评分
        </el-button>
        <el-button
          v-if="currentRecordingUrl"
          class="own-recording-button"
          type="primary"
          circle
          :disabled="pronunciationBusy"
          aria-label="播放我的发音"
          title="播放我的发音"
          @click="playOwnPronunciation"
        >
          <svg-icon icon-class="sound" />
        </el-button>
        <el-button
          v-if="currentPronunciationResult"
          type="success"
          round
          :disabled="pronunciationBusy"
          @click="advanceOrComplete"
        >
          {{ index < questionList.length - 1 ? '下一题' : '完成测试' }}
        </el-button>
      </div>
    </section>

    <section v-else-if="isChoiceQuestion(currentQuestion)" class="answer">
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
import { assessChallengePronunciation, checkArticleChallengeAnswer } from '@/api/eng/study'
import { play, playAnswerFeedback, prepareAnswerFeedback } from '@/utils/audio'
import PcmRecorder, { MAX_DURATION_MS } from '@/utils/pcmRecorder'

const AUTO_ADVANCE_DELAY = 1000

export default {
  name: 'EngArticleTestQuestions',
  props: {
    attemptId: {
      type: [String, Number],
      required: true
    },
    mode: {
      type: String,
      required: true
    },
    articleId: {
      type: [String, Number],
      default: null
    },
    levelNo: {
      type: [String, Number],
      default: null
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
      completionEmitted: false,
      pronunciationStates: {},
      pronunciationResults: {},
      pronunciationAttempts: {},
      pronunciationRecordingUrls: {},
      recorder: null,
      recordingPlayer: null,
      recordingElapsedMs: 0,
      recordingTimer: null
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
    currentPronunciationState() {
      return this.pronunciationStates[this.currentQuestion.questionId] || { status: 'idle', error: '' }
    },
    currentPronunciationResult() {
      return this.pronunciationResults[this.currentQuestion.questionId] || null
    },
    currentPronunciationPhones() {
      const result = this.currentPronunciationResult
      return result && Array.isArray(result.phones) ? result.phones : []
    },
    currentPronunciationAttempt() {
      return this.pronunciationAttempts[this.currentQuestion.questionId] || {
        attemptCount: 0,
        remainingAttempts: 3
      }
    },
    currentRemainingAttempts() {
      const remainingAttempts = Number(this.currentPronunciationAttempt.remainingAttempts)
      return Number.isInteger(remainingAttempts)
        ? Math.min(3, Math.max(0, remainingAttempts))
        : 3
    },
    currentRecordingUrl() {
      return this.pronunciationRecordingUrls[this.currentQuestion.questionId] || ''
    },
    pronunciationBusy() {
      return ['preparing', 'recording', 'stopping', 'assessing'].includes(this.currentPronunciationState.status)
    },
    recordingSeconds() {
      return (this.recordingElapsedMs / 1000).toFixed(1)
    },
    isSpelling() {
      return String(this.mode || '').toUpperCase() === 'SPELLING'
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
    this.clearRecordingTimer()
    if (this.recorder) this.recorder.cancel()
    this.releaseRecordingPlayer()
    Object.keys(this.pronunciationRecordingUrls).forEach(questionId => {
      URL.revokeObjectURL(this.pronunciationRecordingUrls[questionId])
    })
  },
  methods: {
    /** 为所有题目预建响应式答案，填词题同时记录各答案格占用的候选项索引。 */
    initializeAnswers() {
      this.questionList.forEach(question => {
        this.$set(this.answers, question.questionId, '')
        if (question.type === 'PRONUNCIATION') {
          this.$set(this.pronunciationStates, question.questionId, { status: 'idle', error: '' })
          this.$set(this.pronunciationAttempts, question.questionId, {
            attemptCount: 0,
            remainingAttempts: 3
          })
        }
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
        SENTENCE_FILL: this.isSpelling ? '随机挖空拼写' : '句子挖空填词',
        PRONUNCIATION: '单词跟读'
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
        attemptId: this.attemptId,
        mode: this.mode,
        articleId: this.articleId,
        levelNo: this.levelNo,
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
    setPronunciationState(questionId, state) {
      this.$set(this.pronunciationStates, questionId, Object.assign({ status: 'idle', error: '' }, state))
    },
    async startPronunciationRecording() {
      const question = this.currentQuestion
      if (!question || question.type !== 'PRONUNCIATION' || this.pronunciationBusy || this.currentRemainingAttempts <= 0) return
      const questionId = question.questionId
      this.clearAutoAdvance()
      // 开始采集前停止回听，避免扬声器声音被麦克风再次录入。
      this.releaseRecordingPlayer()
      this.$set(this.answers, questionId, '')
      this.$delete(this.pronunciationResults, questionId)
      this.setPronunciationState(questionId, { status: 'preparing', error: '' })
      const recorder = new PcmRecorder({
        maxDurationMs: MAX_DURATION_MS,
        onAutoStop: () => this.finishPronunciationRecording()
      })
      this.recorder = recorder
      try {
        await recorder.start()
        if (this.completionEmitted || this.recorder !== recorder || this.currentQuestion.questionId !== questionId) {
          await recorder.cancel()
          return
        }
        this.recordingElapsedMs = 0
        this.recordingTimer = setInterval(() => {
          this.recordingElapsedMs = Math.min(MAX_DURATION_MS, this.recordingElapsedMs + 100)
        }, 100)
        this.setPronunciationState(questionId, { status: 'recording', error: '' })
      } catch (error) {
        if (this.recorder === recorder) this.recorder = null
        this.setPronunciationState(questionId, { status: 'error', error: this.recordingErrorMessage(error) })
      }
    },
    async finishPronunciationRecording() {
      const question = this.currentQuestion
      const recorder = this.recorder
      if (!question || question.type !== 'PRONUNCIATION' || !recorder || !recorder.recording) return
      const questionId = question.questionId
      this.setPronunciationState(questionId, { status: 'stopping', error: '' })
      this.clearRecordingTimer()
      try {
        const recording = await recorder.stop()
        if (this.recorder === recorder) this.recorder = null
        this.replaceRecordingUrl(questionId, recording.blob)
        await this.assessPronunciation(question, recording.blob)
      } catch (error) {
        if (this.recorder === recorder) this.recorder = null
        this.setPronunciationState(questionId, { status: 'error', error: this.recordingErrorMessage(error) })
      }
    },
    async assessPronunciation(question, audio) {
      const questionId = question.questionId
      this.setPronunciationState(questionId, { status: 'assessing', error: '' })
      const data = new FormData()
      data.append('attemptId', this.attemptId)
      data.append('mode', this.mode)
      if (this.articleId != null) data.append('articleId', this.articleId)
      if (this.levelNo != null) data.append('levelNo', this.levelNo)
      data.append('questionId', questionId)
      data.append('audio', audio, 'pronunciation.wav')
      try {
        const response = await assessChallengePronunciation(data)
        if (this.completionEmitted || this.currentQuestion.questionId !== questionId) return
        const result = response.data || {}
        if (String(result.questionId) !== String(questionId) || typeof result.passed !== 'boolean') {
          throw new Error('评分结果无效，请重新录制')
        }
        this.updatePronunciationAttempts(questionId, result)
        this.$set(this.pronunciationResults, questionId, result)
        // 客户端只提交已评测标记，最终分数由服务端缓存的可信结果决定。
        this.$set(this.answers, questionId, 'ASSESSED')
        this.setPronunciationState(questionId, { status: 'success', error: '' })
      } catch (error) {
        this.$set(this.answers, questionId, '')
        this.setPronunciationState(questionId, { status: 'error', error: this.assessmentErrorMessage(error) })
      }
    },
    clearRecordingTimer() {
      if (this.recordingTimer !== null) clearInterval(this.recordingTimer)
      this.recordingTimer = null
    },
    /** 使用服务端返回的权威次数，避免前端计数与并发请求结果不一致。 */
    updatePronunciationAttempts(questionId, result) {
      const attemptCount = Number(result.attemptCount)
      const remainingAttempts = Number(result.remainingAttempts)
      if (!Number.isInteger(attemptCount) || !Number.isInteger(remainingAttempts)) return
      this.$set(this.pronunciationAttempts, questionId, {
        attemptCount: Math.min(3, Math.max(0, attemptCount)),
        remainingAttempts: Math.min(3, Math.max(0, remainingAttempts))
      })
    },
    /** 每道题仅保留最新录音，并及时释放旧的本地音频地址。 */
    replaceRecordingUrl(questionId, audioBlob) {
      const previousUrl = this.pronunciationRecordingUrls[questionId]
      if (previousUrl) {
        this.releaseRecordingPlayer()
        URL.revokeObjectURL(previousUrl)
      }
      this.$set(this.pronunciationRecordingUrls, questionId, URL.createObjectURL(audioBlob))
    },
    playOwnPronunciation() {
      const recordingUrl = this.currentRecordingUrl
      if (!recordingUrl) return
      this.releaseRecordingPlayer()
      const player = new Audio(recordingUrl)
      this.recordingPlayer = player
      player.onended = () => {
        if (this.recordingPlayer === player) this.releaseRecordingPlayer()
      }
      player.play().catch(() => {
        if (this.recordingPlayer !== player) return
        this.$modal.msgWarning('录音播放失败，请重新录制')
        this.releaseRecordingPlayer()
      })
    },
    releaseRecordingPlayer() {
      if (!this.recordingPlayer) return
      this.recordingPlayer.onended = null
      this.recordingPlayer.pause()
      this.recordingPlayer.removeAttribute('src')
      this.recordingPlayer.load()
      this.recordingPlayer = null
    },
    recordingErrorMessage(error) {
      if (error && error.message) return error.message
      return '录音失败，请检查麦克风后重试'
    },
    assessmentErrorMessage(error) {
      const responseMessage = error && error.response && error.response.data && error.response.data.msg
      return responseMessage || (error && error.message) || '发音评分失败，请重新录制'
    },
    scoreText(value) {
      const score = Number(value)
      return Number.isFinite(score) ? Math.round(score) : '--'
    },
    phoneLabel(phone) {
      if (!phone || typeof phone !== 'object') return String(phone || '')
      return phone.phone || phone.phoneme || phone.symbol || phone.name || '音素'
    },
    phoneScore(phone) {
      if (!phone || typeof phone !== 'object') return 0
      const score = Number(phone.score != null ? phone.score : phone.accuracy)
      return Number.isFinite(score) ? score : 0
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
    /** 看词选中文题及跟读题进入时自动播放一次标准发音。 */
    tryAutoPlay(question) {
      if (!question || !['WORD_TO_CN', 'PRONUNCIATION'].includes(question.type) || !question.audioUrl) return
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

  .pronunciation-answer {
    display: flex;
    flex: none;
    flex-direction: column;
    align-items: stretch;
    gap: 14px;
    width: 100%;
  }

  .pronunciation-tip {
    margin: 0;
    color: #606266;
    font-size: 13px;
    line-height: 1.6;
    text-align: center;
  }

  .pronunciation-attempts {
    margin: 0;
    color: #909399;
    font-size: 13px;
    text-align: center;
  }

  .recording-status,
  .assessing-status {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    min-height: 32px;
    color: #606266;
  }

  .recording-status { color: #f56c6c; }

  .recording-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: #f56c6c;
    animation: recording-pulse 1s infinite;
  }

  .pronunciation-result {
    padding: 14px;
    border: 1px solid rgba(220, 223, 230, .8);
    border-radius: 12px;
    background: rgba(255, 255, 255, .65);
  }

  .pronunciation-score {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: center;
    gap: 8px;
  }

  .pronunciation-score strong {
    color: #409eff;
    font-size: 30px;
  }

  .pronunciation-score > span { color: #606266; }

  .score-details,
  .phone-feedback,
  .pronunciation-actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: center;
    gap: 10px;
  }

  .score-details {
    margin-top: 10px;
    color: #606266;
    font-size: 13px;
  }

  .phone-feedback { margin-top: 10px; }

  .phone-feedback span {
    padding: 3px 8px;
    border-radius: 12px;
    color: #67c23a;
    background: #f0f9eb;
    font-size: 12px;
  }

  .phone-feedback .phone-feedback--weak {
    color: #e6a23c;
    background: #fdf6ec;
  }

  .own-recording-button {
    flex: 0 0 auto;
    font-size: 18px;
  }

  @keyframes recording-pulse {
    0%, 100% { opacity: 1; transform: scale(1); }
    50% { opacity: .45; transform: scale(.8); }
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
    .question { min-height: 64px; }

    .question-prompt {
      font-size: 17px;
      overflow-wrap: anywhere;
    }

    .pronunciation-actions .el-button {
      flex: 1 1 130px;
      min-width: 0;
      margin-left: 0;
    }

    .pronunciation-actions .own-recording-button {
      flex: 0 0 40px;
    }

    .score-details { gap: 6px 12px; }

    .fill-option-list {
      gap: 8px;
    }
  }
}
</style>
