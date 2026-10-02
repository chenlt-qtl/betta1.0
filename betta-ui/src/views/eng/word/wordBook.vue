<template>
  <div class="app-container">
    <el-form
      ref="queryForm"
      :model="queryParams"
      size="small"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item label="单词" prop="wordName">
        <el-input
          v-model="queryParams.wordName"
          placeholder="请输入单词"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
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
      </el-form-item>
    </el-form>

    <ArticleWordList
      :play="play"
      :loading="loading"
      :articleId="0"
      :listData="wordList"
      :getWordList="getList"
    >
      <template v-slot:rightBar>
        <right-toolbar
          :showSearch.sync="showSearch"
          @queryTable="getList"
        ></right-toolbar>
      </template>
    </ArticleWordList>

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script>
import { listByUser } from "@/api/eng/score";
import { play } from "@/utils/audio";
import ArticleWordList from "@/components/Eng/wordList/articleWordList.vue";

export default {
  name: "WordBook",
  components: { ArticleWordList },
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      wordList: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        wordName: null,
      },
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询当前用户手动收藏的生词。 */
    getList() {
      this.loading = true;
      listByUser({ ...this.queryParams, articleId: 0 })
        .then((response) => {
          this.wordList = response.rows || [];
          this.total = response.total || 0;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    play(url) {
      url && play(url);
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
  },
};
</script>
