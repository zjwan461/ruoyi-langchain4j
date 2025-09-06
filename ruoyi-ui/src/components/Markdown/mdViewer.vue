<template>
  <div class="markdown-viewer">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading">加载中...</div>
    
    <!-- 错误状态 -->
    <div v-else-if="error" class="error">
      无法加载 Markdown 内容: {{ error }}
    </div>
    
    <!-- Markdown 内容展示 -->
    <div v-else class="markdown-content" v-html="renderedHtml"></div>
  </div>
</template>

<script>
import marked from 'marked'
import hljs from 'highlight.js'
// 选择一个代码高亮样式
import 'highlight.js/styles/atom-one-dark.css'

// 配置 marked 解析器
marked.setOptions({
  // 启用 GitHub 风格的 Markdown
  gfm: true,
  // 支持换行符
  breaks: true,
  // 代码高亮处理
  highlight: function(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      // 已知语言，使用指定语言高亮
      return hljs.highlight(code, { language: lang }).value
    }
    // 未知语言，自动检测
    return hljs.highlightAuto(code).value
  }
})

export default {
  name: 'MarkdownViewer',
  props: {
    // Markdown 原始文本
    markdown: {
      type: String,
      default: ''
    },
    // 可选：通过 URL 加载 Markdown 内容
    url: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      renderedHtml: '',
      loading: false,
      error: ''
    }
  },
  watch: {
    // 当 markdown 内容变化时重新渲染
    markdown: {
      immediate: true,
      handler() {
        if (this.markdown) {
          this.renderedHtml = marked(this.markdown)
        }
      }
    },
    // 当 URL 变化时重新加载内容
    url: {
      immediate: true,
      handler() {
        if (this.url) {
          this.loadFromUrl()
        }
      }
    }
  },
  methods: {
    // 从 URL 加载 Markdown 内容
    async loadFromUrl() {
      this.loading = true
      this.error = ''
      try {
        const response = await fetch(this.url)
        if (!response.ok) {
          throw new Error(`HTTP 错误: ${response.status}`)
        }
        const content = await response.text()
        this.renderedHtml = marked(content)
      } catch (err) {
        this.error = err.message
        console.error('加载 Markdown 失败:', err)
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.markdown-viewer {
  width: 100%;
  max-width: 900px;
  margin: 0 auto;
  padding: 2rem;
  box-sizing: border-box;
}

.loading, .error {
  padding: 2rem;
  text-align: center;
  color: #666;
}

.error {
  color: #dc3545;
}

.markdown-content {
  color: #333;
  line-height: 1.8;
}

/* Markdown 基础样式 */
.markdown-content h1,
.markdown-content h2,
.markdown-content h3,
.markdown-content h4,
.markdown-content h5,
.markdown-content h6 {
  margin: 1.5rem 0 1rem;
  font-weight: 600;
  color: #222;
}

.markdown-content p {
  margin-bottom: 1rem;
}

.markdown-content ul,
.markdown-content ol {
  margin: 0 0 1rem 1.5rem;
  padding-left: 1rem;
}

.markdown-content ul {
  list-style-type: disc;
}

.markdown-content ol {
  list-style-type: decimal;
}

.markdown-content li {
  margin-bottom: 0.5rem;
}

.markdown-content a {
  color: #007bff;
  text-decoration: none;
}

.markdown-content a:hover {
  text-decoration: underline;
}

.markdown-content strong {
  font-weight: 700;
}

.markdown-content em {
  font-style: italic;
}

.markdown-content pre {
  margin: 1rem 0;
  padding: 1rem;
  border-radius: 4px;
  background-color: #282c34;
  color: #abb2bf;
  overflow-x: auto;
}

.markdown-content code {
  padding: 0.2rem 0.4rem;
  border-radius: 3px;
  background-color: #f5f5f5;
  font-family: SFMono-Regular, Consolas, Liberation Mono, Menlo, monospace;
  font-size: 0.9rem;
}

.markdown-content pre code {
  padding: 0;
  background: none;
  color: inherit;
  font-size: inherit;
}

.markdown-content blockquote {
  margin: 1rem 0;
  padding: 0.5rem 1rem;
  border-left: 3px solid #ddd;
  background-color: #f9f9f9;
  color: #666;
}

.markdown-content img {
  max-width: 100%;
  height: auto;
  margin: 1rem 0;
  border-radius: 4px;
}

.markdown-content table {
  width: 100%;
  border-collapse: collapse;
  margin: 1rem 0;
}

.markdown-content th,
.markdown-content td {
  padding: 0.75rem;
  border: 1px solid #ddd;
  text-align: left;
}

.markdown-content th {
  background-color: #f5f5f5;
  font-weight: 600;
}
</style>

