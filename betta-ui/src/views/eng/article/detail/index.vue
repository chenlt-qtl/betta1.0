<template>
  <div class="app-container">
    <el-descriptions class="margin-top" title="文章信息" :column="2">
      <template slot="extra">
        <play-article-btn :articleId="articleId" />
        <el-divider direction="vertical" />
        <read-article-btn :articleId="articleId" />
        <el-divider direction="vertical" />
        <test-article-btn :articleId="articleId" />
      </template>
      <el-descriptions-item label="标题">{{
        article.title
      }}</el-descriptions-item>
      <el-descriptions-item label="分组"
        ><el-tag size="small">{{
          article.groupName
        }}</el-tag></el-descriptions-item
      >
      <el-descriptions-item label="图片">
        <img
          v-if="article.picture"
          style="width: 200px"
          :src="baseUrl + article.picture"
      /></el-descriptions-item>
      <el-descriptions-item label="音频">
        <el-link :underline="false" type="primary">
          <svg-icon icon-class="sound" @click="() => play(article.mp3)"
        /></el-link>
      </el-descriptions-item>
      <el-descriptions-item label="描述">{{
        article.comment
      }}</el-descriptions-item>
    </el-descriptions>
    <el-descriptions class="margin-top" title="句子信息" />
    <sentence-list
      :article="article"
      :play="play"
      :getWordList="getWordList"
      :wordList="wordList"
    ></sentence-list>
    <el-descriptions
      class="margin-top word-list-header"
      :title="`单词信息 (共` + wordTotal + `条)`"
    >
      <template slot="extra">
        <el-radio-group v-model="wordSort" size="mini" @change="getWordList">
          <el-radio-button label="time">按时间</el-radio-button>
          <el-radio-button label="name">按名称</el-radio-button>
        </el-radio-group>
      </template>
    </el-descriptions>
    <ArticleWordList
      :play="play"
      :showScore="true"
      :articleId="articleId"
      :listData="wordList"
      :getWordList="getWordList"
      :batchAdd="true"
    ></ArticleWordList>
  </div>
</template>

<script>
import { getArticle } from "@/api/eng/article";

import { listByArticle } from "@/api/eng/score";

import { play } from "@/utils/audio";
import ArticleWordList from "@/components/Eng/wordList/articleWordList.vue";
import SentenceList from "@/components/Eng/sentenceList.vue";
import TestArticleBtn from "@/components/Eng/btns/testArticleBtn.vue";
import ReadArticleBtn from "@/components/Eng/btns/readArticleBtn.vue";
import PlayArticleBtn from "@/components/Eng/btns/playArticleBtn.vue";

export default {
  name: "Article",
  components: {
    ArticleWordList,
    SentenceList,
    TestArticleBtn,
    ReadArticleBtn,
    PlayArticleBtn,
  },
  data() {
    return {
      articleId: 0,
      article: {},
      baseUrl: process.env.VUE_APP_RESOURCE,
      // 遮罩层
      loading: true,
      wordTotal: 0,
      form: {},
      wordList: [],
      wordSort: "time",
    };
  },
  created() {
    this.articleId = this.$route.params && this.$route.params.articleId;
    if (this.articleId) {
      this.getArticle();
      this.getWordList();
    }
  },
  methods: {
    /** 查询英语文章 */
    getArticle() {
      this.loading = true;
      getArticle(this.articleId).then(({ data }) => {
        this.article = data;
        this.loading = false;
      });
    },
    play(url, mp3Time) {
      url && play(url, mp3Time);
    },
    /** 按当前选择的添加时间或名称顺序查询单词列表。 */
    getWordList() {
      this.loading = true;
      const byName = this.wordSort === "name";
      listByArticle(
        this.articleId,
        false,
        1000,
        byName ? "word_name" : "rel_id",
        byName ? "ascending" : "descending"
      ).then(
        (response) => {
          this.wordList = response.rows || [];
          this.wordTotal = response.total;
          this.loading = false;
        }
      );
    },
  },
};
</script>
<style scoped>
.app-container {
  padding: 12px 48px;
}
.margin-top {
  margin-top: 24px;
}
@media (max-width: 600px) {
  .app-container {
    padding: 12px;
  }
  .word-list-header ::v-deep .el-descriptions__header {
    align-items: flex-start;
    flex-wrap: wrap;
    gap: 12px;
  }
}
</style>
