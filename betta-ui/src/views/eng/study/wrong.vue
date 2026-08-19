<template>
  <div class="app-container wrong-page">
    <el-card shadow="never">
      <div slot="header" class="wrong-header">
        <div>
          <strong>错词本</strong>
          <span class="wrong-tip">集中复习闯关中答错的单词</span>
        </div>
        <el-button icon="el-icon-arrow-left" @click="backToStudy">返回学习中心</el-button>
      </div>

      <el-form :inline="true" :model="queryParams" size="small">
        <el-form-item label="掌握状态">
          <el-select v-model="queryParams.mastered" clearable placeholder="全部" @change="handleQuery">
            <el-option label="待掌握" :value="false" />
            <el-option label="已掌握" :value="true" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="wrongWords" empty-text="暂无错词，继续保持">
        <el-table-column label="单词" prop="wordName" min-width="140" />
        <el-table-column label="释义" prop="acceptation" min-width="220" show-overflow-tooltip />
        <el-table-column label="来源文章" min-width="180">
          <template slot-scope="scope">{{ scope.row.articleTitle || ('文章 #' + scope.row.articleId) }}</template>
        </el-table-column>
        <el-table-column label="错误次数" prop="wrongCount" width="100" align="center" />
        <el-table-column label="最近错误" min-width="160">
          <template slot-scope="scope">
            {{ parseTime(scope.row.updateTime || scope.row.lastWrongTime) || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.mastered ? 'success' : 'danger'">
              {{ scope.row.mastered ? '已掌握' : '待掌握' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template slot-scope="scope">
            <el-button
              type="text"
              :loading="masteringId === scope.row.id"
              :disabled="scope.row.mastered || masteringId !== null"
              @click="markMastered(scope.row)"
            >
              标记已掌握
            </el-button>
            <el-button type="text" :disabled="!scope.row.articleId" @click="continueStudy(scope.row.articleId)">
              继续学习
            </el-button>
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
    </el-card>
  </div>
</template>

<script>
import { listWrongWords, markWrongWordMastered } from '@/api/eng/study'

export default {
  name: 'EngWrongWord',
  data() {
    return {
      loading: false,
      masteringId: null,
      wrongWords: [],
      total: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        mastered: undefined
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      return listWrongWords(this.queryParams).then(response => {
        this.wrongWords = response.rows || []
        this.total = response.total || 0
      }).finally(() => {
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.queryParams.mastered = undefined
      this.handleQuery()
    },
    markMastered(row) {
      if (!row.id || row.mastered || this.masteringId !== null) return
      this.masteringId = row.id
      markWrongWordMastered(row.id).then(() => {
        this.$modal.msgSuccess('已标记为掌握')
        return this.getList()
      }).finally(() => {
        this.masteringId = null
      })
    },
    continueStudy(articleId) {
      if (!articleId) return
      this.$router.push('/eng/study/challenge/' + articleId)
    },
    backToStudy() {
      this.$router.push('/eng/study/index')
    }
  }
}
</script>

<style scoped lang="scss">
.wrong-page {
  .wrong-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .wrong-tip { margin-left: 12px; color: #909399; font-size: 13px; }
}
</style>
