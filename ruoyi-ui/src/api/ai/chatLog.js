import request from '@/utils/request'

/**
 * 查询对话日志分页（按 traceId 聚合）
 * @param {Object} query
 *   type: agent | agentFlow
 *   agentId: 单 Agent 必填
 *   agentFlowId: Agent Flow 必填
 *   sessionId: 可选
 *   traceId: 可选
 *   pageNum / pageSize: 分页
 */
export function listChatLog(query) {
  return request({
    url: '/ai/chatLog/list',
    method: 'get',
    params: query
  })
}

/**
 * 查询某次对话的全部消息（含 invoke 数据）
 */
export function getChatLogDetail(type, traceId) {
  return request({
    url: '/ai/chatLog/detail',
    method: 'get',
    params: { type, traceId }
  })
}

/**
 * 查询 Agent Flow 一次对话中参与的 Agent 列表
 */
export function getChatLogAgentChain(traceId) {
  return request({
    url: '/ai/chatLog/agentChain',
    method: 'get',
    params: { traceId }
  })
}

/**
 * Agent 下拉选项
 */
export function listChatLogAgentOptions() {
  return request({
    url: '/ai/chatLog/agentOptions',
    method: 'get'
  })
}

/**
 * Agent Flow 下拉选项
 */
export function listChatLogAgentFlowOptions() {
  return request({
    url: '/ai/chatLog/agentFlowOptions',
    method: 'get'
  })
}

/**
 * 单 Agent 历史会话下拉
 */
export function listAgentSessions(agentId) {
  return request({
    url: '/ai/chatLog/agentSessions',
    method: 'get',
    params: { agentId }
  })
}

/**
 * Agent Flow 历史会话下拉
 */
export function listAgentFlowSessions(agentFlowId) {
  return request({
    url: '/ai/chatLog/agentFlowSessions',
    method: 'get',
    params: { agentFlowId }
  })
}

/**
 * 删除单次对话（按 traceId）
 */
export function delChatLog(traceId, type) {
  return request({
    url: '/ai/chatLog/' + traceId,
    method: 'delete',
    params: { type }
  })
}

/**
 * 导出对话日志
 */
export function exportChatLog(query) {
  return request({
    url: '/ai/chatLog/export',
    method: 'get',
    params: query
  })
}
