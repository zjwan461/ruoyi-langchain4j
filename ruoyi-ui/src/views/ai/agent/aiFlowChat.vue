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
              <el-tag v-if="agentInfo" type="info" size="medium" effect="plain" style="margin-left: 10px;">Agent Flow</el-tag>
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
            :key="msg._id || ('idx-' + idx)"
            class="message"
            :class="{
              'user-message': msg.role === 'user',
              'ai-message': msg.role === 'assistant'
            }"
          >
            <!-- AI Agent 头像：根据 agentName 自动生成渐变色 + 首字母 -->
            <div
              v-if="msg.role === 'assistant'"
              class="message-avatar agent-avatar-bubble"
              :style="getAgentAvatarStyle(msg)"
              :title="msg.agentName || agentInfo.name || 'AI'"
            >
              <span class="avatar-letter">{{ getAgentInitial(msg) }}</span>
            </div>
            <div class="message-content">
              <!-- Agent 名称标签：区分不同 agent 的气泡 -->
              <div
                v-if="msg.role === 'assistant'"
                class="agent-name-tag"
                :style="getAgentTagStyle(msg)"
              >
                {{ msg.agentName || agentInfo.name || 'AI' }}
              </div>

              <!-- Think Block (foldable) -->
              <div v-if="msg.role === 'assistant' && hasThinkContent(msg.content)" class="think-section">
                <!-- ========= 修改点：增加动态ref ========= -->
                <div
                  class="think-block agent-think-block"
                  :class="{ 'folded': isThinkFolded(msg, msg._id) }"
                  :ref="'thinkBlock-' + (msg._id || ('idx-' + idx))"
                  :style="{ borderLeftColor: getAgentColor(msg)[0] }"
                >
                  {{ getThinkContent(msg.content) }}
                </div>
                <button v-if="!msg.loading" class="think-toggle-btn" @click="toggleThink(msg._id)">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline :points="isThinkFolded(msg, msg._id) ? '6 9 12 15 18 9' : '18 15 12 9 6 15'"></polyline>
                  </svg>
                  {{ isThinkFolded(msg, msg._id) ? '展开思考过程' : '收起思考过程' }}
                </button>
              </div>

              <!-- 知识库召回参考资料 -->
              <div v-if="msg.role === 'assistant' && msg.retrievedList && msg.retrievedList.length > 0" class="retrieved-section">
                <div class="retrieved-header" @click="toggleRetrieved(msg._id)">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline :points="isRetrievedFolded(msg, msg._id) ? '6 9 12 15 18 9' : '18 15 12 9 6 15'"></polyline>
                  </svg>
                  <span class="retrieved-title-text">参考资料 ({{ msg.retrievedList.length }})</span>
                </div>
                <div class="retrieved-list" :class="{ 'folded': isRetrievedFolded(msg, msg._id) }">
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
                  <div class="tool-called-header" @click="toggleToolCalled(msg._id, toolIdx)">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline :points="isToolCalledFolded(msg, msg._id, toolIdx) ? '6 9 12 15 18 9' : '18 15 12 9 6 15'"></polyline>
                    </svg>
                    <span class="tool-name">工具调用：{{ tool.toolName }}</span>
                    <span class="tool-status">已完成</span>
                  </div>
                  <div class="tool-called-result agent-tool-result" :class="{ 'folded': isToolCalledFolded(msg, msg._id, toolIdx) }">
                    <pre>{{ tool.toolInvokeResult }}</pre>
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
        <div class="input-container">
          <textarea
            v-model="userInput"
            ref="inputArea"
            placeholder="输入消息..."
            @keydown="handleKeyDown"
            rows="1"
          ></textarea>
          <button class="send-btn" @click="sendMessage" :disabled="!userInput.trim() || isLoading">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="22" y1="2" x2="11" y2="13"></line>
              <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
            </svg>
          </button>
        </div>
        <p class="input-hint">按 Enter 发送，Shift + Enter 换行</p>
      </footer>
    </main>
  </div>
</template>

<script>
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import {getAgent, createSession, listChatSession, listChatMessage, deleteSession} from '@/api/ai/aiFlowChat'
import {nanoid} from 'nanoid'

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
        return '<pre class="code-block"><code class="hljs language-' + validLang + '">' + hljs.highlight(str, {language: validLang, ignoreIllegals: true}).value + '</code></pre>'
      } catch (__) {}
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
      // agentId -> agentName 缓存，用于历史/跨 agent 复用名称
      agentRegistry: {},
      // 聊天历史分页：初始仅加载时间最近的一页，向上滚动加载更早的页
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
      ]
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
      // 重新加载会话时重置分页状态
      this.pageNum = 1
      this.hasMoreHistory = true
      this.historyTotal = 0
      this.historyLoading = false
      this.$modal.loading("loading...")
      listChatMessage(this.sessionId, this.agentInfo.id, this.pageNum, this.pageSize).then(res => {
        this.$modal.closeLoading()
        const rows = (res && res.rows) || []
        // 后端按 create_time desc 返回，reverse 成 asc 以便按时间正序展示
        const normalized = [...rows].reverse().map((r, i) => this.normalizeHistoryMessage(r, 'p1-' + i))
        this.messages = normalized
        // 顺便把历史消息中的 agentName 注册到 agentRegistry，便于重新流式输出时复用
        for (const m of normalized) {
          if (m.agentId != null && m.agentName) {
            this.$set(this.agentRegistry, m.agentId, m.agentName)
          }
        }
        this.historyTotal = (res && typeof res.total === 'number') ? res.total : rows.length
        this.hasMoreHistory = this.messages.length < this.historyTotal
        this.$nextTick(() => {
          this.scrollToBottom()
        })
      }).catch(err=>{
        this.$modal.closeLoading()
      })
    },
    /**
     * 加载更早一页的历史消息（向前翻页）。后端会按 desc 返回，reverse 后 unshift 到头部，
     * 并恢复滚动位置，避免页面跳动。
     */
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
          // 服务端已经没有更多数据
          this.hasMoreHistory = false
          return
        }
        const ascRows = [...rows].reverse().map((r, i) => this.normalizeHistoryMessage(r, 'p' + nextPage + '-' + i))
        // 把 agentName 注册到 agentRegistry
        for (const m of ascRows) {
          if (m.agentId != null && m.agentName) {
            this.$set(this.agentRegistry, m.agentId, m.agentName)
          }
        }
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
        // 失败时不推进 pageNum，便于下次重试
      }).finally(() => {
        this.historyLoading = false
      })
    },
    handleScroll(e) {
      const el = e && e.target
      if (!el) return
      // 滚动到顶部附近（阈值 50px）时加载更早的一页
      if (el.scrollTop <= 50 && this.hasMoreHistory && !this.historyLoading && this.messages.length > 0) {
        this.loadMoreHistory()
      }
    },
    switchSession(sessionId) {
      this.sessionId = sessionId
      localStorage.setItem('chat-sessionId', sessionId)
      this.getChatHistoryMessage()
    },
    newSession() {
      createSession(this.clientId).then(res => {
        this.sessionId = res.data
        localStorage.setItem('chat-sessionId', this.sessionId)
        this.messages = []
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
        this.getSessionList()
        this.getChatHistoryMessage()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    clearChatHistory() {
      this.$modal.confirm('确认清空当前对话？').then(() => {
        if (this.streamController) {
          this.streamController.abort()
          this.streamController = null
        }
        this.messages = []
        this.$modal.msgSuccess('已清空')
      }).catch(() => {})
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
    sendMessage() {
      const content = this.userInput.trim()
      if (!content || this.isLoading) return

      this.messages.push({
        role: 'user',
        content: content,
        createTime: new Date().toISOString(),
        _id: 'user-' + Date.now()
      })

      this.userInput = ''
      this.isLoading = true
      this.scrollToBottom()

      this.callStreamApi(content)
    },
    async callStreamApi(content) {
      this.streamController = new AbortController()
      const chatApi = process.env.VUE_APP_BASE_API + '/ai-flow-chat'

      // 先 push 一个未指定 agent 的占位气泡，作为“等待首个 agent 上场”的反馈
      const placeholderMsg = {
        role: 'assistant',
        agentId: null,
        agentName: null,
        content: '',
        loading: true,
        createTime: new Date().toISOString(),
        _id: 'ai-placeholder-' + Date.now(),
        thinkFolded: false,
        retrievedList: [],
        currentToolCall: null,
        toolCalledList: []
      }
      this.messages.push(placeholderMsg)
      this.scrollToBottom()

      try {
        const response = await fetch(chatApi, {
          method: 'POST',
          headers: {'Content-Type': 'application/json'},
          body: JSON.stringify({
            prompt: content,
            agentFlowId: this.agentInfo.id,
            clientId: this.clientId,
            sessionId: this.sessionId
          }),
          signal: this.streamController.signal
        })

        const reader = response.body.getReader()
        const decoder = new TextDecoder('utf-8')

        // ⚠️ buffer 必须放在 while 循环外面！！
        let buffer = ''

        while (true) {
          const { done, value } = await reader.read()
          if (done) break

          const chunk = decoder.decode(value)
          buffer += chunk

          // 循环切割完整行
          let newlineIndex
          while ((newlineIndex = buffer.indexOf('\n')) !== -1) {
            const line = buffer.slice(0, newlineIndex).trim()
            buffer = buffer.slice(newlineIndex + 1)

            if (!line || !line.startsWith('data:')) continue
            const jsonRaw = line.replace(/^data:\s*/, '').trim()
            if (!jsonRaw) continue

            let res
            try {
              res = JSON.parse(jsonRaw)
            } catch (e) {
              console.error('解析失败单行：', jsonRaw, e)
              continue
            }

            // 使用 agentId 找到 / 创建对应的 AI 气泡，实现多 agent 气泡隔离
            const aiMsg = this.findOrCreateAgentBubble(res.agentId, res.agentName)
            if (!aiMsg) continue

            if (res.msg != null) {
              aiMsg.content = (aiMsg.content || '') + res.msg
            } else if (res.retrieved) {
              aiMsg.retrievedList = res.retrieved
            } else if (res.tool_call) {
              aiMsg.currentToolCall = res.tool_call
            } else if (res.tool_called) {
              aiMsg.toolCalledList.push(res.tool_called)
              aiMsg.currentToolCall = null
            }
            // 外层聊天容器滚动
            this.scrollToBottom()
            // ==========思考框内部自动滚动到底==========
            this.scrollThinkBlockToBottom(aiMsg._id)
          }
        }

        // 流关闭：把所有仍处于 loading 的 AI 气泡标记为完成
        // 同时识别占位气泡：agentId 仍为 null 表示后端没下发任何数据
        for (let i = this.messages.length - 1; i >= 0; i--) {
          const m = this.messages[i]
          if (m.role === 'assistant' && m.loading) {
            m.loading = false
            // 占位气泡没有真实内容时，提示用户未收到响应
            if (m.agentId == null && !m.content && (!m.toolCalledList || m.toolCalledList.length === 0)) {
              m.content = '⚠️ 未收到任何 agent 的响应，请稍后重试'
            }
          }
        }
        this.scrollToBottom()
      } catch (error) {
        if (error.name !== 'AbortError') {
          placeholderMsg.content += '\n\n⚠️ 响应中断，请重试'
        }
        placeholderMsg.loading = false
        // 让占位气泡外的 AI 气泡也都结束 loading
        for (let i = this.messages.length - 1; i >= 0; i--) {
          const m = this.messages[i]
          if (m.role === 'assistant' && m.loading) {
            m.loading = false
          }
        }
      } finally {
        this.isLoading = false
      }
    },
    // ========== 新增方法：思考区域内部滚动到底 ==========
    scrollThinkBlockToBottom(key) {
      this.$nextTick(() => {
        if (key == null) return
        const refName = 'thinkBlock-' + key
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
        // 思考已结束，返回标签内完整内容
        return content.substring(startIdx + THINK_START.length, endIdx)
      } else {
        // 思考进行中，返回从开始标签后到当前末尾的所有已接收内容
        return content.substring(startIdx + THINK_START.length)
      }
    },
    getOutputContent(content) {
      if (!content) return ''
      const endIdx = content.indexOf(THINK_END)
      if (endIdx !== -1) {
        // 思考已结束，返回结束标签后的正文内容
        return content.substring(endIdx + THINK_END.length)
      }
      // 有开始标签但无结束标签：思考进行中，正文暂为空
      const startIdx = content.indexOf(THINK_START)
      if (startIdx !== -1) {
        return ''
      }
      // 无思考标签，直接返回全部内容
      return content
    },
    renderMarkdown(content) {
      if (!content) return ''
      const output = this.getOutputContent(content)
      return md.render(output)
    },
    // ============ 折叠状态：使用 msg._id 作为 key，避免多 agent 时索引错乱 ============
    _findMsgByKey(key) {
      if (key == null) return null
      return this.messages.find(m => m._id === key) || null
    },
    isThinkFolded(msg, key) {
      if (!msg) return false
      const k = 'think-' + key
      // 用户手动切换过，优先尊重用户操作
      if (this.foldedStates.hasOwnProperty(k)) {
        return this.foldedStates[k]
      }
      // 自动逻辑：思考结束则自动折叠，思考过程中保持展开
      return this.isThinkFinished(msg.content)
    },
    toggleThink(key) {
      const msg = this._findMsgByKey(key)
      if (!msg) return
      const k = 'think-' + key
      const current = this.isThinkFolded(msg, key)
      this.$set(this.foldedStates, k, !current)
    },
    isRetrievedFolded(msg, key) {
      if (!msg) return true
      const k = 'retrieved-' + key
      if (this.foldedStates.hasOwnProperty(k)) {
        return this.foldedStates[k]
      }
      return true
    },
    toggleRetrieved(key) {
      const msg = this._findMsgByKey(key)
      if (!msg) return
      const k = 'retrieved-' + key
      const current = this.isRetrievedFolded(msg, key)
      this.$set(this.foldedStates, k, !current)
    },
    isToolCalledFolded(msg, key, toolIndex) {
      if (!msg) return true
      const k = 'tool-' + key + '-' + toolIndex
      if (this.foldedStates.hasOwnProperty(k)) {
        return this.foldedStates[k]
      }
      return true
    },
    toggleToolCalled(key, toolIndex) {
      const msg = this._findMsgByKey(key)
      if (!msg) return
      const k = 'tool-' + key + '-' + toolIndex
      const current = this.isToolCalledFolded(msg, key, toolIndex)
      this.$set(this.foldedStates, k, !current)
    },
    // ============ 多 Agent 头像与名称标签样式 ============
    getAgentInitial(msg) {
      const name = msg && (msg.agentName || (this.agentInfo && this.agentInfo.name))
      if (!name) return 'A'
      return String(name).trim().charAt(0).toUpperCase()
    },
    getAgentColor(msg) {
      // 调色板：根据 agentName/agentId hash 出稳定的渐变组合
      const palette = [
        ['#667eea', '#764ba2'], // 紫蓝
        ['#f093fb', '#f5576c'], // 粉红
        ['#4facfe', '#00f2fe'], // 蓝色
        ['#43e97b', '#38f9d7'], // 翠绿
        ['#fa709a', '#fee140'], // 橙黄
        ['#30cfd0', '#330867'], // 青紫
        ['#ff9a9e', '#fad0c4'], // 暖粉
        ['#a18cd1', '#fbc2eb'], // 紫粉
        ['#84fab0', '#8fd3f4'], // 薄荷
        ['#ffecd2', '#fcb69f']  // 暖橙
      ]
      const seed = (msg && (msg.agentName || (msg.agentId != null ? String(msg.agentId) : ''))) || 'AI'
      let hash = 0
      for (let i = 0; i < seed.length; i++) {
        hash = ((seed.charCodeAt(i) * 31) + hash) | 0
      }
      const idx = Math.abs(hash) % palette.length
      return palette[idx]
    },
    getAgentAvatarStyle(msg) {
      const [c1, c2] = this.getAgentColor(msg)
      return {
        background: 'linear-gradient(135deg, ' + c1 + ' 0%, ' + c2 + ' 100%)'
      }
    },
    getAgentTagStyle(msg) {
      const [c1, c2] = this.getAgentColor(msg)
      return {
        background: 'linear-gradient(135deg, ' + c1 + ' 0%, ' + c2 + ' 100%)'
      }
    },
    // ============ 按 agentId 查找/创建 AI 气泡 ============
    findOrCreateAgentBubble(agentId, agentName) {
      const last = this.messages.length > 0 ? this.messages[this.messages.length - 1] : null
      // 优先复用：最后一条 AI 气泡且仍 loading + agentId 一致（或占位 null）
      if (last && last.role === 'assistant' && last.loading && (last.agentId === agentId || last.agentId == null)) {
        if (last.agentId == null) {
          last.agentId = agentId
        }
        if (agentName && !last.agentName) {
          last.agentName = agentName
        }
        if (agentId != null && agentName) {
          this.$set(this.agentRegistry, agentId, agentName)
        }
        return last
      }
      // 同一 agent 反复出现场景：最后一条 AI 气泡 agentId 一致 + loading 仍 true，复用
      if (last && last.role === 'assistant' && last.loading && last.agentId === agentId) {
        if (agentName && !last.agentName) {
          last.agentName = agentName
        }
        return last
      }
      // agent 切换：把上一条仍然 loading 的气泡标记为完成
      if (last && last.role === 'assistant' && last.loading) {
        last.loading = false
      }
      // 创建新气泡
      const resolvedName = agentName || (agentId != null ? this.agentRegistry[agentId] : '') || ''
      const newMsg = {
        role: 'assistant',
        agentId: agentId != null ? agentId : null,
        agentName: resolvedName,
        content: '',
        loading: true,
        createTime: new Date().toISOString(),
        _id: 'ai-' + (agentId != null ? agentId : 'unknown') + '-' + Date.now() + '-' + Math.random().toString(36).slice(2, 6),
        thinkFolded: false,
        retrievedList: [],
        currentToolCall: null,
        toolCalledList: []
      }
      this.messages.push(newMsg)
      if (agentId != null && agentName) {
        this.$set(this.agentRegistry, agentId, agentName)
      }
      return newMsg
    },
    // 校验并补齐历史消息字段（兼容旧数据无 agentId/agentName）
    normalizeHistoryMessage(row, fallbackId) {
      const base = row || {}
      return Object.assign({}, base, {
        _id: base._id || base.id || ('history-' + (fallbackId || '') + '-' + Math.random().toString(36).slice(2, 8)),
        agentId: base.agentId != null ? base.agentId : null,
        agentName: base.agentName || (this.agentInfo && this.agentInfo.name) || 'AI',
        retrievedList: base.retrievedList || [],
        toolCalledList: base.toolCalledList || [],
        currentToolCall: base.currentToolCall || null,
        loading: false
      })
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
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
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

.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }
.typing-indicator span:nth-child(3) { animation-delay: 0s; }

@keyframes typing {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
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
/* 多 Agent 头像与名称标签样式 */
.agent-avatar-bubble {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  align-self: flex-end;
  font-weight: 600;
  text-transform: uppercase;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.25);
  overflow: hidden;
}

.agent-avatar-bubble .avatar-letter {
  font-size: 16px;
  line-height: 1;
  letter-spacing: 0;
}

.agent-name-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 500;
  color: #fff;
  margin-bottom: 6px;
  letter-spacing: 0.4px;
  align-self: flex-start;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
  max-width: fit-content;
  word-break: break-all;
}

/* think 块边框色由 inline style 覆盖为 agent 颜色 */
.agent-think-block {
  border-left-width: 3px;
  border-left-style: solid;
}
</style>
