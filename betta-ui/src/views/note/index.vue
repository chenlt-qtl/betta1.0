<template>
  <div class="app-container note-page">
    <nav class="note-rail">
      <el-tooltip content="文件" placement="right">
        <i class="el-icon-folder-opened" :class="{ active: activePanel === 'files' }" @click="switchPanel('files')"></i>
      </el-tooltip>
      <el-tooltip content="搜索" placement="right">
        <i class="el-icon-search" :class="{ active: activePanel === 'search' }" @click="switchPanel('search')"></i>
      </el-tooltip>
      <el-tooltip content="收藏" placement="right">
        <i class="el-icon-collection-tag"></i>
      </el-tooltip>
<!--      <el-tooltip content="日历" placement="right">-->
<!--        <i class="el-icon-date"></i>-->
<!--      </el-tooltip>-->
    </nav>

    <aside class="note-sidebar">
      <div v-if="activePanel === 'search'" class="sidebar-toolbar">
        <el-input
          ref="searchInput"
          v-model="keyword"
          size="small"
          clearable
          prefix-icon="el-icon-search"
          placeholder="输入并开始搜索..."
          @input="queueSearch"
          @keyup.enter.native="handleSearch"
          @clear="clearSearch"
        />
      </div>
      <div v-if="activePanel === 'files'" class="sidebar-actions">
        <el-button size="mini" icon="el-icon-plus" type="primary" plain @click="handleCreate('file')" v-hasPermi="['system:note:add']">笔记</el-button>
        <el-button size="mini" icon="el-icon-folder-add" plain @click="handleCreate('directory')" v-hasPermi="['system:note:add']">文件夹</el-button>
      </div>
      <div class="sidebar-body">
        <el-tree
          v-if="activePanel === 'files'"
          ref="tree"
          class="note-tree"
          node-key="path"
          :data="treeData"
          :props="treeProps"
          :expand-on-click-node="false"
          default-expand-all
          @node-click="handleNodeClick"
        >
          <span slot-scope="{ node, data }" class="tree-node" :class="{ active: data.path === currentPath }">
            <i :class="data.type === 'directory' ? 'el-icon-folder' : 'el-icon-document'"></i>
            <span class="tree-label">{{ node.label }}</span>
          </span>
        </el-tree>
        <div v-else class="search-results">
          <div v-if="searchLoading" class="search-empty">搜索中...</div>
          <template v-else>
            <div
              v-for="item in searchResults"
              :key="item.path"
              class="search-item"
              @click="openNote(item.path)"
            >
              <div class="search-title">{{ item.name }}</div>
              <div class="search-path">{{ item.path }}</div>
              <div class="search-snippet">{{ item.snippet }}</div>
            </div>
          </template>
          <div v-if="!searchLoading && !searchResults.length" class="search-empty">
            {{ keyword ? '未找到匹配结果。' : '输入关键词后开始搜索。' }}
          </div>
        </div>
      </div>
    </aside>

    <main class="note-workspace">
      <div class="workspace-topbar">
        <div class="workspace-nav">
          <el-button type="text" icon="el-icon-back" />
          <el-button type="text" icon="el-icon-right" />
        </div>
        <el-input
          v-model="title"
          class="workspace-title-input"
          size="mini"
          :disabled="!currentPath"
          @blur="handleTitleBlur"
        />
        <div class="workspace-actions">
          <el-tag v-if="currentPath" size="mini" :type="dirty ? 'warning' : 'success'">
            {{ dirty ? '未保存' : '已保存' }}
          </el-tag>
          <el-button-group>
            <el-button size="mini" icon="el-icon-edit" :type="viewer ? '' : 'primary'" @click="viewer = false"></el-button>
            <el-button size="mini" icon="el-icon-view" :type="viewer ? 'primary' : ''" @click="viewer = true"></el-button>
          </el-button-group>
          <el-tooltip :content="tocCollapsed ? '展开目录' : '收起目录'" placement="bottom">
            <el-button
              size="mini"
              :icon="tocCollapsed ? 'el-icon-s-unfold' : 'el-icon-s-fold'"
              :disabled="!currentPath || currentNodeType !== 'file' || !viewer"
              @click="tocCollapsed = !tocCollapsed"
            ></el-button>
          </el-tooltip>
          <el-button size="mini" icon="el-icon-check" type="primary" :disabled="!currentPath || currentNodeType !== 'file' || !dirty" @click="save" v-hasPermi="['system:note:edit']">保存</el-button>
          <el-button size="mini" icon="el-icon-download" :disabled="!currentPath || currentNodeType !== 'file'" @click="handleDownload" v-hasPermi="['system:note:download']"></el-button>
          <el-button size="mini" icon="el-icon-delete" type="danger" plain :disabled="!currentPath" @click="handleDelete" v-hasPermi="['system:note:remove']"></el-button>
        </div>
      </div>

      <div class="note-toolbar">
        <div class="title-input path-title">{{ currentPath}}</div>
      </div>

      <div class="note-canvas">
        <div v-if="currentPath && currentNodeType === 'file'" class="editor-wrap">
          <note-markdown
            v-model="content"
            :viewer="viewer"
            :note-path="currentPath"
            :resource-base="resourceBase"
            :toc-collapsed="tocCollapsed"
            @change="markDirty"
            @blur="handleEditorBlur"
          />
        </div>
        <div v-else-if="currentPath && currentNodeType === 'directory'" class="note-folder-empty">
          <i class="el-icon-folder"></i>
          <div class="folder-name">{{ title }}</div>
        </div>
        <el-empty v-else class="note-empty" description="选择或新建一篇笔记" />
      </div>
    </main>
  </div>
</template>

<script>
import { saveAs } from 'file-saver'
import NoteMarkdown from '@/components/NoteMarkdown'
import {
  createNoteFile,
  deleteNoteFile,
  downloadNoteFile,
  getNoteContent,
  getNoteTree,
  renameNoteFile,
  saveNoteContent,
  searchNotes
} from '@/api/note'

export default {
  name: 'NoteIndex',
  components: { NoteMarkdown },
  data() {
    return {
      // 左侧搜索关键词；搜索面板输入时会做防抖请求。
      keyword: '',
      // 左侧工具栏当前激活的面板：files 显示文件树，search 显示搜索框和结果。
      activePanel: 'files',
      searchResults: [],
      // 搜索 loading 与请求序号配合使用，避免慢请求覆盖后输入的新结果。
      searchLoading: false,
      searchTimer: null,
      searchSeq: 0,
      // 后端返回的 Markdown vault 文件树，仅展示笔记和普通目录，图片附件目录在后端已隐藏。
      treeData: [],
      treeProps: {
        children: 'children',
        label: 'name'
      },
      // 当前选中的 vault 相对路径和节点类型；文件夹选中时会清空正文区域。
      currentPath: '',
      currentNodeType: '',
      // 顶部标题输入框内容。文件失焦时会重命名文件，文件夹失焦时会重命名目录。
      title: '',
      // 当前笔记正文、服务端 hash 和资源访问前缀。hash 用于保存时做冲突检测。
      content: '',
      hash: '',
      resourceBase: '',
      // dirty 只代表正文是否有真实用户改动；初始化/切换笔记不会置为未保存。
      dirty: false,
      loadingNote: false,
      renamingTitle: false,
      // 目录状态由页面持有，按钮放在顶部工具栏；手机端默认收起以节省横向空间。
      tocCollapsed: this.isMobileViewport(),
      viewer: true
    }
  },
  created() {
    this.loadTree()
  },
  mounted() {
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
    window.clearTimeout(this.searchTimer)
  },
  methods: {
    isMobileViewport() {
      return window.innerWidth <= 768
    },
    handleResize() {
      if (this.isMobileViewport()) {
        this.tocCollapsed = true
      }
    },
    loadTree() {
      getNoteTree().then(res => {
        this.treeData = res.data || []
      })
    },
    switchPanel(panel) {
      this.activePanel = panel
      if (panel === 'files') {
        // 切回文件树时清空搜索状态，避免下次进入搜索面板看到旧结果。
        this.clearSearch()
      } else {
        // 搜索面板打开后自动聚焦，让用户可以直接输入。
        this.$nextTick(() => {
          if (this.$refs.searchInput) {
            this.$refs.searchInput.focus()
          }
        })
      }
    },
    clearSearch() {
      window.clearTimeout(this.searchTimer)
      this.keyword = ''
      this.searchResults = []
      this.searchLoading = false
    },
    queueSearch() {
      // 输入过程中做短防抖，减少连续请求；回车仍会立即触发 handleSearch。
      window.clearTimeout(this.searchTimer)
      this.searchTimer = window.setTimeout(() => {
        this.handleSearch()
      }, 300)
    },
    handleSearch() {
      const keyword = (this.keyword || '').trim()
      window.clearTimeout(this.searchTimer)
      if (!keyword) {
        this.searchResults = []
        this.searchLoading = false
        return
      }
      const seq = ++this.searchSeq
      this.searchLoading = true
      searchNotes(keyword).then(res => {
        // 只接收最后一次搜索请求结果，避免网络慢的旧请求覆盖新关键词结果。
        if (seq !== this.searchSeq) {
          return
        }
        this.searchResults = res.data || []
      }).finally(() => {
        if (seq === this.searchSeq) {
          this.searchLoading = false
        }
      })
    },
    handleNodeClick(data) {
      if (data.type === 'file') {
        this.openNote(data.path)
      } else {
        // 文件夹不是可编辑笔记，选中时只显示文件夹占位并清空正文相关状态。
        this.currentPath = data.path
        this.currentNodeType = data.type
        this.title = data.name
        this.content = ''
        this.hash = ''
        this.resourceBase = ''
        this.dirty = false
      }
    },
    openNote(path) {
      if (this.dirty) {
        // 防止用户切换笔记时丢失未保存正文。
        this.$confirm('当前笔记尚未保存，是否继续打开其他笔记？', '提示', { type: 'warning' })
          .then(() => this.loadNote(path))
          .catch(() => {})
        return
      }
      this.loadNote(path)
    },
    loadNote(path) {
      // loadingNote 用来屏蔽编辑器初始化时可能抛出的 change 事件，避免刚打开就变成未保存。
      this.loadingNote = true
      getNoteContent(path).then(res => {
        const data = res.data || {}
        this.currentPath = data.path
        this.currentNodeType = 'file'
        this.title = this.fileTitle(data.path)
        this.content = data.content || ''
        this.hash = data.hash || ''
        this.resourceBase = data.resourceBase || ''
        this.dirty = false
        this.viewer = true
        this.$nextTick(() => {
          this.dirty = false
          this.loadingNote = false
        })
      }).catch(() => {
        this.loadingNote = false
      })
    },
    markDirty() {
      // 只有用户真实编辑正文时才置 dirty；加载笔记期间的同步事件会被忽略。
      if (!this.loadingNote) {
        this.dirty = true
      }
    },
    save() {
      saveNoteContent({
        path: this.currentPath,
        content: this.content,
        lastKnownHash: this.hash
      }).then(res => {
        const data = res.data || {}
        this.currentPath = data.path
        this.hash = data.hash || ''
        this.resourceBase = data.resourceBase || this.resourceBase
        this.dirty = false
        this.$modal.msgSuccess('保存成功')
        this.loadTree()
      })
    },
    handleEditorBlur() {
      // 保持轻量自动保存：编辑器失焦且正文有改动时保存。
      if (this.dirty) {
        this.save()
      }
    },
    handleCreate(type) {
      const base = this.selectedDirectory()
      const label = type === 'directory' ? '文件夹名称' : '笔记名称'
      this.$prompt(label, '新建', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /^(?!\s*$).+/,
        inputErrorMessage: label + '不能为空'
      }).then(({ value }) => {
        const path = this.joinPath(base, type === 'directory' ? value : this.ensureMd(value))
        createNoteFile({ path, type, content: '' }).then(res => {
          this.$modal.msgSuccess('创建成功')
          this.loadTree()
          if (type === 'file') {
            this.loadNote(res.data.path)
          }
        })
      }).catch(() => {})
    },
    handleDelete() {
      const path = this.currentPath
      this.$confirm(`确认删除 ${path}？`, '提示', { type: 'warning' }).then(() => {
        deleteNoteFile(path).then(() => {
          this.$modal.msgSuccess('删除成功')
          this.currentPath = ''
          this.currentNodeType = ''
          this.title = ''
          this.content = ''
          this.hash = ''
          this.dirty = false
          this.loadTree()
        })
      }).catch(() => {})
    },
    handleDownload() {
      downloadNoteFile(this.currentPath).then(blob => {
        saveAs(blob, this.basename(this.currentPath))
      })
    },
    handleTitleBlur() {
      this.renameByTitle(this.title)
    },
    renameByTitle(value) {
      if (!this.currentPath || this.renamingTitle) {
        return
      }
      const name = (value || '').trim()
      const oldName = this.currentNodeType === 'file' ? this.fileTitle(this.currentPath) : this.basename(this.currentPath)
      if (!name) {
        // 空标题不发起重命名，直接恢复原名称。
        this.title = oldName
        return
      }
      if (name === oldName) {
        return
      }
      this.renamingTitle = true
      const parent = this.dirname(this.currentPath)
      // 文件标题显示时不带 .md，但落盘仍保持 Markdown 文件后缀；文件夹则直接使用输入名称。
      const nextName = this.currentNodeType === 'file' ? this.ensureMd(name) : name
      renameNoteFile({ path: this.currentPath, newPath: this.joinPath(parent, nextName) }).then(res => {
        this.currentPath = res.data.path
        this.currentNodeType = res.data.type
        this.title = res.data.type === 'file' ? this.fileTitle(this.currentPath) : this.basename(this.currentPath)
        this.$modal.msgSuccess('标题已更新')
        this.loadTree()
      }).catch(() => {
        this.title = oldName
      }).finally(() => {
        this.renamingTitle = false
      })
    },
    selectedDirectory() {
      if (!this.currentPath) {
        return ''
      }
      return this.currentNodeType === 'directory' ? this.currentPath : this.dirname(this.currentPath)
    },
    ensureMd(name) {
      return name.endsWith('.md') ? name : name + '.md'
    },
    fileTitle(path) {
      return this.basename(path).replace(/\.md$/i, '')
    },
    basename(path) {
      const parts = (path || '').split('/')
      return parts[parts.length - 1] || ''
    },
    dirname(path) {
      const index = (path || '').lastIndexOf('/')
      return index > -1 ? path.substring(0, index) : ''
    },
    joinPath(parent, name) {
      return parent ? parent + '/' + name : name
    }
  }
}
</script>

<style scoped lang="scss">
.note-page {
  display: flex;
  height: calc(100vh - 84px);
  min-height: 640px;
  padding: 0;
  background: #fff;
  border: 1px solid #ebeef5;
  overflow: hidden;
}

.note-rail {
  width: 48px;
  flex: 0 0 48px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
  padding-top: 14px;
  color: #7d7d7d;
  background: #f6f6f6;
  border-right: 1px solid #e6e6e6;
}

.note-rail i {
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  font-size: 18px;
  cursor: pointer;
}

.note-rail i:hover,
.note-rail i.active {
  color: #303133;
  background: #e9e9e9;
}

.note-sidebar {
  width: 360px;
  flex: 0 0 360px;
  background: #f7f7f7;
  border-right: 1px solid #dedede;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.sidebar-toolbar {
  padding: 5px;
}

.sidebar-toolbar ::v-deep .el-input__inner {
  height: 40px;
  line-height: 40px;
  border-color: #dcdfe6;
  border-radius: 7px;
  background: #fff;
  color: #606266;
}

.sidebar-actions {
  display: flex;
  gap: 8px;
  padding: 0 16px 10px;
}

.sidebar-actions .el-button {
  border-color: transparent;
  background: transparent;
  color: #606266;
}

.sidebar-body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.note-tree,
.search-results {
  flex: 1;
  height: 100%;
  overflow: auto;
  padding: 10px;
}

.note-tree ::v-deep .el-tree {
  background: transparent;
}

.note-tree ::v-deep .el-tree-node__content {
  height: 28px;
  border-radius: 5px;
  color: #606266;
}

.note-tree ::v-deep .el-tree-node__content:hover {
  background: #ededed;
}

.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.tree-node.active {
  width: 100%;
  height: 24px;
  padding-right: 8px;
  border-radius: 5px;
  color: #303133;
  background: #e2e2e2;
}

.tree-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.search-item {
  padding: 9px 10px;
  border-radius: 6px;
  cursor: pointer;
}

.search-item:hover {
  background: #ededed;
}

.search-title {
  font-weight: 600;
  color: #303133;
}

.search-path,
.search-snippet {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.search-empty {
  color: #a8a8a8;
  font-size: 12px;
  font-weight: 600;
}

.note-workspace {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.workspace-topbar {
  height: 44px;
  flex: 0 0 44px;
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 0 18px;
  border-bottom: 1px solid #eeeeee;
  color: #909399;
}

.workspace-nav {
  display: flex;
  gap: 8px;
}

.workspace-nav .el-button {
  padding: 0;
  color: #909399;
}

.workspace-title-input {
  overflow: hidden;
}

.workspace-title-input ::v-deep .el-input__inner {
  height: 30px;
  border: none;
  text-align: center;
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  background: transparent;
}

.workspace-title-input.is-disabled ::v-deep .el-input__inner {
  background: transparent;
  color: #909399;
}

.workspace-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.note-toolbar {
  // display: flex;
  // align-items: center;
  padding: 10px 24px;
}

.title-input {
  width: min(880px, 100%);
}

.path-title {
  font-size: 12px;
  color: #909399;
  font-weight: 500;
  overflow-wrap: anywhere;
}

.title-input ::v-deep .el-input__inner {
  height: 52px;
  border: none;
  padding: 0;
  font-size: 34px;
  line-height: 52px;
  font-weight: 700;
  color: #2f2f2f;
  background: transparent;
}

.title-input.is-disabled ::v-deep .el-input__inner {
  background: transparent;
  color: #b0b0b0;
}

.note-canvas {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.editor-wrap {
  flex: 1;
  min-height: 0;
  padding: 24px 8vw 48px;
}

.note-empty {
  flex: 1;
  padding-top: 18vh;
}

.note-folder-empty {
  width: min(880px, calc(100% - 80px));
  min-height: 360px;
  margin: 56px auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.note-folder-empty i {
  font-size: 76px;
  color: #c0c4cc;
}

.folder-name {
  max-width: 80%;
  margin-top: 12px;
  font-size: 14px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 960px) {
  .note-page {
    flex-direction: column;
    height: auto;
    min-height: 0;
  }

  .note-rail {
    width: 100%;
    height: 42px;
    flex: 0 0 42px;
    flex-direction: row;
    padding: 0 12px;
    border-right: none;
    border-bottom: 1px solid #e6e6e6;
  }

  .note-sidebar {
    width: 100%;
    flex-basis: auto;
    height: 320px;
    border-right: none;
    border-bottom: 1px solid #e6e8eb;
    padding-right: 0;
    padding-bottom: 12px;
  }

  .workspace-topbar {
    grid-template-columns: auto minmax(0, 1fr);
    height: auto;
    min-height: 44px;
  }

  .workspace-actions {
    grid-column: 1 / -1;
    flex-wrap: wrap;
    padding-bottom: 10px;
  }

  .note-toolbar {
    padding: 14px 18px 0;
  }

  .title-input ::v-deep .el-input__inner {
    height: 42px;
    font-size: 26px;
  }

  .editor-wrap {
    padding: 18px;
  }
}
</style>
