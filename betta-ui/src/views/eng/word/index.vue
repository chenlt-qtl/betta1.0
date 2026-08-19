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
      <el-form-item label="单词内容" prop="wordName">
        <el-input
          v-model="queryParams.wordName"
          placeholder="请输入单词内容"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="手动注释" prop="exchange">
        <el-input
          v-model="queryParams.exchange"
          placeholder="请输入手动注释"
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

    <WordTable
      :play="play"
      :listData="wordList"
      :getWordList="getList"
      :manualAdd="true"
      :hideScore="true"
    >
      <template v-slot:toolBtn="slotProps">
        <el-col :span="1.5">
          <el-button
            type="danger"
            icon="el-icon-delete"
            size="mini"
            @click="() => handleDelete(slotProps.ids)"
            :disabled="slotProps.ids.length == 0"
            >删除</el-button
          >
        </el-col>
        <right-toolbar
          :showSearch.sync="showSearch"
          @queryTable="getList"
        ></right-toolbar>
      </template>
      <template v-slot:tableBtn="slotProps">
        <el-button
          size="mini"
          type="text"
          icon="el-icon-delete"
          @click="handleDelete([slotProps.word.id])"
          >删除</el-button
        >
      </template>
    </WordTable>
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
import { listWord, delWord } from "@/api/eng/word";
import { play } from "@/utils/audio";
import WordTable from "@/components/Eng/wordList/wordTable.vue";

export default {
  name: "Word",
  components: { WordTable },
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
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        wordName: [
          { required: true, message: "单词内容不能为空", trigger: "blur" },
        ],
      },
    };
  },
  created() {
    this.getList();
  },
  methods: {
    play(url, mp3Time) {
      play(url, mp3Time);
    },
    /** 查询单词列表 */
    getList() {
      this.loading = true;
      listWord(this.queryParams).then((response) => {
        this.wordList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
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
    /** 删除按钮操作 */
    handleDelete(ids) {
      this.$modal
        .confirm('是否确认删除单词编号为"' + ids + '"的数据项？')
        .then(function () {
          return delWord(ids);
        })
        .then(() => {
          this.getList();
          this.$modal.msgSuccess("删除成功");
        })
        .catch(() => {});
    },
  },
};
</script>
