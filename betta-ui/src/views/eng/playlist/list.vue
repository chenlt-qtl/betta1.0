<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryForm"
      size="small"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
      @submit.native.prevent
    >
      <el-form-item label="关键字" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入关键字"
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

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['eng:playList:add']"
          >新增</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['eng:playList:remove']"
          >删除</el-button
        >
      </el-col>
      <right-toolbar
        :showSearch.sync="showSearch"
        @queryTable="getList"
      ></right-toolbar>
    </el-row>

    <el-table
      v-loading="loading"
      :data="playListList"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column
        label="分组"
        align="center"
        prop="groupName"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="文章"
        align="center"
        prop="title"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="操作"
        align="center"
        class-name="small-padding fixed-width"
      >
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['eng:playList:remove']"
            >删除</el-button
          >
        </template>
      </el-table-column>
    </el-table>
    <selectArticle
      ref="selectArticle"
      :playList="form"
      @ok="() => getList()"
    ></selectArticle>
  </div>
</template>

<script>
import { getPlayList, addPlayList, updatePlayList } from "@/api/eng/playList";
import selectArticle from "./selectArticle.vue";
import { listPlay } from "@/api/eng/article";

export default {
  components: { selectArticle },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 播放列表表格数据
      playListList: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 1000,
        content: null,
        inPlayList: true,
      },
      // 表单参数
      form: {},
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询播放列表列表 */
    getList() {
      this.loading = true;
      listPlay(this.queryParams).then((response) => {
        this.playListList = response.rows;
        this.loading = false;
      });
    },
    getByUser() {
      return getPlayList().then((response) => {
        if (response.data) {
          this.form = response.data;
        }
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
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map((item) => item.id);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.getByUser();
      this.$refs.selectArticle.show();
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate((valid) => {
        if (valid) {
          if (this.form.id != null) {
            updatePlayList(this.form).then((response) => {
              this.$modal.msgSuccess("修改成功");
              this.getList();
            });
          } else {
            addPlayList(this.form).then((response) => {
              this.$modal.msgSuccess("新增成功");
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id ? [row.id] : this.ids;
      this.$modal
        .confirm('是否确认从播放列表删除编号为"' + ids + '"的数据项？')
        .then(() => {
          this.getByUser().then(() => {
            const sentenceIds = this.form.sentenceIds
              ? this.form.sentenceIds.split(",")
              : []; //获取原来的数据
            const idsStr = ids.join(",").split(","); //转成字符串数组
            const newIds = sentenceIds.filter((id) => !idsStr.includes(id)); //新的ID
            updatePlayList({
              ...this.form,
              sentenceIds: newIds.join(","),
            }).then(() => {
              this.getList();
              this.$modal.msgSuccess("删除成功");
            });
          });
        })
        .catch(() => {});
    },
  },
};
</script>
