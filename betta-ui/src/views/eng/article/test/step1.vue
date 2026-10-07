<template>
  <div class="test-step1">
    <div v-if="!reviewing" class="word-table">
      <el-table :data="wordList" :show-header="false" class="table">
        <el-table-column label="单词" prop="wordName">
          <template slot-scope="scope">
            <div class="word-container">
              <span class="word">{{ scope.row.wordName }}</span>
              <span v-if="scope.row.phonetics"> / {{ scope.row.phonetics }} /</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="toolbar">
        <button class="block-button" @click="$emit('back')">返回地图</button>
        <button v-if="hasNewWords" class="block-button primary-action" @click="startReview">开始学习</button>
        <template v-else>
          <button class="block-button primary-action" @click="$emit('test')">开始测试</button>
          <el-tooltip :content="pronunciationDisabledReason" placement="top" :disabled="pronunciationAvailable">
            <span class="button-wrapper">
              <button class="block-button" :disabled="!pronunciationAvailable" @click="$emit('pronunciation')">跟读测试</button>
            </span>
          </el-tooltip>
          <el-tooltip content="本关没有不少于 4 个字符的纯英文单词" placement="top" :disabled="spellingAvailable">
            <span class="button-wrapper">
              <button class="block-button" :disabled="!spellingAvailable" @click="$emit('spelling')">拼写测试</button>
            </span>
          </el-tooltip>
        </template>
      </div>
    </div>

    <div v-else class="word-detail">
      <div class="review-progress">{{ index + 1 }}/{{ wordList.length }}</div>
      <section class="word-name">
        {{ currentWord.wordName }}
        <el-button
          v-if="currentWord.phMp3"
          class="sound-button"
          type="text"
          aria-label="播放单词发音"
          @click="playWord"
        >
          <svg-icon icon-class="sound" />
        </el-button>
      </section>
      <div v-if="currentWord.phonetics" class="phonetics">
        / {{ currentWord.phonetics }} /
      </div>
      <ul class="acceptations">
        <li v-for="text in acceptations" :key="text">{{ text }}</li>
        <li v-if="acceptations.length === 0" class="empty-acceptation">暂无释义</li>
      </ul>
      <div class="toolbar">
        <button
          class="block-button icon-button"
          :disabled="index === 0"
          aria-label="上一个单词"
          @click="changeIndex(-1)"
        >
          <i class="el-icon-back" />
        </button>
        <button class="block-button" @click="next">
          {{ index < wordList.length - 1 ? '下一个' : '开始测试' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { play } from '@/utils/audio'

export default {
  name: 'EngArticleTestPreview',
  props: {
    wordList: {
      type: Array,
      default: () => []
    },
    newWordCount: {
      type: Number,
      default: 0
    },
    pronunciationEnabled: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      index: 0,
      reviewing: false
    }
  },
  computed: {
    hasNewWords() {
      return this.newWordCount > 0
    },
    httpsAccess() {
      return typeof window !== 'undefined' && window.location && window.location.protocol === 'https:'
    },
    pronunciationAvailable() {
      return this.httpsAccess && this.pronunciationEnabled
    },
    pronunciationDisabledReason() {
      return this.httpsAccess
        ? '跟读评分服务未启用或配置不完整'
        : '当前使用 HTTP 访问，请改用 HTTPS 后进行跟读测试'
    },
    spellingAvailable() {
      return this.wordList.some(word => /^[A-Za-z]{4,}$/.test(String(word.wordName || '')))
    },
    currentWord() {
      return this.wordList[this.index] || {}
    },
    acceptations() {
      const text = this.currentWord.exchange || this.currentWord.acceptation || ''
      return text.split('|').map(item => item.trim()).filter(Boolean)
    }
  },
  methods: {
    startReview() {
      if (!this.wordList.length) {
        this.$emit('next')
        return
      }
      this.reviewing = true
      this.$nextTick(this.playWord)
    },
    changeIndex(offset) {
      const targetIndex = this.index + offset
      if (targetIndex < 0 || targetIndex >= this.wordList.length) return
      this.index = targetIndex
      this.$nextTick(this.playWord)
    },
    next() {
      if (this.index < this.wordList.length - 1) {
        this.changeIndex(1)
        return
      }
      this.$emit('next')
    },
    playWord() {
      if (this.currentWord.phMp3) play(this.currentWord.phMp3)
    }
  }
}
</script>

<style scoped lang="scss">
.test-step1 {
  flex: 1 1 0;
  min-height: 0;

  .word-table,
  .word-detail {
    display: flex;
    flex-direction: column;
    height: 100%;
    min-height: 0;
  }

  .table,
  .acceptations {
    flex: 1;
    min-height: 0;
  }

  .table { overflow-y: auto; }

  .toolbar {
    flex: none;
    flex-wrap: wrap;
  }

  .button-wrapper {
    display: inline-flex;
  }

  .primary-action {
    color: #fff;
    border-color: #67c23a;
    background: #67c23a;
  }

  .word-container {
    color: #333;
  }

  .word {
    font-size: 18px;
    font-weight: 600;
  }

  .el-table {
    background-color: transparent;

    tr {
      background-color: transparent;
    }

    td.el-table__cell {
      border-color: rgba(255, 255, 255, 0.8);
    }
  }

  .review-progress {
    color: #909399;
    text-align: right;
  }

  .word-name {
    display: flex;
    align-items: center;
    margin-top: 28px;
    font-size: 32px;
    font-weight: 600;
  }

  .sound-button {
    margin-left: 20px;
    color: #333;
    font-size: 24px;
  }

  .phonetics {
    margin-top: 12px;
    font-size: 18px;
  }

  .acceptations {
    padding: 0;
    overflow-y: auto;
    list-style: none;
    font-size: 16px;
    line-height: 1.8;
  }

  .empty-acceptation {
    color: #909399;
  }

  .icon-button {
    min-width: 52px;
  }
}

@media (max-width: 600px) {
  .test-step1 .toolbar {
    flex-wrap: wrap;
  }

  .test-step1 .toolbar .block-button,
  .test-step1 .button-wrapper {
    flex: 1 1 calc(50% - 12px);
    min-width: 0;
  }

  .test-step1 .button-wrapper .block-button {
    width: 100%;
  }
}
</style>
