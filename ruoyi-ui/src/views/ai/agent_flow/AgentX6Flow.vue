<template>
  <div class="agent-flow-container">
    <!-- 左侧：Agent 组件列表 -->
    <div class="left-panel">
      <h3>🧩 Agent 列表</h3>

      <div class="action-group">
        <div class="flow-btn btn-danger" @click="deleteSelected">
          <i class="el-icon-delete"></i>
          <span>删除选中</span>
        </div>
        <div class="flow-btn btn-primary" @click="exportFlow">
          <i class="el-icon-download"></i>
          <span>导出编排JSON</span>
        </div>
      </div>

      <div class="list-divider"></div>

      <div
        v-for="agent in agentList"
        :key="agent.id"
        class="agent-item"
        @mousedown="onStartDrag(agent, $event)"
      >
        <span class="agent-icon">🤖</span>
        {{ agent.name }}
      </div>

      <div class="tip-box">
        💡 <strong>操作说明：</strong><br>
        1. 拖拽左侧Agent到画布创建节点<br>
        2. 从节点右侧圆点拖出连线<br>
        3. 点击节点在右侧面板编辑属性<br>
        4. 选中后按Delete键删除
      </div>
    </div>

    <!-- 中间：画布区域 -->
    <div class="canvas-wrapper" ref="graphContainer"></div>

    <!-- 右侧：属性配置面板 -->
    <div class="right-panel">
      <h3>⚙️ 属性配置</h3>

      <div v-if="currentNode" class="property-form">
        <div class="form-item">
          <label class="form-label">Agent 名称</label>
          <el-input
            placeholder="请输入Agent名称"
            v-model="currentNodeData.name"
            @input="syncNodeData"
          ></el-input>
        </div>

        <div class="form-item">
          <label class="form-label">职责描述</label>
          <el-input
            type="textarea"
            :rows="4"
            placeholder="请输入该Agent的职责描述"
            v-model="currentNodeData.description"
            @input="syncNodeData"
          ></el-input>
        </div>

        <div class="form-item">
          <label class="form-label">输出推理内容</label>
          <el-switch
            v-model="currentNodeData.showReasoning"
            :disabled="isCurrentRoot"
            @change="syncNodeData"
          ></el-switch>
          <div v-if="isCurrentRoot" class="root-tip">根节点强制开启</div>
        </div>
      </div>

      <div v-else class="empty-tip">
        <i class="el-icon-document-copy"></i>
        <p>请点击画布中的节点<br>查看并编辑属性</p>
      </div>
    </div>
  </div>
</template>

<script>
import { Graph } from '@antv/x6'
import { Dnd } from '@antv/x6-plugin-dnd'
import { Snapline } from '@antv/x6-plugin-snapline'
import { History } from '@antv/x6-plugin-history'

export default {
  name: 'AgentX6Flow',
  data() {
    return {
      graph: null,
      dnd: null,
      selectedCell: null,
      currentNode: null,
      currentNodeData: {
        agentId: '',
        name: '',
        description: '',
        showReasoning: false,
      },
      // Agent 基础配置
      agentList: [
        {
          id: 'agent-1',
          name: '感知 Agent',
          description: '负责采集外部输入信息，进行数据预处理与格式标准化',
          showReasoning: false
        },
        {
          id: 'agent-2',
          name: '决策 Agent',
          description: '根据上下文信息进行逻辑判断，制定执行策略与决策',
          showReasoning: false
        },
        {
          id: 'agent-3',
          name: '执行 Agent',
          description: '接收决策指令，执行具体业务操作并输出结果',
          showReasoning: false
        },
        {
          id: 'agent-4',
          name: '记忆 Agent',
          description: '存储与管理流程上下文，提供历史信息检索能力',
          showReasoning: false
        },
        {
          id: 'agent-5',
          name: '通信 Agent',
          description: '负责多智能体间消息转发与协同，保障通信链路稳定',
          showReasoning: false
        },
      ],
    }
  },

  computed: {
    // 当前选中节点是否为根节点（无入边）
    isCurrentRoot() {
      if (!this.currentNode || !this.graph) return false
      const incomingEdges = this.graph.getIncomingEdges(this.currentNode)
      return !incomingEdges || incomingEdges.length === 0
    }
  },

  mounted() {
    this.$nextTick(() => {
      this.initGraph()
      this.initDnd()
      window.addEventListener('resize', this.onResize)
      window.addEventListener('keydown', this.handleKeydown)
    })
  },

  beforeDestroy() {
    window.removeEventListener('resize', this.onResize)
    window.removeEventListener('keydown', this.handleKeydown)
    if (this.graph) {
      this.graph.dispose()
    }
  },

  methods: {
    initGraph() {
      const container = this.$refs.graphContainer
      const width = container.clientWidth || 800
      const height = container.clientHeight || 600

      // 注册自定义 Agent 节点
      Graph.registerNode(
        'agent-node',
        {
          inherit: 'rect',
          width: 160,
          height: 80,
          attrs: {
            body: {
              fill: '#ffffff',
              stroke: '#5F95FF',
              strokeWidth: 2,
              rx: 8,
              ry: 8,
              filter: 'none',
            },
            label: {
              fill: '#333',
              fontSize: 14,
              fontWeight: '500',
              refX: '50%',
              refY: '50%',
              textAnchor: 'middle',
              textVerticalAnchor: 'middle',
            },
          },
          ports: {
            groups: {
              left: {
                position: 'left',
                single: false,
                attrs: {
                  circle: {
                    r: 7,
                    magnet: true,
                    stroke: '#5F95FF',
                    strokeWidth: 2,
                    fill: '#FFFFFF',
                    cursor: 'crosshair',
                  },
                },
              },
              right: {
                position: 'right',
                single: false,
                attrs: {
                  circle: {
                    r: 7,
                    magnet: true,
                    stroke: '#5F95FF',
                    strokeWidth: 2,
                    fill: '#FFFFFF',
                    cursor: 'crosshair',
                  },
                },
              },
            },
            items: [
              { id: 'left-port', group: 'left' },
              { id: 'right-port', group: 'right' },
            ],
          },
        },
        true
      )

      this.graph = new Graph({
        container,
        width,
        height,
        grid: { size: 10, visible: true },
        interacting: {
          magnetConnectable: true,
        },
        connecting: {
          allowBlank: false,
          allowLoop: false,
          allowNode: false,
          allowEdge: false,
          allowPort: true,
          highlight: true,
          portGravity: 50,
          snap: { radius: 25 },
          anchor: 'center',
          connectionPoint: 'anchor',
          validateMagnet({ magnet, view }) {
            const portId = magnet.getAttribute('port')
            const portGroup = view.cell.getPort(portId)?.group
            return portGroup === 'right'
          },
          validateConnection({ targetPort, targetView }) {
            if (!targetPort) return false
            const portGroup = targetView.cell.getPort(targetPort)?.group
            return portGroup === 'left'
          },
        },
        defaultEdge: {
          attrs: {
            line: {
              stroke: '#5F95FF',
              strokeWidth: 2,
              targetMarker: { name: 'block', size: 8 },
              lineCap: 'round',
              lineJoin: 'round',
            },
          },
          zIndex: 0,
        },
      })

      this.graph.use(new Snapline())
      this.graph.use(new History())

      // 点击元素选中
      this.graph.on('cell:click', ({ cell }) => {
        this.setSelected(cell)
      })

      // 点击空白取消选中
      this.graph.on('blank:click', () => {
        this.clearSelected()
      })

      // 节点添加到画布：根节点自动开启推理显示
      this.graph.on('node:added', ({ node }) => {
        this.$nextTick(() => {
          const incoming = this.graph.getIncomingEdges(node)
          if (!incoming || incoming.length === 0) {
            const data = node.getData()
            node.setData({ ...data, showReasoning: true })
            if (this.currentNode === node) {
              this.currentNodeData.showReasoning = true
            }
          }
        })
      })

      // 连线成功：更新目标节点根状态
      this.graph.on('edge:connected', ({ edge }) => {
        const targetNode = edge.getTargetCell()
        const sourceNode = edge.getSourceCell()
        console.log('🔗 新增连线：', sourceNode.getData()?.name, '→', targetNode.getData()?.name)

        if (this.currentNode === targetNode) {
          this.currentNodeData = { ...targetNode.getData() }
        }
      })

      // 连线删除：目标节点若变为根节点，强制开启推理
      this.graph.on('edge:removed', ({ edge }) => {
        const targetNode = edge.getTargetCell()
        this.$nextTick(() => {
          const incoming = this.graph.getIncomingEdges(targetNode)
          if (!incoming || incoming.length === 0) {
            const data = targetNode.getData()
            targetNode.setData({ ...data, showReasoning: true })
            if (this.currentNode === targetNode) {
              this.currentNodeData.showReasoning = true
            }
          }
        })
      })
    },

    initDnd() {
      this.dnd = new Dnd({
        target: this.graph,
        scaled: false,
        animation: true,
      })
    },

    onStartDrag(agent, e) {
      const node = this.graph.createNode({
        shape: 'agent-node',
        label: agent.name,
        data: {
          agentId: agent.id,
          name: agent.name,
          description: agent.description,
          showReasoning: agent.showReasoning,
        },
      })
      this.dnd.start(node, e, { offsetX: -80, offsetY: -40 })
    },

    // ========== 选中与属性同步 ==========
    setSelected(cell) {
      this.clearSelected()
      this.selectedCell = cell
      if (cell.isNode()) {
        this.currentNode = cell
        this.currentNodeData = { ...cell.getData() }
      }
    },

    clearSelected() {
      if (this.selectedCell) {
        if (this.selectedCell.isNode()) {
          this.selectedCell.attr('body/stroke', '#5F95FF')
          this.selectedCell.attr('body/strokeWidth', 2)
        } else if (this.selectedCell.isEdge()) {
          this.selectedCell.attr('line/stroke', '#5F95FF')
          this.selectedCell.attr('line/strokeWidth', 2)
        }
      }
      this.selectedCell = null
      this.currentNode = null
      this.currentNodeData = {
        agentId: '',
        name: '',
        description: '',
        showReasoning: false,
      }
    },

    // 将面板修改同步到节点数据与显示
    syncNodeData() {
      if (!this.currentNode) return
      this.currentNode.setData({ ...this.currentNodeData })
      // 同步更新画布上节点的显示名称
      this.currentNode.attr('label/text', this.currentNodeData.name)
    },

    // ========== 键盘事件 ==========
    handleKeydown(e) {
      const tag = e.target.tagName.toLowerCase()
      if (tag === 'input' || tag === 'textarea') return

      if (e.key === 'Delete' || e.key === 'Backspace') {
        e.preventDefault()
        this.deleteSelected()
      }

      if (e.ctrlKey && e.key === 'z') {
        e.preventDefault()
        if (this.graph.canUndo()) {
          this.graph.undo()
          this.clearSelected()
        }
      }

      if (e.ctrlKey && e.key === 'y') {
        e.preventDefault()
        if (this.graph.canRedo()) {
          this.graph.redo()
          this.clearSelected()
        }
      }
    },

    // ========== 删除选中 ==========
    deleteSelected() {
      if (!this.selectedCell) {
        this.$message.info('请先点击选中要删除的节点或连线')
        return
      }
      this.graph.removeCells([this.selectedCell])
      this.clearSelected()
      this.$message.success('删除成功')
    },

    // ========== 导出编排JSON ==========
    exportFlow() {
      const nodes = this.graph.getNodes().map(node => {
        const data = node.getData() || {}
        const position = node.position()
        return {
          nodeId: node.id,
          agentId: data.agentId,
          agentName: data.name,
          description: data.description,
          showReasoning: data.showReasoning,
          position: {
            x: Math.round(position.x),
            y: Math.round(position.y),
          },
        }
      })

      const edges = this.graph.getEdges().map(edge => {
        const sourceNode = edge.getSourceCell()
        const targetNode = edge.getTargetCell()
        return {
          edgeId: edge.id,
          sourceNodeId: sourceNode?.id,
          targetNodeId: targetNode?.id,
          sourceAgentName: sourceNode?.getData()?.name,
          targetAgentName: targetNode?.getData()?.name,
        }
      })

      const flowData = {
        version: '1.0.0',
        exportTime: new Date().toLocaleString(),
        nodeCount: nodes.length,
        edgeCount: edges.length,
        nodes,
        edges,
      }

      console.log('📋 完整编排JSON结构：', flowData)
      this.$message.success('编排JSON已输出到浏览器控制台')
      return flowData
    },

    onResize() {
      if (this.graph) {
        const container = this.$refs.graphContainer
        this.graph.resize(container.clientWidth, container.clientHeight)
      }
    },
  },
}
</script>

<style scoped>
.agent-flow-container {
  display: flex;
  height: 100vh;
  background: #f5f7fa;
}

/* ========== 左侧面板 ========== */
.left-panel {
  width: 220px;
  background: #ffffff;
  padding: 20px 16px;
  border-right: 1px solid #e8ecf1;
  box-shadow: 2px 0 8px rgba(0,0,0,0.03);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.left-panel h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1d2129;
}

.action-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
  margin-bottom: 8px;
}

.flow-btn {
  width: 100%;
  height: 32px;
  line-height: 32px;
  padding: 0 15px;
  box-sizing: border-box;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  user-select: none;
  display: flex;
  align-items: center;
  transition: all 0.2s;
}

.flow-btn i {
  font-size: 14px;
  margin-right: 8px;
  line-height: 1;
}

.flow-btn span {
  flex: 1;
  text-align: left;
  line-height: 1;
}

.btn-danger {
  color: #fff;
  background: #f56c6c;
  border: 1px solid #f56c6c;
}
.btn-danger:hover {
  background: #f78989;
  border-color: #f78989;
}
.btn-danger:active {
  background: #e64242;
  border-color: #e64242;
}

.btn-primary {
  color: #fff;
  background: #409eff;
  border: 1px solid #409eff;
}
.btn-primary:hover {
  background: #66b1ff;
  border-color: #66b1ff;
}
.btn-primary:active {
  background: #337ecc;
  border-color: #337ecc;
}

.list-divider {
  height: 1px;
  background: #f0f2f5;
  margin: 16px 0;
}

.agent-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  margin-bottom: 8px;
  background: #f7f9fc;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  cursor: grab;
  user-select: none;
  transition: all 0.2s;
  font-size: 14px;
  color: #2c3e50;
}

.agent-item:hover {
  background: #e6f0ff;
  border-color: #5F95FF;
  transform: translateX(2px);
  box-shadow: 0 2px 6px rgba(95, 149,255,0.15);
}

.agent-item:active {
  cursor: grabbing;
}

.agent-icon {
  margin-right: 8px;
  font-size: 18px;
}

.tip-box {
  margin-top: 16px;
  font-size: 12px;
  color: #888;
  line-height: 1.8;
  padding: 10px;
  background: #fafbfc;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}

/* ========== 中间画布 ========== */
.canvas-wrapper {
  flex: 1;
  height: 100vh;
  background: #ffffff;
  position: relative;
  min-width: 0;
}

.canvas-wrapper /deep/ .x6-port-body {
  transition: all 0.2s;
}

.canvas-wrapper /deep/ .x6-port:hover .x6-port-body {
  stroke-width: 3;
  fill: #5F95FF;
  r: 9;
}

/* ========== 右侧属性面板 ========== */
.right-panel {
  width: 280px;
  background: #ffffff;
  padding: 20px 16px;
  border-left: 1px solid #e8ecf1;
  box-shadow: -2px 0 8px rgba(0,0,0,0.03);
  overflow-y: auto;
  flex-shrink: 0;
}

.right-panel h3 {
  margin: 0 0 20px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1d2129;
}

.property-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 13px;
  font-weight: 500;
  color: #606266;
}

.root-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.empty-tip {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
  font-size: 13px;
  line-height: 1.8;
}

.empty-tip i {
  font-size: 40px;
  color: #dcdfe6;
  margin-bottom: 12px;
  display: block;
}

.empty-tip p {
  margin: 0;
}
</style>
