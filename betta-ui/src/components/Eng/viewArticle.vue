<template>
  <div>
    <el-button icon="el-icon-search" size="mini" @click="handleViewArticle">
      查看文章
    </el-button>

    <el-dialog
      title="查看文章内容"
      :visible.sync="open"
      width="500px"
      append-to-body
    >
      <section
        class="view-article-sentence"
        v-for="(str, index) in displayList"
        :key="str + index"
      >
        {{ str }}<br /><br />
      </section>
      <div slot="footer" class="dialog-footer">
        <el-button @click="() => (open = false)">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { exportArticle } from "@/api/eng/article";

export default {
  props: ["articleId"],
  data() {
    return {
      // 是否显示弹出层
      open: false,
      displayList: [],
    };
  },
  methods: {
    /**查看文章内容 */
    handleViewArticle() {
      exportArticle(this.articleId).then((res) => {
        this.displayList = res.data;
        this.open = true;
      });
    },
  },
};
</script>

<style scoped lang="scss">
.view-article-sentence {
  word-break: normal;
  letter-spacing: 1px;
}
</style>
