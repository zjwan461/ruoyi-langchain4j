<template>
  <div class="markdown-editor-wrap">
    <!-- 隐藏上传组件，用于图片上传 -->
    <el-upload
      :action="uploadUrl"
      :before-upload="handleBeforeUpload"
      :on-success="handleUploadSuccess"
      :on-error="handleUploadError"
      name="file"
      :show-file-list="false"
      :headers="headers"
      style="display: none"
      ref="upload"
    >
    </el-upload>

    <!-- 编辑器主体布局：编辑区 + 预览区 -->
    <div class="markdown-editor-container" :style="styles">
      <textarea
        ref="editor"
        class="markdown-input"
        v-model="markdownText"
        :placeholder="readOnly ? '' : '请输入Markdown内容'"
        :readonly="readOnly"
        @input="handleInput"
      ></textarea>
      <div v-if="showPreview" class="markdown-preview" v-html="renderedHtml"></div>
    </div>
  </div>
</template>

<script>
import axios from "axios"
import MarkdownIt from "markdown-it"
import hljs from "highlight.js"
import "highlight.js/styles/atom-one-dark.css"
import { getToken } from "@/utils/auth"

// 初始化markdown-it配置（对齐富文本高亮体验）
const md = new MarkdownIt({
  gfm: true,
  breaks: true,
  highlight: function (code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(code, { language: lang }).value
    }
    return hljs.highlightAuto(code).value
  }
})

export default {
  name: "MarkdownEditor",
  props: {
    /* 编辑器内容 */
    value: {
      type: String,
      default: "",
    },
    /* 整体高度 */
    height: {
      type: Number,
      default: null,
    },
    /* 最小高度 */
    minHeight: {
      type: Number,
      default: null,
    },
    /* 只读模式 */
    readOnly: {
      type: Boolean,
      default: false,
    },
    /* 上传文件大小限制(MB) */
    fileSize: {
      type: Number,
      default: 5,
    },
    /* 是否实时预览 */
    preview: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      uploadUrl: process.env.VUE_APP_BASE_API + "/common/upload",
      headers: {
        Authorization: "Bearer " + getToken()
      },
      markdownText: "",
      renderedHtml: "",
      currentValue: ""
    }
  },
  computed: {
    // 编辑器样式适配
    styles() {
      let style = {}
      if (this.minHeight) {
        style.minHeight = `${this.minHeight}px`
      }
      if (this.height) {
        style.height = `${this.height}px`
      }
      return style
    },
    // 是否显示预览区
    showPreview() {
      return this.preview && !this.readOnly
    }
  },
  watch: {
    // 监听外部value赋值，同步编辑器内容
    value: {
      handler(val) {
        const newVal = val === null ? "" : val
        if (newVal !== this.currentValue) {
          this.currentValue = newVal
          this.markdownText = newVal
          this.renderHtml()
        }
      },
      immediate: true
    }
  },
  mounted() {
    // 初始化渲染内容
    this.renderHtml()
    // 监听粘贴图片事件
    window.addEventListener("paste", this.handlePasteCapture)
  },
  beforeDestroy() {
    window.removeEventListener("paste", this.handlePasteCapture)
  },
  methods: {
    // 输入监听，同步值并触发事件
    handleInput() {
      this.currentValue = this.markdownText
      this.renderHtml()
      // 同步v-model
      this.$emit("input", this.markdownText)
      // 对外暴露变更事件，和富文本编辑器参数对齐
      this.$emit("on-change", {
        html: this.renderedHtml,
        text: this.markdownText
      })
      this.$emit("on-text-change")
    },

    // 渲染markdown为html
    renderHtml() {
      this.renderedHtml = md.render(this.markdownText)
    },

    // 上传前校验图片格式和大小
    handleBeforeUpload(file) {
      const type = ["image/jpeg", "image/jpg", "image/png", "image/svg"]
      const isJPG = type.includes(file.type)
      if (!isJPG) {
        this.$message.error(`图片格式错误!`)
        return false
      }
      if (this.fileSize) {
        const isLt = file.size / 1024 / 1024 < this.fileSize
        if (!isLt) {
          this.$message.error(`上传文件大小不能超过 ${this.fileSize} MB!`)
          return false
        }
      }
      return true
    },

    // 图片上传成功回调
    handleUploadSuccess(res) {
      if (res.code == 200) {
        // 在光标位置插入markdown图片语法
        const imgMd = `![图片](${process.env.VUE_APP_BASE_API + res.fileName})`
        this.insertTextAtCursor(imgMd)
        this.$message.success("图片上传成功")
      } else {
        this.$message.error("图片插入失败")
      }
    },

    // 上传失败
    handleUploadError() {
      this.$message.error("图片插入失败")
    },

    // 粘贴图片捕获
    handlePasteCapture(e) {
      const clipboard = e.clipboardData || window.clipboardData
      if (!clipboard || !clipboard.items) return

      for (let i = 0; i < clipboard.items.length; i++) {
        const item = clipboard.items[i]
        if (item.type.indexOf("image") !== -1) {
          e.preventDefault()
          const file = item.getAsFile()
          this.insertImage(file)
        }
      }
    },

    // 手动上传图片
    insertImage(file) {
      const formData = new FormData()
      formData.append("file", file)
      axios.post(this.uploadUrl, formData, {
        headers: {
          "Content-Type": "multipart/form-data",
          Authorization: this.headers.Authorization
        }
      }).then(res => {
        this.handleUploadSuccess(res.data)
      })
    },

    // 光标位置插入文本
    insertTextAtCursor(text) {
      const textarea = this.$refs.editor
      const start = textarea.selectionStart
      const end = textarea.selectionEnd
      const content = this.markdownText
      // 拼接新内容
      const newContent = content.substring(0, start) + text + content.substring(end)
      this.markdownText = newContent
      // 光标定位到插入内容末尾
      this.$nextTick(() => {
        textarea.selectionStart = textarea.selectionEnd = start + text.length
        this.handleInput()
      })
    }
  }
}
</script>

<style scoped>
.markdown-editor-wrap {
  width: 100%;
  box-sizing: border-box;
}

.markdown-editor-container {
  display: flex;
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.markdown-input {
  flex: 1;
  width: 100%;
  padding: 16px;
  border: none;
  outline: none;
  resize: none;
  font-size: 14px;
  line-height: 1.8;
  color: #333;
  background: #fff;
  box-sizing: border-box;
}

.markdown-input:read-only {
  background: #fafafa;
  cursor: default;
}

.markdown-preview {
  flex: 1;
  width: 100%;
  padding: 16px;
  border-left: 1px solid #dcdfe6;
  overflow-y: auto;
  box-sizing: border-box;
  background: #fff;
}

/* Markdown 预览样式 和之前Viewer保持一致 */
.markdown-preview h1,
.markdown-preview h2,
.markdown-preview h3,
.markdown-preview h4,
.markdown-preview h5,
.markdown-preview h6 {
  margin: 1.5rem 0 1rem;
  font-weight: 600;
  color: #222;
}

.markdown-preview p {
  margin-bottom: 1rem;
  line-height: 1.8;
}

.markdown-preview ul,
.markdown-preview ol {
  margin: 0 0 1rem 1.5rem;
  padding-left: 1rem;
}

.markdown-preview ul {
  list-style-type: disc;
}

.markdown-preview ol {
  list-style-type: decimal;
}

.markdown-preview li {
  margin-bottom: 0.5rem;
}

.markdown-preview a {
  color: #007bff;
  text-decoration: none;
}

.markdown-preview a:hover {
  text-decoration: underline;
}

.markdown-preview strong {
  font-weight: 700;
}

.markdown-preview em {
  font-style: italic;
}

.markdown-preview pre {
  margin: 1rem 0;
  padding: 1rem;
  border-radius: 4px;
  background-color: #282c34;
  color: #abb2bf;
  overflow-x: auto;
}

.markdown-preview code {
  padding: 0.2rem 0.4rem;
  border-radius: 3px;
  background-color: #f5f5f5;
  font-family: SFMono-Regular, Consolas, Liberation Mono, Menlo, monospace;
  font-size: 0.9rem;
}

.markdown-preview pre code {
  padding: 0;
  background: none;
  color: inherit;
  font-size: inherit;
}

.markdown-preview blockquote {
  margin: 1rem 0;
  padding: 0.5rem 1rem;
  border-left: 3px solid #ddd;
  background-color: #f9f9f9;
  color: #666;
}

.markdown-preview img {
  max-width: 100%;
  height: auto;
  margin: 1rem 0;
  border-radius: 4px;
}

.markdown-preview table {
  width: 100%;
  border-collapse: collapse;
  margin: 1rem 0;
}

.markdown-preview th,
.markdown-preview td {
  padding: 0.75rem;
  border: 1px solid #ddd;
  text-align: left;
}

.markdown-preview th {
  background-color: #f5f5f5;
  font-weight: 600;
}
</style>
