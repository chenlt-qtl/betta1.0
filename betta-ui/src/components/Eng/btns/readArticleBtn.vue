<template>
  <el-button
    size="mini"
    type="text"
    icon="el-icon-chat-line-square"
    @click="handleReadArticle"
    v-hasPermi="['eng:article:edit']"
  >
    跟读
  </el-button>
</template>

<script>
export default {
  props: ["articleId"],
  methods: {
    /** 跟读文章操作 */
    handleReadArticle: function () {
      this.$router.push("/eng/article/read/" + this.articleId);
    },
  },
};
</script>