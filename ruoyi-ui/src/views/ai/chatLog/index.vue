<template>
  <div class="app-container">
    <!-- 筛选条件 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="日志类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="请选择类型" style="width: 180px" @change="handleTypeChange">
          <el-option label="Agent" value="agent"/>
          <el-option label="Agent Flow" value="agentFlow"/>
        </el-select>
      </el-form-item>

      <el-form-item :label="queryParams.type === 'agentFlow' ? 'Agent Flow' : 'Agent'" prop="targetId">
        <el-select
          v-if="queryParams.type === 'agentFlow'"
          v-model="queryParams.agentFlowId"
          placeholder="请选择Agent Flow"
          filterable
          clearable
          style="width: 240px"
          @change="handleTargetChange"
        >
          <el-option
            v-for="item in agentFlowOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <el-select
          v-else
          v-model="queryParams.agentId"
          placeholder="请选择Agent"
          filterable
          clearable
          style="width: 240px"
          @change="handleTargetChange"
        >
          <el-option
            v-for="item in agentOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="会话ID" prop="sessionId">
        <el-select
          v-model="queryParams.sessionId"
          placeholder="请选择会话"
          filterable
          clearable
          style="width: 240px"
        >
          <el-option
            v-for="item in sessionOptions"
            :key="item.sessionId"
            :label="item.sessionId"
            :value="item.sessionId"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="追踪ID" prop="traceId">
        <el-input
          v-model="queryParams.traceId"
          placeholder="请输入追踪ID（支持模糊匹配）"
          clearable
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['ai:chatLog:export']"
        >导出
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['ai:chatLog:remove']"
        >批量删除
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="chatLogList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="追踪ID" align="center" prop="traceId" width="220" show-overflow-tooltip/>
      <el-table-column label="会话ID" align="center" prop="sessionId" width="180" show-overflow-tooltip/>
      <el-table-column label="客户端ID" align="center" prop="clientId" width="120" show-overflow-tooltip/>
      <el-table-column label="用户问题" align="center" prop="userQuestion" min-width="180" show-overflow-tooltip/>
      <el-table-column v-if="queryParams.type === 'agentFlow'" label="参与Agent" align="center" prop="agentNames" width="220" show-overflow-tooltip/>
      <el-table-column label="消息数" align="center" prop="msgCount" width="80"/>
      <el-table-column label="开始时间" align="center" prop="startTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.startTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="结束时间" align="center" prop="endTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.endTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="200">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleViewChain(scope.row)"
            v-hasPermi="['ai:chatLog:query']"
          >查看调用链路
          </el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDeleteRow(scope.row)"
            v-hasPermi="['ai:chatLog:remove']"
          >删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 调用链路详情对话框 -->
    <el-dialog
      :title="dialog.title"
      :visible.sync="dialog.visible"
      width="60%"
      append-to-body
      top="5vh"
      @close="resetDialog"
    >
      <div v-loading="dialog.loading">
        <!-- 头部 info -->
        <el-descriptions :column="3" border size="small" style="margin-bottom: 12px">
          <el-descriptions-item label="追踪ID">{{ dialog.detail.traceId }}</el-descriptions-item>
          <el-descriptions-item label="会话ID">{{ dialog.detail.sessionId }}</el-descriptions-item>
          <el-descriptions-item label="消息数">{{ (dialog.detail.messages || []).length }}</el-descriptions-item>
        </el-descriptions>

        <!-- Agent Flow：流程图参与者 -->
        <el-card v-if="isAgentFlow" shadow="never" class="chain-card">
          <div slot="header" class="clearfix">
            <span>Agent 调用链路</span>
            <el-tag size="mini" style="margin-left: 8px">共 {{ (dialog.detail.participantAgents || []).length }} 个 Agent 参与</el-tag>
          </div>
          <div v-if="!dialog.detail.participantAgents || dialog.detail.participantAgents.length === 0" class="chain-empty">
            暂无参与者信息
          </div>
          <div v-else class="chain-flow">
            <template v-for="(agent, idx) in dialog.detail.participantAgents">
              <div class="chain-node" :key="agent.agentId">
                <div class="chain-node-box">
                  <i class="el-icon-cpu"></i>
                  <div class="chain-node-name">{{ agent.agentName }}</div>
                  <div class="chain-node-meta">消息: {{ agent.msgCount }}</div>
                </div>
                <i v-if="idx < dialog.detail.participantAgents.length - 1" class="el-icon-right chain-arrow"></i>
              </div>
            </template>
          </div>
        </el-card>

        <!-- 单 Agent：参与者 -->
        <el-card v-else shadow="never" class="chain-card">
          <div slot="header" class="clearfix">
            <span>Agent 调用链路</span>
          </div>
          <div v-if="dialog.detail.messages && dialog.detail.messages.length">
            <div class="chain-flow">
              <div class="chain-node">
                <div class="chain-node-box chain-node-user">
                  <i class="el-icon-user"></i>
                  <div class="chain-node-name">用户</div>
                </div>
                <i class="el-icon-right chain-arrow"></i>
              </div>
              <div class="chain-node">
                <div class="chain-node-box chain-node-ai">
                  <i class="el-icon-cpu"></i>
                  <div class="chain-node-name">Agent</div>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="chain-empty">暂无消息</div>
        </el-card>

        <!-- 消息流 -->
        <el-card shadow="never" style="margin-top: 12px">
          <div slot="header" class="clearfix">
            <span>对话消息</span>
          </div>
          <div v-if="!dialog.detail.messages || dialog.detail.messages.length === 0" class="chain-empty">
            暂无消息
          </div>
          <div v-else class="message-list">
            <div
              v-for="msg in dialog.detail.messages"
              :key="msg.id"
              class="message-item"
              :class="{ 'message-assistant': msg.role === 'assistant', 'message-user': msg.role === 'user' }"
            >
              <div class="message-meta">
                <el-tag :type="msg.role === 'user' ? 'success' : 'primary'" size="mini">
                  {{ msg.role === 'user' ? '用户' : (msg.agentName || 'AI') }}
                </el-tag>
                <span class="message-time">{{ parseTime(msg.createTime) }}</span>
              </div>
              <div class="message-body">{{ msg.content }}</div>
              <div v-if="msg.toolCalledList && msg.toolCalledList.length" class="message-tools">
                <el-tag v-for="tool in msg.toolCalledList" :key="tool.id" type="info" size="mini" effect="plain" style="margin-right: 4px">
                  工具: {{ tool.toolName }}
                </el-tag>
              </div>
            </div>
          </div>
        </el-card>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialog.visible = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  listChatLog,
  getChatLogDetail,
  listChatLogAgentOptions,
  listChatLogAgentFlowOptions,
  listAgentSessions,
  listAgentFlowSessions,
  delChatLog
} from '@/api/ai/chatLog'
import { parseTime } from '@/utils/ruoyi'

export default {
  name: 'ChatLog',
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 对话日志表格数据
      chatLogList: [],
      // Agent 下拉
      agentOptions: [],
      // Agent Flow 下拉
      agentFlowOptions: [],
      // 会话下拉
      sessionOptions: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        type: 'agent',
        agentId: null,
        agentFlowId: null,
        sessionId: null,
        traceId: null
      },
      // 详情对话框
      dialog: {
        visible: false,
        title: '',
        loading: false,
        detail: {
          type: 'agent',
          traceId: null,
          sessionId: null,
          messages: [],
          participantAgents: []
        }
      }
    }
  },
  computed: {
    isAgentFlow() {
      return this.dialog.detail.type === 'agentFlow'
    }
  },
  created() {
    this.loadOptions()
    this.getList()
  },
  methods: {
    parseTime,
    /** 加载 Agent / Agent Flow 下拉 */
    loadOptions() {
      listChatLogAgentOptions().then(res => {
        this.agentOptions = res.data || []
      })
      listChatLogAgentFlowOptions().then(res => {
        this.agentFlowOptions = res.data || []
      })
    },
    /** 类型切换清空目标 & 列表 */
    handleTypeChange() {
      this.queryParams.agentId = null
      this.queryParams.agentFlowId = null
      this.queryParams.sessionId = null
      this.sessionOptions = []
      this.chatLogList = []
      this.total = 0
    },
    /** 目标切换加载会话列表 */
    handleTargetChange() {
      this.queryParams.sessionId = null
      this.sessionOptions = []
      if (this.queryParams.type === 'agentFlow' && this.queryParams.agentFlowId) {
        listAgentFlowSessions(this.queryParams.agentFlowId).then(res => {
          this.sessionOptions = res.data || []
        })
      } else if (this.queryParams.type === 'agent' && this.queryParams.agentId) {
        listAgentSessions(this.queryParams.agentId).then(res => {
          this.sessionOptions = res.data || []
        })
      }
    },
    /** 查询列表 */
    getList() {
      this.loading = true
      listChatLog(this.queryParams).then(res => {
        this.chatLogList = res.rows || []
        this.total = res.total || 0
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    /** 搜索 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置 */
    resetQuery() {
      this.queryParams = {
        pageNum: 1,
        pageSize: 10,
        type: 'agent',
        agentId: null,
        agentFlowId: null,
        sessionId: null,
        traceId: null
      }
      this.sessionOptions = []
      this.handleQuery()
    },
    /** 多选 */
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.traceId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 查看调用链路 */
    handleViewChain(row) {
      this.dialog.visible = true
      this.dialog.title = row.agentFlowId
        ? `Agent Flow 调用链路 - ${row.traceId}`
        : `Agent 调用链路 - ${row.traceId}`
      this.dialog.loading = true
      getChatLogDetail(row.agentFlowId ? 'agentFlow' : 'agent', row.traceId).then(res => {
        this.dialog.detail = {
          type: row.agentFlowId ? 'agentFlow' : 'agent',
          traceId: row.traceId,
          sessionId: row.sessionId,
          agentId: row.agentId,
          agentFlowId: row.agentFlowId,
          messages: res.data.messages || [],
          participantAgents: res.data.participantAgents || []
        }
        this.dialog.loading = false
      }).catch(() => {
        this.dialog.loading = false
      })
    },
    /** 关闭对话框 */
    resetDialog() {
      this.dialog.detail = {
        type: 'agent',
        traceId: null,
        sessionId: null,
        messages: [],
        participantAgents: []
      }
    },
    /** 批量删除 */
    handleDelete() {
      const traceIds = this.ids.join(',')
      this.$modal.confirm('是否确认删除所选对话？此操作不可恢复').then(() => {
        // 串行删除，简单可靠
        const promises = this.ids.map(id => delChatLog(id, this.queryParams.type))
        return Promise.all(promises)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    /** 删除单行 */
    handleDeleteRow(row) {
      this.$modal.confirm(`是否确认删除 traceId=${row.traceId} 的对话？此操作不可恢复`).then(() => {
        return delChatLog(row.traceId, this.queryParams.type)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    /** 导出 */
    handleExport() {
      this.$modal.confirm('是否导出当前查询条件的对话日志？').then(() => {
        this.$message.info('已请求导出，请联系管理员在服务端 /ai/chatLog/export 获取数据。当前条件 traceIds: ' + this.ids.join(','))
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.chain-card {
  margin-bottom: 0;
}

.chain-flow {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.chain-node {
  display: flex;
  align-items: center;
}

.chain-node-box {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 8px 16px;
  background: #f5f7fa;
  min-width: 120px;
  text-align: center;
}

.chain-node-box.chain-node-user {
  background: #f0f9ec;
  border-color: #d8eecf;
}

.chain-node-box.chain-node-ai {
  background: #ecf5ff;
  border-color: #c6e2ff;
}

.chain-node-name {
  font-weight: bold;
  margin: 4px 0;
}

.chain-node-meta {
  font-size: 12px;
  color: #909399;
}

.chain-arrow {
  font-size: 20px;
  color: #409eff;
  margin: 0 6px;
}

.chain-empty {
  text-align: center;
  color: #909399;
  padding: 20px;
}

.message-list {
  max-height: 460px;
  overflow-y: auto;
}

.message-item {
  border-left: 3px solid #67c23a;
  background: #fafafa;
  padding: 10px 12px;
  margin-bottom: 8px;
  border-radius: 4px;
}

.message-item.message-assistant {
  border-left-color: #409eff;
  background: #f4faff;
}

.message-item.message-user {
  border-left-color: #67c23a;
  background: #fafdf6;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.message-time {
  font-size: 12px;
  color: #909399;
}

.message-body {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.6;
}

.message-tools {
  margin-top: 6px;
}
</style>
