<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
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
          v-hasPermi="['eng:sentence:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['eng:sentence:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['eng:sentence:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['eng:sentence:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="sentenceList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="文章" align="center" prop="articleName" />
      <el-table-column label="句子内容" align="center" prop="content" />
      <el-table-column label="解释" align="center" prop="acceptation" />
      <el-table-column label="序号" align="center" prop="idx" />
      <el-table-column label="图片" align="center" prop="picture" width="100">
        <template slot-scope="scope">
          <image-preview :src="scope.row.picture" :width="50" :height="50"/>
        </template>
      </el-table-column>
      <el-table-column label="音频" align="center" prop="mp3" >
         <el-button type="text">
          <svg-icon icon-class="sound" />
        </el-button>
      </el-table-column>
      <el-table-column label="MP3开始结束时间" align="center" prop="mp3Time" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['eng:sentence:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['eng:sentence:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改文章句子对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="文章ID" prop="articleId">
          <el-select v-model="form.articleId" placeholder="请选择文章ID">
            <el-option
              v-for="dict in dict.type.sys_user_sex"
              :key="dict.value"
              :label="dict.label"
              :value="parseInt(dict.value)"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="句子内容" prop="content">
          <el-input v-model="form.content" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="解释" prop="acceptation">
          <el-input v-model="form.acceptation" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="序号" prop="idx">
          <el-input v-model="form.idx" placeholder="请输入序号" />
        </el-form-item>
        <el-form-item label="图片" prop="picture">
          <image-upload v-model="form.picture"/>
        </el-form-item>
        <el-form-item label="音频" prop="mp3">
          <file-upload :fileType="['mp3']" :limit="1" v-model="form.mp3"/>
        </el-form-item>
        <el-form-item label="MP3开始结束时间" prop="mp3Time">
          <el-input v-model="form.mp3Time" placeholder="请输入MP3开始结束时间" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listSentence, getSentence, delSentence, addSentence, updateSentence } from "@/api/eng/sentence";

export default {
  name: "Sentence",
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
      // 总条数
      total: 0,
      // 文章句子表格数据
      sentenceList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        articleId: null,
        content: null,
        acceptation: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        articleId: [
          { required: true, message: "文章ID不能为空", trigger: "change" }
        ],
        content: [
          { required: true, message: "句子内容不能为空", trigger: "blur" }
        ],
        idx: [
          { required: true, message: "序号不能为空", trigger: "blur" }
        ],
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询文章句子列表 */
    getList() {
      this.loading = true;
      listSentence(this.queryParams).then(response => {
        this.sentenceList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        id: null,
        articleId: null,
        content: null,
        acceptation: null,
        idx: null,
        picture: null,
        mp3: null,
        mp3Time: null,
        status: null,
        createTime: null,
        createBy: null,
        updateTime: null,
        updateBy: null
      };
      this.resetForm("form");
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
      this.ids = selection.map(item => item.id)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加文章句子";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const id = row.id || this.ids
      getSentence(id).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改文章句子";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateSentence(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addSentence(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids;
      this.$modal.confirm('是否确认删除文章句子编号为"' + ids + '"的数据项？').then(function() {
        return delSentence(ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('eng/sentence/export', {
        ...this.queryParams
      }, `sentence_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
