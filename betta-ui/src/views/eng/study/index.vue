<template>
  <div class="app-container study-center" v-loading="loading">
    <div class="study-header">
      <div><h2>英语学习中心</h2><p>新单词按文章闯关，已学单词在复习区巩固。</p></div>
      <div class="header-actions">
        <el-button type="primary" plain icon="el-icon-setting" @click="openChallengeSettings">题型设置</el-button>
        <el-button type="success" icon="el-icon-refresh" @click="openReview">单词复习</el-button>
        <el-button type="warning" plain icon="el-icon-collection" @click="openWrongWords">错词本</el-button>
      </div>
    </div>

    <el-row :gutter="16" class="summary-row">
      <el-col v-for="item in summaryCards" :key="item.label" :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-value">{{ item.value }}</div><div class="summary-label">{{ item.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="study-section" shadow="never">
      <div slot="header" class="section-header">
        <div><strong>新词闯关</strong><span class="section-tip">每篇文章按单词添加顺序，每 5 个词一关</span></div>
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
            <el-button type="success" size="small" @click="openLevelMap(article.id)">进入地图</el-button>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="study-section" shadow="never">
      <div slot="header">最近学习</div>
      <el-empty v-if="!loading && recentRecords.length === 0" description="暂无学习记录" />
      <el-table v-else :data="recentRecords">
        <el-table-column label="类型" width="90" align="center">
          <template slot-scope="scope">
            <el-tag :type="recordModeTagType(scope.row)" size="small">
              {{ recordModeLabel(scope.row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="文章/关卡" min-width="180">
          <template slot-scope="scope">
            <span v-if="recordMode(scope.row) === 'REVIEW'">全局单词复习</span>
            <span v-else-if="recordMode(scope.row) === 'SPELLING'">全局拼写测试</span>
            <span v-else>{{ scope.row.articleTitle || ('文章 #' + scope.row.articleId) }}<small v-if="scope.row.levelNo"> · 第 {{ scope.row.levelNo }} 关</small></span>
          </template>
        </el-table-column>
        <el-table-column label="星级" width="90" align="center"><template slot-scope="scope"><span class="record-stars">{{ scope.row.stars || 0 }}★</span></template></el-table-column>
        <el-table-column label="金币" width="90" align="center"><template slot-scope="scope">{{ scope.row.coinReward || 0 }}</template></el-table-column>
        <el-table-column label="答对" width="110" align="center"><template slot-scope="scope">{{ scope.row.correctCount || 0 }}/{{ scope.row.totalCount || 0 }}</template></el-table-column>
        <el-table-column label="学习时间" min-width="160"><template slot-scope="scope">{{ parseTime(scope.row.studyTime || scope.row.createTime) || '-' }}</template></el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" :disabled="!scope.row.id" @click="openRecordWords(scope.row)">本次单词</el-button>
            <el-button type="text" @click="continueRecord(scope.row)">继续</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog title="本次测试单词" :visible.sync="recordDialogOpen" width="720px" custom-class="record-word-dialog" append-to-body>
      <el-table v-loading="recordLoading" :data="recordWords" empty-text="暂无单词明细">
        <el-table-column label="单词" prop="wordName" min-width="140" />
        <el-table-column label="星级" width="80" align="center"><template slot-scope="scope">{{ scope.row.stars || 0 }}★</template></el-table-column>
        <el-table-column label="答对/总数" width="110" align="center"><template slot-scope="scope">{{ scope.row.correctCount || 0 }}/{{ scope.row.totalCount || 0 }}</template></el-table-column>
        <el-table-column label="里程碑金币" width="120" align="center" prop="milestoneCoin" />
        <el-table-column label="复习金币" width="100" align="center" prop="reviewCoin" />
      </el-table>
      <div slot="footer"><el-button @click="recordDialogOpen = false">关闭</el-button></div>
    </el-dialog>

    <el-dialog
      title="单词测试题型设置"
      :visible.sync="settingDialogVisible"
      width="440px"
      custom-class="challenge-setting-dialog"
      append-to-body
      :close-on-click-modal="false"
    >
      <div v-loading="settingLoading">
        <p class="setting-description">设置仅对当前登录用户生效，新词学习和单词复习会按已开启的题型出题，拼写测试不受影响。</p>
        <div class="setting-item">
          <div class="setting-item__copy">
            <strong>看词选中文</strong>
            <span class="setting-item__description">根据英文单词选择中文释义</span>
          </div>
          <el-switch v-model="challengeSettings.wordToMeaningEnabled" />
        </div>
        <div class="setting-item">
          <div class="setting-item__copy">
            <strong>看中文选英文</strong>
            <span class="setting-item__description">根据中文释义选择英文单词</span>
          </div>
          <el-switch v-model="challengeSettings.meaningToWordEnabled" />
        </div>
        <div class="setting-item">
          <div class="setting-item__copy">
            <strong>句子挖空选词</strong>
            <span class="setting-item__description">在例句中选择合适的单词</span>
          </div>
          <el-switch v-model="challengeSettings.sentenceClozeEnabled" />
        </div>
        <div class="setting-item">
          <div class="setting-item__copy">
            <strong>单词跟读</strong>
            <span class="setting-item__description">在设备和语音服务可用时进行跟读</span>
          </div>
          <el-switch v-model="challengeSettings.pronunciationEnabled" />
        </div>
        <p class="setting-tip">“看词选中文”和“看中文选英文”至少开启一项。</p>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="settingDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="settingSaving" :disabled="settingLoading" @click="saveChallengeSettings">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { listArticle } from '@/api/eng/article'
import {
  getChallengeSettings,
  getStudyRecordWords,
  getStudySummary,
  updateChallengeSettings
} from '@/api/eng/study'

export default {
  name: 'EngStudy',
  data() {
    return {
      loading: false,
      articleList: [],
      summary: { coinBalance: 0, studyCount: 0, completedArticleCount: 0, wrongWordCount: 0, masteredWrongWordCount: 0, recentRecords: [] },
      recordDialogOpen: false,
      recordLoading: false,
      recordWords: [],
      settingDialogVisible: false,
      settingLoading: false,
      settingSaving: false,
      challengeSettings: {
        wordToMeaningEnabled: true,
        meaningToWordEnabled: true,
        sentenceClozeEnabled: true,
        pronunciationEnabled: true
      }
    }
  },
  computed: {
    summaryCards() {
      return [
        { label: '金币余额', value: this.summary.coinBalance || 0 },
        { label: '测试次数', value: this.summary.studyCount || 0 },
        { label: '完成文章', value: this.summary.completedArticleCount || 0 },
        { label: '待掌握错词', value: this.summary.wrongWordCount || 0 },
        { label: '已掌握错词', value: this.summary.masteredWrongWordCount || 0 }
      ]
    },
    recentRecords() { return Array.isArray(this.summary.recentRecords) ? this.summary.recentRecords : [] }
  },
  created() { this.loadData() },
  methods: {
    loadData() {
      this.loading = true
      Promise.all([getStudySummary(), listArticle({ pageNum: 1, pageSize: 1000 })]).then(([summaryResponse, articleResponse]) => {
        this.summary = Object.assign({}, this.summary, summaryResponse.data || {})
        this.articleList = articleResponse.rows || []
      }).finally(() => { this.loading = false })
    },
    recordMode(record) { return String(record.studyMode || record.mode || 'NEW').toUpperCase() },
    recordModeLabel(record) {
      const labels = { NEW: '新词', REVIEW: '复习', SPELLING: '拼写' }
      return labels[this.recordMode(record)] || '新词'
    },
    recordModeTagType(record) {
      const types = { NEW: 'success', REVIEW: 'warning', SPELLING: 'danger' }
      return types[this.recordMode(record)] || 'success'
    },
    openLevelMap(articleId) { if (articleId) this.$router.push('/eng/study/levels/' + articleId) },
    openReview() { this.$router.push('/eng/study/review') },
    openWrongWords() { this.$router.push('/eng/study/wrong') },
    openChallengeSettings() {
      this.settingDialogVisible = true
      this.settingLoading = true
      getChallengeSettings().then(response => {
        const settings = response.data || {}
        Object.keys(this.challengeSettings).forEach(key => {
          this.challengeSettings[key] = settings[key] !== false
        })
      }).catch(() => {
        this.settingDialogVisible = false
      }).finally(() => {
        this.settingLoading = false
      })
    },
    saveChallengeSettings() {
      if (!this.challengeSettings.wordToMeaningEnabled && !this.challengeSettings.meaningToWordEnabled) {
        this.$modal.msgWarning('“看词选中文”和“看中文选英文”至少开启一项')
        return
      }
      this.settingSaving = true
      updateChallengeSettings({ ...this.challengeSettings }).then(() => {
        this.$modal.msgSuccess('题型设置已保存')
        this.settingDialogVisible = false
      }).finally(() => {
        this.settingSaving = false
      })
    },
    continueRecord(record) {
      if (['REVIEW', 'SPELLING'].includes(this.recordMode(record))) return this.openReview()
      this.openLevelMap(record.articleId)
    },
    openRecordWords(record) {
      if (!record.id) return
      this.recordWords = []
      this.recordDialogOpen = true
      this.recordLoading = true
      getStudyRecordWords(record.id).then(response => {
        this.recordWords = Array.isArray(response.data) ? response.data : (response.rows || [])
      }).finally(() => { this.recordLoading = false })
    }
  }
}
</script>

<style lang="scss">
.study-center {
  .study-header, .section-header, .article-card, .header-actions { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
  .study-header { margin-bottom: 18px; }
  .study-header h2 { margin: 0 0 8px; }
  .study-header p { margin: 0; color: #909399; }
  .summary-row { margin-bottom: 18px; }
  .summary-card { margin-bottom: 12px; text-align: center; }
  .summary-value { color: #409eff; font-size: 28px; font-weight: 600; }
  .summary-label { margin-top: 8px; color: #606266; }
  .study-section { margin-bottom: 18px; }
  .section-tip { margin-left: 10px; color: #909399; font-size: 12px; }
  .article-card { min-height: 82px; margin-bottom: 16px; padding: 16px; border: 1px solid #ebeef5; border-radius: 8px; }
  .article-info { min-width: 0; margin-right: 12px; }
  .article-title { overflow: hidden; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
  .article-group { margin-top: 8px; color: #909399; font-size: 12px; }
  .record-stars { color: #e6a23c; }
}
.challenge-setting-dialog {
  .setting-description { margin: 0 0 16px; color: #606266; line-height: 1.6; }
  .setting-item { display: flex; align-items: center; justify-content: space-between; gap: 20px; padding: 12px 0; border-bottom: 1px solid #ebeef5; }
  .setting-item__copy { min-width: 0; }
  .setting-item strong, .setting-item__description { display: block; }
  .setting-item__description { margin-top: 4px; color: #909399; font-size: 12px; line-height: 1.5; }
  .setting-tip { margin: 14px 0 0; color: #e6a23c; font-size: 12px; }
}
@media (max-width: 600px) {
  .study-center { padding: 12px; overflow-x: hidden; }
  .study-center .study-header { align-items: flex-start; flex-direction: column; }
  .study-center .header-actions { width: 100%; flex-wrap: wrap; }
  .study-center .header-actions .el-button { flex: 1; margin: 0; }
  .study-center .section-tip { display: block; margin: 5px 0 0; }
  .record-word-dialog { max-width: calc(100vw - 24px); }
  .challenge-setting-dialog { width: calc(100% - 24px) !important; margin-top: 8vh !important; }
  .challenge-setting-dialog .setting-item { gap: 12px; }
}
</style>
