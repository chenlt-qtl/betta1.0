<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryForm"
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
      :showScore="true"
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
  name: "Word",
  components: { ArticleWordList },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 单词表格数据
      wordList: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        wordName: null,
        acceptation: null,
        exchange: null,
      },
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询单词列表 */
    getList() {
      this.loading = true;
      listByUser(this.queryParams).then((response) => {
        this.wordList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    play(url) {
      play(url);
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
  },
};
</script>
