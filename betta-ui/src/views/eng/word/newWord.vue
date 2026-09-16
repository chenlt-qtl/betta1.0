<template>
  <WordQuery />
</template>

<script>
import WordQuery from "./detail.vue";

/** 兼容“查单词”菜单路由，统一复用完整单词查询页面。 */
export default {
  name: "WordQueryRoute",
  components: { WordQuery },
};
</script>
