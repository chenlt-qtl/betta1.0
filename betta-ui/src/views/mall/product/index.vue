<template>
  <div class="app-container">
    <el-form ref="queryForm" :model="queryParams" size="small" :inline="true" v-show="showSearch">
      <el-form-item label="商品名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入商品名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          v-hasPermi="['mall:product:add']"
          @click="handleAdd"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-upload2"
          size="mini"
          v-hasPermi="['mall:product:add']"
          @click="handleBatchAdd"
        >批量新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-setting"
          size="mini"
          v-hasPermi="['mall:product:rate']"
          @click="handleRateSetting"
        >汇率设置</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          v-hasPermi="['mall:product:edit']"
          @click="handleUpdate"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          v-hasPermi="['mall:product:remove']"
          @click="handleDelete"
        >删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="productList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="商品图片" width="100" align="center">
        <template slot-scope="scope">
          <image-preview :src="firstImage(scope.row.images)" :width="60" :height="60" />
        </template>
      </el-table-column>
      <el-table-column label="商品名称" prop="name" min-width="180" show-overflow-tooltip />
      <el-table-column label="人民币价格" prop="yuanPrice" width="130" align="center">
        <template slot-scope="scope">¥ {{ formatYuanPrice(scope.row.yuanPrice) }}</template>
      </el-table-column>
      <el-table-column label="金币价格" prop="coinPrice" width="120" align="center" />
      <el-table-column label="创建时间" prop="createTime" width="170" align="center" />
      <el-table-column label="操作" width="150" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            type="text"
            size="mini"
            icon="el-icon-edit"
            v-hasPermi="['mall:product:edit']"
            @click="handleUpdate(scope.row)"
          >修改</el-button>
          <el-button
            type="text"
            size="mini"
            icon="el-icon-delete"
            v-hasPermi="['mall:product:remove']"
            @click="handleDelete(scope.row)"
          >删除</el-button>
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

    <el-dialog :title="title" :visible.sync="open" width="900px" append-to-body custom-class="mall-product-form-dialog">
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="16">
            <el-form-item label="商品名称" prop="name">
              <el-input v-model="form.name" maxlength="100" show-word-limit placeholder="商品名称（选填）" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="人民币价格" prop="yuanPrice">
              <el-input-number v-model="form.yuanPrice" :min="0.01" :max="9999999999.99" :step="0.01" :precision="2" controls-position="right" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="商品图片" prop="images">
          <image-upload v-model="form.images" :limit="5" />
          <div class="form-tip">请上传 1–5 张图片，拖动图片可调整展示顺序。</div>
        </el-form-item>
        <el-form-item label="详情(选填)" prop="description">
          <mall-markdown v-model="form.description" height="380px" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="submitting" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog
      title="批量新增商品"
      :visible.sync="batchOpen"
      width="920px"
      append-to-body
      custom-class="mall-batch-dialog"
      @closed="resetBatch"
    >
      <el-upload
        ref="batchUpload"
        class="batch-uploader"
        drag
        multiple
        :action="uploadUrl"
        :headers="uploadHeaders"
        :limit="50"
        :file-list="batchFileList"
        :before-upload="beforeBatchUpload"
        :on-success="handleBatchUploadSuccess"
        :on-error="handleBatchUploadError"
        :on-remove="handleBatchFileRemove"
        :on-exceed="handleBatchExceed"
      >
        <i class="el-icon-upload" />
        <div class="el-upload__text">将图片拖到此处，或<em>点击选择</em></div>
        <div slot="tip" class="el-upload__tip">一次最多 50 张，仅支持 png/jpg/jpeg，单张小于 5MB。</div>
      </el-upload>

      <div v-if="batchItems.length" class="batch-list">
        <div v-for="(item, index) in batchItems" :key="item.uid" class="batch-item">
          <image-preview :src="item.images" :width="72" :height="72" />
          <div class="batch-fields">
            <el-input
              v-model="item.name"
              maxlength="100"
              placeholder="商品名称（选填）"
              clearable
            />
            <el-input-number
              v-model="item.yuanPrice"
              :min="0.01"
              :max="9999999999.99"
              :step="0.01"
              :precision="2"
              controls-position="right"
              placeholder="人民币价格"
            />
            <span class="coin-preview">约 {{ previewCoinPrice(item.yuanPrice) }} 金币</span>
          </div>
          <el-button type="text" class="batch-remove" @click="removeBatchItem(index)">删除</el-button>
        </div>
      </div>
      <el-empty v-else :image-size="72" description="请先选择商品图片" />

      <div slot="footer" class="dialog-footer">
        <el-button
          type="primary"
          :loading="batchSubmitting"
          :disabled="batchUploading > 0 || batchItems.length === 0"
          @click="submitBatch"
        >批量保存</el-button>
        <el-button @click="batchOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="汇率设置" :visible.sync="rateOpen" width="460px" append-to-body custom-class="mall-rate-dialog">
      <el-form ref="rateForm" :model="rateForm" :rules="rateRules" label-width="120px">
        <el-form-item label="1 元人民币 =" prop="rate">
          <el-input-number
            v-model="rateForm.rate"
            :max="922337203"
            :step="0.01"
            controls-position="right"
          />
          <span class="rate-unit">金币</span>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="rateSubmitting" @click="submitRate">保 存</el-button>
        <el-button @click="rateOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import MallMarkdown from '@/components/MallMarkdown'
import {
  addProduct,
  batchAddProducts,
  delProduct,
  getExchangeRate,
  getProduct,
  listProducts,
  updateExchangeRate,
  updateProduct
} from '@/api/mall'
import { getToken } from '@/utils/auth'

export default {
  name: 'MallProduct',
  components: { MallMarkdown },
  data() {
    const validateImages = (rule, value, callback) => {
      const count = this.splitImages(value).length
      if (count < 1 || count > 5) {
        callback(new Error('请上传 1–5 张商品图片'))
      } else {
        callback()
      }
    }
    const validateYuanPrice = (rule, value, callback) => {
      if (!this.isValidYuanPrice(value)) {
        callback(new Error('人民币价格必须大于 0 且最多保留两位小数'))
      } else {
        callback()
      }
    }
    const validateRate = (rule, value, callback) => {
      if (!this.isValidRate(value)) {
        callback(new Error('汇率必须大于 0 且不能超过 922337203'))
      } else {
        callback()
      }
    }
    return {
      loading: false,
      submitting: false,
      showSearch: true,
      ids: [],
      single: true,
      multiple: true,
      total: 0,
      productList: [],
      open: false,
      batchOpen: false,
      batchSubmitting: false,
      // 记录仍在传输的图片，避免上传未完成时提交不完整批次。
      batchUploading: 0,
      batchPendingUids: [],
      batchItems: [],
      batchFileList: [],
      uploadUrl: process.env.VUE_APP_BASE_API + '/common/upload',
      uploadHeaders: { Authorization: 'Bearer ' + getToken() },
      exchangeRate: 1,
      rateOpen: false,
      rateSubmitting: false,
      rateForm: { rate: 1 },
      title: '',
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        name: undefined
      },
      form: {},
      rules: {
        images: [{ required: true, validator: validateImages, trigger: 'change' }],
        yuanPrice: [
          { required: true, message: '请输入人民币价格', trigger: 'change' },
          { validator: validateYuanPrice, trigger: 'change' }
        ]
      },
      rateRules: {
        rate: [
          { required: true, message: '请输入汇率', trigger: 'change' },
          { validator: validateRate, trigger: 'change' }
        ]
      }
    }
  },
  created() {
    this.getList()
    this.loadExchangeRate()
  },
  methods: {
    getList() {
      this.loading = true
      listProducts(this.queryParams).then(response => {
        this.productList = response.rows || []
        this.total = response.total || 0
      }).finally(() => {
        this.loading = false
      })
    },
    reset() {
      this.form = {
        id: undefined,
        name: '',
        images: '',
        description: '',
        yuanPrice: 0.01
      }
      this.resetForm('form')
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length !== 1
      this.multiple = selection.length === 0
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增商品'
    },
    handleBatchAdd() {
      this.resetBatch()
      this.batchOpen = true
      this.loadExchangeRate()
    },
    handleRateSetting() {
      getExchangeRate().then(response => {
        this.exchangeRate = Number(response.data) || 1
        this.rateForm = { rate: this.exchangeRate }
        this.rateOpen = true
        this.$nextTick(() => this.$refs.rateForm && this.$refs.rateForm.clearValidate())
      })
    },
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids[0]
      getProduct(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = '修改商品'
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.submitting = true
        const request = this.form.id ? updateProduct(this.form) : addProduct(this.form)
        request.then(() => {
          this.$message.success(this.form.id ? '修改成功' : '新增成功')
          this.open = false
          this.getList()
        }).finally(() => {
          this.submitting = false
        })
      })
    },
    handleDelete(row) {
      const ids = row.id || this.ids.join(',')
      this.$confirm('是否确认删除选中的商品？删除后历史兑换记录仍会保留。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => delProduct(ids)).then(() => {
        this.$message.success('删除成功')
        this.getList()
      }).catch(() => {})
    },
    cancel() {
      this.open = false
      this.reset()
    },
    splitImages(images) {
      if (!images) return []
      return String(images).split(',').map(item => item.trim()).filter(Boolean)
    },
    firstImage(images) {
      return this.splitImages(images)[0] || ''
    },
    loadExchangeRate() {
      getExchangeRate().then(response => {
        this.exchangeRate = Number(response.data) || 1
      })
    },
    submitRate() {
      this.$refs.rateForm.validate(valid => {
        if (!valid) return
        this.rateSubmitting = true
        updateExchangeRate(this.rateForm.rate).then(() => {
          this.exchangeRate = Number(this.rateForm.rate)
          this.rateOpen = false
          this.$message.success('汇率设置成功')
          this.getList()
        }).finally(() => {
          this.rateSubmitting = false
        })
      })
    },
    beforeBatchUpload(file) {
      const extension = file.name.includes('.') ? file.name.slice(file.name.lastIndexOf('.') + 1).toLowerCase() : ''
      const validType = ['png', 'jpg', 'jpeg'].includes(extension) || ['image/png', 'image/jpeg'].includes(file.type)
      if (!validType) {
        this.$message.error('文件格式不正确，请上传 png/jpg/jpeg 图片')
        return false
      }
      if (file.name.includes(',')) {
        this.$message.error('文件名不能包含英文逗号')
        return false
      }
      if (file.size / 1024 / 1024 >= 5) {
        this.$message.error('单张图片大小不能超过 5MB')
        return false
      }
      this.addBatchPending(file)
      return true
    },
    handleBatchUploadSuccess(response, file) {
      // 已删除或已终止的上传即使迟到返回，也不再生成商品条目。
      if (!this.removeBatchPending(file)) return
      if (response.code !== 200 || !response.fileName) {
        this.$message.error(response.msg || '图片上传失败，请重试')
        this.removeUploadFile(file)
        return
      }
      this.batchItems.push({
        uid: file.uid,
        // 商品名保持为空，由用户按需填写，不从本地文件名推导。
        name: '',
        images: response.fileName,
        description: '',
        yuanPrice: undefined
      })
    },
    handleBatchUploadError(error, file) {
      this.removeBatchPending(file)
      this.$message.error('图片上传失败，请重试')
    },
    handleBatchFileRemove(file) {
      this.removeBatchPending(file)
      const index = this.batchItems.findIndex(item => item.uid === file.uid)
      if (index > -1) this.batchItems.splice(index, 1)
      this.batchFileList = this.batchFileList.filter(item => item.uid !== file.uid)
    },
    handleBatchExceed() {
      this.$message.error('单次批量新增不能超过 50 个商品')
    },
    removeUploadFile(file) {
      if (this.$refs.batchUpload) this.$refs.batchUpload.handleRemove(file)
    },
    removeBatchItem(index) {
      const item = this.batchItems[index]
      const uploadFile = this.$refs.batchUpload && this.$refs.batchUpload.uploadFiles.find(file => file.uid === item.uid)
      if (uploadFile) {
        this.$refs.batchUpload.handleRemove(uploadFile)
      } else {
        this.batchItems.splice(index, 1)
      }
    },
    resetBatch() {
      if (this.$refs.batchUpload) this.$refs.batchUpload.abort()
      this.batchItems = []
      this.batchFileList = []
      this.batchPendingUids = []
      this.batchUploading = 0
      if (this.$refs.batchUpload) this.$refs.batchUpload.clearFiles()
    },
    submitBatch() {
      if (this.batchUploading > 0) {
        this.$message.warning('图片仍在上传，请稍候')
        return
      }
      if (this.batchItems.length < 1 || this.batchItems.length > 50) {
        this.$message.warning('请选择 1–50 张商品图片')
        return
      }
      const invalidIndex = this.batchItems.findIndex(item => !this.isValidYuanPrice(item.yuanPrice))
      if (invalidIndex > -1) {
        this.$message.warning(`请为第 ${invalidIndex + 1} 个商品填写有效的人民币价格`)
        return
      }
      const products = this.batchItems.map(item => ({
        name: item.name ? item.name.trim() : '',
        images: item.images,
        description: '',
        yuanPrice: Number(item.yuanPrice)
      }))
      this.batchSubmitting = true
      batchAddProducts(products).then(() => {
        this.$message.success(`成功新增 ${products.length} 个商品`)
        this.batchOpen = false
        this.getList()
      }).finally(() => {
        this.batchSubmitting = false
      })
    },
    isValidYuanPrice(value) {
      const text = String(value === undefined || value === null ? '' : value)
      return Number(value) > 0 && Number(value) <= 9999999999.99 && /^\d+(\.\d{1,2})?$/.test(text)
    },
    isValidRate(value) {
      return Number.isFinite(Number(value)) && Number(value) > 0 && Number(value) <= 922337203
    },
    addBatchPending(file) {
      if (file && file.uid !== undefined && !this.batchPendingUids.includes(file.uid)) {
        this.batchPendingUids.push(file.uid)
      }
      this.batchUploading = this.batchPendingUids.length
    },
    removeBatchPending(file) {
      const uid = file && file.uid
      const pending = uid !== undefined && this.batchPendingUids.includes(uid)
      if (pending) {
        this.batchPendingUids = this.batchPendingUids.filter(item => item !== uid)
      }
      this.batchUploading = this.batchPendingUids.length
      return pending
    },
    previewCoinPrice(yuanPrice) {
      if (!this.isValidYuanPrice(yuanPrice)) return '--'
      return Math.max(1, Math.round(Number(yuanPrice) * this.exchangeRate))
    },
    formatYuanPrice(value) {
      const price = Number(value)
      return Number.isFinite(price) ? price.toFixed(2) : '--'
    }
  }
}
</script>

<style scoped>
.form-tip {
  color: #909399;
  font-size: 12px;
  line-height: 20px;
}
.batch-uploader {
  margin-bottom: 18px;
}
.batch-uploader ::v-deep .el-upload,
.batch-uploader ::v-deep .el-upload-dragger {
  width: 100%;
}
.batch-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 420px;
  overflow-y: auto;
}
.batch-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}
.batch-fields {
  display: grid;
  flex: 1;
  grid-template-columns: minmax(180px, 1fr) 180px 110px;
  align-items: center;
  gap: 12px;
  min-width: 0;
}
.coin-preview {
  color: #e6a23c;
  white-space: nowrap;
}
.batch-remove {
  flex: none;
  color: #f56c6c;
}
.rate-unit {
  margin-left: 10px;
}
</style>

<style>
.mall-product-form-dialog,
.mall-batch-dialog,
.mall-rate-dialog {
  max-width: calc(100vw - 32px);
}
@media (max-width: 768px) {
  .mall-product-form-dialog,
  .mall-batch-dialog,
  .mall-rate-dialog {
    width: calc(100vw - 24px) !important;
    margin: 12px auto !important;
  }
  .mall-batch-dialog .el-dialog__body,
  .mall-product-form-dialog .el-dialog__body {
    max-height: calc(100vh - 160px);
    overflow-y: auto;
    padding: 14px;
  }
  .mall-batch-dialog .batch-item {
    align-items: flex-start;
    flex-wrap: wrap;
  }
  .mall-batch-dialog .batch-fields {
    grid-template-columns: 1fr;
    width: calc(100% - 86px);
  }
  .mall-batch-dialog .batch-fields .el-input-number {
    width: 100%;
  }
  .mall-product-form-dialog .el-input-number {
    width: 100%;
  }
  .mall-batch-dialog .batch-remove {
    margin-left: 86px;
  }
  .mall-rate-dialog .el-form-item__label {
    float: none;
    display: block;
    width: auto !important;
    text-align: left;
  }
  .mall-rate-dialog .el-form-item__content {
    margin-left: 0 !important;
  }
}
</style>
