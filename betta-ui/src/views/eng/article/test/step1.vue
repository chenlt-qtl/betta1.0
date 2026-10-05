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
        <button class="block-button" @click="startReview">
          {{ wordList.length ? '开始学习' : '直接测试' }}
        </button>
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
    }
  },
  data() {
    return {
      index: 0,
      reviewing: false
    }
  },
  computed: {
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

  .toolbar { flex: none; }

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
</style>
