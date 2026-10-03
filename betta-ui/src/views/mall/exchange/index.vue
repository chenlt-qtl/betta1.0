<template>
  <div class="app-container">
    <el-form ref="queryForm" :model="queryParams" size="small" :inline="true" v-show="showSearch">
      <el-form-item label="用户名" prop="userName">
        <el-input
          v-model="queryParams.userName"
          placeholder="请输入用户名"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商品名称" prop="productName">
        <el-input
          v-model="queryParams.productName"
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
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="exchangeList">
      <el-table-column label="记录编号" prop="id" width="100" align="center" />
      <el-table-column label="用户" min-width="150">
        <template slot-scope="scope">
          <div>{{ scope.row.userName || scope.row.username || '-' }}</div>
          <small class="secondary-text">用户ID：{{ scope.row.userId }}</small>
        </template>
      </el-table-column>
      <el-table-column label="商品" min-width="220">
        <template slot-scope="scope">
          <div class="product-cell">
            <image-preview :src="firstImage(scope.row.productImages)" :width="54" :height="54" />
            <div>
              <div>{{ scope.row.productName }}</div>
              <small class="secondary-text">商品ID：{{ scope.row.productId || '-' }}</small>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="兑换价格" prop="coinPrice" width="120" align="center">
        <template slot-scope="scope"><span class="coin-price">{{ scope.row.coinPrice }}</span></template>
      </el-table-column>
      <el-table-column label="兑换时间" prop="createTime" width="180" align="center" />
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script>
import { listExchanges } from '@/api/mall'

export default {
  name: 'MallExchange',
  data() {
    return {
      loading: false,
      showSearch: true,
      total: 0,
      exchangeList: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userName: undefined,
        productName: undefined
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listExchanges(this.queryParams).then(response => {
        this.exchangeList = response.rows || []
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
      this.resetForm('queryForm')
      this.handleQuery()
    },
    firstImage(images) {
      if (!images) return ''
      return String(images).split(',').map(item => item.trim()).filter(Boolean)[0] || ''
    }
  }
}
</script>

<style scoped>
.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}
.secondary-text { color: #909399; }
.coin-price { color: #e6a23c; font-weight: 600; }
</style>
