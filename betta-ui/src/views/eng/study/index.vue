<template>
  <div class="app-container study-center" v-loading="loading">
    <div class="study-header">
      <div>
        <h2>英语学习中心</h2>
        <p>坚持练习，在文章闯关中巩固句子和单词。</p>
      </div>
      <el-button type="warning" plain icon="el-icon-collection" @click="openWrongWords">
        错词本
      </el-button>
    </div>

    <el-row :gutter="16" class="summary-row">
      <el-col v-for="item in summaryCards" :key="item.label" :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-value">{{ item.value }}</div>
          <div class="summary-label">{{ item.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="study-section" shadow="never">
      <div slot="header" class="section-header">
        <span>选择文章开始闯关</span>
        <el-button type="text" icon="el-icon-refresh" @click="loadData">刷新</el-button>
      </div>
      <el-empty v-if="!loading && articleList.length === 0" description="暂无可学习的英语文章" />
      <el-row v-else :gutter="16">
        <el-col v-for="article in articleList" :key="article.id" :xs="24" :sm="12" :lg="8">
          <div class="article-card">
            <div class="article-info">
              <div class="article-title">{{ article.title || '未命名文章' }}</div>
              <div class="article-group">{{ article.groupName || '未分组' }}</div>
            </div>
            <el-button type="primary" size="small" @click="startChallenge(article.id)">
              开始闯关
            </el-button>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="study-section" shadow="never">
      <div slot="header">最近学习</div>
      <el-empty v-if="!loading && recentRecords.length === 0" description="暂无学习记录，先完成一次文章闯关吧" />
      <el-table v-else :data="recentRecords">
        <el-table-column label="文章" min-width="180">
          <template slot-scope="scope">{{ scope.row.articleTitle || ('文章 #' + scope.row.articleId) }}</template>
        </el-table-column>
        <el-table-column label="得分" prop="score" width="100" align="center" />
        <el-table-column label="答对" width="120" align="center">
          <template slot-scope="scope">
            {{ scope.row.correctCount || 0 }}/{{ scope.row.totalCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="学习时间" min-width="160">
          <template slot-scope="scope">{{ parseTime(scope.row.studyTime || scope.row.createTime) || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template slot-scope="scope">
            <el-button type="text" :disabled="!scope.row.articleId" @click="startChallenge(scope.row.articleId)">
              继续学习
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { listArticle } from '@/api/eng/article'
import { getStudySummary } from '@/api/eng/study'

export default {
  name: 'EngStudy',
  data() {
    return {
      loading: false,
      articleList: [],
      summary: {
        totalScore: 0,
        studyCount: 0,
        completedArticleCount: 0,
        wrongWordCount: 0,
        masteredWrongWordCount: 0,
        recentRecords: []
      }
    }
  },
  computed: {
    summaryCards() {
      return [
        { label: '累计积分', value: this.summary.totalScore || 0 },
        { label: '闯关次数', value: this.summary.studyCount || 0 },
        { label: '完成文章', value: this.summary.completedArticleCount || 0 },
        { label: '待掌握错词', value: this.summary.wrongWordCount || 0 },
        { label: '已掌握错词', value: this.summary.masteredWrongWordCount || 0 }
      ]
    },
    recentRecords() {
      return Array.isArray(this.summary.recentRecords) ? this.summary.recentRecords : []
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    loadData() {
      this.loading = true
      Promise.all([
        getStudySummary(),
        listArticle({ pageNum: 1, pageSize: 1000 })
      ]).then(([summaryResponse, articleResponse]) => {
        this.summary = Object.assign({}, this.summary, summaryResponse.data || {})
        this.articleList = articleResponse.rows || []
      }).finally(() => {
        this.loading = false
      })
    },
    startChallenge(articleId) {
      if (!articleId) return
      this.$router.push('/eng/study/challenge/' + articleId)
    },
    openWrongWords() {
      this.$router.push('/eng/study/wrong')
    }
  }
}
</script>

<style scoped lang="scss">
.study-center {
  .study-header,
  .section-header,
  .article-card {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .study-header {
    margin-bottom: 18px;
    h2 { margin: 0 0 8px; }
    p { margin: 0; color: #909399; }
  }
  .summary-row { margin-bottom: 18px; }
  .summary-card { margin-bottom: 12px; text-align: center; }
  .summary-value { color: #409eff; font-size: 28px; font-weight: 600; }
  .summary-label { margin-top: 8px; color: #606266; }
  .study-section { margin-bottom: 18px; }
  .article-card {
    min-height: 82px;
    margin-bottom: 16px;
    padding: 16px;
    border: 1px solid #ebeef5;
    border-radius: 6px;
  }
  .article-info { min-width: 0; margin-right: 12px; }
  .article-title { overflow: hidden; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
  .article-group { margin-top: 8px; color: #909399; font-size: 12px; }
}
</style>
