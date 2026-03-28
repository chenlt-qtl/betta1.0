<template>
  <div class="app-container">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="通道配置" name="config">
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
          <el-form-item label="渠道" prop="channelType">
            <el-select v-model="queryParams.channelType" placeholder="请选择" clearable style="width: 120px">
              <el-option label="飞书" value="FEISHU" />
              <el-option label="企业微信" value="WECHAT" />
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
            <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['message:config:add']">新增</el-button>
          </el-col>
          <right-toolbar :showSearch.sync="showSearch" @queryTable="getConfigList" />
        </el-row>
        <el-table v-loading="configLoading" :data="configList">
          <el-table-column label="ID" align="center" prop="id" width="60" />
          <el-table-column label="渠道" align="center" prop="channelType" width="90">
            <template slot-scope="scope">
              {{ scope.row.channelType === 'FEISHU' ? '飞书' : scope.row.channelType === 'WECHAT' ? '企业微信' : scope.row.channelType }}
            </template>
          </el-table-column>
          <el-table-column label="名称" align="center" prop="channelName" :show-overflow-tooltip="true" />
          <el-table-column label="AppId/CorpId" align="center" prop="appId" :show-overflow-tooltip="true" />
          <el-table-column label="状态" align="center" prop="status" width="80">
            <template slot-scope="scope">
              <el-tag :type="scope.row.status === '0' ? 'success' : 'info'">{{ scope.row.status === '0' ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
          <el-table-column label="操作" align="center" width="140" class-name="small-padding fixed-width">
            <template slot-scope="scope">
              <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['message:config:edit']">修改</el-button>
              <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['message:config:remove']">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="configTotal > 0" :total="configTotal" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getConfigList" />
      </el-tab-pane>
      <el-tab-pane label="消息记录" name="record">
        <el-form :model="recordQueryParams" ref="recordQueryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
          <el-form-item label="渠道" prop="channelType">
            <el-select v-model="recordQueryParams.channelType" placeholder="请选择" clearable style="width: 120px">
              <el-option label="飞书" value="FEISHU" />
              <el-option label="企业微信" value="WECHAT" />
            </el-select>
          </el-form-item>
          <el-form-item label="方向" prop="direction">
            <el-select v-model="recordQueryParams.direction" placeholder="请选择" clearable style="width: 100px">
              <el-option label="接收" value="RECEIVE" />
              <el-option label="发送" value="SEND" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="recordQueryParams.status" placeholder="请选择" clearable style="width: 100px">
              <el-option label="处理中" value="PENDING" />
              <el-option label="成功" value="SUCCESS" />
              <el-option label="失败" value="FAIL" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" @click="handleRecordQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="mini" @click="resetRecordQuery">重置</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="recordLoading" :data="recordList">
          <el-table-column label="ID" align="center" prop="id" width="60" />
          <el-table-column label="渠道" align="center" prop="channelType" width="80" />
          <el-table-column label="方向" align="center" prop="direction" width="70">
            <template slot-scope="scope">
              {{ scope.row.direction === 'RECEIVE' ? '接收' : '发送' }}
            </template>
          </el-table-column>
          <el-table-column label="内容" align="left" prop="content" :show-overflow-tooltip="true" min-width="160" />
          <el-table-column label="回复" align="left" prop="replyContent" :show-overflow-tooltip="true" min-width="160" />
          <el-table-column label="意图" align="center" prop="parsedIntent" width="90" />
          <el-table-column label="状态" align="center" prop="status" width="80">
            <template slot-scope="scope">
              <el-tag :type="scope.row.status === 'SUCCESS' ? 'success' : scope.row.status === 'FAIL' ? 'danger' : 'info'">
                {{ scope.row.status === 'PENDING' ? '处理中' : scope.row.status === 'SUCCESS' ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="时间" align="center" prop="createTime" width="160" />
        </el-table>
        <pagination v-show="recordTotal > 0" :total="recordTotal" :page.sync="recordQueryParams.pageNum" :limit.sync="recordQueryParams.pageSize" @pagination="getRecordList" />
      </el-tab-pane>
      <el-tab-pane label="大模型配置" name="llm">
        <el-form :model="llmQueryParams" ref="llmQueryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
          <el-form-item label="配置名称" prop="configName">
            <el-input v-model="llmQueryParams.configName" placeholder="请输入配置名称" clearable style="width: 180px" @keyup.enter.native="handleLlmQuery" />
          </el-form-item>
          <el-form-item label="提供商" prop="provider">
            <el-select v-model="llmQueryParams.provider" placeholder="请选择" clearable style="width: 120px">
              <el-option label="OpenAI" value="OPENAI" />
              <el-option label="Anthropic" value="ANTHROPIC" />
              <el-option label="百度" value="BAIDU" />
              <el-option label="阿里云" value="ALIYUN" />
              <el-option label="本地部署" value="LOCAL" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="llmQueryParams.status" placeholder="请选择" clearable style="width: 100px">
              <el-option label="启用" value="0" />
              <el-option label="停用" value="1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" @click="handleLlmQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="mini" @click="resetLlmQuery">重置</el-button>
          </el-form-item>
        </el-form>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleLlmAdd" v-hasPermi="['message:llm:add']">新增</el-button>
          </el-col>
          <right-toolbar :showSearch.sync="showSearch" @queryTable="getLlmList" />
        </el-row>
        <el-table v-loading="llmLoading" :data="llmList">
          <el-table-column label="ID" align="center" prop="id" width="60" />
          <el-table-column label="配置名称" align="center" prop="configName" :show-overflow-tooltip="true" />
          <el-table-column label="提供商" align="center" prop="provider" width="100">
            <template slot-scope="scope">
              {{ getLlmProviderName(scope.row.provider) }}
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
              <el-button size="mini" type="text" icon="el-icon-edit" @click="handleLlmUpdate(scope.row)" v-hasPermi="['message:llm:edit']">修改</el-button>
              <el-button size="mini" type="text" icon="el-icon-delete" @click="handleLlmDelete(scope.row)" v-hasPermi="['message:llm:remove']">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="llmTotal > 0" :total="llmTotal" :page.sync="llmQueryParams.pageNum" :limit.sync="llmQueryParams.pageSize" @pagination="getLlmList" />
      </el-tab-pane>
      <el-tab-pane label="API配置" name="api">
        <el-form :model="apiQueryParams" ref="apiQueryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
          <el-form-item label="配置名称" prop="configName">
            <el-input v-model="apiQueryParams.configName" placeholder="请输入配置名称" clearable style="width: 180px" @keyup.enter.native="handleApiQuery" />
          </el-form-item>
          <el-form-item label="关键词" prop="keywords">
            <el-input v-model="apiQueryParams.keywords" placeholder="请输入关键词" clearable style="width: 180px" @keyup.enter.native="handleApiQuery" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="apiQueryParams.status" placeholder="请选择" clearable style="width: 100px">
              <el-option label="启用" value="0" />
              <el-option label="停用" value="1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" @click="handleApiQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="mini" @click="resetApiQuery">重置</el-button>
          </el-form-item>
        </el-form>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleApiAdd" v-hasPermi="['message:api:add']">新增</el-button>
          </el-col>
          <right-toolbar :showSearch.sync="showSearch" @queryTable="getApiList" />
        </el-row>
        <el-table v-loading="apiLoading" :data="apiList">
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
              <el-button size="mini" type="text" icon="el-icon-edit" @click="handleApiUpdate(scope.row)" v-hasPermi="['message:api:edit']">修改</el-button>
              <el-button size="mini" type="text" icon="el-icon-delete" @click="handleApiDelete(scope.row)" v-hasPermi="['message:api:remove']">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="apiTotal > 0" :total="apiTotal" :page.sync="apiQueryParams.pageNum" :limit.sync="apiQueryParams.pageSize" @pagination="getApiList" />
      </el-tab-pane>
    </el-tabs>

    <!-- 通道配置 新增/修改 对话框 -->
    <el-dialog :title="configFormTitle" :visible.sync="configOpen" width="560px" append-to-body>
      <el-form ref="configForm" :model="configForm" :rules="configRules" label-width="120px">
        <el-form-item label="渠道类型" prop="channelType">
          <el-select v-model="configForm.channelType" placeholder="请选择" style="width: 100%">
            <el-option label="飞书" value="FEISHU" />
            <el-option label="企业微信" value="WECHAT" />
          </el-select>
        </el-form-item>
        <el-form-item label="渠道名称" prop="channelName">
          <el-input v-model="configForm.channelName" placeholder="如：公司飞书机器人" />
        </el-form-item>
        <el-form-item label="AppId" prop="appId" v-if="configForm.channelType === 'FEISHU'">
          <el-input v-model="configForm.appId" placeholder="飞书应用 App ID" />
        </el-form-item>
        <el-form-item label="AppSecret" prop="appSecret" v-if="configForm.channelType === 'FEISHU'">
          <el-input v-model="configForm.appSecret" type="password" placeholder="飞书应用 App Secret" show-password />
        </el-form-item>
        <el-form-item label="CorpId" prop="corpId" v-if="configForm.channelType === 'WECHAT'">
          <el-input v-model="configForm.corpId" placeholder="企业微信 企业ID" />
        </el-form-item>
        <el-form-item label="AgentId" prop="agentId" v-if="configForm.channelType === 'WECHAT'">
          <el-input v-model="configForm.agentId" placeholder="企业微信 应用 AgentId" />
        </el-form-item>
        <el-form-item label="Secret" prop="appSecret" v-if="configForm.channelType === 'WECHAT'">
          <el-input v-model="configForm.appSecret" type="password" placeholder="企业微信 应用 Secret" show-password />
        </el-form-item>
        <el-form-item label="加密Key" prop="encryptKey">
          <el-input v-model="configForm.encryptKey" placeholder="回调加密密钥（可选）" />
        </el-form-item>
        <el-form-item label="校验Token" prop="verificationToken">
          <el-input v-model="configForm.verificationToken" placeholder="回调校验 Token（可选）" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="configForm.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="configForm.remark" type="textarea" placeholder="可选" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitConfigForm">确 定</el-button>
        <el-button @click="configOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- LLM配置 新增/修改 对话框 -->
    <el-dialog :title="llmFormTitle" :visible.sync="llmOpen" width="600px" append-to-body>
      <el-form ref="llmForm" :model="llmForm" :rules="llmRules" label-width="100px">
        <el-form-item label="配置名称" prop="configName">
          <el-input v-model="llmForm.configName" placeholder="请输入配置名称" />
        </el-form-item>
        <el-form-item label="提供商" prop="provider">
          <el-select v-model="llmForm.provider" placeholder="请选择提供商" style="width: 100%">
            <el-option label="OpenAI" value="OPENAI" />
            <el-option label="Anthropic (Claude)" value="ANTHROPIC" />
            <el-option label="百度 (文心一言)" value="BAIDU" />
            <el-option label="阿里云 (通义千问)" value="ALIYUN" />
            <el-option label="本地部署" value="LOCAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="API Key" prop="apiKey">
          <el-input v-model="llmForm.apiKey" type="password" placeholder="请输入API Key" show-password />
        </el-form-item>
        <el-form-item label="API端点" prop="apiEndpoint">
          <el-input v-model="llmForm.apiEndpoint" placeholder="可选，本地部署时填写" />
        </el-form-item>
        <el-form-item label="模型名称" prop="modelName">
          <el-input v-model="llmForm.modelName" placeholder="如：gpt-4、gpt-3.5-turbo、claude-3-opus等" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="温度" prop="temperature">
              <el-input-number v-model="llmForm.temperature" :min="0" :max="1" :step="0.1" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Top P" prop="topP">
              <el-input-number v-model="llmForm.topP" :min="0" :max="1" :step="0.1" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="最大Token" prop="maxTokens">
              <el-input-number v-model="llmForm.maxTokens" :min="100" :max="32000" :step="100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="超时(秒)" prop="timeout">
              <el-input-number v-model="llmForm.timeout" :min="5" :max="300" :step="5" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="llmForm.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="llmForm.remark" type="textarea" placeholder="可选" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitLlmForm">确 定</el-button>
        <el-button @click="llmOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- API配置 新增/修改 对话框 -->
    <el-dialog :title="apiFormTitle" :visible.sync="apiOpen" width="700px" append-to-body>
      <el-form ref="apiForm" :model="apiForm" :rules="apiRules" label-width="100px">
        <el-form-item label="配置名称" prop="configName">
          <el-input v-model="apiForm.configName" placeholder="请输入配置名称" />
        </el-form-item>
        <el-form-item label="API地址" prop="apiUrl">
          <el-input v-model="apiForm.apiUrl" placeholder="请输入API地址，如：http://xxx/api" />
        </el-form-item>
        <el-form-item label="API参数" prop="apiParams">
          <el-input v-model="apiForm.apiParams" type="textarea" :rows="3" placeholder='JSON格式，如：{"name":"${name}","age":"${age}"}' />
          <div class="el-form-item__tip">使用 ${参数名} 引用从消息中提取的参数</div>
        </el-form-item>
        <el-form-item label="关键词" prop="keywords">
          <el-input v-model="apiForm.keywords" placeholder="多个关键词用逗号分隔，如：加卡,打卡" />
          <div class="el-form-item__tip">消息中包含任一关键词则命中；留空则所有消息都命中</div>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="apiForm.priority" :min="0" :max="100" :step="1" style="width: 200px" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="apiForm.description" type="textarea" :rows="2" placeholder="用于大模型选择最合适的配置" />
        </el-form-item>
        <el-form-item label="提示词" prop="prompt">
          <el-input v-model="apiForm.prompt" type="textarea" :rows="3" placeholder="用于从消息中提取参数的提示词" />
          <div class="el-form-item__tip">如：从消息中提取姓名和卡号，输出JSON</div>
        </el-form-item>
        <el-form-item label="大模型" prop="llmConfigId">
          <el-select v-model="apiForm.llmConfigId" placeholder="请选择大模型配置" clearable style="width: 100%">
            <el-option v-for="item in llmOptions" :key="item.id" :label="item.configName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="apiForm.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="apiForm.remark" type="textarea" placeholder="可选" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitApiForm">确 定</el-button>
        <el-button @click="apiOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listMessageConfig, getMessageConfig, addMessageConfig, updateMessageConfig, delMessageConfig } from '@/api/message/config'
import { listMessageRecord } from '@/api/message/record'
import { listLlmConfig, getLlmConfig, addLlmConfig, updateLlmConfig, delLlmConfig } from '@/api/message/llm'
import { listApiConfig, getApiConfig, addApiConfig, updateApiConfig, delApiConfig } from '@/api/message/api'

export default {
  name: 'MessageConfig',
  data() {
    return {
      activeTab: 'config',
      showSearch: true,
      configLoading: false,
      configList: [],
      configTotal: 0,
      configOpen: false,
      configFormTitle: '',
      configForm: {},
      configRules: {
        channelType: [{ required: true, message: '请选择渠道类型', trigger: 'change' }]
      },
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        channelType: undefined,
        status: undefined
      },
      recordLoading: false,
      recordList: [],
      recordTotal: 0,
      recordQueryParams: {
        pageNum: 1,
        pageSize: 10,
        channelType: undefined,
        direction: undefined,
        status: undefined
      },
      llmLoading: false,
      llmList: [],
      llmTotal: 0,
      llmOpen: false,
      llmFormTitle: '',
      llmForm: {},
      llmRules: {
        configName: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
        provider: [{ required: true, message: '请选择提供商', trigger: 'change' }],
        apiKey: [{ required: true, message: '请输入API Key', trigger: 'blur' }],
        modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }]
      },
      llmQueryParams: {
        pageNum: 1,
        pageSize: 10,
        configName: undefined,
        provider: undefined,
        status: undefined
      },
      apiLoading: false,
      apiList: [],
      apiTotal: 0,
      apiOpen: false,
      apiFormTitle: '',
      apiForm: {},
      apiRules: {
        configName: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
        apiUrl: [{ required: true, message: '请输入API地址', trigger: 'blur' }]
      },
      apiQueryParams: {
        pageNum: 1,
        pageSize: 10,
        configName: undefined,
        keywords: undefined,
        status: undefined
      },
      llmOptions: []
    }
  },
  created() {
    this.getConfigList()
    this.getLlmOptions()
  },
  methods: {
    /** 查询通道配置列表 */
    getConfigList() {
      this.configLoading = true
      listMessageConfig(this.queryParams).then(response => {
        this.configList = response.rows
        this.configTotal = response.total
        this.configLoading = false
      }).catch(() => {
        this.configLoading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getConfigList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleAdd() {
      this.resetConfigForm()
      this.configFormTitle = '新增通道配置'
      this.configOpen = true
    },
    handleUpdate(row) {
      this.resetConfigForm()
      const id = row.id
      getMessageConfig(id).then(response => {
        this.configForm = response.data
        this.configFormTitle = '修改通道配置'
        this.configOpen = true
      })
    },
    submitConfigForm() {
      this.$refs['configForm'].validate(valid => {
        if (!valid) return
        if (this.configForm.id != null) {
          updateMessageConfig(this.configForm).then(() => {
            this.$modal.msgSuccess('修改成功')
            this.configOpen = false
            this.getConfigList()
          })
        } else {
          addMessageConfig(this.configForm).then(() => {
            this.$modal.msgSuccess('新增成功')
            this.configOpen = false
            this.getConfigList()
          })
        }
      })
    },
    resetConfigForm() {
      this.configForm = {
        id: undefined,
        channelType: 'FEISHU',
        channelName: undefined,
        appId: undefined,
        appSecret: undefined,
        encryptKey: undefined,
        verificationToken: undefined,
        agentId: undefined,
        corpId: undefined,
        status: '0',
        remark: undefined
      }
      this.resetForm('configForm')
    },
    handleDelete(row) {
      const id = row.id
      this.$modal.confirm('是否确认删除该通道配置？').then(() => {
        return delMessageConfig(id)
      }).then(() => {
        this.getConfigList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    /** 消息记录 */
    getRecordList() {
      if (this.activeTab !== 'record') return
      this.recordLoading = true
      listMessageRecord(this.recordQueryParams).then(response => {
        this.recordList = response.rows
        this.recordTotal = response.total
        this.recordLoading = false
      }).catch(() => {
        this.recordLoading = false
      })
    },
    handleRecordQuery() {
      this.recordQueryParams.pageNum = 1
      this.getRecordList()
    },
    resetRecordQuery() {
      this.resetForm('recordQueryForm')
      this.handleRecordQuery()
    },
    /** LLM配置 */
    getLlmList() {
      if (this.activeTab !== 'llm') return
      this.llmLoading = true
      listLlmConfig(this.llmQueryParams).then(response => {
        this.llmList = response.rows
        this.llmTotal = response.total
        this.llmLoading = false
      }).catch(() => {
        this.llmLoading = false
      })
    },
    handleLlmQuery() {
      this.llmQueryParams.pageNum = 1
      this.getLlmList()
    },
    resetLlmQuery() {
      this.resetForm('llmQueryForm')
      this.handleLlmQuery()
    },
    handleLlmAdd() {
      this.resetLlmForm()
      this.llmFormTitle = '新增大模型配置'
      this.llmOpen = true
    },
    handleLlmUpdate(row) {
      this.resetLlmForm()
      const id = row.id
      getLlmConfig(id).then(response => {
        this.llmForm = response.data
        this.llmFormTitle = '修改大模型配置'
        this.llmOpen = true
      })
    },
    submitLlmForm() {
      this.$refs['llmForm'].validate(valid => {
        if (!valid) return
        if (this.llmForm.id != null) {
          updateLlmConfig(this.llmForm).then(() => {
            this.$modal.msgSuccess('修改成功')
            this.llmOpen = false
            this.getLlmList()
          })
        } else {
          addLlmConfig(this.llmForm).then(() => {
            this.$modal.msgSuccess('新增成功')
            this.llmOpen = false
            this.getLlmList()
          })
        }
      })
    },
    resetLlmForm() {
      this.llmForm = {
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
      this.resetForm('llmForm')
    },
    handleLlmDelete(row) {
      const id = row.id
      this.$modal.confirm('是否确认删除该大模型配置？').then(() => {
        return delLlmConfig(id)
      }).then(() => {
        this.getLlmList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    getLlmProviderName(provider) {
      const providerMap = {
        'OPENAI': 'OpenAI',
        'ANTHROPIC': 'Anthropic',
        'BAIDU': '百度',
        'ALIYUN': '阿里云',
        'LOCAL': '本地部署'
      }
      return providerMap[provider] || provider
    },
    getLlmOptions() {
      listLlmConfig({ status: '0', pageNum: 1, pageSize: 100 }).then(response => {
        this.llmOptions = response.rows
      })
    },
    /** API配置 */
    getApiList() {
      if (this.activeTab !== 'api') return
      this.apiLoading = true
      listApiConfig(this.apiQueryParams).then(response => {
        this.apiList = response.rows
        this.apiTotal = response.total
        this.apiLoading = false
      }).catch(() => {
        this.apiLoading = false
      })
    },
    handleApiQuery() {
      this.apiQueryParams.pageNum = 1
      this.getApiList()
    },
    resetApiQuery() {
      this.resetForm('apiQueryForm')
      this.handleApiQuery()
    },
    handleApiAdd() {
      this.resetApiForm()
      this.apiFormTitle = '新增API配置'
      this.apiOpen = true
      this.getLlmOptions()
    },
    handleApiUpdate(row) {
      this.resetApiForm()
      const id = row.id
      getApiConfig(id).then(response => {
        this.apiForm = response.data
        this.apiFormTitle = '修改API配置'
        this.apiOpen = true
      })
      this.getLlmOptions()
    },
    submitApiForm() {
      this.$refs['apiForm'].validate(valid => {
        if (!valid) return
        if (this.apiForm.id != null) {
          updateApiConfig(this.apiForm).then(() => {
            this.$modal.msgSuccess('修改成功')
            this.apiOpen = false
            this.getApiList()
          })
        } else {
          addApiConfig(this.apiForm).then(() => {
            this.$modal.msgSuccess('新增成功')
            this.apiOpen = false
            this.getApiList()
          })
        }
      })
    },
    resetApiForm() {
      this.apiForm = {
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
      this.resetForm('apiForm')
    },
    handleApiDelete(row) {
      const id = row.id
      this.$modal.confirm('是否确认删除该API配置？').then(() => {
        return delApiConfig(id)
      }).then(() => {
        this.getApiList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  },
  watch: {
    activeTab(val) {
      if (val === 'record' && this.recordList.length === 0) {
        this.getRecordList()
      } else if (val === 'llm' && this.llmList.length === 0) {
        this.getLlmList()
      } else if (val === 'api' && this.apiList.length === 0) {
        this.getApiList()
      }
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
