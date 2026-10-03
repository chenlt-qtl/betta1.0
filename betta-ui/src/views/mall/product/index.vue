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

    <el-dialog :title="title" :visible.sync="open" width="900px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="20">
          <el-col :span="16">
            <el-form-item label="商品名称" prop="name">
              <el-input v-model="form.name" maxlength="100" show-word-limit placeholder="请输入商品名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="金币价格" prop="coinPrice">
              <el-input-number v-model="form.coinPrice" :min="1" :step="1" :precision="0" controls-position="right" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="商品图片" prop="images">
          <image-upload v-model="form.images" :limit="5" />
          <div class="form-tip">请上传 1–5 张图片，拖动图片可调整展示顺序。</div>
        </el-form-item>
        <el-form-item label="商品描述" prop="description">
          <mall-markdown v-model="form.description" height="380px" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="submitting" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import MallMarkdown from '@/components/MallMarkdown'
import { addProduct, delProduct, getProduct, listProducts, updateProduct } from '@/api/mall'

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
    const validateDescription = (rule, value, callback) => {
      if (!value || !value.trim()) {
        callback(new Error('请输入商品描述'))
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
      title: '',
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        name: undefined
      },
      form: {},
      rules: {
        name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
        images: [{ required: true, validator: validateImages, trigger: 'change' }],
        description: [{ required: true, validator: validateDescription, trigger: 'change' }],
        coinPrice: [
          { required: true, message: '请输入金币价格', trigger: 'change' },
          { type: 'integer', min: 1, message: '金币价格必须为正整数', trigger: 'change' }
        ]
      }
    }
  },
  created() {
    this.getList()
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
        coinPrice: 1
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
</style>
