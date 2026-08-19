<template>
  <div class="test-article" v-loading="loading">
    <section class="test-container">
      <step1
        v-if="step == 1"
        :wordList="wordList"
        :questionList="questionList"
        @next="toStep2"
      ></step1>
      <step2
        v-if="step == 2"
        :wordList="wordList"
        :questionList="questionList"
        :result="result"
        @next="toStep3"
      ></step2>
      <step3
        v-if="step == 3"
        :wordList="wordList"
        :result="result"
        :questionList="questionList"
        @next="newLoop"
      ></step3>
    </section>
  </div>
</template>
<script>
import { listByArticle, batchUpdate } from "@/api/eng/score";
import { play } from "@/utils/audio";
import step1 from "./step1.vue";
import step2 from "./step2.vue";
import step3 from "./step3.vue";
import { getQuestions } from "./utils";

export default {
  components: { step1, step2, step3 },
  data() {
    return {
      // 遮罩层
      loading: true,
      wordList: [],
      questionList: [],
      result: { right: 0, wrong: 0 },
      step: 0,
    };
  },
  created() {
    this.getWords();
  },
  methods: {
    newLoop() {
      this.getWords();
    },
    /** 查询单词列表 */
    getWords() {
      const articleId = this.$route.params && this.$route.params.articleId;
      if (!articleId) {
        this.$message.error(`请指定文章ID`);
        this.loading = false;
        return;
      }
      this.loading = true;
      listByArticle(articleId,true).then((response) => {
        this.wordList = (response.rows || []).filter((w) => w.wordName);
        if (this.wordList.length < 5) {
          this.$message.error(`单词数小于5个，不能测试`);
          this.$router.push("/eng/manage/article");
          this.loading = false;
          return;
        } else {
          this.questionList = getQuestions(this.wordList);
        }
        this.start();
        this.loading = false;
      });
    },
    start() {
      this.step = 1;
      this.result = { right: 0, wrong: 0 };
    },
    toStep2() {
      this.wordList.forEach((word) => {
        word.familiarity = (word.familiarity || 0) + 1;
      });
      this.step = 2;
    },
    toStep3() {
      this.loading = true;
      //保存结果数据
      batchUpdate(this.wordList).then((res) => {
        this.step = 3;
        this.loading = false;
      });
    },
    play(url, time) {
      play(url, time);
    },
    acceptationFormatter(row) {
      const acceptation = row.acceptation || "";
      const strs = acceptation.split("|");
      return strs[0] + (strs.length > 1 ? "..." : "");
    },
  },
};
</script>
<style lang='scss'>
.test-article{
  padding: 24px;
  flex: 1;
  /* 目标项目没有参考图片资源，使用项目内可直接渲染的渐变保持测试区域层次。 */
  background: linear-gradient(135deg, #e8f3ff 0%, #f4ecff 55%, #fff6e5 100%);
  height: calc(100vh - 84px);
  .test-container {
    color: #333;
    background: rgba(255, 255, 255, 0.1);
    width: 400px;
    height: calc(100% - 80px);
    padding: 20px;
    margin: 40px auto;
    border-radius: 10px;
    backdrop-filter: blur(18px);
    box-shadow: 0 25px 45px rgba(0, 0, 0, 0.1);
    border: 1px solid rgba(255, 255, 255, 0.3);
    border-right: 1px solid rgba(255, 255, 255, 0.2);
    border-bottom: 1px solid rgba(255, 255, 255, 0.2);
    position: relative;
    .tip {
      top: 24px;
      right: 24px;
      position: absolute;
    }
  }

  .box-block {
    width: 100%;
    background: rgba(255, 255, 255, 0.3);
    padding: 15px 20px;
    margin-top: 20px;
    border-radius: 35px;
    border: 1px solid rgba(255, 255, 255, 0.3);
    border-right: 1px solid rgba(255, 255, 255, 0.2);
    border-bottom: 1px solid rgba(255, 255, 255, 0.2);
    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.05);
    font-size: 0.8rem;
  }

  .wordName {
    font-size: 2rem;
    font-weight: 600;
  }
  .ph {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 10px;
    font-size: 1.2rem;
  }
  .block-button {
    background: #fff;
    color: #333;
    width: 120px;
    cursor: pointer;
    font-weight: 600;
    padding: 15px 20px;
    border-radius: 35px;
    border: 1px solid rgba(255, 255, 255, 0.3);
    border-right: 1px solid rgba(255, 255, 255, 0.2);
    border-bottom: 1px solid rgba(255, 255, 255, 0.2);
    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.05);
  }
}
</style>
