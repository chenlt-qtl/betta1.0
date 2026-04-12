<template>
  <div class="api-config-container">
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
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-chat-dot-round" size="mini" @click="handleSimulateMessage">模拟消息</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" align="center" prop="id" width="60" />
      <el-table-column label="配置名称" align="center" prop="configName" :show-overflow-tooltip="true" />
      <el-table-column label="完整类名" align="center" prop="className" :show-overflow-tooltip="true" min-width="150" />
      <el-table-column label="关键词" align="center" prop="keywords" :show-overflow-tooltip="true" />
      <el-table-column label="正则表达式" align="center" prop="regexPattern" :show-overflow-tooltip="true" width="120" />
      <el-table-column label="优先级" align="center" prop="priority" width="80" />
      <el-table-column label="描述" align="center" prop="description" :show-overflow-tooltip="true" />
      <el-table-column label="大模型" align="center" prop="llmConfigId" width="90" />
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
    <el-dialog :title="formTitle" :visible.sync="open" width="700px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="配置名称" prop="configName">
          <el-input v-model="form.configName" placeholder="请输入配置名称" />
        </el-form-item>
        <el-form-item label="完整类名" prop="className">
          <el-input v-model="form.className" placeholder="请输入完整类名，如：http://xxx/api" />
        </el-form-item>
        <el-form-item label="参数配置">
          <el-table :data="paramList" border size="small" style="margin-bottom: 12px;">
            <el-table-column label="DTO字段名" prop="name" min-width="150">
              <template slot-scope="scope">
                <el-input v-model="scope.row.name" placeholder="如：account" />
              </template>
            </el-table-column>
            <el-table-column label="正则组号" prop="regexGroup" width="100">
              <template slot-scope="scope">
                <el-input-number v-model="scope.row.regexGroup" :min="1" controls-position="right" style="width: 100%" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="60" align="center">
              <template slot-scope="scope">
                <el-button type="text" icon="el-icon-delete" style="color: #f56c6c;" @click="removeParam(scope.$index)" />
              </template>
            </el-table-column>
          </el-table>
          <el-button type="primary" plain size="mini" icon="el-icon-plus" @click="addParam">添加参数</el-button>
        </el-form-item>
        <el-form-item label="关键词" prop="keywords">
          <el-input v-model="form.keywords" placeholder="多个关键词用逗号分隔，如：加卡,打卡" />
          <div class="el-form-item__tip">消息中包含任一关键词则命中；留空则所有消息都命中</div>
        </el-form-item>
        <el-form-item label="正则表达式" prop="regexPattern">
          <el-input v-model="form.regexPattern" placeholder="如：(豆芽|桐桐)\s*(.*?)\s*(加卡|扣卡)\s*(\d+)" />
          <div class="el-form-item__tip">用于匹配用户消息的正则表达式，留空则使用大模型智能匹配</div>
        </el-form-item>
        <el-divider content-position="left">
          <el-button type="text" size="small" @click="showRegexTest = !showRegexTest">
            <i :class="showRegexTest ? 'el-icon-arrow-up' : 'el-icon-arrow-down'" />
            正则测试
          </el-button>
        </el-divider>
        <div v-show="showRegexTest" style="background: #f5f7fa; padding: 16px; border-radius: 4px; margin-bottom: 16px;">
          <el-form-item label="测试消息">
            <el-input v-model="testText" placeholder="输入要测试的消息" @keyup.enter.native="handleTestRegex" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="small" @click="handleTestRegex">测试匹配</el-button>
          </el-form-item>
          <div v-if="regexTestResult" style="margin-top: 12px;">
            <div v-if="regexTestResult.matched" style="color: #67c23a; margin-bottom: 8px;">
              <i class="el-icon-success" /> 匹配成功
            </div>
            <div v-else style="color: #f56c6c; margin-bottom: 8px;">
              <i class="el-icon-error" /> 匹配失败
            </div>
            <div v-if="regexTestResult.error" style="color: #e6a23c; margin-bottom: 8px;">
              <i class="el-icon-warning" /> {{ regexTestResult.error }}
            </div>
            <div v-if="regexTestResult.matched && Object.keys(regexTestResult.groups).length > 0">
              <div style="font-weight: bold; margin-bottom: 8px; font-size: 13px; color: #303133;">捕获组明细：</div>
              <div v-for="(value, key) in regexTestResult.groups" :key="key" style="font-size: 12px; color: #606266; line-height: 1.8; padding-left: 16px;">
                <span style="color: #409eff; font-weight: 500;">Group {{ key }}</span
                >{{ key === '0' ? '（整句）' : '' }}：{{ value }}
              </div>
            </div>
            <div v-if="regexTestResult.matched && Object.keys(regexTestResult.params).length > 0" style="margin-top: 12px;">
              <div style="font-weight: bold; margin-bottom: 8px; font-size: 13px; color: #303133;">生成的参数JSON：</div>
              <pre style="background: #fff; padding: 12px; border-radius: 4px; font-size: 12px; color: #303133; overflow-x: auto;">{{ JSON.stringify(regexTestResult.params, null, 2) }}</pre>
            </div>
          </div>
        </div>
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="form.priority" :min="0" :max="100" :step="1" style="width: 200px" />
          <div class="el-form-item__tip">数值越大优先级越高</div>
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
        <el-button @click="open = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 模拟消息对话框 -->
    <el-dialog title="模拟飞书消息" :visible.sync="simulateOpen" width="700px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="消息内容">
          <el-input v-model="simulateMessageText" type="textarea" :rows="4" placeholder="请输入要模拟发送的消息..." />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="sendSimulateMessage" :loading="simulateSending">发送</el-button>
        </el-form-item>
      </el-form>
      <el-divider content-position="left">执行结果</el-divider>
      <div v-if="simulateResult" style="background: #f5f7fa; padding: 16px; border-radius: 4px;">
        <div style="margin-bottom: 8px; color: #909399; font-size: 13px;">
          <i class="el-icon-time" /> 耗时: {{ simulateDuration }}s
        </div>
        <div v-if="simulateResult.matchType" style="margin-bottom: 8px; color: #606266; font-size: 13px;">
          <i class="el-icon-cpu" /> 匹配类型: {{ simulateResult.matchType }}
        </div>
        <div v-if="simulateResult.toolName" style="margin-bottom: 8px; color: #606266; font-size: 13px;">
          <i class="el-icon-setting" /> 工具: {{ simulateResult.toolName }}
        </div>
        <el-divider style="margin: 12px 0;" />
        <div style="white-space: pre-wrap; word-wrap: break-word; line-height: 1.6; color: #303133;">{{ simulateResult.result }}</div>
      </div>
      <div slot="footer">
        <el-button @click="simulateOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listConfig, getConfig, addConfig, updateConfig, delConfig, testRegex, simulateMessage } from '@/api/robot/toolConfig'
import { listLlmConfig } from '@/api/robot/llm'

export default {
  name: 'ApiConfig',
  props: {
    showSearch: {
      type: Boolean,
      default: true
    },
    activeTab: {
      type: String,
      default: 'api'
    }
  },
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      open: false,
      formTitle: '',
      form: {},
      paramList: [],
      rules: {
        configName: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
        className: [{ required: true, message: '请输入完整类名', trigger: 'blur' }]
      },
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        configName: undefined,
        keywords: undefined,
        status: undefined
      },
      llmOptions: [],
      showRegexTest: false,
      testText: '',
      regexTestResult: null,
      simulateOpen: false,
      simulateMessageText: '',
      simulateSending: false,
      simulateResult: null,
      simulateDuration: 0
    }
  },
  watch: {
    activeTab(val) {
      if (val === 'api' && this.list.length === 0) {
        this.getList()
      }
    }
  },
  methods: {
    /** 查询列表 */
    getList() {
      if (this.activeTab !== 'api') return
      this.loading = true
      listConfig(this.queryParams).then(response => {
        this.list = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => {
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
      this.resetForm()
      this.formTitle = '新增API配置'
      this.open = true
      this.getLlmOptions()
    },
    handleUpdate(row) {
      this.resetForm()
      const id = row.id
      getConfig(id).then(response => {
        this.form = response.data
        this.formTitle = '修改API配置'
        this.parseParamsToList()
        this.open = true
      })
      this.getLlmOptions()
    },
    submitForm() {
      for (let i = 0; i < this.paramList.length; i++) {
        const p = this.paramList[i]
        if (!p.name) {
          this.$modal.msgWarning(`第 ${i + 1} 行参数的 DTO 字段名不能为空`)
          return
        }
        if (!p.regexGroup) {
          this.$modal.msgWarning(`第 ${i + 1} 行参数的正则组号不能为空`)
          return
        }
      }
      this.buildParamsFromList()
      this.$refs['form'].validate(valid => {
        if (!valid) return
        if (this.form.id != null) {
          updateConfig(this.form).then(() => {
            this.$modal.msgSuccess('修改成功')
            this.open = false
            this.getList()
          })
        } else {
          addConfig(this.form).then(() => {
            this.$modal.msgSuccess('新增成功')
            this.open = false
            this.getList()
          })
        }
      })
    },
    resetForm() {
      this.form = {
        id: undefined,
        configName: undefined,
        className: undefined,
        toolParams: undefined,
        keywords: undefined,
        regexPattern: undefined,
        regexParamMap: undefined,
        priority: 0,
        description: undefined,
        prompt: undefined,
        llmConfigId: undefined,
        status: '0',
        remark: undefined
      }
      this.paramList = []
      this.resetFormRef()
      this.showRegexTest = false
      this.testText = ''
      this.regexTestResult = null
    },
    handleDelete(row) {
      const id = row.id
      this.$modal.confirm('是否确认删除该API配置？').then(() => {
        return delConfig(id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    resetFormRef() {
      if (this.$refs.form) {
        this.$refs.form.resetFields()
      }
    },
    getLlmOptions() {
      listLlmConfig({ status: '0', pageNum: 1, pageSize: 100 }).then(response => {
        this.llmOptions = response.rows
      })
    },
    addParam() {
      this.paramList.push({ name: '', regexGroup: 1 })
    },
    removeParam(index) {
      this.paramList.splice(index, 1)
    },
    buildParamsFromList() {
      if (this.paramList.length === 0) {
        this.form.regexParamMap = null
        this.form.toolParams = null
        return
      }
      const regexParamMap = {}
      const toolParams = {}

      this.paramList.forEach(p => {
        regexParamMap[String(p.regexGroup)] = p.name
        toolParams[p.name] = '${' + p.name + '}'
      })

      this.form.regexParamMap = JSON.stringify(regexParamMap)
      this.form.toolParams = JSON.stringify(toolParams)
    },
    parseParamsToList() {
      this.paramList = []
      if (this.form.regexParamMap) {
        try {
          const map = JSON.parse(this.form.regexParamMap)
          for (const [group, field] of Object.entries(map)) {
            this.paramList.push({ name: field, regexGroup: parseInt(group) })
          }
        } catch (e) {
          console.error('解析 regexParamMap 失败', e)
        }
      }
    },
    handleTestRegex() {
      if (!this.form.regexPattern) {
        this.$modal.msgWarning('请先输入正则表达式')
        return
      }
      if (!this.testText) {
        this.$modal.msgWarning('请输入测试消息')
        return
      }
      if (this.paramList.length === 0) {
        this.$modal.msgWarning('请先添加参数配置')
        return
      }
      const regexParamMap = {}
      this.paramList.forEach(p => {
        regexParamMap[String(p.regexGroup)] = p.name
      })
      testRegex({
        regexPattern: this.form.regexPattern,
        regexParamMap: JSON.stringify(regexParamMap),
        testText: this.testText
      }).then(response => {
        this.regexTestResult = response.data
      })
    },
    handleSimulateMessage() {
      this.simulateOpen = true
      this.simulateMessageText = ''
      this.simulateResult = null
      this.simulateDuration = 0
    },
    sendSimulateMessage() {
      if (!this.simulateMessageText.trim()) {
        this.$modal.msgWarning('请输入消息内容')
        return
      }
      this.simulateSending = true
      const startTime = Date.now()
      simulateMessage({ messageText: this.simulateMessageText })
        .then(response => {
          this.simulateDuration = ((Date.now() - startTime) / 1000).toFixed(2)
          this.simulateResult = {
            matchType: '模拟执行',
            toolName: 'API配置',
            result: response.data || '执行完成'
          }
          this.$modal.msgSuccess('消息处理完成')
        })
        .catch(error => {
          this.simulateResult = {
            matchType: '错误',
            toolName: '',
            result: '处理失败: ' + (error.message || '未知错误')
          }
          this.$modal.msgError('消息处理失败')
        })
        .finally(() => {
          this.simulateSending = false
        })
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
pre {
  margin: 8px 0 0 0;
  font-size: 12px;
  color: #606266;
  white-space: pre-wrap;
  word-wrap: break-word;
  background: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
}
</style>
