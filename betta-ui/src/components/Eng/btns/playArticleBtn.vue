<template>
  <el-button
    size="mini"
    type="text"
    icon="el-icon-service"
    @click="handlePlayArticle"
    v-hasPermi="['eng:article:edit']"
    >播放</el-button
  >
</template>

<script>
export default {
  props: ["articleId"],
  methods: {
    handlePlayArticle: function () {
      this.$router.push("/eng/playlist?article=" + this.articleId);
    },
  },
};
</script>