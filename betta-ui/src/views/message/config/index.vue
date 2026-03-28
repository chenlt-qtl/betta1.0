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
  </div>
</template>

<script>
import { listMessageConfig, getMessageConfig, addMessageConfig, updateMessageConfig, delMessageConfig } from '@/api/message/config'
import { listMessageRecord } from '@/api/message/record'

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
      }
    }
  },
  created() {
    this.getConfigList()
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
    }
  },
  watch: {
    activeTab(val) {
      if (val === 'record' && this.recordList.length === 0) {
        this.getRecordList()
      }
    }
  }
}
</script>
