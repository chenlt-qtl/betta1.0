<template>
  <div class="test-step3">
    <section class="step3-result">
      恭喜您，测试完成！<br />
      最终得分
      {{ parseInt((result.right * 100) / questionList.length) }} 分,答对了
      {{ result.right }}题，答错了 {{ result.wrong }} 题。
    </section>
    <div class="toolbar">
      <button class="block-button" @click="next">下一关</button>&nbsp;
      <button class="block-button" @click="back">返回</button>
    </div>
  </div>
</template>
<script>
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
  created() {},
  methods: {
    next() {
      this.$emit("next");
    },
    back() {
      this.$router.push("/eng/manage/article");
    },
  },
};
</script>
<style scoped lang="scss">
.test-step3 {
  display: flex;
  flex-direction: column;
  height: 100%;
  .step3-result {
    display: flex;
    justify-content: center;
    align-items: center;
    flex: 1;
  }
  .toolbar {
    text-align: center;
  }
}
</style>
