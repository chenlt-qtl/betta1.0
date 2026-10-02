<template>
  <article-test />
</template>

<script>
import ArticleTest from '@/views/eng/article/test/index.vue'

/** 学习中心入口复用统一文章测试流程，避免维护第二套测试页面。 */
export default {
  name: 'EngStudyChallenge',
  components: { ArticleTest }
}
</script>
