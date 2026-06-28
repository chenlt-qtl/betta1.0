<template>
  <div class="note-markdown">
    <editor
      v-show="!viewer"
      ref="editor"
      :initial-value="value || ''"
      :options="editorOptions"
      height="calc(100vh - 230px)"
      initial-edit-type="markdown"
      preview-style="vertical"
      @change="handleChange"
      @blur="handleBlur"
    />
    <div v-show="viewer" class="viewer-shell" :class="{ 'toc-collapsed': tocCollapsed }">
      <div class="toc">
        <div class="toc-header">
          <span v-show="!tocCollapsed" class="toc-title">目录</span>
        </div>
        <div
          v-show="!tocCollapsed"
          v-for="item in headings"
          :key="item.id"
          class="toc-item"
          :style="{ paddingLeft: ((item.level - 1) * 12) + 'px' }"
          @click="scrollToHeading(item.id)"
        >
          {{ item.text }}
        </div>
      </div>
      <viewer ref="viewer" class="viewer" height="calc(100vh - 230px)" />
    </div>
  </div>
</template>

<script>
import { Editor, Viewer } from '@toast-ui/vue-editor'
import '@toast-ui/editor/dist/toastui-editor.css'
import '@toast-ui/editor/dist/toastui-editor-viewer.css'
import 'codemirror/lib/codemirror.css'
import { uploadNoteImage } from '@/api/note'

export default {
  name: 'NoteMarkdown',
  components: {
    editor: Editor,
    viewer: Viewer
  },
  props: {
    // v-model 传入的 Markdown 原文，服务端直接按该内容写入 .md 文件。
    value: {
      type: String,
      default: ''
    },
    // true 使用 Toast UI Viewer 预览；false 使用 Editor 编辑。
    viewer: {
      type: Boolean,
      default: false
    },
    // 当前笔记在 vault 内的相对路径，上传图片时后端需要据此计算附件目录。
    notePath: {
      type: String,
      default: ''
    },
    // 后端返回的 /file/notes/{user}/ 前缀，仅用于预览时把相对图片路径转成可访问 URL。
    resourceBase: {
      type: String,
      default: ''
    },
    // 目录展开状态由父页面控制，按钮放在顶部工具栏，组件只负责显示。
    tocCollapsed: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      headings: [],
      // setMarkdown 会触发编辑器 change，使用该标记区分“程序同步”和“用户编辑”。
      syncingEditor: false,
      editorOptions: {
        hooks: {
          addImageBlobHook: this.uploadImage
        }
      }
    }
  },
  watch: {
    value() {
      this.refresh()
    },
    viewer() {
      this.refresh()
    },
    notePath() {
      this.refresh()
    },
    resourceBase() {
      this.refresh()
    }
  },
  mounted() {
    this.refresh()
  },
  methods: {
    refresh() {
      this.$nextTick(() => {
        if (this.$refs.editor) {
          const current = this.$refs.editor.invoke('getMarkdown')
          if (current !== (this.value || '')) {
            // 父组件切换笔记或加载内容时同步编辑器内容，但不能因此标记为未保存。
            this.syncingEditor = true
            this.$refs.editor.invoke('setMarkdown', this.value || '', false)
            window.setTimeout(() => {
              this.syncingEditor = false
            }, 0)
          }
        }
        if (this.$refs.viewer) {
          // 预览内容会把 Markdown 中的相对图片路径转换成浏览器可访问的 /file/** 地址。
          this.$refs.viewer.invoke('setMarkdown', this.displayMarkdown(), false)
          this.$nextTick(this.decorateViewerHeadings)
        }
        this.parseHeadings()
      })
    },
    handleChange() {
      const markdown = this.$refs.editor.invoke('getMarkdown')
      // 忽略初始化/同步触发的 change，以及内容未变化的冗余事件。
      if (this.syncingEditor || markdown === (this.value || '')) {
        return
      }
      this.$emit('input', markdown)
      this.$emit('change', markdown)
    },
    handleBlur() {
      this.$emit('blur', this.$refs.editor.invoke('getMarkdown'))
    },
    uploadImage(file, callback) {
      if (!this.notePath) {
        this.$message.warning('请先选择或保存一篇笔记')
        return
      }
      if (!this.validateImage(file)) {
        return
      }
      uploadNoteImage(file, this.notePath).then(res => {
        // 后端返回完整 Markdown，这里只把图片 src 交给 Toast UI 插入，保持编辑器默认行为。
        const markdown = res.data && res.data.markdown
        const match = markdown && markdown.match(/\]\((.*)\)$/)
        callback(match ? match[1] : res.data.url, file.name)
        this.$emit('image-uploaded', res.data)
      })
    },
    validateImage(file) {
      const allowed = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp', 'image/svg+xml']
      if (!allowed.includes(file.type)) {
        this.$message.error('图片格式仅支持 jpg、png、gif、webp、svg')
        return false
      }
      if (file.size / 1024 / 1024 > 10) {
        this.$message.error('图片大小不能超过 10MB')
        return false
      }
      return true
    },
    displayMarkdown() {
      const content = this.value || ''
      if (!this.resourceBase || !this.notePath) {
        return content
      }
      const noteDir = this.notePath.includes('/') ? this.notePath.substring(0, this.notePath.lastIndexOf('/')) : ''
      return content.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, (all, alt, src) => {
        // 网络地址、data URI、已经是 /file/** 的地址不再转换。
        if (/^(https?:|data:|\/file\/|#)/.test(src)) {
          return all
        }
        // Obsidian 写入的是相对路径，网页预览时需要基于当前笔记目录解析到 vault 根路径。
        const resolved = this.resolveVaultPath(noteDir, decodeURI(src))
        return `![${alt}](${this.resourceBase}${encodeURI(resolved)})`
      })
    },
    resolveVaultPath(noteDir, src) {
      // 手动规整 ./ 和 ../，避免预览图片时生成错误的资源路径。
      const parts = []
      const base = noteDir ? noteDir.split('/') : []
      base.concat(src.split('/')).forEach(part => {
        if (!part || part === '.') {
          return
        }
        if (part === '..') {
          parts.pop()
          return
        }
        parts.push(part)
      })
      return parts.join('/')
    },
    parseHeadings() {
      // 从 Markdown 原文中提取标题，作为左侧目录数据；预览 DOM 的 id 在 decorateViewerHeadings 中补齐。
      const headings = []
      ;(this.value || '').split('\n').forEach((line, index) => {
        const match = line.match(/^(#{1,6})\s+(.+)$/)
        if (match) {
          headings.push({
            id: `note-heading-${index}`,
            level: match[1].length,
            text: match[2].replace(/[#*_`[\]]/g, '').trim()
          })
        }
      })
      this.headings = headings
    },
    decorateViewerHeadings() {
      const root = this.$el.querySelector('.toastui-editor-contents')
      if (!root) {
        return
      }
      // Toast UI 渲染后的标题没有稳定 id，这里和 parseHeadings 使用同一 index 规则便于目录跳转。
      const headings = root.querySelectorAll('h1,h2,h3,h4,h5,h6')
      headings.forEach((heading, index) => {
        heading.id = `note-heading-${index}`
      })
    },
    scrollToHeading(id) {
      const target = this.$el.querySelector(`#${id}`)
      if (target) {
        target.scrollIntoView({ behavior: 'smooth', block: 'start' })
      }
    }
  }
}
</script>

<style scoped lang="scss">
.note-markdown {
  min-height: 420px;
}

.viewer-shell {
  display: flex;
  min-height: 420px;
  background: #fff;
}

.toc {
  width: 190px;
  flex: 0 0 190px;
  border: 1px solid #e6e8eb;
  border-radius: 8px;
  padding: 10px 12px;
  background: #f7f8fa;
  overflow-y: auto;
  transition: width 0.2s ease, flex-basis 0.2s ease, background-color 0.2s ease;
}

.toc-collapsed .toc {
  width: 0;
  flex-basis: 0;
  padding: 0;
  background: #fff;
  border: none;
}

.toc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 28px;
  margin-bottom: 6px;
}

.toc-title {
  font-size: 12px;
  font-weight: 600;
  color: #606266;
}

.toc-item {
  height: 28px;
  line-height: 28px;
  font-size: 12px;
  color: #606266;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.toc-item:hover {
  color: #303133;
  background: #edf0f5;
}

.viewer {
  flex: 1;
  min-width: 0;
  padding-left: 28px;
}

.note-markdown ::v-deep .toastui-editor-defaultUI {
  border: none;
}

.note-markdown ::v-deep .toastui-editor-toolbar {
  border-radius: 8px;
  border: 1px solid #eeeeee;
}

.note-markdown ::v-deep .toastui-editor-md-container,
.note-markdown ::v-deep .toastui-editor-main,
.note-markdown ::v-deep .toastui-editor-defaultUI {
  background: #fff;
}

.note-markdown ::v-deep .toastui-editor-md-preview,
.note-markdown ::v-deep .toastui-editor-md-splitter {
  border-color: #eeeeee;
}

.note-markdown ::v-deep .toastui-editor-contents {
  max-width: 880px;
  margin: 0 auto;
  color: #2f2f2f;
  font-size: 16px;
  line-height: 1.75;
}

.note-markdown ::v-deep .toastui-editor-contents h1 {
  margin-top: 8px;
  margin-bottom: 28px;
  font-size: 38px;
  line-height: 1.25;
}

.note-markdown ::v-deep .toastui-editor-contents h2 {
  margin-top: 28px;
  font-size: 28px;
}

.note-markdown ::v-deep .toastui-editor-contents ul,
.note-markdown ::v-deep .toastui-editor-contents ol {
  padding-left: 1.6em;
}

@media (max-width: 768px) {
  .toc {
    width: 12px;
    flex-basis: 12px;
    padding: 0;
  }

  .viewer {
    padding-left: 12px;
  }
}
</style>
