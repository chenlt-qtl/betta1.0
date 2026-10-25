<template>
  <div class="channel-config-container">
    <div class="front-base-url-panel" v-loading="frontBaseUrlLoading">
      <div class="front-base-url-content">
        <div class="front-base-url-title">前端访问地址</div>
        <div class="front-base-url-editor">
          <el-input
            v-model.trim="frontBaseUrl"
            clearable
            placeholder="例如：https://betta.icu"
          />
          <el-button
            type="primary"
            :loading="frontBaseUrlSaving"
            @click="handleSaveFrontBaseUrl"
            v-hasPermi="['message:config:edit']"
          >保存</el-button>
        </div>
        <div class="front-base-url-tip">
          地址必须包含 http:// 或 https://；留空保存将使用当前访问地址：{{ currentOrigin }}。请确保该地址可从外网访问。
        </div>
      </div>
    </div>
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
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>
    <el-table v-loading="loading" :data="list">
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
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/修改 对话框 -->
    <el-dialog :title="formTitle" :visible.sync="open" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="渠道类型" prop="channelType">
          <el-select v-model="form.channelType" placeholder="请选择" style="width: 100%">
            <el-option label="飞书" value="FEISHU" />
            <el-option label="企业微信" value="WECHAT" />
          </el-select>
        </el-form-item>
        <el-form-item label="渠道名称" prop="channelName">
          <el-input v-model="form.channelName" placeholder="如：公司飞书机器人" />
        </el-form-item>
        <el-form-item label="AppId" prop="appId" v-if="form.channelType === 'FEISHU'">
          <el-input v-model="form.appId" placeholder="飞书应用 App ID" />
        </el-form-item>
        <el-form-item label="AppSecret" prop="appSecret" v-if="form.channelType === 'FEISHU'">
          <el-input v-model="form.appSecret" type="password" placeholder="飞书应用 App Secret" show-password />
        </el-form-item>
        <el-form-item label="CorpId" prop="corpId" v-if="form.channelType === 'WECHAT'">
          <el-input v-model="form.corpId" placeholder="企业微信 企业ID" />
        </el-form-item>
        <el-form-item label="AgentId" prop="agentId" v-if="form.channelType === 'WECHAT'">
          <el-input v-model="form.agentId" placeholder="企业微信 应用 AgentId" />
        </el-form-item>
        <el-form-item label="Secret" prop="appSecret" v-if="form.channelType === 'WECHAT'">
          <el-input v-model="form.appSecret" type="password" placeholder="企业微信 应用 Secret" show-password />
        </el-form-item>
        <el-form-item label="加密Key" prop="encryptKey">
          <el-input v-model="form.encryptKey" placeholder="回调加密密钥（可选）" />
        </el-form-item>
        <el-form-item label="校验Token" prop="verificationToken">
          <el-input v-model="form.verificationToken" placeholder="回调校验 Token（可选）" />
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
  </div>
</template>

<script>
import {
  listMessageConfig,
  getFrontBaseUrl,
  updateFrontBaseUrl,
  getMessageConfig,
  addMessageConfig,
  updateMessageConfig,
  delMessageConfig
} from '@/api/robot/messageConfig'

export default {
  name: 'ChannelConfig',
  props: {
    showSearch: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      loading: false,
      frontBaseUrlLoading: false,
      frontBaseUrlSaving: false,
      frontBaseUrl: '',
      currentOrigin: '',
      list: [],
      total: 0,
      open: false,
      formTitle: '',
      form: {},
      rules: {
        channelType: [{ required: true, message: '请选择渠道类型', trigger: 'change' }]
      },
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        channelType: undefined,
        status: undefined
      }
    }
  },
  created() {
    this.currentOrigin = window.location.origin
    this.getFrontBaseUrl()
    this.getList()
  },
  methods: {
    /** 查询已保存的前端访问地址 */
    getFrontBaseUrl() {
      this.frontBaseUrlLoading = true
      getFrontBaseUrl().then(response => {
        this.frontBaseUrl = response.data || ''
      }).finally(() => {
        this.frontBaseUrlLoading = false
      })
    },
    /** 留空时由后端保存当前浏览器访问地址 */
    handleSaveFrontBaseUrl() {
      if (this.frontBaseUrlSaving) return
      this.frontBaseUrlSaving = true
      updateFrontBaseUrl({
        frontBaseUrl: this.frontBaseUrl,
        currentOrigin: this.currentOrigin
      }).then(response => {
        this.frontBaseUrl = response.data || ''
        this.$modal.msgSuccess('前端访问地址保存成功')
      }).finally(() => {
        this.frontBaseUrlSaving = false
      })
    },
    /** 查询列表 */
    getList() {
      this.loading = true
      listMessageConfig(this.queryParams).then(response => {
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
      this.formTitle = '新增通道配置'
      this.open = true
    },
    handleUpdate(row) {
      this.resetForm()
      const id = row.id
      getMessageConfig(id).then(response => {
        this.form = response.data
        this.formTitle = '修改通道配置'
        this.open = true
      })
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) return
        if (this.form.id != null) {
          updateMessageConfig(this.form).then(() => {
            this.$modal.msgSuccess('修改成功')
            this.open = false
            this.getList()
          })
        } else {
          addMessageConfig(this.form).then(() => {
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
      this.resetFormRef()
    },
    handleDelete(row) {
      const id = row.id
      this.$modal.confirm('是否确认删除该通道配置？').then(() => {
        return delMessageConfig(id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    resetFormRef() {
      if (this.$refs.form) {
        this.$refs.form.resetFields()
      }
    }
  }
}
</script>

<style scoped>
.front-base-url-panel {
  margin-bottom: 18px;
  padding: 16px;
  border: 1px solid #e6ebf5;
  border-radius: 4px;
  background: #f8f9fb;
}

.front-base-url-title {
  margin-bottom: 10px;
  color: #303133;
  font-weight: 600;
}

.front-base-url-editor {
  display: flex;
  max-width: 720px;
  gap: 10px;
}

.front-base-url-tip {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
  line-height: 20px;
  word-break: break-all;
}

@media (max-width: 600px) {
  .front-base-url-panel {
    padding: 12px;
  }

  .front-base-url-editor {
    flex-direction: column;
  }

  .front-base-url-editor .el-button {
    width: 100%;
    margin-left: 0;
  }
}
</style>
