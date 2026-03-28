<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="配置名称" prop="configName">
        <el-input v-model="queryParams.configName" placeholder="请输入配置名称" clearable style="width: 180px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="关键词" prop="keywords">
        <el-input v-model="queryParams.keywords" placeholder="请输入关键词" clearable style="width: 180px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width: 100px">
          <el-option label="启用" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['message:api:add']">新增</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="apiList">
      <el-table-column label="ID" align="center" prop="id" width="60" />
      <el-table-column label="配置名称" align="center" prop="configName" :show-overflow-tooltip="true" />
      <el-table-column label="API地址" align="center" prop="apiUrl" :show-overflow-tooltip="true" min-width="150" />
      <el-table-column label="关键词" align="center" prop="keywords" :show-overflow-tooltip="true" />
      <el-table-column label="优先级" align="center" prop="priority" width="80" />
      <el-table-column label="描述" align="center" prop="description" :show-overflow-tooltip="true" />
      <el-table-column label="关联LLM" align="center" prop="llmConfigId" width="90" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'">{{ scope.row.status === '0' ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['message:api:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['message:api:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/修改 对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="700px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="配置名称" prop="configName">
          <el-input v-model="form.configName" placeholder="请输入配置名称" />
        </el-form-item>
        <el-form-item label="API地址" prop="apiUrl">
          <el-input v-model="form.apiUrl" placeholder="请输入API地址，如：http://xxx/api" />
        </el-form-item>
        <el-form-item label="API参数" prop="apiParams">
          <el-input v-model="form.apiParams" type="textarea" :rows="3" placeholder='JSON格式，如：{"name":"${name}","age":"${age}"}' />
          <div class="el-form-item__tip">使用 ${参数名} 引用从消息中提取的参数</div>
        </el-form-item>
        <el-form-item label="关键词" prop="keywords">
          <el-input v-model="form.keywords" placeholder="多个关键词用逗号分隔，如：加卡,打卡" />
          <div class="el-form-item__tip">消息中包含任一关键词则命中；留空则所有消息都命中</div>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="form.priority" :min="0" :max="100" :step="1" style="width: 200px" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="用于大模型选择最合适的配置" />
        </el-form-item>
        <el-form-item label="提示词" prop="prompt">
          <el-input v-model="form.prompt" type="textarea" :rows="3" placeholder="用于从消息中提取参数的提示词" />
          <div class="el-form-item__tip">如：从消息中提取姓名和卡号，输出JSON</div>
        </el-form-item>
        <el-form-item label="大模型" prop="llmConfigId">
          <el-select v-model="form.llmConfigId" placeholder="请选择大模型配置" clearable style="width: 100%">
            <el-option v-for="item in llmOptions" :key="item.id" :label="item.configName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="可选" />
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
import { listApiConfig, getApiConfig, addApiConfig, updateApiConfig, delApiConfig } from '@/api/message/api'
import { listLlmConfig } from '@/api/message/llm'

export default {
  name: 'MessageApiConfig',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      apiList: [],
      llmOptions: [],
      title: '',
      open: false,
      form: {},
      rules: {
        configName: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
        apiUrl: [{ required: true, message: '请输入API地址', trigger: 'blur' }]
      },
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        configName: undefined,
        keywords: undefined,
        status: undefined
      }
    }
  },
  created() {
    this.getList()
    this.getLlmOptions()
  },
  methods: {
    getList() {
      this.loading = true
      listApiConfig(this.queryParams).then(response => {
        this.apiList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    getLlmOptions() {
      listLlmConfig({ status: '0', pageNum: 1, pageSize: 100 }).then(response => {
        this.llmOptions = response.rows
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增API配置'
    },
    handleUpdate(row) {
      this.reset()
      const id = row.id
      getApiConfig(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = '修改API配置'
      })
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) return
        if (this.form.id != null) {
          updateApiConfig(this.form).then(() => {
            this.$modal.msgSuccess('修改成功')
            this.open = false
            this.getList()
          })
        } else {
          addApiConfig(this.form).then(() => {
            this.$modal.msgSuccess('新增成功')
            this.open = false
            this.getList()
          })
        }
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        id: undefined,
        configName: undefined,
        apiUrl: undefined,
        apiParams: undefined,
        keywords: undefined,
        priority: 0,
        description: undefined,
        prompt: undefined,
        llmConfigId: undefined,
        status: '0',
        remark: undefined
      }
      this.resetForm('form')
    },
    handleDelete(row) {
      const id = row.id
      this.$modal.confirm('是否确认删除该API配置？').then(() => {
        return delApiConfig(id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.el-form-item__tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin-top: 4px;
}
</style>
