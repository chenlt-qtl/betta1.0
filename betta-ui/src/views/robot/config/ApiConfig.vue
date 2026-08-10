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
      <el-table-column label="工具类名" align="center" prop="className" :show-overflow-tooltip="true" min-width="150" />
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
    <el-dialog :title="formTitle" :visible.sync="open" width="1100px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <!-- 四个页签共用同一表单，切换时保留已填写内容和测试结果 -->
        <el-tabs v-model="formActiveTab">
          <el-tab-pane label="基本信息" name="basic">
            <el-form-item label="配置名称" prop="configName">
              <el-input v-model="form.configName" placeholder="请输入配置名称" />
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
          </el-tab-pane>

          <el-tab-pane label="工具配置" name="params">
            <el-form-item label="工具类名" prop="className">
              <el-input v-model="form.className" placeholder="请输入工具类名，如：http://xxx/api" />
            </el-form-item>
            <el-form-item label="关键词" prop="keywords">
              <el-input v-model="form.keywords" placeholder="多个关键词用逗号分隔，如：加卡,打卡" />
              <div class="el-form-item__tip">消息中包含任一关键词则命中；留空则所有消息都命中</div>
            </el-form-item>
            <el-form-item label="优先级" prop="priority">
              <el-input-number v-model="form.priority" :min="0" :max="100" :step="1" style="width: 200px" />
              <div class="el-form-item__tip">数值越大优先级越高</div>
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane label="正则配置" name="matching">
            <el-form-item label="正则表达式" prop="regexPattern">
              <el-input v-model="form.regexPattern" placeholder="如：(豆芽|桐桐)\s*(.*?)\s*(加卡|扣卡)\s*(\d+)" />
              <div class="el-form-item__tip">用于匹配用户消息的正则表达式，留空则使用大模型智能匹配</div>
            </el-form-item>
            <el-form-item label="参数配置">
              <el-table :data="paramList" border size="small" style="margin-bottom: 12px;">
                <el-table-column label="参数名" prop="name" min-width="150">
                  <template slot-scope="scope">
                    <el-input v-model="scope.row.name" placeholder="如：account" />
                  </template>
                </el-table-column>
                <el-table-column label="来源" prop="source" width="120">
                  <template slot-scope="scope">
                    <el-select v-model="scope.row.source" @change="handleParamSourceChange(scope.row)">
                      <el-option label="固定值" value="fixed" />
                      <el-option label="动态提取" value="dynamic" />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column label="数据类型" prop="dataType" width="120">
                  <template slot-scope="scope">
                    <el-select v-if="scope.row.source === 'fixed'" v-model="scope.row.dataType">
                      <el-option label="string" value="string" />
                      <el-option label="number" value="number" />
                      <el-option label="boolean" value="boolean" />
                      <el-option label="json" value="json" />
                    </el-select>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
                <el-table-column label="固定值" prop="fixedValue" min-width="200">
                  <template slot-scope="scope">
                    <el-select v-if="scope.row.source === 'fixed' && scope.row.dataType === 'boolean'" v-model="scope.row.fixedValue" placeholder="请选择">
                      <el-option label="true" value="true" />
                      <el-option label="false" value="false" />
                    </el-select>
                    <el-input
                      v-else
                      v-model="scope.row.fixedValue"
                      :disabled="scope.row.source !== 'fixed'"
                      :placeholder="scope.row.source === 'fixed' ? '请输入固定值' : '由消息动态提取'"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="正则组号" prop="regexGroup" width="110">
                  <template slot-scope="scope">
                    <el-input-number
                      v-if="scope.row.source === 'dynamic'"
                      v-model="scope.row.regexGroup"
                      :min="1"
                      :precision="0"
                      controls-position="right"
                      style="width: 100%"
                    />
                    <span v-else>-</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="60" align="center">
                  <template slot-scope="scope">
                    <el-button type="text" icon="el-icon-delete" style="color: #f56c6c;" @click="removeParam(scope.$index)" />
                  </template>
                </el-table-column>
              </el-table>
              <el-button type="primary" plain size="mini" icon="el-icon-plus" @click="addParam('fixed')">添加固定参数</el-button>
              <el-button type="success" plain size="mini" icon="el-icon-plus" @click="addParam('dynamic')">添加动态参数</el-button>
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
          </el-tab-pane>

          <el-tab-pane label="大模型配置" name="llm">
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
          </el-tab-pane>
        </el-tabs>
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
        <el-divider content-position="left">直接模拟处理</el-divider>
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
      <el-divider content-position="left">真实飞书回调测试</el-divider>
      <el-alert
        title="请谨慎填写真实 chat_id：回调会执行真实工具业务，并尝试向该飞书会话回复。"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 16px;"
      />
      <el-form label-width="100px">
        <el-form-item label="chat_id" required>
          <el-input v-model="feishuChatId" placeholder="请输入飞书会话 chat_id" />
        </el-form-item>
        <el-form-item label="open_id">
          <el-input v-model="feishuOpenId" placeholder="可选，发送人的飞书 open_id" />
        </el-form-item>
        <el-form-item>
          <el-button type="warning" @click="sendFeishuCallback" :loading="feishuCallbackSending">调用真实回调</el-button>
        </el-form-item>
      </el-form>
      <div v-if="feishuCallbackResult" style="background: #f5f7fa; padding: 16px; border-radius: 4px;">
        <div :style="{ color: feishuCallbackResult.success ? '#67c23a' : '#f56c6c', marginBottom: '8px' }">
          <i :class="feishuCallbackResult.success ? 'el-icon-success' : 'el-icon-error'" />
          {{ feishuCallbackResult.message }}
        </div>
        <div style="margin-bottom: 8px; color: #909399; font-size: 13px;">
          <i class="el-icon-time" /> 耗时: {{ feishuCallbackDuration }}s
        </div>
        <div v-if="feishuCallbackResult.success" style="color: #606266; font-size: 13px;">
          回调处理为异步流程，请到“消息记录”页签查看最终执行与回复结果。
        </div>
      </div>
      <div slot="footer">
        <el-button @click="simulateOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listConfig, getConfig, addConfig, updateConfig, delConfig, testRegex, simulateMessage, simulateFeishuCallback } from '@/api/robot/toolConfig'
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
      formActiveTab: 'basic',
      form: {},
      paramList: [],
      rules: {
        configName: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
        //className: [{ required: true, message: '请输入工具类名', trigger: 'blur' }]
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
      simulateMessageText: '豆芽加卡30张吧',
      simulateSending: false,
      simulateResult: null,
      simulateDuration: 0,
      feishuChatId: '',
      feishuOpenId: '',
      feishuCallbackSending: false,
      feishuCallbackResult: null,
      feishuCallbackDuration: 0
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
      if (!this.validateParamList()) return
      this.buildParamsFromList()
      this.$refs['form'].validate(valid => {
        if (!valid) {
          // 校验项位于隐藏页签时，先展示基础信息以便用户处理错误。
          this.formActiveTab = 'basic'
          return
        }
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
      this.formActiveTab = 'basic'
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
    addParam(source) {
      this.paramList.push({
        name: '',
        source,
        dataType: 'string',
        fixedValue: '',
        regexGroup: source === 'dynamic' ? undefined : null
      })
    },
    handleParamSourceChange(param) {
      // 切换来源时清理另一种来源的输入，避免隐藏值被误序列化。
      if (param.source === 'fixed') {
        param.regexGroup = null
      } else {
        param.fixedValue = ''
        param.dataType = 'string'
      }
    },
    removeParam(index) {
      this.paramList.splice(index, 1)
    },
    buildParamsFromList() {
      const regexParamMap = {}
      const toolParams = {}

      this.paramList.forEach(p => {
        const name = p.name.trim()
        if (p.source === 'dynamic') {
          if (p.regexGroup != null) regexParamMap[String(p.regexGroup)] = name
          // 动态参数沿用后端既有占位符契约，捕获值会在执行时覆盖该占位值。
          toolParams[name] = this.getDynamicPlaceholder(name)
        } else {
          toolParams[name] = this.convertFixedValue(p.fixedValue, p.dataType)
        }
      })

      this.form.regexParamMap = JSON.stringify(regexParamMap)
      this.form.toolParams = JSON.stringify(toolParams)
    },
    parseParamsToList() {
      this.paramList = []
      const toolParams = this.parseJsonObject(this.form.toolParams, 'toolParams')
      const regexParamMap = this.parseJsonObject(this.form.regexParamMap, 'regexParamMap')
      const dynamicMappings = Object.entries(regexParamMap).map(([group, name]) => ({
        name: String(name),
        regexGroup: Number(group)
      }))

      // 先按 toolParams 顺序生成完整参数，再补充仅存在于旧正则映射中的动态参数。
      Object.entries(toolParams).forEach(([name, value]) => {
        const mappingIndex = dynamicMappings.findIndex(item => item.name === name)
        const mapping = mappingIndex >= 0 ? dynamicMappings.splice(mappingIndex, 1)[0] : null
        const isDynamic = Boolean(mapping || this.isDynamicPlaceholder(name, value))
        this.paramList.push({
          name,
          source: isDynamic ? 'dynamic' : 'fixed',
          dataType: isDynamic ? 'string' : this.inferDataType(value),
          fixedValue: isDynamic ? '' : this.formatFixedValue(value),
          regexGroup: mapping ? mapping.regexGroup : null
        })
      })
      dynamicMappings.forEach(mapping => {
        this.paramList.push({
          name: mapping.name,
          source: 'dynamic',
          dataType: 'string',
          fixedValue: '',
          regexGroup: mapping.regexGroup
        })
      })
    },
    parseJsonObject(rawValue, fieldName) {
      if (!rawValue) return {}
      try {
        const value = typeof rawValue === 'string' ? JSON.parse(rawValue) : rawValue
        if (value && typeof value === 'object' && !Array.isArray(value)) return value
      } catch (e) {
        console.error(`解析 ${fieldName} 失败`, e)
      }
      return {}
    },
    inferDataType(value) {
      if (typeof value === 'number') return 'number'
      if (typeof value === 'boolean') return 'boolean'
      if (value === null || typeof value === 'object') return 'json'
      return 'string'
    },
    formatFixedValue(value) {
      if (value !== null && typeof value === 'object') return JSON.stringify(value)
      if (value === null) return 'null'
      return String(value)
    },
    getDynamicPlaceholder(name) {
      return '${' + name + '}'
    },
    isDynamicPlaceholder(name, value) {
      return value === '${' + name + '}'
    },
    convertFixedValue(value, dataType) {
      if (dataType === 'number') return Number(value)
      if (dataType === 'boolean') return value === true || value === 'true'
      if (dataType === 'json') return JSON.parse(value)
      return value == null ? '' : String(value)
    },
    validateParamList() {
      const names = new Set()
      const groups = new Set()
      const allowedSources = ['fixed', 'dynamic']
      const allowedTypes = ['string', 'number', 'boolean', 'json']
      for (let i = 0; i < this.paramList.length; i++) {
        const param = this.paramList[i]
        const rowNumber = i + 1
        const name = typeof param.name === 'string' ? param.name.trim() : ''
        let message = ''
        if (!name) message = `第 ${rowNumber} 行参数名不能为空`
        else if (names.has(name)) message = `第 ${rowNumber} 行参数名“${name}”重复`
        else if (!allowedSources.includes(param.source)) message = `第 ${rowNumber} 行参数来源无效`
        else if (!allowedTypes.includes(param.dataType)) message = `第 ${rowNumber} 行数据类型无效`

        if (!message && param.source === 'dynamic') {
          if (param.regexGroup != null && (!Number.isInteger(param.regexGroup) || param.regexGroup < 1)) {
            message = `第 ${rowNumber} 行正则组号必须为正整数`
          } else if (param.regexGroup != null && groups.has(param.regexGroup)) {
            message = `第 ${rowNumber} 行正则组号 ${param.regexGroup} 重复`
          } else if (this.form.regexPattern && param.regexGroup == null) {
            message = `第 ${rowNumber} 行动态参数在正则场景下必须填写正则组号`
          }
        }
        if (!message && param.source === 'fixed') {
          if (param.dataType === 'number' && (param.fixedValue === '' || !Number.isFinite(Number(param.fixedValue)))) {
            message = `第 ${rowNumber} 行固定值不是有效数字`
          } else if (param.dataType === 'boolean' && ![true, false, 'true', 'false'].includes(param.fixedValue)) {
            message = `第 ${rowNumber} 行固定值不是有效布尔值`
          } else if (param.dataType === 'json') {
            try {
              JSON.parse(param.fixedValue)
            } catch (e) {
              message = `第 ${rowNumber} 行固定值不是有效 JSON`
            }
          }
        }
        if (message) {
          this.formActiveTab = 'matching'
          this.$modal.msgWarning(message)
          return false
        }
        names.add(name)
        if (param.source === 'dynamic' && param.regexGroup != null) groups.add(param.regexGroup)
      }
      return true
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
      if (!this.validateParamList()) return
      const regexParamMap = {}
      this.paramList.filter(p => p.source === 'dynamic' && p.regexGroup != null).forEach(p => {
        regexParamMap[String(p.regexGroup)] = p.name.trim()
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
      this.simulateMessageText = '豆芽加卡30张吧'
      this.simulateResult = null
      this.simulateDuration = 0
      this.feishuCallbackResult = null
      this.feishuCallbackDuration = 0
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
    },
    sendFeishuCallback() {
      const messageText = this.simulateMessageText.trim()
      const chatId = this.feishuChatId.trim()
      const openId = this.feishuOpenId.trim()
      if (!messageText) {
        this.$modal.msgWarning('请输入消息内容')
        return
      }
      if (!chatId) {
        this.$modal.msgWarning('请输入 chat_id')
        return
      }

      const timestamp = Date.now()
      const uniqueSuffix = timestamp + '_' + Math.random().toString(36).slice(2, 10)
      // 飞书的 message.content 字段本身是 JSON 字符串，不能直接传入对象。
      const payload = {
        schema: '2.0',
        header: {
          event_id: 'event_' + uniqueSuffix,
          event_type: 'im.message.receive_v1',
          create_time: String(timestamp)
        },
        event: {
          sender: {
            sender_id: openId ? { open_id: openId } : {}
          },
          message: {
            message_id: 'message_' + uniqueSuffix,
            create_time: String(timestamp),
            chat_id: chatId,
            chat_type: 'group',
            message_type: 'text',
            content: JSON.stringify({ text: messageText })
          }
        }
      }

      this.feishuCallbackSending = true
      this.feishuCallbackResult = null
      const startTime = Date.now()
      simulateFeishuCallback(payload)
        .then(() => {
          this.feishuCallbackDuration = ((Date.now() - startTime) / 1000).toFixed(2)
          // HTTP 成功仅代表服务端已接收，工具执行和飞书回复由后端异步完成。
          this.feishuCallbackResult = {
            success: true,
            message: '飞书回调已接收'
          }
          this.$modal.msgSuccess('飞书回调已接收')
        })
        .catch(error => {
          this.feishuCallbackDuration = ((Date.now() - startTime) / 1000).toFixed(2)
          this.feishuCallbackResult = {
            success: false,
            message: '回调失败: ' + (error.message || '未知错误')
          }
        })
        .finally(() => {
          this.feishuCallbackSending = false
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
