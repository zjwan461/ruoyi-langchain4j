<template>
  <div class="chat-page">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
      <div class="sidebar-header">
        <button class="new-chat-btn" @click="newSession">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          新对话
        </button>
        <button class="collapse-btn" @click="sidebarCollapsed = !sidebarCollapsed">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline v-if="sidebarCollapsed" points="9 18 15 12 9 6"></polyline>
            <polyline v-else points="15 18 9 12 15 6"></polyline>
          </svg>
        </button>
      </div>

      <div class="session-list">
        <div
          v-for="item in sessionList"
          :key="item.sessionId"
          class="session-item"
          :class="{ active: item.sessionId === sessionId }"
          @click="switchSession(item.sessionId)"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
          </svg>
          <span class="session-title">{{ item.title }}</span>
          <button class="delete-session-btn" @click.stop="deleteSession(item.sessionId)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="3 6 5 6 21 6"></polyline>
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
            </svg>
          </button>
        </div>
      </div>
    </aside>

    <!-- 主聊天区域 -->
    <main class="chat-main">
      <!-- 顶部栏 -->
      <header class="chat-header">
        <button v-if="sidebarCollapsed" class="expand-sidebar-btn" @click="sidebarCollapsed = false" title="展开侧边栏">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </button>
        <div class="agent-info">
          <div class="agent-avatar">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"></circle>
              <path d="M12 8v4l3 3"></path>
            </svg>
          </div>
          <div class="agent-details">
            <h1>{{ agentInfo.name || 'AI 助手' }}
              <el-tag v-if="agentInfo" type="info" size="medium" effect="plain" style="margin-left: 10px;">{{ agentInfo.agentType === 'dify' ? 'Dify 智能体' : (agentInfo.agentType || 'origin') }}</el-tag>
            </h1>
            <p v-if="agentInfo.description">{{ agentInfo.description }}</p>
          </div>
        </div>
        <div class="header-actions">
          <button class="icon-btn" @click="clearChatHistory" title="清空对话">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="3 6 5 6 21 6"></polyline>
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
            </svg>
          </button>
        </div>
      </header>

      <!-- 消息区域 -->
      <div class="messages-container" ref="messagesContainer" @scroll="handleScroll">
        <!-- 欢迎界面 -->
        <div v-if="messages.length === 0" class="welcome-screen">
          <div class="welcome-icon">
            <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M12 2L2 7l10 5 10-5-10-5z"></path>
              <path d="M2 17l10 5 10-5"></path>
              <path d="M2 12l10 5 10-5"></path>
            </svg>
          </div>
          <h2>你好，我是 {{ agentInfo.name || 'AI 助手' }}</h2>
          <p>有什么我可以帮助你的吗？</p>
          <div class="quick-prompts">
            <button v-for="prompt in quickPrompts" :key="prompt" @click="usePrompt(prompt)" class="prompt-btn">
              {{ prompt }}
            </button>
          </div>
        </div>

        <!-- 消息列表 -->
        <div v-else class="message-list">
          <div v-if="historyLoading" class="history-loading">
            <div class="typing-indicator">
              <span></span>
              <span></span>
              <span></span>
            </div>
            <span>加载更早的历史消息…</span>
          </div>
          <div
            v-for="(msg, idx) in messages"
            :key="idx"
            class="message"
            :class="{ 'user-message': msg.role === 'user', 'ai-message': msg.role === 'assistant' }"
          >
            <div v-if="msg.role === 'assistant'" class="message-avatar">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="12 2 2 7 12 12 22 7 12 2"></polygon>
                <polyline points="2 17 12 22 22 17"></polyline>
                <polyline points="2 12 12 17 22 12"></polyline>
              </svg>
            </div>
            <div class="message-content">
              <!-- Think Block (foldable) -->
              <div v-if="msg.role === 'assistant' && hasThinkContent(msg.content)" class="think-section">
                <div
                  class="think-block"
                  :class="{ 'folded': isThinkFolded(msg, idx) }"
                  :ref="'thinkBlock-' + idx"
                >
                  {{ getThinkContent(msg.content) }}
                </div>
                <button v-if="!msg.loading" class="think-toggle-btn" @click="toggleThink(idx)">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline :points="isThinkFolded(msg, idx) ? '6 9 12 15 18 9' : '18 15 12 9 6 15'"></polyline>
                  </svg>
                  {{ isThinkFolded(msg, idx) ? '展开思考过程' : '收起思考过程' }}
                </button>
              </div>

              <!-- 知识库召回参考资料 -->
              <div v-if="msg.role === 'assistant' && msg.retrievedList && msg.retrievedList.length > 0" class="retrieved-section">
                <div class="retrieved-header" @click="toggleRetrieved(idx)">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline :points="isRetrievedFolded(msg, idx) ? '6 9 12 15 18 9' : '18 15 12 9 6 15'"></polyline>
                  </svg>
                  <span class="retrieved-title-text">参考资料 ({{ msg.retrievedList.length }})</span>
                </div>
                <div class="retrieved-list" :class="{ 'folded': isRetrievedFolded(msg, idx) }">
                  <div v-for="(item, itemIdx) in msg.retrievedList" :key="itemIdx" class="retrieved-item">
                    <div class="retrieved-filename">{{ item.fileName }}</div>
                    <div class="retrieved-content">{{ item.text }}</div>
                  </div>
                </div>
              </div>

              <!-- 工具调用进行中 -->
              <div v-if="msg.role === 'assistant' && msg.currentToolCall" class="tool-call-loading">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="tool-icon">
                  <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"></path>
                </svg>
                <span class="tool-call-text">正在调用工具：{{ msg.currentToolCall.toolName }}</span>
                <div class="typing-indicator tool-dots">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>

              <!-- 工具调用结果 -->
              <div v-if="msg.role === 'assistant' && msg.toolCalledList && msg.toolCalledList.length > 0" class="tool-called-section">
                <div v-for="(tool, toolIdx) in msg.toolCalledList" :key="toolIdx" class="tool-called-item">
                  <div class="tool-called-header" @click="toggleToolCalled(idx, toolIdx)">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline :points="isToolCalledFolded(msg, idx, toolIdx) ? '6 9 12 15 18 9' : '18 15 12 9 6 15'"></polyline>
                    </svg>
                    <span class="tool-name">工具调用：{{ tool.toolName }}</span>
                    <span class="tool-status">已完成</span>
                  </div>
                  <div class="tool-called-result" :class="{ 'folded': isToolCalledFolded(msg, idx, toolIdx) }">
                    <pre>{{ tool.toolInvokeResult }}</pre>
                  </div>
                </div>
              </div>

              <!-- 用户消息附件展示 -->
              <div v-if="msg.role === 'user' && msg.attachments && msg.attachments.length > 0" class="message-attachments">
                <div
                  v-for="(att, attIdx) in msg.attachments"
                  :key="attIdx"
                  class="message-attachment-item"
                  @click="handleAttachmentClick(att)"
                >
                  <img v-if="isImageFile(att.fileExt)" :src="att.fileContent?att.fileContent:att.url" :alt="att.fileName" class="attachment-img" />
                  <div v-else class="attachment-doc-item">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                      <polyline points="14 2 14 8 20 8"></polyline>
                    </svg>
                    <span>{{ att.fileName }}</span>
                  </div>
                </div>
              </div>

              <!-- Message Bubble / 加载占位 -->
              <template v-if="msg.loading && !msg.content">
                <div class="typing-indicator">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </template>
              <div v-else class="message-bubble" v-html="msg.role === 'user' ? escapeHtml(msg.content) : renderMarkdown(msg.content)"></div>

              <div class="message-meta">
                <span class="message-time">{{ formatTime(msg.createTime) }}</span>
                <div v-if="!msg.loading" class="message-actions">
                  <button @click="copyMessage(msg.content, msg.role)" title="复制">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                      <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                    </svg>
                  </button>
                </div>
              </div>
            </div>
            <div v-if="msg.role === 'user'" class="message-avatar">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
              </svg>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <footer class="chat-footer">
        <div class="input-wrapper">
          <!-- 附件预览区 -->
          <div v-if="attachmentList.length > 0" class="attachment-preview">
            <div
              v-for="(item, index) in attachmentList"
              :key="item.tempId"
              class="attachment-preview-item"
            >
              <div v-if="item.isImage" class="attachment-image-preview">
                <img :src="item.previewUrl" :alt="item.fileName" />
              </div>
              <div v-else class="attachment-doc-preview">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                  <polyline points="14 2 14 8 20 8"></polyline>
                </svg>
                <span class="attachment-name">{{ item.fileName }}</span>
              </div>

              <!-- 上传中遮罩 -->
              <div v-if="item.loading" class="attachment-loading-mask">
                <div class="typing-indicator">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>

              <button class="remove-attachment-btn" @click="removeAttachment(index)" title="移除">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"></line>
                  <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
              </button>
            </div>
          </div>

          <div class="input-container">
            <!-- 文件上传按钮 -->
            <button class="upload-btn" @click="triggerFileUpload" :disabled="uploading" title="上传附件">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48"></path>
              </svg>
            </button>
            <input
              type="file"
              ref="fileInput"
              style="display: none"
              multiple
              @change="handleFileChange"
            />

            <textarea
              v-model="userInput"
              ref="inputArea"
              placeholder="输入消息..."
              @keydown="handleKeyDown"
              rows="1"
            ></textarea>
            <button class="send-btn" @click="sendMessage" :disabled="(!userInput.trim() && attachmentList.length === 0) || isLoading || uploading">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="22" y1="2" x2="11" y2="13"></line>
                <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
              </svg>
            </button>
          </div>
        </div>
        <p class="input-hint">按 Enter 发送，Shift + Enter 换行</p>
      </footer>
    </main>

    <!-- 图片预览弹窗 -->
    <ImagePreview
      :visible="imagePreview.visible"
      :image-url="imagePreview.url"
      :file-name="imagePreview.name"
      @close="imagePreview.visible = false"
    />

    <!-- 文档预览弹窗 -->
    <DocPreview
      :visible="docPreview.visible"
      :file-name="docPreview.name"
      :content="docPreview.content"
      :file-url="docPreview.url"
      @close="docPreview.visible = false"
    />
  </div>
</template>

<script>
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import {getAgent, createSession, listChatSession, listChatMessage, deleteSession, attachFile} from '@/api/ai/aiChat'
import {nanoid} from 'nanoid'
import ImagePreview from '@/components/Chat/ImagePreview.vue'
import DocPreview from '@/components/Chat/DocPreview.vue'

const THINK_START = '<' + 'think' + '>'
const THINK_END = '<' + '/think' + '>'

const md = new MarkdownIt({
  html: true,
  breaks: true,
  linkify: true,
  typographer: true,
  highlight: function (str, lang) {
    let validLang = ''
    if (lang && typeof lang === 'string') {
      const trimmed = lang.trim()
      if (trimmed) {
        validLang = trimmed.split(/\s+/)[0]
      }
    }

    if (validLang && hljs.getLanguage(validLang)) {
      try {
        return '<pre class="code-block"><code class="hljs language-' + validLang + '">' + hljs.highlight(str, {
          language: validLang,
          ignoreIllegals: true
        }).value + '</code></pre>'
      } catch (__) {
      }
    }

    try {
      const auto = hljs.highlightAuto(str)
      if (auto && auto.value) {
        return '<pre class="code-block"><code class="hljs">' + auto.value + '</code></pre>'
      }
    } catch (__) {}

    return '<pre class="code-block"><code class="hljs">' + md.utils.escapeHtml(str) + '</code></pre>'
  }
})

export default {
  name: 'AiChat',
  components: { ImagePreview, DocPreview },
  data() {
    return {
      sidebarCollapsed: false,
      sessionList: [],
      sessionId: null,
      clientId: null,
      visitId: null,
      agentInfo: {},
      messages: [],
      userInput: '',
      isLoading: false,
      streamController: null,
      foldedStates: {},
      pageNum: 1,
      pageSize: 20,
      historyTotal: 0,
      historyLoading: false,
      hasMoreHistory: true,
      quickPrompts: [
        '帮我写一段Python代码',
        '解释一下什么是机器学习',
        '给我讲个笑话',
        '如何提高工作效率'
      ],
      // 附件相关
      attachmentList: [],
      uploading: false,
      // 预览弹窗状态
      imagePreview: {
        visible: false,
        url: '',
        name: ''
      },
      docPreview: {
        visible: false,
        name: '',
        content: '',
        url: ''
      }
    }
  },
  mounted() {
    this.visitId = this.$route.params && this.$route.params.agentId
    this.initClientAndSession()
  },
  methods: {
    initClientAndSession() {
      getAgent(this.visitId).then(res => {
        if (res.data.status !== 1) {
          this.$message.error('智能体未启用，页面即将跳转')
          setTimeout(() => {
            this.$router.push('/404')
          }, 1000)
        } else {
          this.agentInfo = res.data
          const clientId = localStorage.getItem('chat-clientId')
          if (!clientId) {
            this.clientId = nanoid()
            localStorage.setItem('chat-clientId', this.clientId)
          } else {
            this.clientId = clientId
          }
          const sessionId = localStorage.getItem('chat-sessionId')
          if (!sessionId) {
            createSession(this.clientId).then(res => {
              this.sessionId = res.data
              localStorage.setItem('chat-sessionId', this.sessionId)
            })
          } else {
            this.sessionId = sessionId
          }
          this.getSessionList()
          this.getChatHistoryMessage()
        }
      })
    },
    getSessionList() {
      listChatSession(this.clientId, this.agentInfo.id).then(res => {
        this.sessionList = res.data
        if (!this.sessionList || this.sessionList.length === 0) {
          this.newSession()
        }
      })
    },
    getChatHistoryMessage() {
      if (!this.sessionId || !this.agentInfo || !this.agentInfo.id) return
      this.pageNum = 1
      this.hasMoreHistory = true
      this.historyTotal = 0
      this.historyLoading = false
      this.$modal.loading("loading...")
      listChatMessage(this.sessionId, this.agentInfo.id, this.pageNum, this.pageSize).then(res => {
        this.$modal.closeLoading()
        const rows = (res && res.rows) || []
        this.messages = [...rows].reverse()
        this.historyTotal = (res && typeof res.total === 'number') ? res.total : rows.length
        this.hasMoreHistory = this.messages.length < this.historyTotal
        this.$nextTick(() => {
          this.scrollToBottom()
        })
      }).catch(err=>{
        this.$modal.closeLoading()
      })
    },
    loadMoreHistory() {
      if (this.historyLoading || !this.hasMoreHistory) return
      if (!this.sessionId || !this.agentInfo || !this.agentInfo.id) return
      this.historyLoading = true
      const nextPage = this.pageNum + 1
      const container = this.$refs.messagesContainer
      const oldScrollHeight = container ? container.scrollHeight : 0
      const oldScrollTop = container ? container.scrollTop : 0
      listChatMessage(this.sessionId, this.agentInfo.id, nextPage, this.pageSize).then(res => {
        const rows = (res && res.rows) || []
        if (rows.length === 0) {
          this.hasMoreHistory = false
          return
        }
        const ascRows = [...rows].reverse()
        this.messages = [...ascRows, ...this.messages]
        this.pageNum = nextPage
        if (res && typeof res.total === 'number') {
          this.historyTotal = res.total
        }
        this.hasMoreHistory = this.messages.length < this.historyTotal
        this.$nextTick(() => {
          if (container) {
            const newScrollHeight = container.scrollHeight
            container.scrollTop = oldScrollTop + (newScrollHeight - oldScrollHeight)
          }
        })
      }).catch(() => {
      }).finally(() => {
        this.historyLoading = false
      })
    },
    handleScroll(e) {
      const el = e && e.target
      if (!el) return
      if (el.scrollTop <= 50 && this.hasMoreHistory && !this.historyLoading && this.messages.length > 0) {
        this.loadMoreHistory()
      }
    },
    switchSession(sessionId) {
      this.sessionId = sessionId
      localStorage.setItem('chat-sessionId', sessionId)
      this.clearAttachmentList()
      this.getChatHistoryMessage()
    },
    newSession() {
      createSession(this.clientId).then(res => {
        this.sessionId = res.data
        localStorage.setItem('chat-sessionId', this.sessionId)
        this.messages = []
        this.clearAttachmentList()
        this.pageNum = 1
        this.historyTotal = 0
        this.hasMoreHistory = true
        this.historyLoading = false
        this.sessionList.unshift({
          sessionId: this.sessionId,
          title: '新对话'
        })
      })
    },
    deleteSession(sessionId) {
      this.$modal.confirm('确认删除此会话？').then(() => {
        return deleteSession(sessionId)
      }).then(() => {
        if (this.sessionList.length > 1) {
          this.sessionId = this.sessionList.find(s => s.sessionId !== sessionId)?.sessionId
        } else {
          this.sessionId = null
        }
        this.clearAttachmentList()
        this.getSessionList()
        this.getChatHistoryMessage()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {
      })
    },
    clearChatHistory() {
      this.$modal.confirm('确认清空当前对话？').then(() => {
        if (this.streamController) {
          this.streamController.abort()
          this.streamController = null
        }
        this.messages = []
        this.clearAttachmentList()
        this.$modal.msgSuccess('已清空')
      }).catch(() => {
      })
    },
    usePrompt(prompt) {
      this.userInput = prompt
      this.sendMessage()
    },
    handleKeyDown(e) {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault()
        this.sendMessage()
      }
    },

    // ========== 文件上传相关 ==========
    triggerFileUpload() {
      this.$refs.fileInput.click()
    },
    handleFileChange(e) {
      const files = e.target.files
      if (!files || files.length === 0) return

      this.uploading = true
      const uploadPromises = Array.from(files).map(file => {
        // 1. 立即加入列表，显示本地预览 + loading
        const tempId = 'temp-' + Date.now() + '-' + Math.random().toString(36).slice(2, 8)
        const fileName = file.name
        const fileExt = fileName.substring(fileName.lastIndexOf('.') + 1)
        const isImage = this.isImageFile(fileExt)
        const previewUrl = isImage ? URL.createObjectURL(file) : ''

        const tempItem = {
          tempId,
          id: null,
          fileName,
          fileExt,
          url: '',
          fileContent: '',
          isImage,
          previewUrl,
          loading: true
        }
        this.attachmentList.push(tempItem)

        // 2. 异步上传，完成后回填
        return this.uploadSingleFile(tempId, file)
      })

      Promise.all(uploadPromises).finally(() => {
        this.uploading = false
        this.$refs.fileInput.value = ''
      })
    },
    async uploadSingleFile(tempId, file) {
      try {
        const res = await attachFile(file)
        const index = this.attachmentList.findIndex(item => item.tempId === tempId)
        if (index === -1) return

        if (res.code === 200 && res.attachment) {
          // 上传成功，回填数据，关闭loading
          this.$set(this.attachmentList, index, {
            ...this.attachmentList[index],
            id: res.attachment.id,
            fileName: res.attachment.fileName,
            fileExt: res.attachment.fileExt,
            url: res.url,
            fileContent: res.attachment.fileContent || '',
            loading: false
          })
          // 释放本地预览URL
          if (this.attachmentList[index].previewUrl) {
            URL.revokeObjectURL(this.attachmentList[index].previewUrl)
          }
        } else {
          // 上传失败，移除该项
          this.attachmentList.splice(index, 1)
          this.$message.error(res.msg || '文件上传失败')
        }
      } catch (err) {
        console.error('文件上传异常', err)
        const index = this.attachmentList.findIndex(item => item.tempId === tempId)
        if (index > -1) {
          this.attachmentList.splice(index, 1)
        }
        this.$message.error('文件上传失败，请重试')
      }
    },
    isImageFile(ext) {
      if (!ext) return false
      const imageExts = ["jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "tif", "svg", "heic", "heif", "ico"]
      return imageExts.includes(ext.toLowerCase())
    },
    removeAttachment(index) {
      const item = this.attachmentList[index]
      if (item && item.previewUrl) {
        URL.revokeObjectURL(item.previewUrl)
      }
      this.attachmentList.splice(index, 1)
    },
    clearAttachmentList() {
      // 清空列表前释放所有本地预览
      this.attachmentList.forEach(item => {
        if (item.previewUrl) {
          URL.revokeObjectURL(item.previewUrl)
        }
      })
      this.attachmentList = []
    },

    // ========== 附件预览 ==========
    handleAttachmentClick(att) {
      if (this.isImageFile(att.fileExt)) {
        this.imagePreview = {
          visible: true,
          url: att.fileContent || att.url,
          name: att.fileName
        }
      } else {
        this.docPreview = {
          visible: true,
          name: att.fileName,
          content: att.fileContent || '',
          url: att.fileUrl || att.url || ''
        }
      }
    },

    // ========== 发送消息 ==========
    sendMessage() {
      const content = this.userInput.trim()
      if ((!content && this.attachmentList.length === 0) || this.isLoading || this.uploading) return

      // 过滤掉还在上传中的附件（理论上uploading为true时按钮禁用，不会走到这里）
      const readyAttachments = this.attachmentList.filter(item => !item.loading && item.id)
      const attachmentIds = readyAttachments.map(item => item.id)
      const attachments = readyAttachments.map(item => ({
        id: item.id,
        fileName: item.fileName,
        fileExt: item.fileExt,
        url: item.url,
        fileContent: item.fileContent,
        isImage: item.isImage
      }))

      this.messages.push({
        role: 'user',
        content: content,
        attachments: attachments,
        createTime: new Date().toISOString(),
        _id: 'user-' + Date.now()
      })

      this.userInput = ''
      this.clearAttachmentList()
      this.isLoading = true
      this.scrollToBottom()

      this.callStreamApi(content, attachmentIds)
    },

    // ========== 流式接口 ==========
    async callStreamApi(content, attachmentIds = []) {
      this.streamController = new AbortController()
      const chatApi = process.env.VUE_APP_BASE_API + '/ai-chat'
      const msgId = 'ai-' + Date.now()

      const aiMessage = {
        role: 'assistant',
        content: '',
        loading: true,
        createTime: new Date().toISOString(),
        _id: msgId,
        thinkFolded: false,
        retrievedList: [],
        currentToolCall: null,
        toolCalledList: []
      }
      this.messages.push(aiMessage)
      this.scrollToBottom()

      try {
        const response = await fetch(chatApi, {
          method: 'POST',
          headers: {'Content-Type': 'application/json'},
          body: JSON.stringify({
            prompt: content,
            agentId: this.agentInfo.id,
            clientId: this.clientId,
            sessionId: this.sessionId,
            attachmentIds: attachmentIds
          }),
          signal: this.streamController.signal
        })

        const reader = response.body.getReader()
        const decoder = new TextDecoder('utf-8')

        let buffer = ''

        while (true) {
          const {done, value} = await reader.read()
          if (done) break

          const chunk = decoder.decode(value)
          buffer += chunk

          let idx
          while ((idx = buffer.indexOf('\n')) !== -1) {
            const line = buffer.slice(0, idx).trim()
            buffer = buffer.slice(idx + 1)

            if (!line || !line.startsWith('data:')) continue
            const jsonRaw = line.replace(/^data:\s*/, '').trim()
            if (!jsonRaw) continue

            try {
              const res = JSON.parse(jsonRaw)
              if (res.msg) {
                aiMessage.content += res.msg
              } else if (res.retrieved) {
                aiMessage.retrievedList = res.retrieved
              } else if (res.tool_call) {
                aiMessage.currentToolCall = res.tool_call
              } else if (res.tool_called) {
                aiMessage.toolCalledList.push(res.tool_called)
                aiMessage.currentToolCall = null
              }
              this.scrollToBottom()
              const msgIndex = this.messages.findIndex(m => m._id === aiMessage._id)
              if (msgIndex > -1) {
                this.scrollThinkBlockToBottom(msgIndex)
              }
            } catch (e) {
              console.error('单行JSON解析失败', jsonRaw, e)
            }
          }
        }

        aiMessage.loading = false
        aiMessage.content = aiMessage.content.replace('Deep thinking...', '')
        this.scrollToBottom()
      } catch (error) {
        if (error.name !== 'AbortError') {
          aiMessage.content += '\n\n⚠️ 响应中断，请重试'
        }
        aiMessage.loading = false
      } finally {
        this.isLoading = false
      }
    },

    scrollThinkBlockToBottom(idx) {
      this.$nextTick(() => {
        const refName = 'thinkBlock-' + idx
        const el = this.$refs[refName]
        if (!el) return
        const dom = Array.isArray(el) ? el[0] : el
        if (!dom) return
        if (dom.scrollHeight > dom.clientHeight) {
          dom.scrollTop = dom.scrollHeight
        }
      })
    },
    hasThinkContent(content) {
      if (!content) return false
      return content.indexOf(THINK_START) !== -1
    },
    isThinkFinished(content) {
      if (!content) return false
      return content.indexOf(THINK_END) !== -1
    },
    getThinkContent(content) {
      if (!content) return ''
      const startIdx = content.indexOf(THINK_START)
      if (startIdx === -1) return ''
      const endIdx = content.indexOf(THINK_END)
      if (endIdx !== -1) {
        return content.substring(startIdx + THINK_START.length, endIdx)
      } else {
        return content.substring(startIdx + THINK_START.length)
      }
    },
    getOutputContent(content) {
      if (!content) return ''
      const endIdx = content.indexOf(THINK_END)
      if (endIdx !== -1) {
        return content.substring(endIdx + THINK_END.length)
      }
      const startIdx = content.indexOf(THINK_START)
      if (startIdx !== -1) {
        return ''
      }
      return content
    },
    renderMarkdown(content) {
      if (!content) return ''
      const output = this.getOutputContent(content)
      return md.render(output)
    },
    isThinkFolded(msg, idx) {
      if (!msg) return false
      if (this.foldedStates.hasOwnProperty(idx)) {
        return this.foldedStates[idx]
      }
      return this.isThinkFinished(msg.content)
    },
    toggleThink(idx) {
      const current = this.isThinkFolded(this.messages[idx], idx)
      this.$set(this.foldedStates, idx, !current)
    },
    isRetrievedFolded(msg, idx) {
      if (!msg) return true
      const key = 'retrieved-' + idx
      if (this.foldedStates.hasOwnProperty(key)) {
        return this.foldedStates[key]
      }
      return true
    },
    toggleRetrieved(idx) {
      const key = 'retrieved-' + idx
      const current = this.isRetrievedFolded(this.messages[idx], idx)
      this.$set(this.foldedStates, key, !current)
    },
    isToolCalledFolded(msg, idx, toolIndex) {
      if (!msg) return true
      const key = 'tool-' + idx + '-' + toolIndex
      if (this.foldedStates.hasOwnProperty(key)) {
        return this.foldedStates[key]
      }
      return true
    },
    toggleToolCalled(idx, toolIndex) {
      const key = 'tool-' + idx + '-' + toolIndex
      const current = this.isToolCalledFolded(this.messages[idx], idx, toolIndex)
      this.$set(this.foldedStates, key, !current)
    },
    escapeHtml(text) {
      const div = document.createElement('div')
      div.textContent = text
      return div.innerHTML
    },
    copyMessage(content, role) {
      const text = content || ''
      navigator.clipboard.writeText(text).then(() => {
        this.$message.success('已复制')
      }).catch(() => {
        this.$message.error('复制失败')
      })
    },
    formatTime(isoString) {
      if (!isoString) return ''
      const date = new Date(isoString)
      const now = new Date()
      const pad = (n) => String(n).padStart(2, '0')
      const y = date.getFullYear()
      const m = pad(date.getMonth() + 1)
      const d = pad(date.getDate())
      const hh = pad(date.getHours())
      const mm = pad(date.getMinutes())
      const isSameYear = date.getFullYear() === now.getFullYear()
      if (isSameYear && y === now.getFullYear() && m === pad(now.getMonth() + 1) && d === pad(now.getDate())) {
        return hh + ':' + mm
      }
      return isSameYear ? m + '-' + d + ' ' + hh + ':' + mm : y + '-' + m + '-' + d + ' ' + hh + ':' + mm
    },
    scrollToBottom() {
      this.$nextTick(() => {
        setTimeout(() => {
          const container = this.$refs.messagesContainer
          if (container) {
            container.scrollTop = container.scrollHeight
          }
        }, 30)
      })
    }
  },
  beforeDestroy() {
    if (this.streamController) {
      this.streamController.abort()
    }
    this.clearAttachmentList()
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.chat-page {
  display: flex;
  height: 100vh;
  background: #0f0f0f;
  color: #e5e5e5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
}

/* Sidebar */
.sidebar {
  width: 280px;
  background: #171717;
  border-right: 1px solid #2a2a2a;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
}

.sidebar-collapsed {
  width: 0 !important;
  min-width: 0 !important;
  overflow: hidden;
  padding: 0;
  border: none;
}

.sidebar-collapsed > * {
  min-width: 0;
  max-width: 0;
  overflow: hidden;
  padding: 0;
  margin: 0;
  opacity: 0;
  visibility: hidden;
}

.sidebar-header {
  padding: 16px;
  display: flex;
  gap: 8px;
  border-bottom: 1px solid #2a2a2a;
}

.new-chat-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  background: transparent;
  border: 1px solid #3f3f3f;
  border-radius: 6px;
  color: #e5e5e5;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.new-chat-btn:hover {
  background: #262626;
  border-color: #525252;
}

.collapse-btn {
  padding: 10px;
  background: transparent;
  border: 1px solid #3f3f3f;
  border-radius: 6px;
  color: #a3a3a3;
  cursor: pointer;
  transition: all 0.2s;
}

.collapse-btn:hover {
  color: #e5e5e5;
  background: #262626;
}

.expand-sidebar-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  margin-right: 8px;
  background: transparent;
  border: 1px solid #3f3f3f;
  border-radius: 6px;
  color: #a3a3a3;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.expand-sidebar-btn:hover {
  color: #e5e5e5;
  background: #262626;
  border-color: #525252;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-list::-webkit-scrollbar {
  width: 6px;
}

.session-list::-webkit-scrollbar-track {
  background: transparent;
}

.session-list::-webkit-scrollbar-thumb {
  background: #3f3f3f;
  border-radius: 3px;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-radius: 6px;
  cursor: pointer;
  color: #a3a3a3;
  transition: all 0.2s;
  position: relative;
}

.session-item:hover {
  background: #262626;
  color: #e5e5e5;
}

.session-item.active {
  background: #262626;
  color: #e5e5e5;
}

.session-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.delete-session-btn {
  opacity: 0;
  padding: 4px;
  background: transparent;
  border: none;
  color: #a3a3a3;
  cursor: pointer;
  transition: all 0.2s;
}

.session-item:hover .delete-session-btn {
  opacity: 1;
}

.delete-session-btn:hover {
  color: #ef4444;
}

/* Main Chat Area */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid #2a2a2a;
  background: #171717;
}

.agent-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.agent-avatar {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.agent-details h1 {
  font-size: 18px;
  font-weight: 600;
  color: #f5f5f5;
}

.agent-details p {
  font-size: 12px;
  color: #737373;
  margin-top: 2px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.icon-btn {
  padding: 8px;
  background: transparent;
  border: 1px solid #3f3f3f;
  border-radius: 6px;
  color: #a3a3a3;
  cursor: pointer;
  transition: all 0.2s;
}

.icon-btn:hover {
  background: #262626;
  color: #e5e5e5;
}

/* Messages */
.messages-container {
  flex: 1;
  overflow-y: auto;
  background: #0f0f0f;
}

.messages-container::-webkit-scrollbar {
  width: 8px;
}

.messages-container::-webkit-scrollbar-track {
  background: transparent;
}

.messages-container::-webkit-scrollbar-thumb {
  background: #2a2a2a;
  border-radius: 4px;
}

.welcome-screen {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  text-align: center;
}

.welcome-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-bottom: 24px;
}

.welcome-screen h2 {
  font-size: 28px;
  font-weight: 600;
  color: #f5f5f5;
  margin-bottom: 8px;
}

.welcome-screen p {
  font-size: 16px;
  color: #737373;
  margin-bottom: 32px;
}

.quick-prompts {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  max-width: 600px;
}

.prompt-btn {
  padding: 10px 16px;
  background: #262626;
  border: 1px solid #3f3f3f;
  border-radius: 8px;
  color: #e5e5e5;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.prompt-btn:hover {
  background: #333333;
  border-color: #525252;
}

.message-list {
  padding: 24px;
  max-width: 900px;
  margin: 0 auto;
}

.history-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 0 20px;
  color: #808080;
  font-size: 13px;
  user-select: none;
}

.history-loading .typing-indicator {
  padding: 0;
}

.message {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  align-self: flex-end;
}

.user-message .message-avatar {
  background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%);
  color: white;
  order: 2;
}

.user-message .message-content {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  order: 1;
}

.user-message .message-bubble {
  background: #3b82f6;
  color: white;
  border-bottom-right-radius: 4px;
  max-width: 70%;
  word-break: break-word;
}

.ai-message .message-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.ai-message .message-content {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.message-content {
  flex: 1;
  min-width: 0;
  max-width: 80%;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
  font-size: 15px;
  word-break: break-word;
}

.ai-message .message-bubble {
  background: #262626;
  color: #e5e5e5;
  border-bottom-left-radius: 4px;
  width: fit-content;
  max-width: 100%;
}

/* Think Block Foldable */
.think-section {
  width: 100%;
  margin-bottom: 8px;
}

.think-block {
  background: #1e1e1e;
  border-left: 3px solid #667eea;
  padding: 10px 14px;
  border-radius: 0 8px 8px 0;
  font-size: 13px;
  color: #9ca3af;
  line-height: 1.6;
  transition: all 0.3s ease;
  max-height: 300px;
  overflow-y: auto;
  opacity: 1;
}

.think-block.folded {
  max-height: 0;
  padding: 0 14px;
  margin-top: 0;
  margin-bottom: 0;
  opacity: 0;
  border: none;
}

.think-toggle-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #262626;
  border: 1px solid #3f3f3f;
  border-radius: 6px;
  color: #808080;
  font-size: 12px;
  cursor: pointer;
  margin-bottom: 8px;
  transition: all 0.2s;
}

.think-toggle-btn:hover {
  background: #333;
  color: #a3a3a3;
}

/* 知识库召回样式 */
.retrieved-section {
  width: 100%;
  margin-bottom: 8px;
}

.retrieved-header {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #262626;
  border: 1px solid #3f3f3f;
  border-radius: 6px;
  color: #808080;
  font-size: 12px;
  cursor: pointer;
  margin-bottom: 8px;
  transition: all 0.2s;
}

.retrieved-header:hover {
  background: #333;
  color: #a3a3a3;
}

.retrieved-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  transition: all 0.3s ease;
  max-height: 500px;
  overflow: hidden;
  opacity: 1;
}

.retrieved-list.folded {
  max-height: 0;
  opacity: 0;
  margin: 0;
}

.retrieved-item {
  background: #1e1e1e;
  border-left: 3px solid #10b981;
  padding: 10px 14px;
  border-radius: 0 8px 8px 0;
}

.retrieved-filename {
  font-size: 13px;
  color: #10b981;
  font-weight: 500;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.retrieved-content {
  font-size: 12px;
  color: #9ca3af;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 工具调用进行中样式 */
.tool-call-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: #1e1e1e;
  border-left: 3px solid #f59e0b;
  border-radius: 0 8px 8px 0;
  font-size: 13px;
  color: #f59e0b;
  margin-bottom: 8px;
  width: fit-content;
}

.tool-call-loading .tool-icon {
  flex-shrink: 0;
}

.tool-call-text {
  margin-right: 4px;
}

.tool-dots {
  padding: 0;
}

.tool-dots span {
  width: 6px;
  height: 6px;
  background: #f59e0b;
}

/* 工具调用结果样式 */
.tool-called-section {
  width: 100%;
  margin-bottom: 8px;
}

.tool-called-item {
  margin-bottom: 8px;
}

.tool-called-header {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #262626;
  border: 1px solid #3f3f3f;
  border-radius: 6px;
  color: #808080;
  font-size: 12px;
  cursor: pointer;
  margin-bottom: 8px;
  transition: all 0.2s;
}

.tool-called-header:hover {
  background: #333;
  color: #a3a3a3;
}

.tool-status {
  margin-left: auto;
  color: #10b981;
  font-size: 11px;
}

.tool-called-result {
  background: #1e1e1e;
  border-left: 3px solid #8b5cf6;
  padding: 10px 14px;
  border-radius: 0 8px 8px 0;
  transition: all 0.3s ease;
  max-height: 300px;
  overflow: hidden;
  opacity: 1;
}

.tool-called-result.folded {
  max-height: 0;
  padding: 0 14px;
  opacity: 0;
  border: none;
}

.tool-called-result pre {
  margin: 0;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
  color: #9ca3af;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 6px;
  padding: 0 4px;
}

.message-time {
  font-size: 11px;
  color: #525252;
}

.message-actions {
  display: flex;
  gap: 4px;
}

.message-actions button {
  padding: 4px;
  background: transparent;
  border: none;
  color: #525252;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
}

.message-actions button:hover {
  background: #333;
  color: #a3a3a3;
}

/* Typing Indicator */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #667eea;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(1) {
  animation-delay: -0.32s;
}

.typing-indicator span:nth-child(2) {
  animation-delay: -0.16s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0s;
}

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

/* ========== 新增：附件相关样式 ========== */
.input-wrapper {
  max-width: 900px;
  margin: 0 auto;
}

.attachment-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px;
  margin-bottom: 8px;
  background: #1e1e1e;
  border-radius: 8px;
  border: 1px solid #2a2a2a;
}

.attachment-preview-item {
  position: relative;
  width: 64px;
  height: 64px;
  border-radius: 6px;
  overflow: hidden;
  background: #262626;
  border: 1px solid #3f3f3f;
}

.attachment-image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.attachment-doc-preview {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: #a3a3a3;
  padding: 4px;
}

.attachment-doc-preview .attachment-name {
  font-size: 10px;
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: center;
}

.remove-attachment-btn {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  border: none;
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  opacity: 0;
  transition: opacity 0.2s;
}

.attachment-preview-item:hover .remove-attachment-btn {
  opacity: 1;
}

.upload-btn {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: transparent;
  border: none;
  color: #a3a3a3;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s;
}

.upload-btn:hover:not(:disabled) {
  background: #333333;
  color: #e5e5e5;
}

.upload-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 消息内附件 */
.message-attachments {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
  max-width: 100%;
  cursor: pointer;
}

.user-message .message-attachments {
  justify-content: flex-end;
}

.message-attachment-item {
  border-radius: 8px;
  overflow: hidden;
}

.attachment-img {
  max-width: 200px;
  max-height: 200px;
  object-fit: cover;
  border-radius: 8px;
}

.attachment-doc-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 13px;
  min-width: 150px;
}

.user-message .attachment-doc-item {
  background: rgba(255, 255, 255, 0.15);
  color: white;
}

.ai-message .attachment-doc-item {
  background: #1e1e1e;
  color: #e5e5e5;
  border: 1px solid #3f3f3f;
}

/* Footer */
.chat-footer {
  padding: 16px 24px 24px;
  background: #171717;
  border-top: 1px solid #2a2a2a;
}

.input-container {
  display: flex;
  gap: 12px;
  max-width: 900px;
  margin: 0 auto;
  background: #262626;
  border: 1px solid #3f3f3f;
  border-radius: 12px;
  padding: 8px;
  transition: border-color 0.2s;
}

.input-container:focus-within {
  border-color: #667eea;
}

.input-container textarea {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: #e5e5e5;
  font-size: 15px;
  line-height: 1.5;
  padding: 8px 12px;
  resize: none;
  font-family: inherit;
}

.input-container textarea::placeholder {
  color: #525252;
}

.send-btn {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.send-btn:hover:not(:disabled) {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.input-hint {
  text-align: center;
  font-size: 11px;
  color: #3f3f3f;
  margin-top: 8px;
}

/* Markdown Styles */
.message-bubble p {
  margin: 0 0 8px 0;
}

.message-bubble p:last-child {
  margin-bottom: 0;
}

.message-bubble ul,
.message-bubble ol {
  margin: 8px 0;
  padding-left: 20px;
}

.message-bubble li {
  margin: 4px 0;
}

.message-bubble .code-block {
  background: #1a1a1a;
  border-radius: 8px;
  padding: 12px 16px;
  margin: 8px 0;
  overflow-x: auto;
}

.message-bubble .code-block code {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  line-height: 1.5;
}

.message-bubble blockquote {
  border-left: 3px solid #525252;
  padding-left: 12px;
  margin: 8px 0;
  color: #808080;
}

.message-bubble table {
  border-collapse: collapse;
  width: 100%;
  margin: 8px 0;
}

.message-bubble th,
.message-bubble td {
  border: 1px solid #3f3f3f;
  padding: 8px 12px;
  text-align: left;
}

.message-bubble th {
  background: #262626;
}

.message-bubble img {
  max-width: 100%;
  border-radius: 8px;
  margin: 8px 0;
}

/* Responsive */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 100;
  }

  .sidebar-collapsed {
    width: 0;
  }

  .chat-main {
    width: 100%;
  }

  .message-list {
    padding: 16px;
  }
}
</style>
