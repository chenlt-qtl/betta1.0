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
        <i class="el-icon-collection-tag" :class="{ active: activePanel === 'favorites' }" @click="switchPanel('favorites')"></i>
      </el-tooltip>
      <el-tooltip content="打开/创建今天的日记" placement="right">
        <i
          :class="journalOpening ? 'el-icon-loading' : 'el-icon-date'"
          @click="handleOpenJournal"
          v-hasPermi="['system:note:add']"
        ></i>
      </el-tooltip>
      <el-tooltip content="日记设置" placement="right">
        <i class="el-icon-setting" @click="openJournalSettings" v-hasPermi="['system:note:list']"></i>
      </el-tooltip>
      <el-tooltip :content="sidebarCollapsed ? '展开侧栏' : '收起侧栏'" placement="right">
        <i
          class="sidebar-toggle"
          :class="sidebarCollapsed ? 'el-icon-s-unfold' : 'el-icon-s-fold'"
          @click="toggleSidebar"
        ></i>
      </el-tooltip>
    </nav>

    <aside v-show="!sidebarCollapsed" class="note-sidebar">
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
        <el-popover
          v-model="folderDropdownVisible"
          placement="bottom-start"
          width="260"
          trigger="click"
          popper-class="note-folder-popover"
        >
          <el-tree
            class="folder-filter-tree"
            node-key="path"
            :data="folderTreeData"
            :props="treeProps"
            :expand-on-click-node="false"
            default-expand-all
            @node-click="handleFolderFilterClick"
          >
            <span slot-scope="{ node, data }" class="tree-node" :class="{ active: data.path === selectedFolderPath }">
              <i :class="data.path ? 'el-icon-folder' : 'el-icon-folder-opened'"></i>
              <span class="tree-label">{{ node.label }}</span>
            </span>
          </el-tree>
          <el-button slot="reference" class="folder-filter-btn" size="mini" icon="el-icon-folder-opened">
            {{ selectedFolderName }}
            <i class="el-icon-arrow-down el-icon--right"></i>
          </el-button>
        </el-popover>
        <el-dropdown trigger="click" @command="handleCreate" v-hasPermi="['system:note:add']">
          <el-button size="mini" icon="el-icon-plus" type="primary" plain>
            <i class="el-icon-arrow-down el-icon--right"></i>
          </el-button>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item command="file">文件</el-dropdown-item>
            <el-dropdown-item command="directory">文件夹</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
        <el-tooltip content="移动已勾选项目" placement="top">
          <span>
            <el-button
              size="mini"
              icon="el-icon-rank"
              :disabled="!moveSelection.length || moveSubmitting"
              @click="openMoveDialog"
              v-hasPermi="['system:note:edit']"
            >移动</el-button>
          </span>
        </el-tooltip>
      </div>
      <div class="sidebar-body">
        <el-tree
          v-if="activePanel === 'files'"
          ref="tree"
          class="note-tree"
          node-key="path"
          :data="visibleTreeData"
          :props="treeProps"
          :expand-on-click-node="false"
          show-checkbox
          check-strictly
          default-expand-all
          @check-change="handleMoveCheckChange"
          @node-click="handleNodeClick"
        >
          <span slot-scope="{ node, data }" class="tree-node" :class="{ active: data.path === currentPath }">
            <i :class="data.type === 'directory' ? 'el-icon-folder' : 'el-icon-document'"></i>
            <span class="tree-label">{{ node.label }}</span>
          </span>
        </el-tree>
        <div v-else-if="activePanel === 'search'" class="search-results">
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
        <div v-else class="favorite-results">
          <div v-if="favoriteLoading" class="favorite-empty">收藏加载中...</div>
          <template v-else>
            <div v-if="favoriteLoadError" class="favorite-empty">
              收藏加载失败，
              <el-button type="text" size="mini" @click="loadFavorites">重新加载</el-button>
            </div>
            <div
              v-for="item in favoriteNotes"
              :key="item.path"
              class="favorite-item"
              @click="openNote(item.path)"
            >
              <div class="favorite-title">{{ item.name }}</div>
              <div class="favorite-path">{{ item.path }}</div>
            </div>
            <div v-if="!favoriteLoadError && !favoriteNotes.length" class="favorite-empty">暂无收藏笔记。</div>
          </template>
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
          @input="incrementNoteContextVersion"
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
          <el-tooltip :content="currentFavorite ? '取消收藏' : '收藏笔记'" placement="bottom">
            <span>
              <el-button
                class="favorite-button"
                :class="{ 'is-favorite': currentFavorite }"
                size="mini"
                :icon="'el-icon-star-off'"
                :disabled="!currentPath || currentNodeType !== 'file' || favoriteUpdating"
                @click="handleFavorite"
                v-hasPermi="['system:note:edit']"
              ></el-button>
            </span>
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

    <el-dialog
      title="移动到"
      :visible.sync="moveDialogVisible"
      width="500px"
      append-to-body
      :close-on-click-modal="false"
      @closed="resetMoveTarget"
    >
      <div class="move-summary">
        已选择 {{ moveSelection.length }} 个{{ moveSelectionType === 'directory' ? '文件夹' : '文件' }}
      </div>
      <el-tree
        class="move-folder-tree"
        node-key="path"
        :data="moveFolderTreeData"
        :props="treeProps"
        :expand-on-click-node="false"
        default-expand-all
        @node-click="handleMoveTargetClick"
      >
        <span
          slot-scope="{ node, data }"
          class="tree-node"
          :class="{ active: moveTargetSelected && data.path === moveTargetDirectory, disabled: data.disabled }"
        >
          <i :class="data.path ? 'el-icon-folder' : 'el-icon-folder-opened'"></i>
          <span class="tree-label">{{ node.label }}</span>
        </span>
      </el-tree>
      <span slot="footer" class="dialog-footer">
        <el-button @click="moveDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="moveSubmitting" @click="submitMove">确定移动</el-button>
      </span>
    </el-dialog>

    <el-dialog
      title="日记设置"
      :visible.sync="journalSettingsVisible"
      width="500px"
      append-to-body
      :close-on-click-modal="false"
    >
      <div class="journal-settings-label">日记的存放位置</div>
      <div class="journal-settings-help">选择每天日记笔记的保存目录，默认保存到根目录。</div>
      <el-tree
        ref="journalFolderTree"
        class="journal-folder-tree"
        node-key="path"
        :data="journalFolderTreeData"
        :props="treeProps"
        :expand-on-click-node="false"
        :current-node-key="journalDirectory"
        highlight-current
        default-expand-all
        @node-click="handleJournalDirectoryClick"
      >
        <span slot-scope="{ node, data }" class="tree-node" :class="{ active: data.path === journalDirectory }">
          <i :class="data.path ? 'el-icon-folder' : 'el-icon-folder-opened'"></i>
          <span class="tree-label">{{ node.label }}</span>
        </span>
      </el-tree>
      <span slot="footer" class="dialog-footer">
        <el-button @click="journalSettingsVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="journalSettingsSaving"
          @click="saveJournalSettings"
          v-hasPermi="['system:note:edit']"
        >保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { saveAs } from 'file-saver'
import NoteMarkdown from '@/components/NoteMarkdown'
import {
  createNoteFile,
  deleteNoteFile,
  downloadNoteFile,
  getFavoriteNotes,
  getJournalSettings,
  getNoteContent,
  getNoteTree,
  moveNoteFiles,
  openTodayJournal,
  renameNoteFile,
  saveNoteContent,
  searchNotes,
  updateJournalSettings,
  updateNoteFavorite
} from '@/api/note'

export default {
  name: 'NoteIndex',
  components: { NoteMarkdown },
  data() {
    return {
      // 左侧搜索关键词；搜索面板输入时会做防抖请求。
      keyword: '',
      // 左侧工具栏当前激活的面板：files 文件树、search 搜索结果、favorites 收藏列表。
      activePanel: 'files',
      // 折叠时仅隐藏 360px 侧栏，保留 48px 工具栏和侧栏内的业务状态。
      sidebarCollapsed: false,
      searchResults: [],
      // 搜索 loading 与请求序号配合使用，避免慢请求覆盖后输入的新结果。
      searchLoading: false,
      searchTimer: null,
      searchSeq: 0,
      // 收藏列表是当前笔记收藏状态的唯一数据源，接口失败时不提前修改本地状态。
      favoriteNotes: [],
      favoriteLoading: false,
      favoriteLoadError: false,
      favoriteLoadSeq: 0,
      favoriteUpdating: false,
      // 日记创建和设置分别加锁，防止连续点击产生重复请求或旧设置覆盖新选择。
      journalOpening: false,
      journalSettingsVisible: false,
      journalSettingsLoading: false,
      journalSettingsSaving: false,
      journalDirectory: '',
      // 后端返回的 Markdown vault 文件树，仅展示笔记和普通目录，图片附件目录在后端已隐藏。
      treeData: [],
      treeProps: {
        children: 'children',
        label: 'name'
      },
      // 文件夹筛选只影响左侧主树展示；空路径代表 vault 根目录。
      selectedFolderPath: '',
      folderDropdownVisible: false,
      // 移动选择与普通节点点击相互独立；文件可多选，文件夹只能单选且不能与文件混选。
      moveSelection: [],
      syncingMoveChecks: false,
      moveDialogVisible: false,
      moveTargetDirectory: '',
      moveTargetSelected: false,
      moveSubmitting: false,
      // 当前选中的 vault 相对路径和节点类型；文件夹选中时会清空正文区域。
      currentPath: '',
      currentNodeType: '',
      // 单调版本用于识别导航、正文和标题发生过 A→B→A，避免迟到的日记响应覆盖新上下文。
      noteContextVersion: 0,
      // 顶部标题输入框内容。文件失焦时会重命名文件，文件夹失焦时会重命名目录。
      title: '',
      // 当前笔记正文、服务端 hash 和资源访问前缀。hash 用于保存时做冲突检测。
      content: '',
      hash: '',
      resourceBase: '',
      // dirty 只代表正文是否有真实用户改动；初始化/切换笔记不会置为未保存。
      dirty: false,
      loadingNote: false,
      // 保存 Promise 用于串行化移动，避免旧路径保存与文件移动并发后重新创建源文件。
      savePromise: null,
      renamingTitle: false,
      // 目录状态由页面持有，按钮放在顶部工具栏；手机端默认收起以节省横向空间。
      tocCollapsed: this.isMobileViewport(),
      viewer: true
    }
  },
  created() {
    this.loadTree()
    // 初始化即加载收藏，用于推导默认打开笔记后的收藏按钮状态。
    this.loadFavorites()
  },
  computed: {
    folderTreeData() {
      // 下拉框只展示文件夹节点，并额外提供根目录入口用于恢复显示全部根级内容。
      return [{
        name: '全部笔记',
        path: '',
        type: 'directory',
        children: this.buildFolderTree(this.treeData)
      }]
    },
    visibleTreeData() {
      if (!this.selectedFolderPath) {
        return this.treeData
      }
      const selectedNode = this.findNodeByPath(this.treeData, this.selectedFolderPath)
      return selectedNode && selectedNode.children ? selectedNode.children : []
    },
    selectedFolderName() {
      if (!this.selectedFolderPath) {
        return '全部笔记'
      }
      const selectedNode = this.findNodeByPath(this.treeData, this.selectedFolderPath)
      return selectedNode ? selectedNode.name : '全部笔记'
    },
    moveSelectionType() {
      return this.moveSelection.length ? this.moveSelection[0].type : ''
    },
    moveFolderTreeData() {
      // 目标树只保留目录，并标记会造成原地移动或目录循环的非法目标。
      return [{
        name: '全部笔记（根目录）',
        path: '',
        type: 'directory',
        disabled: this.isInvalidMoveTarget(''),
        children: this.buildMoveFolderTree(this.treeData)
      }]
    },
    journalFolderTreeData() {
      // 日记只能保存到真实目录；空路径作为固定的 vault 根目录入口。
      return [{
        name: '根目录',
        path: '',
        type: 'directory',
        children: this.buildFolderTree(this.treeData)
      }]
    },
    currentFavorite() {
      return this.currentNodeType === 'file' && this.favoriteNotes.some(item => item.path === this.currentPath)
    }
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
      return getNoteTree().then(res => {
        this.treeData = res.data || []
        this.ensureSelectedFolderExists()
        this.syncMoveSelectionWithTree()
      })
    },
    loadFavorites() {
      // 仅允许最后发起的请求落地，避免旧响应覆盖收藏操作、重命名或删除后的新状态。
      const seq = ++this.favoriteLoadSeq
      this.favoriteLoading = true
      this.favoriteLoadError = false
      return getFavoriteNotes().then(res => {
        if (seq !== this.favoriteLoadSeq) {
          // 旧请求已被更新请求接管，不再参与当前页面状态和失败提示。
          return true
        }
        this.favoriteNotes = res.data || []
        return true
      }).catch(() => {
        // 加载失败时保留已有列表；错误状态只由最新请求维护，旧请求失败不干扰当前视图。
        if (seq !== this.favoriteLoadSeq) {
          return true
        }
        this.favoriteLoadError = true
        // 收藏加载已转换为页面错误态，返回明确标记避免 fire-and-forget 调用产生未处理拒绝。
        return false
      }).finally(() => {
        if (seq === this.favoriteLoadSeq) {
          this.favoriteLoading = false
        }
      })
    },
    buildFolderTree(nodes) {
      // 主树里同时有文件和文件夹；下拉筛选树只保留文件夹，避免用户选中文件后语义不清。
      return (nodes || []).filter(item => item.type === 'directory').map(item => ({
        ...item,
        children: this.buildFolderTree(item.children || [])
      }))
    },
    buildMoveFolderTree(nodes) {
      return (nodes || []).filter(item => item.type === 'directory').map(item => ({
        ...item,
        disabled: this.isInvalidMoveTarget(item.path),
        children: this.buildMoveFolderTree(item.children || [])
      }))
    },
    findNodeByPath(nodes, path) {
      for (const item of nodes || []) {
        if (item.path === path) {
          return item
        }
        const child = this.findNodeByPath(item.children || [], path)
        if (child) {
          return child
        }
      }
      return null
    },
    ensureSelectedFolderExists() {
      // 目录被删除或重命名后，筛选路径会失效；此时回到根目录，避免主树长期显示空白。
      if (this.selectedFolderPath && !this.findNodeByPath(this.treeData, this.selectedFolderPath)) {
        this.selectedFolderPath = ''
      }
    },
    handleFolderFilterClick(data) {
      const nextPath = data.path || ''
      const applyFolderFilter = () => {
        // 主树数据范围即将变化，清空移动选择，避免隐藏项目仍保留在待移动列表中。
        this.clearMoveSelection()
        this.selectedFolderPath = nextPath
        this.folderDropdownVisible = false
        // 切换筛选目录后，如果右侧仍停留在目录外的旧笔记，清空详情避免保存/删除对象和左树上下文不一致。
        if (!this.isPathInDirectory(this.currentPath, nextPath)) {
          this.clearCurrentSelection()
        }
      }
      if (this.dirty && !this.isPathInDirectory(this.currentPath, nextPath)) {
        this.$confirm('当前笔记尚未保存，切换文件夹会清空当前详情，是否继续？', '提示', { type: 'warning' })
          .then(applyFolderFilter)
          .catch(() => {})
        return
      }
      applyFolderFilter()
    },
    isPathInDirectory(path, directory) {
      if (!path || !directory) {
        return true
      }
      return path === directory || path.indexOf(directory + '/') === 0
    },
    clearCurrentSelection() {
      // 统一清空右侧详情状态，供删除和筛选目录切换复用。
      this.incrementNoteContextVersion()
      this.currentPath = ''
      this.currentNodeType = ''
      this.title = ''
      this.content = ''
      this.hash = ''
      this.resourceBase = ''
      this.dirty = false
      this.loadingNote = false
    },
    switchPanel(panel) {
      this.activePanel = panel
      if (panel === 'files') {
        // 切回文件树时清空搜索状态，避免下次进入搜索面板看到旧结果。
        this.clearSearch()
      } else if (panel === 'favorites') {
        this.clearMoveSelection()
        // 每次进入收藏面板都从服务端刷新，覆盖重命名等场景下可能变化的路径。
        this.loadFavorites()
      } else if (panel === 'search') {
        this.clearMoveSelection()
        // 搜索面板打开后自动聚焦，让用户可以直接输入。
        this.$nextTick(() => {
          if (this.$refs.searchInput) {
            this.$refs.searchInput.focus()
          }
        })
      }
    },
    toggleSidebar() {
      // v-show 只切换可见性，再次展开时不会重建树或触发额外请求。
      this.sidebarCollapsed = !this.sidebarCollapsed
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
        this.incrementNoteContextVersion()
        this.currentPath = data.path
        this.currentNodeType = data.type
        this.title = data.name
        this.content = ''
        this.hash = ''
        this.resourceBase = ''
        this.dirty = false
      }
    },
    handleMoveCheckChange(data, checked) {
      if (this.syncingMoveChecks || !this.$refs.tree) {
        return
      }
      const checkedNodes = this.$refs.tree.getCheckedNodes(false, false)
      let nextSelection = checkedNodes
      if (checked && data.type === 'directory') {
        // 目录移动会携带全部后代，一次仅允许选择一个目录。
        nextSelection = [data]
      } else if (checked && data.type === 'file' && checkedNodes.some(item => item.type === 'directory')) {
        // 从目录切换到文件时清除目录，随后可以继续勾选其他文件。
        nextSelection = [data]
      }
      this.moveSelection = nextSelection
      const nextKeys = nextSelection.map(item => item.path)
      if (nextKeys.length !== checkedNodes.length || checkedNodes.some(item => !nextKeys.includes(item.path))) {
        this.syncingMoveChecks = true
        this.$refs.tree.setCheckedKeys(nextKeys)
        this.$nextTick(() => {
          this.syncingMoveChecks = false
        })
      }
    },
    openMoveDialog() {
      if (!this.moveSelection.length || this.moveSubmitting) {
        return
      }
      // 默认选择当前筛选目录；若任一源项目已在该目录，则要求用户重新选择有效目标。
      this.moveTargetDirectory = this.selectedFolderPath || ''
      this.moveTargetSelected = !this.isInvalidMoveTarget(this.moveTargetDirectory)
      this.moveDialogVisible = true
    },
    handleMoveTargetClick(data) {
      if (data.disabled || this.isInvalidMoveTarget(data.path)) {
        return
      }
      this.moveTargetDirectory = data.path || ''
      this.moveTargetSelected = true
    },
    isInvalidMoveTarget(targetDirectory) {
      if (!this.moveSelection.length) {
        return false
      }
      return this.moveSelection.some(item => {
        // 任一项目已位于目标目录时，整批移动都会被后端拒绝。
        if (this.dirname(item.path) === targetDirectory) {
          return true
        }
        return item.type === 'directory'
          && (targetDirectory === item.path || targetDirectory.indexOf(item.path + '/') === 0)
      })
    },
    submitMove() {
      if (!this.moveTargetSelected || this.isInvalidMoveTarget(this.moveTargetDirectory)) {
        this.$modal.msgWarning('请选择其他目标文件夹')
        return
      }
      const sources = this.moveSelection.map(item => item.path)
      const sourceTypes = this.moveSelection.map(item => item.type)
      this.moveSubmitting = true
      if (sources.some((source, index) => this.currentPath === source
        || (sourceTypes[index] === 'directory' && this.currentPath.indexOf(source + '/') === 0))) {
        // 当前对象即将移动时立即推进版本，不等待接口返回，避免并发日记响应先落地。
        this.incrementNoteContextVersion()
      }
      // 若编辑器失焦已经触发自动保存，必须等旧路径保存完成后才能移动。
      const pendingSave = this.savePromise || Promise.resolve()
      pendingSave.then(() => moveNoteFiles({
        paths: sources,
        targetDirectory: this.moveTargetDirectory
      })).then(res => {
        const movedPaths = res.data || []
        // 响应顺序与请求一致，可据此更新当前编辑对象和筛选目录而无需重新读取正文。
        this.updatePathsAfterMove(sources, sourceTypes, movedPaths)
        this.clearMoveSelection()
        this.moveDialogVisible = false
        this.$modal.msgSuccess('移动成功')
        this.loadTree()
        this.loadFavorites()
      }).finally(() => {
        this.moveSubmitting = false
      })
    },
    updatePathsAfterMove(sources, sourceTypes, movedPaths) {
      sources.forEach((source, index) => {
        const movedPath = movedPaths[index]
        if (!movedPath) {
          return
        }
        if (this.currentPath === source) {
          this.currentPath = movedPath
        } else if (sourceTypes[index] === 'directory' && this.currentPath.indexOf(source + '/') === 0) {
          this.currentPath = movedPath + this.currentPath.substring(source.length)
        }
        if (this.selectedFolderPath === source) {
          this.selectedFolderPath = movedPath
        } else if (sourceTypes[index] === 'directory' && this.selectedFolderPath.indexOf(source + '/') === 0) {
          this.selectedFolderPath = movedPath + this.selectedFolderPath.substring(source.length)
        }
      })
    },
    clearMoveSelection() {
      this.moveSelection = []
      if (this.$refs.tree) {
        this.syncingMoveChecks = true
        this.$refs.tree.setCheckedKeys([])
        this.$nextTick(() => {
          this.syncingMoveChecks = false
        })
      }
    },
    syncMoveSelectionWithTree() {
      const selectedPaths = this.moveSelection.map(item => item.path)
      // 树刷新会重建 Element UI 节点，只恢复当前筛选范围内仍存在的项目，保持视觉勾选与业务状态一致。
      const nextSelection = selectedPaths.map(path => this.findNodeByPath(this.visibleTreeData, path)).filter(Boolean)
      this.moveSelection = nextSelection
      this.$nextTick(() => {
        if (!this.$refs.tree) {
          return
        }
        this.syncingMoveChecks = true
        this.$refs.tree.setCheckedKeys(nextSelection.map(item => item.path))
        this.$nextTick(() => {
          this.syncingMoveChecks = false
        })
      })
    },
    resetMoveTarget() {
      this.moveTargetDirectory = ''
      this.moveTargetSelected = false
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
    handleOpenJournal() {
      if (this.journalOpening) {
        return
      }
      const openJournal = () => {
        // 用户可能连续确认多个弹窗，发请求前再次检查锁，确保始终只有一个日记请求。
        if (this.journalOpening) {
          return
        }
        // 请求期间仍允许用户继续操作；记录当前正文上下文，防止迟到响应覆盖后续编辑或导航。
        const context = {
          version: this.noteContextVersion,
          content: this.content,
          dirty: this.dirty
        }
        this.journalOpening = true
        // 接口已经返回完整正文，直接打开可避免创建后再发起一次内容读取。
        openTodayJournal().then(res => {
          this.loadTree()
          const contextUnchanged = this.noteContextVersion === context.version
            && this.content === context.content
            && this.dirty === context.dirty
          if (contextUnchanged) {
            this.applyNoteContent(res.data || {})
            return
          }
          this.$modal.msgWarning('日记已创建/打开但当前内容已变化，可再次点击日记打开')
        }).finally(() => {
          this.journalOpening = false
        })
      }
      if (this.dirty) {
        this.$confirm('当前笔记尚未保存，是否继续打开今天的日记？', '提示', { type: 'warning' })
          .then(openJournal)
          .catch(() => {})
        return
      }
      openJournal()
    },
    openJournalSettings() {
      if (this.journalSettingsLoading) {
        return
      }
      this.journalSettingsLoading = true
      const showJournalSettings = () => {
        this.journalSettingsVisible = true
        this.$nextTick(() => {
          if (this.$refs.journalFolderTree) {
            this.$refs.journalFolderTree.setCurrentKey(this.journalDirectory)
          }
        })
      }
      getJournalSettings().then(res => {
        const data = res.data || {}
        this.journalDirectory = data.directory || ''
        showJournalSettings()
      }).catch(() => {
        // 已配置目录失效时仍开放设置入口，让用户可以从根目录重新选择并修复配置。
        this.journalDirectory = ''
        showJournalSettings()
        this.$modal.msgWarning('日记保存位置读取失败，请重新选择并保存目录')
      }).finally(() => {
        this.journalSettingsLoading = false
      })
    },
    handleJournalDirectoryClick(data) {
      this.journalDirectory = data.path || ''
    },
    saveJournalSettings() {
      if (this.journalSettingsSaving) {
        return
      }
      this.journalSettingsSaving = true
      updateJournalSettings({ directory: this.journalDirectory }).then(res => {
        const data = res.data || {}
        this.journalDirectory = data.directory || ''
        this.journalSettingsVisible = false
        this.$modal.msgSuccess('日记保存位置已更新')
      }).finally(() => {
        this.journalSettingsSaving = false
      })
    },
    loadNote(path) {
      // loadingNote 用来屏蔽编辑器初始化时可能抛出的 change 事件，避免刚打开就变成未保存。
      this.incrementNoteContextVersion()
      this.loadingNote = true
      getNoteContent(path).then(res => {
        this.applyNoteContent(res.data || {})
      }).catch(() => {
        this.loadingNote = false
      })
    },
    applyNoteContent(data) {
      // 普通打开和日记创建共用同一状态入口，保证 viewer、hash 与资源路径保持一致。
      this.loadingNote = true
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
    },
    handleFavorite() {
      if (!this.currentPath || this.currentNodeType !== 'file' || this.favoriteUpdating) {
        return
      }
      const favorite = !this.currentFavorite
      this.favoriteUpdating = true
      updateNoteFavorite({ path: this.currentPath, favorite }).then(res => {
        // 更新成功后重新读取收藏列表，按钮状态和收藏面板始终以服务端最终数据为准。
        return this.loadFavorites().then(refreshSucceeded => {
          if (refreshSucceeded) {
            this.$modal.msgSuccess(res.data === true ? '收藏成功' : '已取消收藏')
            return
          }
          this.$modal.msgWarning(res.data === true
            ? '收藏已成功，但列表刷新失败，请重新加载'
            : '收藏已取消，但列表刷新失败，请重新加载')
        })
      }).finally(() => {
        this.favoriteUpdating = false
      })
    },
    markDirty() {
      // 只有用户真实编辑正文时才置 dirty；加载笔记期间的同步事件会被忽略。
      if (!this.loadingNote) {
        this.incrementNoteContextVersion()
        this.dirty = true
      }
    },
    incrementNoteContextVersion() {
      this.noteContextVersion += 1
    },
    save() {
      if (this.savePromise) {
        return this.savePromise
      }
      const savingPath = this.currentPath
      const savingContent = this.content
      const request = saveNoteContent({
        path: savingPath,
        content: savingContent,
        lastKnownHash: this.hash
      }).then(res => {
        const data = res.data || {}
        // 路径已被移动或重命名时，迟到的保存响应不得把页面状态覆盖回旧路径。
        if (this.currentPath !== savingPath) {
          return
        }
        if (data.path !== this.currentPath) {
          this.incrementNoteContextVersion()
        }
        this.currentPath = data.path
        this.hash = data.hash || ''
        this.resourceBase = data.resourceBase || this.resourceBase
        // 保存期间继续编辑时仅更新服务端 hash，保留未保存标记等待下一次保存。
        this.dirty = this.content !== savingContent
        this.$modal.msgSuccess('保存成功')
        this.loadTree()
      }).finally(() => {
        if (this.savePromise === request) {
          this.savePromise = null
        }
      })
      this.savePromise = request
      return request
    },
    handleEditorBlur() {
      // 保持轻量自动保存：编辑器失焦且正文有改动时保存。
      if (this.dirty && !this.moveSubmitting) {
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
        // 删除确认即代表用户要离开当前对象，先失效仍在途的日记响应。
        this.incrementNoteContextVersion()
        deleteNoteFile(path).then(() => {
          this.$modal.msgSuccess('删除成功')
          this.clearCurrentSelection()
          this.loadTree()
          this.loadFavorites()
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
        this.incrementNoteContextVersion()
        this.currentPath = res.data.path
        this.currentNodeType = res.data.type
        this.title = res.data.type === 'file' ? this.fileTitle(this.currentPath) : this.basename(this.currentPath)
        this.$modal.msgSuccess('标题已更新')
        this.loadTree()
        this.loadFavorites()
      }).catch(() => {
        this.title = oldName
      }).finally(() => {
        this.renamingTitle = false
      })
    },
    selectedDirectory() {
      // 下拉筛选目录代表当前文件管理上下文，新建笔记/文件夹时优先使用它作为目标目录。
      if (this.selectedFolderPath) {
        return this.selectedFolderPath
      }
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

.note-rail .sidebar-toggle {
  margin-top: auto;
  margin-bottom: 14px;
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
  // 侧栏宽度不足时允许操作入口换行，避免移动按钮被父级裁剪。
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 0 16px 10px;
}

.sidebar-actions .el-button {
  // 按钮间距统一由 flex gap 控制，避免 Element UI 默认左边距重复叠加。
  margin-left: 0;
  border-color: transparent;
  background: transparent;
  color: #606266;
}

.folder-filter-btn {
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.folder-filter-tree {
  max-height: 320px;
  overflow: auto;
  background: #fff;
}

.move-summary {
  margin-bottom: 10px;
  color: #606266;
  font-size: 13px;
}

.move-folder-tree {
  max-height: 360px;
  overflow: auto;
}

.journal-settings-label {
  color: #303133;
  font-weight: 600;
}

.journal-settings-help {
  margin: 6px 0 12px;
  color: #909399;
  font-size: 13px;
}

.journal-folder-tree {
  max-height: 360px;
  overflow: auto;
}

.move-folder-tree .tree-node.disabled {
  color: #c0c4cc;
  cursor: not-allowed;
}

.folder-filter-tree ::v-deep .el-tree-node__content {
  height: 28px;
  border-radius: 5px;
}

.folder-filter-tree ::v-deep .el-tree-node__content:hover {
  background: #f5f7fa;
}

.sidebar-body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.note-tree,
.search-results,
.favorite-results {
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

.search-item,
.favorite-item {
  padding: 9px 10px;
  border-radius: 6px;
  cursor: pointer;
}

.search-item:hover,
.favorite-item:hover {
  background: #ededed;
}

.search-title,
.favorite-title {
  font-weight: 600;
  color: #303133;
}

.search-path,
.search-snippet,
.favorite-path {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.favorite-path {
  // 收藏面板需要展示完整路径，长路径允许换行而不是省略。
  white-space: normal;
  overflow-wrap: anywhere;
}

.search-empty,
.favorite-empty {
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

// 已收藏时仅突出星标图标，未收藏状态继续沿用 Element UI 默认颜色。
.favorite-button.is-favorite {
  color: #E6A23C;
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
  padding: 24px 2vw 48px;
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

  .note-rail .sidebar-toggle {
    margin-top: 0;
    margin-bottom: 0;
    margin-left: auto;
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
