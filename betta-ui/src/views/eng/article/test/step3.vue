<template>
  <div class="test-step3">
    <section class="step3-result">
      <i :class="result.passed ? 'el-icon-success passed' : 'el-icon-warning-outline pending'" />
      <h2>{{ result.passed ? '恭喜您，测试通过！' : '测试完成，继续加油！' }}</h2>
      <div class="score">{{ result.score || 0 }} 分</div>
      <p>答对 {{ result.correctCount || 0 }}/{{ result.totalCount || 0 }} 题</p>
    </section>
    <div class="toolbar result-actions">
      <button class="block-button" @click="$emit('restart')">重新测试</button>
      <button class="block-button" @click="$emit('study')">继续学习</button>
      <button class="block-button" @click="$emit('wrong')">错词本</button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'EngArticleTestResult',
  props: {
    result: {
      type: Object,
      default: () => ({})
    }
  }
}
</script>

<style scoped lang="scss">
.test-step3 {
  display: flex;
  flex-direction: column;
  height: calc(100% - 40px);

  .step3-result {
    display: flex;
    flex: 1;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    text-align: center;
  }

  .step3-result > i {
    font-size: 56px;
  }

  .passed {
    color: #67c23a;
  }

  .pending {
    color: #e6a23c;
  }

  h2 {
    margin: 20px 0 12px;
  }

  .score {
    color: #409eff;
    font-size: 42px;
    font-weight: 600;
  }

  p {
    color: #606266;
  }

  .result-actions {
    flex-wrap: wrap;
  }
}
</style>
