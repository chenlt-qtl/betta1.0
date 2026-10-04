<template>
  <article-test />
</template>

<script>
import ArticleTest from '@/views/eng/article/test/index.vue'

/** 新词与复习入口复用统一测试流程，题目范围由路由模式和服务端 attemptId 固定。 */
export default {
  name: 'EngStudyChallenge',
  components: { ArticleTest }
}
</script>
