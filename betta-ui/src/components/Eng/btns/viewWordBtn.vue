<template>
  <span>
    <el-button
      size="mini"
      type="text"
      icon="el-icon-search"
      @click="handlePlayArticle"
      v-hasPermi="['eng:article:edit']"
    >
      查看
    </el-button>
    <el-dialog title="查看单词" :visible.sync="open">
      <div class="wordDetail">
        <word-detail :wordName="wordName"></word-detail>
      </div>
    </el-dialog>
  </span>
</template>

<script>
import WordDetail from "@/components/Eng/wordDetail";

export default {
  data() {
    return { open: false };
  },
  components: { WordDetail },
  props: ["wordName"],
  methods: {
    handlePlayArticle: function () {
      this.open = true;
    },
  },
};
</script>
<style lang="scss">
.wordDetail {
  max-height: 500px;
  overflow: auto;
}
</style>
