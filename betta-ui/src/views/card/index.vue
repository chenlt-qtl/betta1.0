<template>
  <div class="card-container">
    <!-- 账户展示区 -->
    <div class="head">
      <div
        v-for="(account, index) in accountList"
        :key="account.id"
        @click="setAccount(account)"
        :class="(activeAccount && activeAccount.id === account.id ? 'active' : 'noActive') + ' account'"
      >
        <h5>{{ account.name }}</h5>
        <h2>{{ account.balance }}张</h2>
        <img :src="getAvatar(index)" />
      </div>
    </div>

    <!-- 操作输入区 -->
    <div class="input-bar">
      <div class="symbol">
        <el-radio-group v-model="symbol">
          <el-radio :label="1">+</el-radio>
          <el-radio :label="-1">-</el-radio>
        </el-radio-group>
      </div>
      <el-input-number
        v-model="score"
        class="input-with-select"
        clearable
        controls-position="right"
        :min="0"
        :max="999"
      />
      <el-button round @click="onReset" type="text" icon="el-icon-refresh-left">清空</el-button>
      <el-button round type="primary" @click="showConfirm" icon="el-icon-check">提交</el-button>
      <el-button type="text" @click="showHistory" icon="el-icon-time">历史</el-button>
    </div>

    <!-- 快速数值按钮 -->
    <div class="quick">
      <span
        class="red"
        v-for="item in quickNumbers"
        :key="'-' + item"
        @click="() => addScore(-item)"
      >-{{ item }}</span>
    </div>
    <div class="quick">
      <span
        class="green"
        v-for="item in quickNumbers"
        :key="item"
        @click="() => addScore(item)"
      >{{ item }}</span>
    </div>

    <!-- 预设项标签页 -->
    <div class="tab">
      <el-tabs v-model="activeTab">
        <el-tab-pane
          v-for="dict in dict.type.card_type"
          :key="dict.value"
          :label="dict.label"
          :name="dict.value"
        >
          <div class="quick">
            <span
              :class="item.value > 0 ? 'green' : 'red'"
              v-for="item in listData[dict.value]"
              :key="item.id"
              @click="() => addScore(item.value, item.name)"
            >
              {{ item.name }} {{ item.value > 0 ? "+" : "" }}{{ item.value }}
            </span>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 加卡确认对话框 -->
    <el-dialog
      :title="confirmTitle"
      :visible.sync="openConfirm"
      width="400px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-input
        v-model="content"
        placeholder="请输入原因(可选)"
        type="textarea"
        :rows="3"
        maxlength="500"
        show-word-limit
      />
      <span slot="footer" class="dialog-footer">
        <el-button @click="openConfirm = false">取 消</el-button>
        <el-button type="primary" @click="onSubmit" :loading="submitting">确 定</el-button>
      </span>
    </el-dialog>

    <!-- 历史记录对话框 -->
    <el-dialog
      :title="historyDialogTitle"
      :visible.sync="openHistory"
      width="600px"
      append-to-body
    >
      <el-table v-loading="historyLoading" :data="historyList" max-height="400">
        <el-table-column label="变动值" prop="changeValue" width="80" align="center">
          <template slot-scope="scope">
            <span :class="scope.row.changeValue >= 0 ? 'text-green' : 'text-red'">
              {{ scope.row.changeValue >= 0 ? '+' : '' }}{{ scope.row.changeValue }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="变动后" prop="remainValue" width="80" align="center" />
        <el-table-column label="原因" prop="content" show-overflow-tooltip />
        <el-table-column label="时间" prop="createTime" width="150" align="center" />
      </el-table>
      <pagination
        v-show="historyTotal > 0"
        :total="historyTotal"
        :page.sync="historyQuery.pageNum"
        :limit.sync="historyQuery.pageSize"
        @pagination="getHistoryList"
      />
      <span slot="footer" class="dialog-footer">
        <el-button @click="openHistory = false">关 闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { listAccount } from "@/api/card/account";
import { listItem } from "@/api/card/item";
import { listHistoryByAccount, listHistory } from "@/api/card/history";
import { operate } from "@/api/card/operate";

export default {
  name: "CardIndex",
  dicts: ["card_type"],
  created() {
    this.getAccountList();
    this.getItemList();
  },
  data() {
    return {
      // 快速数值
      quickNumbers: [50, 20, 10, 5, 3, 2, 1],
      // 符号
      symbol: 1,
      // 当前选中的账户
      activeAccount: null,
      // 账户列表
      accountList: [],
      // 分数
      score: 0,
      // 原因内容
      content: "",
      // 当前激活的标签
      activeTab: "1",
      // 预设项列表数据（按类型分组）
      listData: {},
      // 确认对话框
      openConfirm: false,
      // 历史对话框
      openHistory: false,
      // 历史记录列表
      historyList: [],
      historyLoading: false,
      historyTotal: 0,
      historyQuery: {
        pageNum: 1,
        pageSize: 20
      },
      // 提交中
      submitting: false
    };
  },
  computed: {
    // 计算实际变动值
    changeValue() {
      return this.symbol * this.score;
    },
    // 确认标题
    confirmTitle() {
      if (!this.activeAccount) return "请选择账户";
      const operator = this.symbol === 1 ? "+" : "-";
      return `${this.activeAccount.name} ${operator} ${this.score} ?`;
    },
    // 历史记录标题
    historyDialogTitle() {
      if (this.activeAccount) {
        return `${this.activeAccount.name} - 历史记录`;
      }
      return "全部历史记录";
    }
  },
  methods: {
    /** 获取账户列表 */
    getAccountList() {
      listAccount({ pageNum: 1, pageSize: 1000 }).then(response => {
        this.accountList = response.rows || [];
        if (this.accountList.length > 0 && !this.activeAccount) {
          this.activeAccount = this.accountList[0];
        }
      });
    },
    /** 获取预设项列表 */
    getItemList() {
      listItem({ pageNum: 1, pageSize: 1000 }).then(response => {
        const data = response.rows || [];
        const result = {};
        data.forEach(item => {
          if (!result[item.type]) {
            result[item.type] = [];
          }
          result[item.type].push(item);
        });
        this.listData = result;
      });
    },
    /** 设置当前账户 */
    setAccount(account) {
      this.activeAccount = account;
    },
    /** 获取头像 */
    getAvatar(index) {
      const avatars = [
        require("@/assets/images/girl-5.svg"),
        require("@/assets/images/boy-2.svg")
      ];
      return avatars[index % avatars.length];
    },
    /** 重置 */
    onReset() {
      this.score = 0;
      this.symbol = 1;
      this.content = "";
    },
    /** 添加分数 */
    addScore(value, name) {
      const newValue = value + this.changeValue;
      if (newValue < 0) {
        this.symbol = -1;
      } else {
        this.symbol = 1;
      }
      this.score = Math.abs(newValue);
      if (name) {
        if (this.content) {
          this.content += ",";
        }
        this.content += name;
      }
    },
    /** 显示确认对话框 */
    showConfirm() {
      if (!this.activeAccount) {
        this.$modal.msgError("请选择账户");
        return;
      }
      if (this.score === 0) {
        this.$modal.msgError("请输入变动数值");
        return;
      }
      this.openConfirm = true;
    },
    /** 提交操作 */
    onSubmit() {
      if (!this.activeAccount) {
        this.$modal.msgError("请选择账户");
        return;
      }

      this.submitting = true;
      const data = {
        name: this.activeAccount.name,
        changeValue: this.changeValue,
        content: this.content
      };

      operate(data)
        .then(() => {
          this.$modal.msgSuccess("操作成功");
          this.openConfirm = false;
          this.onReset();
          this.getAccountList(); // 刷新余额
        })
        .finally(() => {
          this.submitting = false;
        });
    },
    /** 显示历史记录 */
    showHistory() {
      this.openHistory = true;
      if (this.activeAccount) {
        this.getHistoryList();
      } else {
        this.getHistoryList(null);
      }
    },
    /** 获取历史记录 */
    getHistoryList(accountId) {
      this.historyLoading = true;
      const actualAccountId = accountId || (this.activeAccount ? this.activeAccount.id : null);
      
      const apiFunc = actualAccountId ? listHistoryByAccount(actualAccountId, this.historyQuery) : listHistory(this.historyQuery);
      apiFunc
        .then(response => {
          this.historyList = response.rows || [];
          this.historyTotal = response.total || 0;
        })
        .finally(() => {
          this.historyLoading = false;
        });
    }
  }
};
</script>

<style lang="scss" scoped>
.card-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.head {
  max-width: 600px;
  width: 100%;
  margin: 0 auto 20px;
  padding: 20px;
  display: flex;
  gap: 10px;

  .account {
    position: relative;
    flex: 1;
    cursor: pointer;
    transition: all 0.3s;

    h2 {
      font-size: 20px;
      font-weight: bold;
      margin-top: 16px;
      margin-bottom: 0;
    }

    img {
      position: absolute;
      right: 10px;
      bottom: 40px;
      transition: 0.3s;
    }
  }

  .active {
    background-color: #f28086;
    color: #fff;
    border-radius: 10px;
    box-shadow: 0 2px 8px #f0f1f2;
    padding: 20px;
    transform: scale(1.05);

    h5, h2 {
      color: #fff;
    }

    img {
      bottom: 60px;
      right: 5px;
      width: 80px;
    }
  }

  .noActive {
    background-color: #fff;
    border-radius: 10px;
    box-shadow: 0 2px 8px #f0f1f2;
    padding: 20px;
    transform: scale(0.95);

    h5 {
      color: #888;
    }

    img {
      width: 50px;
    }
  }
}

.input-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 15px;
  margin-bottom: 20px;

  .symbol {
    display: flex;
    flex-wrap: nowrap;
  }

  .input-with-select {
    width: 150px;
  }
}

.quick {
  padding-top: 10px;
  text-align: center;

  span {
    cursor: pointer;
    display: inline-block;
    border-radius: 5px;
    padding: 8px 20px;
    text-align: center;
    margin: 5px;
    transition: all 0.3s;
    font-weight: 500;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    }
  }

  .red {
    border: #f28086 1px solid;
    color: #f28086;

    &:hover {
      background-color: #f28086;
      color: #fff;
    }
  }

  .green {
    border: #53ac68 1px solid;
    color: #53ac68;

    &:hover {
      background-color: #53ac68;
      color: #fff;
    }
  }
}

.tab {
  max-width: 600px;
  width: 100%;
  margin: 20px auto;
}

.text-green {
  color: #53ac68;
  font-weight: bold;
}

.text-red {
  color: #f28086;
  font-weight: bold;
}
</style>
