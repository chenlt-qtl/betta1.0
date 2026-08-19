<template>
  <el-button
    size="mini"
    type="text"
    icon="el-icon-coordinate"
    @click="handleTest"
    >测试</el-button
  >
</template>

<script>
export default {
  props: ["articleId"],
  methods: {
    handleTest: function () {
      this.$router.push("/eng/article/test/" + this.articleId);
    },
  },
};
</script>