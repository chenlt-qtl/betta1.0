<template>
  <div class="word-detail">
    <el-input
      style="width: 50%; margin-right: 10px"
      v-model="wordName"
      placeholder="请输入单词"
      clearable
      @keyup.enter.native="handleQuery"
    />
    <el-button
      type="primary"
      icon="el-icon-search"
      size="mini"
      @click="handleQuery"
      >搜索</el-button
    >
    <el-button icon="el-icon-refresh" size="mini" @click="resetQuery"
      >重置</el-button
    >
    <el-divider content-position="center"></el-divider>
    <word-detail :wordName="w"></word-detail>
  </div>
</template>
<style lang="scss">
.word-detail {
  padding: 16px 24px;
}
</style>
<script>
import WordDetail from "@/components/Eng/wordDetail";

export default {
  name: "Word",
  components: { WordDetail },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 表单参数
      form: {},
      open: false,
      wordName: "",
    };
  },
  created() {
    this.wordName = this.w;
  },
  computed: {
    w() {
      return this.$route.query && this.$route.query.w;
    },
  },
  methods: {
    /** 搜索按钮操作 */
    handleQuery() {
      this.$router.push("/eng/manage/word?w=" + this.wordName);
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.wordName = "";
      this.handleQuery();
    },
  },
  watch: {
    $route(route) {
      const w = (route.query && route.query.w) || "";
      this.wordName = w;
    },
  },
};
</script>
