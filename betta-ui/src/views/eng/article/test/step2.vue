<template>
  <div class="test-step2">
    <span class="tip">{{ tip }}</span>
    <section class="question">
      <section :style="{ fontSize: question.type ? '16px' : '26px' }">
        {{ question.question }}
      </section>
      <section v-if="question.sentenceAcceptation">
        {{ this.question.sentenceAcceptation }}
      </section>
      <span v-if="!question.type" class="ph">
        / {{ this.question.word.phonetics }} /
        <svg-icon
          @click="() => play(this.question.word.phMp3)"
          icon-class="sound"
        />
      </span>
    </section>
    <section class="answer">
      <ul>
        <li
          :class="'box-block ' + getAnswerClass(index)"
          v-for="(answer, index) in this.question.answers"
          :key="answer"
          @click="() => choseAnswer(index)"
        >
          {{ answer }}
        </li>
      </ul>
    </section>
  </div>
</template>
<script>
import { play } from "@/utils/audio";

export default {
  props: ["wordList", "result", "questionList"],
  data() {
    return {
      index: 0,
      userAnswer: -1,
      question: {},
      isStart: false,
      tip: "",
    };
  },
  created() {
    this.question = this.questionList[this.index];
  },
  watch: {
    question() {
      if (this.question.type == 0 && this.question.word.phMp3) {
        play(this.question.word.phMp3);
      }
      this.tip =
        this.index + 1 + "/" + this.questionList.length;
    },
  },
  methods: {
    getAnswerClass(index) {
      if (this.userAnswer != -1) {
        if (this.userAnswer == index) {
          if (this.question.answerKey == index) {
            return "right";
          } else {
            return "wrong";
          }
        } else {
          if (this.question.answerKey == index) {
            return "right";
          }
        }
      }
      return "";
    },
    play(url) {
      play(url);
    },
    choseAnswer(i) {
      if (this.userAnswer != -1) {
        return;
      }
      this.userAnswer = i;
      play(this.question.word.phMp3);
      //答对了
      if (i == this.question.answerKey) {
        this.question.word.familiarity =
          (this.question.word.familiarity || 0) + 1;
        this.result.right = this.result.right + 1;
      } else {
        //答错了
        this.question.word.familiarity =
          (this.question.word.familiarity || 0) - 1;
        this.result.wrong = this.result.wrong + 1;
      }
      setTimeout(() => {
        if (this.index < this.questionList.length - 1) {
          this.index++;
          this.userAnswer = -1;
          this.question = this.questionList[this.index];
        } else {
          this.$emit("next");
        }
      }, 1000);
    },
  },
};
</script>
<style scoped lang="scss">
.test-step2 {
  display: flex;
  flex-direction: column;
  height: 100%;
  .question {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    font-weight: 600;
    gap: 5px;
    overflow: hidden;
  }
  .answer {
    ul {
      padding: 0;
    }
    li {
      list-style: none;
    }
  }
  .right {
    background: rgba(46, 204, 113, 0.2);
  }
  .wrong {
    background: rgba(231, 76, 60, 0.15);
  }
}
</style>
