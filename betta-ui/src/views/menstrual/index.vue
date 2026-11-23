<template>
  <div class="menstrual-page">
    <main class="content">
      <header class="page-header">
        <button v-if="editing" class="back" type="button" aria-label="返回记录" @click="editing = false"><i class="el-icon-arrow-left" /></button>
        <h1>{{ editing ? (form.id ? '编辑记录' : '新增记录') : '美柚' }}</h1>
        <span class="decoration" aria-hidden="true">✿</span>
      </header>
      <section v-if="!editing" v-loading="loading" aria-label="月经记录概览">
        <div class="prediction">
          <i class="el-icon-date" aria-hidden="true" />
          <div><div class="prediction-label">预计开始</div><strong>{{ summary.predictedStartDate || '暂无预测' }}</strong><p>{{ summary.predictedStartDate ? '预测仅供参考' : '记录至少两次后可查看预测' }}</p></div>
        </div>
        <div class="stats">
          <div class="stat"><i class="el-icon-refresh" /><div>平均周期<strong>{{ displayDays(summary.averageCycleDays) }}</strong></div></div>
          <div class="stat"><i class="el-icon-timer" /><div>平均经期<strong>{{ displayDays(summary.averagePeriodDays) }}</strong></div></div>
        </div>
        <h2>历史记录</h2>
        <div v-if="records.length" class="history">
          <article v-for="record in records" :key="record.id" class="record">
            <div class="record-details">
              <strong>{{ record.startDate }} — {{ record.endDate || estimatedEnd(record.startDate) }}</strong>
              <p>经期 {{ periodDays(record) }} 天 <span v-if="!record.endDate" class="tag">按 7 天估算</span></p>
              <p v-if="record.note" class="note">{{ record.note }}</p>
            </div>
            <div class="record-actions"><button type="button" @click="editRecord(record)">编辑</button><button type="button" @click="removeRecord(record)">删除</button></div>
          </article>
        </div>
        <div v-else-if="!loading" class="empty">还没有记录，添加第一次经期吧</div>
        <button class="primary" type="button" @click="newRecord"><i class="el-icon-plus" /> 记录经期</button>
      </section>
      <section v-else class="form-section">
        <el-form ref="form" :model="form" :rules="rules" label-position="top" @submit.native.prevent="saveRecord">
          <el-form-item label="开始日期" prop="startDate"><el-date-picker v-model="form.startDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择开始日期" /></el-form-item>
          <el-form-item label="结束日期（可选）" prop="endDate"><el-date-picker v-model="form.endDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择结束日期" /></el-form-item>
          <div class="hint"><i class="el-icon-info" /> 未填写结束日期时，按 7 天估算</div>
          <el-form-item label="备注" prop="note"><el-input v-model="form.note" type="textarea" :rows="5" maxlength="500" show-word-limit placeholder="可填写本次经期的备注（可选）" /></el-form-item>
          <button class="primary" type="submit" :disabled="saving">{{ saving ? '保存中…' : '保存记录' }}</button>
        </el-form>
      </section>
    </main>
  </div>
</template>
<script>
import { listRecords, getSummary, addRecord, updateRecord, deleteRecord } from '@/api/menstrual'

// 日期按 UTC 日历日计算，避免浏览器时区和夏令时影响天数。
function dateNumber(date) {
  const parts = date.split('-').map(Number)
  return Date.UTC(parts[0], parts[1] - 1, parts[2])
}
export default {
  name: 'Menstrual',
  data() {
    const validateEnd = (rule, value, callback) => {
      if (value && this.form.startDate && value < this.form.startDate) callback(new Error('结束日期不能早于开始日期'))
      else callback()
    }
    return {
      records: [], summary: {}, loading: false, saving: false, editing: false,
      form: { id: null, startDate: '', endDate: null, note: '' },
      rules: { startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }], endDate: [{ validator: validateEnd, trigger: 'change' }] }
    }
  },
  created() { this.refresh() },
  methods: {
    async refresh() {
      this.loading = true
      try {
        const [records, summary] = await Promise.all([listRecords(), getSummary()])
        this.records = records.data || []
        this.summary = summary.data || {}
      } finally { this.loading = false }
    },
    displayDays(value) { return value == null ? '—' : `${value} 天` },
    estimatedEnd(start) { return new Date(dateNumber(start) + 6 * 86400000).toISOString().slice(0, 10) },
    periodDays(record) { return record.endDate ? (dateNumber(record.endDate) - dateNumber(record.startDate)) / 86400000 + 1 : 7 },
    newRecord() { this.form = { id: null, startDate: '', endDate: null, note: '' }; this.editing = true },
    editRecord(record) { this.form = { id: record.id, startDate: record.startDate, endDate: record.endDate, note: record.note || '' }; this.editing = true },
    saveRecord() {
      this.$refs.form.validate(async valid => {
        if (!valid || this.saving) return
        this.saving = true
        try {
          const data = { startDate: this.form.startDate, endDate: this.form.endDate || null, note: this.form.note }
          if (this.form.id) await updateRecord(this.form.id, data)
          else await addRecord(data)
          this.editing = false
          this.$message.success('记录已保存')
          await this.refresh()
        } finally { this.saving = false }
      })
    },
    async removeRecord(record) {
      try { await this.$confirm('确定删除这条记录吗？', '删除记录', { type: 'warning' }) } catch (e) { return }
      await deleteRecord(record.id)
      this.$message.success('记录已删除')
      await this.refresh()
    }
  }
}
</script>
<style lang="scss" scoped>
.menstrual-page { min-height: calc(100vh - 84px); padding: 28px 20px 48px; background: #fff8f1; color: #123d42; }
.content { max-width: 680px; margin: 0 auto; }
.page-header { position: relative; display: flex; align-items: center; min-height: 76px; overflow: hidden; margin-bottom: 16px; }
.page-header h1 { position: relative; z-index: 1; margin: 0; font-size: 30px; }
.decoration { position: absolute; right: 10px; top: -28px; color: #b5e9df; font-size: 110px; line-height: 1; }
.back { position: relative; z-index: 1; border: 0; margin-right: 12px; padding: 6px; background: transparent; color: #123d42; font-size: 22px; cursor: pointer; }
.prediction { display: flex; align-items: center; gap: 26px; padding: 28px; border-radius: 18px; background: #ddf5ef; }
.prediction > i { color: #0aa68f; font-size: 48px; }
.prediction-label { font-size: 18px; font-weight: 700; }
.prediction strong { display: block; margin: 6px 0; font-size: 36px; line-height: 1.2; overflow-wrap: anywhere; }
.prediction p, .record p { margin: 5px 0 0; color: #61767a; }
.stats { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; margin-top: 16px; }
.stat { display: flex; align-items: center; gap: 14px; padding: 20px; border: 1px solid #c7e7e2; border-radius: 16px; background: #fff; font-weight: 700; }
.stat i { color: #0aa68f; font-size: 26px; }
.stat strong { display: block; margin-top: 6px; font-size: 25px; }
h2 { margin: 28px 0 14px; font-size: 22px; }
.history { padding: 0 18px; border: 1px solid #c7e7e2; border-radius: 16px; background: #fff; }
.record { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 16px 0; }
.record + .record { border-top: 1px solid #e5efed; }
.record-details { min-width: 0; overflow-wrap: anywhere; }
.record-actions { display: flex; gap: 8px; flex-shrink: 0; }
.record-actions button { border: 0; padding: 6px; background: transparent; color: #087e78; cursor: pointer; }
.note { white-space: pre-wrap; }
.tag { display: inline-block; margin-left: 6px; padding: 2px 8px; border-radius: 12px; background: #ddf5ef; color: #087e78; font-size: 12px; }
.empty { padding: 28px; border: 1px solid #c7e7e2; border-radius: 16px; text-align: center; color: #61767a; background: #fff; }
.primary { width: 100%; margin-top: 28px; padding: 16px; border: 0; border-radius: 12px; background: #ff7414; color: white; font-size: 18px; font-weight: 700; cursor: pointer; }
.primary:disabled { opacity: .6; cursor: not-allowed; }
.form-section { padding-top: 12px; }
.form-section ::v-deep .el-form-item__label { color: #123d42; font-weight: 700; font-size: 16px; }
.form-section ::v-deep .el-date-editor { width: 100%; }
.form-section ::v-deep .el-input__inner, .form-section ::v-deep .el-textarea__inner { border-color: #bcdeda; border-radius: 10px; }
.hint { margin: -10px 0 22px; padding: 12px; border-radius: 10px; background: #ddf5ef; color: #136b68; }
@media (max-width: 700px) {
  .menstrual-page { min-height: calc(100vh - 50px); padding: 16px 14px 28px; }
  .page-header { min-height: 64px; }
  .page-header h1 { font-size: 24px; }
  .prediction { gap: 16px; padding: 22px 18px; }
  .prediction strong { font-size: 28px; }
  .stats { gap: 10px; }
  .stat { gap: 8px; padding: 14px 10px; }
  .stat strong { font-size: 21px; }
  .record { align-items: flex-start; flex-wrap: wrap; }
  .record-actions { margin-left: auto; }
}
</style>
