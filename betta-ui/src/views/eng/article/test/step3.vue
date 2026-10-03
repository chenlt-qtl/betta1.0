<template>
  <div class="test-step3">
    <section class="step3-result">
      <i :class="result.passed ? 'el-icon-success passed' : 'el-icon-warning-outline pending'" />
      <h2>{{ result.passed ? '恭喜您，测试通过！' : '测试完成，继续加油！' }}</h2>
      <div class="coin-reward">本轮获得 {{ coinReward }} 金币</div>
      <div class="reward-detail">
        <span>基础金币 {{ correctCount }}</span>
        <span>额外奖励 {{ bonusCoin }} 金币</span>
      </div>
      <p>答对 {{ correctCount }}/{{ result.totalCount || 0 }} 题</p>
      <div class="coin-balance">
        <i class="el-icon-coin" /> 金币余额 {{ result.coinBalance || 0 }}
      </div>
    </section>
    <div class="toolbar result-actions">
      <!-- <button class="block-button" @click="$emit('restart')">重新测试</button> -->
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
  },
  computed: {
    correctCount() {
      return Number(this.result.correctCount) || 0
    },
    coinReward() {
      return Number(this.result.coinReward) || 0
    },
    /** 额外奖励以后端入账结果为准，前端不重复计算奖励档位。 */
    bonusCoin() {
      return Math.max(this.coinReward - this.correctCount, 0)
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

  .coin-reward {
    color: #e6a23c;
    font-size: 36px;
    font-weight: 600;
  }

  .reward-detail {
    display: flex;
    gap: 18px;
    margin-top: 14px;
    color: #606266;
    font-size: 14px;
  }

  p {
    color: #606266;
  }

  .coin-balance {
    padding: 8px 16px;
    border-radius: 18px;
    color: #b88230;
    background: rgba(253, 246, 236, 0.85);
    font-weight: 600;
  }

  .result-actions {
    flex-wrap: wrap;
  }
}
</style>
