<template>
  <div class="app-container">
    <el-form ref="queryForm" :model="queryParams" size="small" :inline="true" label-width="68px">
      <el-form-item label="用户名" prop="userName">
        <el-input
          v-model="queryParams.userName"
          placeholder="请输入用户名"
          clearable
          style="width: 200px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="昵称" prop="nickName">
        <el-input
          v-model="queryParams.nickName"
          placeholder="请输入昵称"
          clearable
          style="width: 200px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="scoreList">
      <el-table-column label="用户ID" align="center" prop="userId" width="100" />
      <el-table-column label="用户名" align="center" prop="userName" min-width="140" show-overflow-tooltip />
      <el-table-column label="昵称" align="center" prop="nickName" min-width="140" show-overflow-tooltip>
        <template slot-scope="scope">{{ scope.row.nickName || '-' }}</template>
      </el-table-column>
      <el-table-column label="累计积分" align="center" prop="totalScore" width="110">
        <template slot-scope="scope">{{ scope.row.totalScore || 0 }}</template>
      </el-table-column>
      <el-table-column label="闯关次数" align="center" prop="studyCount" width="110">
        <template slot-scope="scope">{{ getStudyCount(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="最近学习时间" align="center" min-width="170">
        <template slot-scope="scope">{{ formatStudyTime(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="110">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-time"
            @click="handleHistory(scope.row)"
          >查看历史</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog
      :title="historyTitle"
      :visible.sync="historyOpen"
      width="900px"
      custom-class="score-history-dialog"
      append-to-body
      @closed="resetHistory"
    >
      <el-table v-loading="historyLoading" :data="historyList">
        <el-table-column label="类型" width="80" align="center">
          <template slot-scope="scope">{{ recordMode(scope.row) === 'REVIEW' ? '复习' : '新词' }}</template>
        </el-table-column>
        <el-table-column label="文章" min-width="180" show-overflow-tooltip>
          <template slot-scope="scope">
            {{ recordMode(scope.row) === 'REVIEW' ? '全局复习' : (scope.row.articleTitle || ('文章 #' + scope.row.articleId)) }}
            <span v-if="scope.row.levelNo"> · 第 {{ scope.row.levelNo }} 关</span>
          </template>
        </el-table-column>
        <el-table-column label="积分得分" align="center" prop="score" width="100">
          <template slot-scope="scope">{{ scope.row.score || 0 }}</template>
        </el-table-column>
        <el-table-column label="答对/总数" align="center" width="110">
          <template slot-scope="scope">{{ scope.row.correctCount || 0 }}/{{ scope.row.totalCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="星级" align="center" width="100">
          <template slot-scope="scope">
            <span class="history-stars">{{ scope.row.stars || 0 }}★</span>
          </template>
        </el-table-column>
        <el-table-column label="金币奖励" align="center" prop="coinReward" width="100">
          <template slot-scope="scope">{{ scope.row.coinReward || 0 }}</template>
        </el-table-column>
        <el-table-column label="奖励明细" align="center" width="150">
          <template slot-scope="scope">里程碑 {{ scope.row.milestoneCoin || 0 }} / 复习 {{ scope.row.reviewCoin || 0 }}</template>
        </el-table-column>
        <el-table-column label="学习时间" align="center" min-width="170">
          <template slot-scope="scope">{{ parseTime(scope.row.studyTime || scope.row.createTime) || '-' }}</template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="historyTotal > 0"
        :total="historyTotal"
        :page.sync="historyQuery.pageNum"
        :limit.sync="historyQuery.pageSize"
        @pagination="getHistoryList"
      />

      <div slot="footer" class="dialog-footer">
        <el-button @click="historyOpen = false">关 闭</el-button>
      </div>
    </el-dialog>

  </div>
</template>

<script>
import { listAdminScores, listAdminScoreHistory } from '@/api/eng/study'

export default {
  name: 'EngStudyScoreAdmin',
  data() {
    return {
      loading: false,
      listRequestId: 0,
      total: 0,
      scoreList: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userName: null,
        nickName: null
      },
      historyOpen: false,
      historyLoading: false,
      historyRequestId: 0,
      historyTitle: '积分历史',
      historyTotal: 0,
      historyList: [],
      historyQuery: {
        pageNum: 1,
        pageSize: 10,
        userId: null
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询全员积分汇总。 */
    getList() {
      const requestId = ++this.listRequestId
      this.loading = true
      listAdminScores(this.queryParams).then(response => {
        if (requestId !== this.listRequestId) return
        this.scoreList = response.rows || []
        this.total = response.total || 0
      }).finally(() => {
        if (requestId === this.listRequestId) {
          this.loading = false
        }
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
    handleHistory(row) {
      this.historyQuery.pageNum = 1
      this.historyQuery.userId = row.userId
      this.historyTitle = (row.nickName || row.userName || '用户') + '的积分历史'
      this.historyOpen = true
      this.getHistoryList()
    },
    /** 用户切换或翻页时，只查询当前选中用户的积分历史。 */
    getHistoryList() {
      if (!this.historyQuery.userId) return
      const requestId = ++this.historyRequestId
      this.historyLoading = true
      listAdminScoreHistory(this.historyQuery).then(response => {
        // 翻页、切换用户或关闭弹窗后，丢弃已失效请求返回的数据。
        if (requestId !== this.historyRequestId || !this.historyOpen) return
        this.historyList = response.rows || []
        this.historyTotal = response.total || 0
      }).finally(() => {
        if (requestId === this.historyRequestId) {
          this.historyLoading = false
        }
      })
    },
    resetHistory() {
      this.historyRequestId++
      this.historyLoading = false
      this.historyTitle = '积分历史'
      this.historyTotal = 0
      this.historyList = []
      this.historyQuery = {
        pageNum: 1,
        pageSize: 10,
        userId: null
      }
    },
    getStudyCount(row) {
      return row.studyCount == null ? (row.challengeCount || 0) : row.studyCount
    },
    formatStudyTime(row) {
      return this.parseTime(row.latestStudyTime || row.lastStudyTime) || '-'
    },
    recordMode(record) {
      return String(record.studyMode || record.mode || 'NEW').toUpperCase()
    }
  }
}
</script>

<style>
.score-history-dialog {
  max-width: calc(100vw - 32px);
}
</style>
