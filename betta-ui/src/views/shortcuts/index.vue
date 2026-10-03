<template>
  <div class="app-container">

    <div class="container">
      <div class="title">
        正在学习
      </div>
      <div class="title-btn">
        <el-button v-hasPermi="['eng:study:view']" @click="$router.push('/eng/study/index')">学习中心</el-button>
        <el-button @click="changeWordBook">更换词书</el-button>
      </div>
      <div v-if="article.id" class="text article">
        <div class="article-title">
          <img src="@/assets/shortcuts/书.svg" />{{ article.title }}
        </div>
        <div class="toolbar">
          <a :href="'eng/article/test/' + article.id" class="btn">
            <img src="@/assets/shortcuts/youxi.svg" />
            开始测试
          </a>
          <el-divider direction="vertical"></el-divider>
          <a :href="'eng/article-detail/' + article.id" class="btn">
            <img src="@/assets/shortcuts/编辑.svg" />
            编辑内容
          </a>
          <el-divider direction="vertical"></el-divider>
          <a :href="'eng/article-detail/' + article.id" class="btn">
            <img src="@/assets/shortcuts/评论.svg" />
            跟读
          </a>
          <el-divider direction="vertical"></el-divider>
          <a :href="'eng/article-detail/' + article.id" class="btn">
            <img src="@/assets/shortcuts/播放.svg" />
            播放
          </a>
        </div>
      </div>
      <div v-if="!article.id" class="text no-article">
        没有正在学习的文章哦
      </div>
    </div>

    <div class="container">
      <div class="other">
        <a href="other/timer" class="item">
          <img src="@/assets/shortcuts/通知.svg" />
          上课
        </a>
        <a href="dance" class="item">
          <img src="@/assets/shortcuts/运动.svg" />
          跳舞
        </a>
        <a href="card/index" class="item">
          <img src="@/assets/shortcuts/积分.svg" />
          加卡
        </a>
        <a href="/note" class="item">
          <img src="@/assets/shortcuts/档案.svg" />
          笔记
        </a>
        <a href="/eng/manage/word-query" class="item">
          <img src="@/assets/shortcuts/查看搜索.svg" />
          查单词
        </a>
        <a href="video" class="item">
          <img src="@/assets/shortcuts/播放.svg" />
          视频
        </a>
        <a href="other/task" class="item">
          <img src="@/assets/shortcuts/打卡.svg" />
          任务
        </a>
        <router-link to="/mall/index" class="item">
          <img src="@/assets/shortcuts/商城.svg" />
          商城
        </router-link>
      </div>
    </div>

    <el-dialog
      title="更换词书"
      :visible.sync="wordBookDialogVisible"
      width="520px"
      append-to-body
    >
      <el-input
        v-model="wordBookKeyword"
        class="word-book-search"
        prefix-icon="el-icon-search"
        placeholder="搜索词书"
        clearable
      />
      <div v-loading="wordBookLoading" class="word-book-list">
        <el-empty
          v-if="!wordBookLoading && wordBookOptions.length === 0"
          description="暂无可选择的词书"
        />
        <el-empty
          v-else-if="!wordBookLoading && filteredWordBookOptions.length === 0"
          description="未找到匹配的词书"
        />
        <div v-else>
          <section
            v-for="group in groupedWordBookOptions"
            :key="group.name"
            class="word-book-group"
          >
            <div class="word-book-group-title">{{ group.name }}</div>
            <div
              v-for="item in group.items"
              :key="item.id"
              class="word-book-item"
            >
              <span class="word-book-title">{{ item.title || '未命名文章' }}</span>
              <el-button
                type="primary"
                size="small"
                :disabled="wordBookSelectingId !== null || article.id === item.id"
                :loading="wordBookSelectingId === item.id"
                @click="selectWordBook(item)"
              >
                {{ article.id === item.id ? '当前词书' : '选择' }}
              </el-button>
            </div>
          </section>
        </div>
      </div>
    </el-dialog>

  </div>
</template>

<script>

import {
  getCurrentArticle,
  listCurrentArticleOptions,
  setCurrentArticle
} from '@/api/eng/article';

export default {
  name: "Shortcuts",
  data() {
    return {
      article: {},
      wordBookDialogVisible: false,
      wordBookLoading: false,
      wordBookOptions: [],
      wordBookKeyword: '',
      wordBookSelectingId: null
    };
  },
  computed: {
    filteredWordBookOptions() {
      const keyword = this.wordBookKeyword.trim().toLowerCase();
      if (!keyword) {
        return this.wordBookOptions;
      }
      return this.wordBookOptions.filter((item) => {
        const title = item.title || '未命名文章';
        return title.toLowerCase().includes(keyword);
      });
    },
    groupedWordBookOptions() {
      // 分组及组内词书均保持接口中的首次出现顺序。
      return this.filteredWordBookOptions.reduce((groups, item) => {
        const groupName = item.groupName || '未分组';
        let group = groups.find((current) => current.name === groupName);
        if (!group) {
          group = { name: groupName, items: [] };
          groups.push(group);
        }
        group.items.push(item);
        return groups;
      }, []);
    }
  },
  created() {
    getCurrentArticle().then((res) => {
      this.article = res.data || {};
    });
  },
  methods: {
    changeWordBook() {
      this.wordBookKeyword = '';
      this.wordBookDialogVisible = true;
      this.wordBookLoading = true;
      listCurrentArticleOptions().then((res) => {
        this.wordBookOptions = Array.isArray(res.data) ? res.data : [];
      }).finally(() => {
        this.wordBookLoading = false;
      });
    },
    selectWordBook(article) {
      this.wordBookSelectingId = article.id;
      setCurrentArticle(article.id).then((res) => {
        this.article = res.data || article;
        this.wordBookDialogVisible = false;
        this.$message.success('词书更换成功');
      }).finally(() => {
        this.wordBookSelectingId = null;
      });
    }
  }
};
</script>
<style scoped lang="scss">
.app-container {

  background-color: #f4f4f4;
  color: #a2a2a2;
  font-family: "MicroSoft Yahei";
  position: relative;
  height: calc(100vh - 84px);
  padding: 10;

  .container {
    padding: 20px 40px;
    min-height: 188px;
    border-radius: 18px;
    background: #fff;
    box-shadow: 3px 3px 6px #bebebe, -3px -3px 6px #ffffff;

    max-width: 800px;
    width: 100%;
    margin: 0 auto 20px auto;
    position: relative;
    color: #8d96a8;

    .title {
      color: #555f76;
      font-size: 22px;
      padding: 20px 0 10px;
      text-align: start;
    }

    .title-btn {
      position: absolute;
      right: 40px;
      top: 42px;
      display: flex;
      gap: 10px;
    }

    .text {
      padding: 0 0 10px;
      text-align: start;
      color: #8d96a8;
    }

    .small-img {
      width: 30px;
    }

    .no-article {
      padding: 30px 0;
    }

    .article {
      .article-title {
        img {
          width: 60px;
        }

        padding: 20px 0;
        display: flex;
        align-items: center;
        gap: 30px;
      }

      .toolbar {
        display: flex;
        gap: 5px;

        .btn {
          display: flex;
          align-items: center;
          gap: 5px;
          font-size: 14px;

          img {
            width: 22px;
          }
        }
      }
    }

    .other {
      display: flex;
      flex-wrap: wrap;
      align-content: flex-start;
      gap: 20px;

      .item {
        cursor: pointer;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 10px;
        font-size: 14px;
        width: 22%;

        img {
          width: 50px;
        }

        .svg-icon {
          width: 50px;
          height: 50px;
          color: #ffc94c;
        }
      }
    }
  }
}

.word-book-list {
  min-height: 120px;
  max-height: 55vh;
  padding-right: 8px;
  overflow-y: auto;

  .word-book-group + .word-book-group {
    margin-top: 16px;
  }

  .word-book-group-title {
    padding: 8px 12px;
    border-radius: 4px;
    background-color: #f5f7fa;
    color: #909399;
    font-size: 13px;
    font-weight: 600;
  }

  .word-book-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 0;
    border-bottom: 1px solid #ebeef5;

    &:last-child {
      border-bottom: 0;
    }
  }

  .word-book-title {
    min-width: 0;
    margin-right: 20px;
    overflow: hidden;
    color: #606266;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.word-book-search {
  margin-bottom: 16px;
}
</style>
