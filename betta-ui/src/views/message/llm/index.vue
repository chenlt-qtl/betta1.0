<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="配置名称" prop="configName">
        <el-input v-model="queryParams.configName" placeholder="请输入配置名称" clearable style="width: 180px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="提供商" prop="provider">
        <el-select v-model="queryParams.provider" placeholder="请选择" clearable style="width: 120px">
          <el-option label="OpenAI" value="OPENAI" />
          <el-option label="Anthropic" value="ANTHROPIC" />
          <el-option label="百度" value="BAIDU" />
          <el-option label="阿里云" value="ALIYUN" />
          <el-option label="本地部署" value="LOCAL" />
        </el-select>
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
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['message:llm:add']">新增</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="llmList">
      <el-table-column label="ID" align="center" prop="id" width="60" />
      <el-table-column label="配置名称" align="center" prop="configName" :show-overflow-tooltip="true" />
      <el-table-column label="提供商" align="center" prop="provider" width="100">
        <template slot-scope="scope">
          {{ getProviderName(scope.row.provider) }}
        </template>
      </el-table-column>
      <el-table-column label="模型" align="center" prop="modelName" :show-overflow-tooltip="true" />
      <el-table-column label="API端点" align="center" prop="apiEndpoint" :show-overflow-tooltip="true" min-width="150" />
      <el-table-column label="温度" align="center" prop="temperature" width="70" />
      <el-table-column label="最大Token" align="center" prop="maxTokens" width="90" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'">{{ scope.row.status === '0' ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" width="140" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['message:llm:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['message:llm:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/修改 对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="配置名称" prop="configName">
          <el-input v-model="form.configName" placeholder="请输入配置名称" />
        </el-form-item>
        <el-form-item label="提供商" prop="provider">
          <el-select v-model="form.provider" placeholder="请选择提供商" style="width: 100%">
            <el-option label="OpenAI" value="OPENAI" />
            <el-option label="Anthropic (Claude)" value="ANTHROPIC" />
            <el-option label="百度 (文心一言)" value="BAIDU" />
            <el-option label="阿里云 (通义千问)" value="ALIYUN" />
            <el-option label="本地部署" value="LOCAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="API Key" prop="apiKey">
          <el-input v-model="form.apiKey" type="password" placeholder="请输入API Key" show-password />
        </el-form-item>
        <el-form-item label="API端点" prop="apiEndpoint">
          <el-input v-model="form.apiEndpoint" placeholder="可选，本地部署时填写" />
        </el-form-item>
        <el-form-item label="模型名称" prop="modelName">
          <el-input v-model="form.modelName" placeholder="如：gpt-4、gpt-3.5-turbo、claude-3-opus等" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="温度" prop="temperature">
              <el-input-number v-model="form.temperature" :min="0" :max="1" :step="0.1" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Top P" prop="topP">
              <el-input-number v-model="form.topP" :min="0" :max="1" :step="0.1" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="最大Token" prop="maxTokens">
              <el-input-number v-model="form.maxTokens" :min="100" :max="32000" :step="100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="超时(秒)" prop="timeout">
              <el-input-number v-model="form.timeout" :min="5" :max="300" :step="5" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
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
import { listLlmConfig, getLlmConfig, addLlmConfig, updateLlmConfig, delLlmConfig } from '@/api/message/llm'

export default {
  name: 'MessageLlmConfig',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      llmList: [],
      title: '',
      open: false,
      form: {},
      rules: {
        configName: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
        provider: [{ required: true, message: '请选择提供商', trigger: 'change' }],
        apiKey: [{ required: true, message: '请输入API Key', trigger: 'blur' }],
        modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }]
      },
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        configName: undefined,
        provider: undefined,
        status: undefined
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listLlmConfig(this.queryParams).then(response => {
        this.llmList = response.rows
        this.total = response.total
        this.loading = false
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
      this.title = '新增大模型配置'
    },
    handleUpdate(row) {
      this.reset()
      const id = row.id
      getLlmConfig(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = '修改大模型配置'
      })
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) return
        if (this.form.id != null) {
          updateLlmConfig(this.form).then(() => {
            this.$modal.msgSuccess('修改成功')
            this.open = false
            this.getList()
          })
        } else {
          addLlmConfig(this.form).then(() => {
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
        provider: 'OPENAI',
        apiKey: undefined,
        apiEndpoint: undefined,
        modelName: undefined,
        temperature: 0.7,
        maxTokens: 2000,
        topP: 1.0,
        timeout: 30,
        status: '0',
        remark: undefined
      }
      this.resetForm('form')
    },
    handleDelete(row) {
      const id = row.id
      this.$modal.confirm('是否确认删除该大模型配置？').then(() => {
        return delLlmConfig(id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    getProviderName(provider) {
      const providerMap = {
        'OPENAI': 'OpenAI',
        'ANTHROPIC': 'Anthropic',
        'BAIDU': '百度',
        'ALIYUN': '阿里云',
        'LOCAL': '本地部署'
      }
      return providerMap[provider] || provider
    }
  }
}
</script>
