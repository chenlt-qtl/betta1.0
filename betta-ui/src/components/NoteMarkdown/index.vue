<template>
  <div class="note-markdown">
    <el-popover
      v-if="!viewer"
      v-model="emojiPopoverVisible"
      placement="bottom-end"
      width="260"
      trigger="click"
      popper-class="note-emoji-popover"
    >
      <div class="emoji-panel">
        <button
          v-for="emoji in emojiList"
          :key="emoji"
          type="button"
          class="emoji-item"
          @click="insertEmoji(emoji)"
        >
          {{ emoji }}
        </button>
      </div>
      <el-button
        slot="reference"
        class="emoji-trigger"
        size="mini"
        title="插入表情"
      >
        😊
      </el-button>
    </el-popover>
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
      <viewer ref="viewer" class="viewer" height="calc(100vh - 230px)" :options="viewerOptions" />
    </div>
  </div>
</template>

<script>
import { Editor, Viewer } from '@toast-ui/vue-editor'
import mermaid from 'mermaid'
import '@toast-ui/editor/dist/toastui-editor.css'
import '@toast-ui/editor/dist/toastui-editor-viewer.css'
import 'codemirror/lib/codemirror.css'
import { uploadNoteImage } from '@/api/note'

mermaid.initialize({
  startOnLoad: false,
  securityLevel: 'strict'
})

function renderCodeBlock(node) {
  const info = (node.info || '').trim().split(/\s+/)[0].toLowerCase()
  if (info !== 'mermaid') {
    // 普通代码块保持 Toast UI 默认的 pre/code 结构和语言标识。
    const preClasses = info ? [`lang-${info}`] : []
    const codeAttributes = info ? { 'data-language': info } : {}
    return [
      { type: 'openTag', tagName: 'pre', classNames: preClasses },
      { type: 'openTag', tagName: 'code', attributes: codeAttributes },
      { type: 'text', content: node.literal },
      { type: 'closeTag', tagName: 'code' },
      { type: 'closeTag', tagName: 'pre' }
    ]
  }
  // 先输出安全的源码占位，挂载后再由 Mermaid 异步替换为 SVG。
  return [
    { type: 'openTag', tagName: 'div', classNames: ['mermaid-diagram'] },
    { type: 'openTag', tagName: 'pre', classNames: ['mermaid-source'] },
    { type: 'openTag', tagName: 'code', attributes: { 'data-language': 'mermaid' } },
    { type: 'text', content: node.literal },
    { type: 'closeTag', tagName: 'code' },
    { type: 'closeTag', tagName: 'pre' },
    { type: 'closeTag', tagName: 'div' }
  ]
}

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
      // 每次预览刷新递增序号，阻止旧笔记的异步流程图覆盖新内容。
      mermaidRenderSeq: 0,
      emojiPopoverVisible: false,
      // 第一版只放高频表情，作为普通 Unicode 字符写入 Markdown，Obsidian 和网页端都能直接显示。
      emojiList: [
        '😄', '😂', '😊', '😍', '😘', '👍', '👎', '👏',
        '🙏', '🔥', '✨', '🎉', '🎀', '🎊', '🎁', '💡',
        '✅', '❌', '⭐', '📌', '📎', '📅', '⏰', '🚀',
        '❤️', '💔', '☕', '🍀', '🌈', '⚠️', '📊', '📈',
        '✏️', '🍔', '☀️'
      ],
      editorOptions: {
        hooks: {
          addImageBlobHook: this.uploadImage
        }
      },
      viewerOptions: {
        customHTMLRenderer: {
          codeBlock: renderCodeBlock
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
      const renderSeq = ++this.mermaidRenderSeq
      // 编辑状态只同步编辑器；进入预览后再用当前内容快照重建 Viewer，避免反复刷新隐藏 DOM。
      const viewerMarkdown = this.viewer ? this.displayMarkdown() : ''
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
        if (this.viewer && this.$refs.viewer && renderSeq === this.mermaidRenderSeq) {
          // 预览内容会把 Markdown 中的相对图片路径转换成浏览器可访问的 /file/** 地址。
          this.$refs.viewer.invoke('setMarkdown', viewerMarkdown, false)
          this.$nextTick(() => {
            if (renderSeq !== this.mermaidRenderSeq || !this.viewer) {
              return
            }
            this.decorateViewerHeadings()
            this.renderMermaidDiagrams(renderSeq)
          })
        }
        this.parseHeadings()
      })
    },
    async renderMermaidDiagrams(renderSeq) {
      const viewer = this.$refs.viewer && this.$refs.viewer.$el
      if (!viewer || renderSeq !== this.mermaidRenderSeq || !this.viewer) {
        return
      }
      const blocks = Array.from(viewer.querySelectorAll('.toastui-editor-contents .mermaid-diagram'))
      // Mermaid 内部渲染会操作临时 DOM，按顺序处理可避免多图并发时互相影响。
      for (let index = 0; index < blocks.length; index++) {
        const block = blocks[index]
        if (renderSeq !== this.mermaidRenderSeq || !this.viewer || !viewer.isConnected || !block.isConnected) {
          return
        }
        const code = block.querySelector('code')
        const source = code ? code.textContent.trim() : ''
        if (!source) {
          this.showMermaidError(block, '流程图内容为空')
          continue
        }
        try {
          await mermaid.parse(source)
        } catch (error) {
          if (renderSeq === this.mermaidRenderSeq && this.viewer && viewer.isConnected && block.isConnected) {
            // 只有解析失败才属于源码语法错误，保留源码供用户修正。
            this.showMermaidError(block, '流程图语法错误，请检查源码')
          }
          continue
        }
        if (renderSeq !== this.mermaidRenderSeq || !this.viewer || !viewer.isConnected || !block.isConnected) {
          return
        }
        const diagramId = `note-mermaid-${this._uid}-${renderSeq}-${index}`
        try {
          const result = await mermaid.render(diagramId, source)
          // 笔记或模式已切换时放弃迟到结果，避免污染当前预览 DOM。
          if (renderSeq !== this.mermaidRenderSeq || !this.viewer || !viewer.isConnected || !block.isConnected) {
            return
          }
          block.classList.add('is-rendered')
          block.innerHTML = result.svg
        } catch (error) {
          if (renderSeq === this.mermaidRenderSeq && this.viewer && viewer.isConnected && block.isConnected) {
            // 已通过语法解析的图仅提示运行期渲染失败，避免误导用户修改正确源码。
            this.showMermaidError(block, '流程图渲染失败，请重试')
          }
        }
      }
    },
    showMermaidError(block, message) {
      block.classList.add('has-error')
      const error = document.createElement('div')
      error.className = 'mermaid-error'
      error.textContent = message
      block.insertBefore(error, block.firstChild)
    },
    handleChange() {
      this.syncMarkdownFromEditor()
    },
    syncMarkdownFromEditor() {
      const markdown = this.$refs.editor.invoke('getMarkdown')
      // 忽略初始化/同步触发的 change，以及内容未变化的冗余事件。
      if (this.syncingEditor || markdown === (this.value || '')) {
        return
      }
      this.$emit('input', markdown)
      this.$emit('change', markdown)
    },
    insertEmoji(emoji) {
      if (!this.$refs.editor) {
        return
      }
      this.$refs.editor.invoke('insertText', emoji)
      this.emojiPopoverVisible = false
      // insertText 通常会触发 change；这里再主动同步一次，确保父页面 dirty 状态立即更新。
      this.syncMarkdownFromEditor()
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
  position: relative;
  min-height: 420px;
}

.emoji-trigger {
  position: absolute;
  top: 8px;
  right: 12px;
  z-index: 3;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 4px;
  background: transparent;
  color: #555;
  font-size: 18px;
}

.emoji-trigger:hover,
.emoji-trigger:focus {
  border-color: #dadde1;
  background: #f7f8fa;
}

.emoji-panel {
  display: grid;
  grid-template-columns: repeat(7, 28px);
  gap: 6px;
  box-sizing: border-box;
  max-width: 100%;
  padding: 2px;
  overflow: hidden;
}

.emoji-item {
  width: 28px;
  height: 28px;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 6px;
  background: #fff;
  font-size: 18px;
  line-height: 26px;
  cursor: pointer;
}

.emoji-item:hover {
  border-color: #dcdfe6;
  background: #f5f7fa;
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

.note-markdown ::v-deep .mermaid-diagram {
  margin: 20px 0;
  overflow-x: auto;
  text-align: center;
}

.note-markdown ::v-deep .mermaid-diagram svg {
  max-width: 100%;
  height: auto;
}

.note-markdown ::v-deep .mermaid-diagram.has-error {
  padding: 12px;
  border: 1px solid #f5c2c7;
  border-radius: 6px;
  background: #fff5f5;
  text-align: left;
}

.note-markdown ::v-deep .mermaid-error {
  margin-bottom: 8px;
  color: #c45656;
  font-size: 13px;
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
