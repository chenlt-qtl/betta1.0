<template>
  <div class="message-record-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="渠道" prop="channelType">
        <el-select v-model="queryParams.channelType" placeholder="请选择" clearable style="width: 120px">
          <el-option label="飞书" value="FEISHU" />
          <el-option label="企业微信" value="WECHAT" />
        </el-select>
      </el-form-item>
      <el-form-item label="方向" prop="direction">
        <el-select v-model="queryParams.direction" placeholder="请选择" clearable style="width: 100px">
          <el-option label="接收" value="RECEIVE" />
          <el-option label="发送" value="SEND" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width: 100px">
          <el-option label="处理中" value="PENDING" />
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAIL" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="list">
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
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import { listMessageRecord } from '@/api/robot/record'

export default {
  name: 'MessageRecord',
  props: {
    showSearch: {
      type: Boolean,
      default: true
    },
    activeTab: {
      type: String,
      default: 'record'
    }
  },
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        channelType: undefined,
        direction: undefined,
        status: undefined
      }
    }
  },
  watch: {
    activeTab(val) {
      if (val === 'record' && this.list.length === 0) {
        this.getList()
      }
    }
  },
  methods: {
    /** 查询列表 */
    getList() {
      if (this.activeTab !== 'record') return
      this.loading = true
      listMessageRecord(this.queryParams).then(response => {
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
    }
  }
}
</script>
