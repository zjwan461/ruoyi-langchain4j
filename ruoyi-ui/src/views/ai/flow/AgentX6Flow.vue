<template>
  <div class="agent-flow-container">
    <div class="left-panel">
      <h3>🧩 Agent 列表</h3>

      <!-- 操作按钮区：统一宽度，严格对齐 -->
      <div class="action-group">
        <!-- 操作按钮区：原生div实现，保证完全对齐 -->
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
        3. 点击选中节点/连线，按Delete删除
      </div>
    </div>

    <div class="canvas-wrapper" ref="graphContainer"></div>
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
      agentList: [
        { id: 'agent-1', name: '感知 Agent' },
        { id: 'agent-2', name: '决策 Agent' },
        { id: 'agent-3', name: '执行 Agent' },
        { id: 'agent-4', name: '记忆 Agent' },
        { id: 'agent-5', name: '通信 Agent' },
      ],
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

      // 选中逻辑
      this.graph.on('cell:click', ({ cell }) => {
        this.setSelected(cell)
      })
      this.graph.on('blank:click', () => {
        this.clearSelected()
      })

      // 连线成功回调
      this.graph.on('edge:connected', ({ edge }) => {
        const sourceNode = edge.getSourceCell()
        const targetNode = edge.getTargetCell()
        console.log('🔗 新增连线：', sourceNode.getData()?.name, '→', targetNode.getData()?.name)
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
        },
      })
      this.dnd.start(node, e, { offsetX: -80, offsetY: -40 })
    },

    setSelected(cell) {
      this.clearSelected()
      this.selectedCell = cell
      if (cell.isNode()) {
        cell.attr('body/stroke', '#4080FF')
        cell.attr('body/strokeWidth', 3)
      } else if (cell.isEdge()) {
        cell.attr('line/stroke', '#4080FF')
        cell.attr('line/strokeWidth', 3)
      }
    },

    clearSelected() {
      if (!this.selectedCell) return
      if (this.selectedCell.isNode()) {
        this.selectedCell.attr('body/stroke', '#5F95FF')
        this.selectedCell.attr('body/strokeWidth', 2)
      } else if (this.selectedCell.isEdge()) {
        this.selectedCell.attr('line/stroke', '#5F95FF')
        this.selectedCell.attr('line/strokeWidth', 2)
      }
      this.selectedCell = null
    },

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

    deleteSelected() {
      if (!this.selectedCell) {
        this.$message.info('请先点击选中要删除的节点或连线')
        return
      }
      this.graph.removeCells([this.selectedCell])
      this.selectedCell = null
      this.$message.success('删除成功')
    },

    exportFlow() {
      const nodes = this.graph.getNodes().map(node => {
        const data = node.getData() || {}
        const position = node.position()
        return {
          nodeId: node.id,
          agentId: data.agentId,
          agentName: data.name,
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

.left-panel {
  width: 220px;
  background: #ffffff;
  padding: 20px 16px;
  border-right: 1px solid #e8ecf1;
  box-shadow: 2px 0 8px rgba(0,0,0,0.03);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.left-panel h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1d2129;
}

/* ========== 按钮对齐核心修复 ========== */
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

/* 删除按钮 */
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

/* 导出按钮 */
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

/* 分割线 */
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
  box-shadow: 0 2px 6px rgba(95, 149, 255, 0.15);
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

.canvas-wrapper {
  flex: 1;
  height: 100vh;
  background: #ffffff;
  position: relative;
}

.canvas-wrapper /deep/ .x6-port-body {
  transition: all 0.2s;
}

.canvas-wrapper /deep/ .x6-port:hover .x6-port-body {
  stroke-width: 3;
  fill: #5F95FF;
  r: 9;
}
</style>
