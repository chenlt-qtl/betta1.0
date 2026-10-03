<template>
  <div class="app-container mall-store">
    <div class="store-header">
      <div>
        <h2>金币商城</h2>
        <p>使用学习获得的金币兑换喜欢的商品</p>
      </div>
      <div class="coin-balance">
        <i class="el-icon-coin" />
        <span>我的金币</span>
        <strong>{{ coinBalance }}</strong>
      </div>
    </div>

    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <el-tab-pane label="商品" name="products">
        <div v-loading="productLoading" class="product-grid">
          <el-empty v-if="!productLoading && productList.length === 0" description="暂无可兑换商品" />
          <el-card
            v-for="product in productList"
            :key="product.id"
            class="product-card"
            shadow="hover"
            :body-style="{ padding: '0' }"
            @click.native="openProduct(product)"
          >
            <el-image class="product-cover" :src="firstImage(product.images)" fit="cover">
              <div slot="error" class="image-placeholder"><i class="el-icon-picture-outline" /></div>
            </el-image>
            <div class="product-summary">
              <div class="product-name">{{ product.name }}</div>
              <div class="product-footer">
                <span class="price"><i class="el-icon-coin" /> {{ product.coinPrice }}</span>
                <el-button type="primary" size="small" @click.stop="openProduct(product)">查看</el-button>
              </div>
            </div>
          </el-card>
        </div>
        <pagination
          v-show="productTotal > 0"
          :total="productTotal"
          :page.sync="productQuery.pageNum"
          :limit.sync="productQuery.pageSize"
          @pagination="loadProducts"
        />
      </el-tab-pane>

      <el-tab-pane label="我的兑换记录" name="records">
        <el-table v-loading="recordLoading" :data="recordList">
          <el-table-column label="商品" min-width="220">
            <template slot-scope="scope">
              <div class="record-product">
                <el-image :src="firstImage(scope.row.productImages)" fit="cover" />
                <span>{{ scope.row.productName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="消耗金币" prop="coinPrice" width="130" align="center">
            <template slot-scope="scope"><span class="price">{{ scope.row.coinPrice }}</span></template>
          </el-table-column>
          <el-table-column label="兑换时间" prop="createTime" min-width="170" align="center" />
        </el-table>
        <pagination
          v-show="recordTotal > 0"
          :total="recordTotal"
          :page.sync="recordQuery.pageNum"
          :limit.sync="recordQuery.pageSize"
          @pagination="loadRecords"
        />
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      :title="currentProduct.name || '商品详情'"
      :visible.sync="detailOpen"
      width="760px"
      append-to-body
      custom-class="mall-product-dialog"
    >
      <div v-loading="detailLoading" class="product-detail">
        <el-carousel v-if="currentImages.length" height="320px" indicator-position="outside">
          <el-carousel-item v-for="image in currentImages" :key="image">
            <el-image
              class="detail-image"
              :src="imageUrl(image)"
              :preview-src-list="currentImageUrls"
              fit="contain"
            />
          </el-carousel-item>
        </el-carousel>
        <div class="detail-price"><i class="el-icon-coin" /> {{ currentProduct.coinPrice }} 金币</div>
        <mall-markdown :value="currentProduct.description" viewer />
      </div>
      <div slot="footer">
        <el-button @click="detailOpen = false">关 闭</el-button>
        <el-button
          type="primary"
          :loading="exchanging"
          :disabled="!currentProduct.id"
          @click="handleExchange"
        >
          立即兑换
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import MallMarkdown from '@/components/MallMarkdown'
import {
  exchangeProduct,
  getStoreBalance,
  getStoreProduct,
  listMyExchanges,
  listStoreProducts
} from '@/api/mall'
import { isExternal } from '@/utils/validate'

export default {
  name: 'MallStore',
  components: { MallMarkdown },
  data() {
    return {
      baseUrl: process.env.VUE_APP_BASE_API,
      activeTab: 'products',
      coinBalance: 0,
      productLoading: false,
      productList: [],
      productTotal: 0,
      productQuery: { pageNum: 1, pageSize: 8 },
      recordLoading: false,
      recordLoaded: false,
      recordList: [],
      recordTotal: 0,
      recordQuery: { pageNum: 1, pageSize: 10 },
      detailOpen: false,
      detailLoading: false,
      exchanging: false,
      currentProduct: {}
    }
  },
  computed: {
    currentImages() {
      return this.splitImages(this.currentProduct.images)
    },
    currentImageUrls() {
      return this.currentImages.map(this.imageUrl)
    }
  },
  created() {
    this.loadBalance()
    this.loadProducts()
  },
  methods: {
    loadBalance() {
      getStoreBalance().then(response => {
        const data = response.data
        this.coinBalance = typeof data === 'object' && data !== null
          ? Number(data.coinBalance || data.balance || 0)
          : Number(data || 0)
      })
    },
    loadProducts() {
      this.productLoading = true
      listStoreProducts(this.productQuery).then(response => {
        this.productList = response.rows || []
        this.productTotal = response.total || 0
      }).finally(() => {
        this.productLoading = false
      })
    },
    loadRecords() {
      this.recordLoading = true
      listMyExchanges(this.recordQuery).then(response => {
        this.recordList = response.rows || []
        this.recordTotal = response.total || 0
        this.recordLoaded = true
      }).finally(() => {
        this.recordLoading = false
      })
    },
    handleTabClick(tab) {
      if (tab.name === 'records' && !this.recordLoaded) {
        this.loadRecords()
      }
    },
    openProduct(product) {
      this.currentProduct = { ...product }
      this.detailOpen = true
      this.detailLoading = true
      getStoreProduct(product.id).then(response => {
        this.currentProduct = response.data || product
      }).finally(() => {
        this.detailLoading = false
      })
    },
    handleExchange() {
      const product = this.currentProduct
      this.$confirm(`确定使用 ${product.coinPrice} 金币兑换“${product.name}”吗？`, '确认兑换', {
        confirmButtonText: '确定兑换',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.exchanging = true
        return exchangeProduct(product.id).then(response => {
          const result = response.data || {}
          if (result.coinBalance !== undefined) {
            this.coinBalance = Number(result.coinBalance)
          } else {
            this.loadBalance()
          }
          this.recordLoaded = false
          this.detailOpen = false
          this.$message.success(`兑换成功，消耗 ${result.coinCost || product.coinPrice} 金币`)
        }).finally(() => {
          this.exchanging = false
        })
      }).catch(() => {})
    },
    splitImages(images) {
      if (!images) return []
      return String(images).split(',').map(item => item.trim()).filter(Boolean)
    },
    firstImage(images) {
      const image = this.splitImages(images)[0]
      return image ? this.imageUrl(image) : ''
    },
    imageUrl(image) {
      if (!image || isExternal(image)) return image || ''
      return this.baseUrl + image
    }
  }
}
</script>

<style scoped lang="scss">
.mall-store {
  min-height: calc(100vh - 84px);
  background: #f5f7fa;
}

.store-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding: 24px 30px;
  border-radius: 12px;
  color: #fff;
  background: linear-gradient(135deg, #f6b73c, #f07c35);

  h2 { margin: 0 0 8px; }
  p { margin: 0; opacity: .9; }
}

.coin-balance {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, .2);

  strong { font-size: 24px; }
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  min-height: 260px;

  > .el-empty { grid-column: 1 / -1; }
}

.product-card { cursor: pointer; }
.product-cover { display: block; width: 100%; height: 190px; }
.image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 40px;
  color: #c0c4cc;
  background: #f5f7fa;
}
.product-summary { padding: 14px; }
.product-name {
  height: 42px;
  overflow: hidden;
  color: #303133;
  font-size: 16px;
  line-height: 21px;
}
.product-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
}
.price { color: #e6a23c; font-size: 16px; font-weight: 600; }
.record-product { display: flex; align-items: center; gap: 12px; }
.record-product .el-image { flex: none; width: 54px; height: 54px; border-radius: 4px; }
.product-detail { min-height: 220px; }
.detail-image { width: 100%; height: 320px; }
.detail-price { margin: 16px 0; color: #e6a23c; font-size: 22px; font-weight: 600; }

@media (max-width: 1100px) {
  .product-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 768px) {
  .store-header { align-items: flex-start; flex-direction: column; gap: 18px; }
  .product-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .product-cover { height: 150px; }
}
</style>

<style lang="scss">
.mall-product-dialog {
  max-width: calc(100vw - 32px);
}

@media (max-width: 768px) {
  .mall-product-dialog {
    display: flex;
    flex-direction: column;
    width: calc(100vw - 24px) !important;
    max-width: none;
    max-height: calc(100vh - 24px);
    margin: 12px auto !important;

    .el-dialog__header {
      flex: none;
      padding: 16px 44px 14px 16px;
    }

    .el-dialog__body {
      flex: 1;
      min-height: 0;
      overflow-y: auto;
      padding: 0 16px 8px !important;
    }

    .el-dialog__footer {
      flex: none;
      padding: 12px 16px 16px;
    }

    .el-carousel__container,
    .detail-image {
      height: 220px !important;
    }
  }
}
</style>
