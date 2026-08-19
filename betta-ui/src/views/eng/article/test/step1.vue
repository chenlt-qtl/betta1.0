<template>
  <div class="test-step1">
    <div v-if="!isStart" class="word-table">
      <el-table :data="wordList" :show-header="false" class="table">
        <el-table-column label="单词" prop="wordName">
          <template v-if="scope.row.phMp3" slot-scope="scope">
            <div class="word-container">
              <span class="word"> {{ scope.row.wordName }}</span>
              <span> / {{ scope.row.phonetics}} /</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="toolbar">
        <button class="block-button" @click="start">开始</button>
      </div>
    </div>
    <div v-if="isStart" class="word-detail">
      <section class="wordName">
        {{ word.wordName }}
        <el-button
          class="soundBtn"
          type="text"
          @click="() => play(word.phMp3)"
          ><svg-icon icon-class="sound" />
        </el-button>
      </section>
      <div class="ph" v-if="word.phonetics">/ {{ word.phonetics }} /</div>
      <ul class="acceptations">
        <li v-for="text in acceptations" :key="text">
          {{ text }}
        </li>
      </ul>
      <div class="toolbar">
        <button class="block-button" style="width: 52px" @click="nextStep">
          <i class="el-icon-right"></i>
        </button>
      </div>
    </div>
  </div>
</template>
<script>
import { play } from "@/utils/audio";

export default {
  props: ["wordList", "questionList"],
  data() {
    return {
      index: 0,
      word: {},
      isStart: false,
    };
  },
  computed: {
    acceptations() {
      if (this.word && this.word.wordName) {
        if (this.word.exchange) {
          return [this.word.exchange];
        } else {
          return (this.word.acceptation || "").split("|").filter(Boolean);
        }
      }
      return [];
    },
  },
  watch: {
    word() {
      if (this.word && this.word.phMp3) {
        play(this.word.phMp3);
      }
    },
  },
  methods: {
    changeIndex(value) {
      this.index = this.index + value;
      this.word = this.wordList[this.index];
    },
    play(url) {
      play(url);
    },
    start() {
      this.isStart = true;
      this.word = this.wordList[this.index];
    },
    nextStep() {
      if (this.index < this.wordList.length - 1) {
        this.changeIndex(1);
      } else {
        this.$emit("next");
      }
    },
  },
};
</script>
<style lang="scss">
.test-step1 {
  height: 100%;
  .word-table {
    display: flex;
    flex-direction: column;
    height: 100%;
    .table {
      flex: 1;
    }
    .word-container {
      color: #333;
      .word {
        font-weight: 600;
        font-size: 1.2rem;
      }
    }
    .el-table {
      background-color: transparent;
      tr {
        background-color: transparent;
      }
      td.el-table__cell {
        border-color: #fff;
      }
    }
  }
  .word-detail {
    height: 100%;
    display: flex;
    gap: 10px;
    flex-direction: column;
    .wordName {
      display: flex;
      align-items: center;
      //发音按钮
      .soundBtn {
        margin-left: 25px;
        font-size: 1.5rem;
        color: #333;
      }
    }
    .acceptations {
      flex: 1;
      padding: 0;
      list-style: none;
    }
  }
  .toolbar {
    text-align: center;
    padding: 10px 0;
  }
}
</style>
