<template>
  <!-- 授权用户 -->
  <el-dialog
    title="增加文章"
    :visible.sync="visible"
    width="1000px"
    top="5vh"
    append-to-body
  >
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true">
      <el-form-item label="分组" prop="groupId">
        <el-select v-model="queryParams.groupId">
          <el-option
            v-for="group in groupList"
            :key="group.id"
            :label="group.name"
            :value="group.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="文章" prop="title">
        <el-input
          v-model="queryParams.title"
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
    <el-row>
      <el-table
        @row-click="clickRow"
        ref="table"
        :data="sentenceList"
        @selection-change="handleSelectionChange"
        height="260px"
      >
        <el-table-column type="selection" width="55"></el-table-column>
        <el-table-column
          label="分组"
          width="200"
          prop="groupName"
          :show-overflow-tooltip="true"
        />
        <el-table-column
          label="文章"
          prop="title"
          :show-overflow-tooltip="true"
        />
        
      </el-table>
      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize"
        @pagination="getList"
      />
    </el-row>
    <div slot="footer" class="dialog-footer">
      <el-button type="primary" @click="onSubmit">确 定</el-button>
      <el-button @click="visible = false">取 消</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { listPlay } from "@/api/eng/article";
import { listGroup } from "@/api/eng/group";
import { addPlayList, updatePlayList } from "@/api/eng/playList";

export default {
  props: ["playList"],
  data() {
    return {
      // 遮罩层
      visible: false,
      // 选中数组值
      sentenceIds: [],
      // 总条数
      total: 0,
      // 未授权用户数据
      sentenceList: [],
      groupList: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        content: undefined,
        groupId: undefined,
        articleName: undefined,
        inPlayList: false,
      },
    };
  },
  methods: {
    // 显示弹框
    show() {
      this.queryParams.roleId = this.roleId;
      this.getList();
      if (this.groupList.length <= 0) {
        this.getGroupList();
      }
      this.visible = true;
    },
    getGroupList() {
      this.loading = true;
      listGroup({ pageNum: 1, pageSize: 1000 }).then((response) => {
        this.groupList = response.rows;
      });
    },
    clickRow(row) {
      this.$refs.table.toggleRowSelection(row);
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.sentenceIds = selection.map((item) => item.id);
    },
    // 查询表数据
    getList() {
      listPlay(this.queryParams).then((res) => {
        this.sentenceList = res.rows;
        this.total = res.total;
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
    /** 确认 */
    onSubmit() {
      const ids = this.sentenceIds.join(",");
      if (ids == "") {
        this.$modal.msgError("请选择要增加的句子");
        return;
      }
      let method;
      if (this.playList.id) {
        method = updatePlayList;
      } else {
        method = addPlayList;
      }

      const sentenceIds = this.playList.sentenceIds
        ? this.playList.sentenceIds + "," + ids
        : ids;
      const data = { ...this.playList, sentenceIds };

      method(data).then((res) => {
        this.$modal.msgSuccess(res.msg);
        if (res.code === 200) {
          this.visible = false;
          this.$emit("ok");
        }
      });
    },
  },
};
</script>
