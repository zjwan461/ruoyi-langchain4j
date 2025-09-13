<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="知识库名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入知识库名称" clearable
                  @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item label="创建人" prop="createBy">
        <el-input v-model="queryParams.createBy" placeholder="请输入创建人" clearable
                  @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd"
                   v-hasPermi="['ai:knowledgeBase:add']">新增
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate"
                   v-hasPermi="['ai:knowledgeBase:edit']">修改
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete"
                   v-hasPermi="['ai:knowledgeBase:remove']">删除
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport"
                   v-hasPermi="['ai:knowledgeBase:export']">导出
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="knowledgeBaseList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="ID" align="center" prop="id"/>
      <el-table-column label="知识库名称" align="center" prop="name">
        <template slot-scope="scope">
          <router-link :to="'/ai/knowledgeBase-data/index/' + scope.row.id" class="link-type">
            <span>{{ scope.row.name }}</span>
          </router-link>
        </template>
      </el-table-column>
      <el-table-column label="创建人" align="center" prop="createBy"/>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{m}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="更新人" align="center" prop="updateBy"/>
      <el-table-column label="更新时间" align="center" prop="updateTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark"/>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-info" @click="handleTest(scope.row)"
                     v-hasPermi="['ai:knowledgeBase:list']">测试
          </el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
                     v-hasPermi="['ai:knowledgeBase:edit']">修改
          </el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
                     v-hasPermi="['ai:knowledgeBase:remove']">删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize"
                @pagination="getList"/>

    <!-- 添加或修改知识库对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="知识库名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入知识库名称"/>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input type="textarea" v-model="form.remark" placeholder="请输入备注"/>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="testTitle" :visible.sync="openTest" width="800px" append-to-body>
      <el-form ref="testForm" :model="testForm" :rules="testRules" label-width="100px">
        <el-form-item label="问题" prop="content">
          <el-input type="textarea" :rows="5" v-model="testForm.content" placeholder="请输入你的问题" show-word-limit
                    max="100"/>
        </el-form-item>
        <el-form-item label="最小得分" prop="minScore">
          <el-input-number v-model="testForm.minScore" :min="0.1" :max="0.99" :step="0.01"/>
        </el-form-item>
        <el-form-item label="展示数量" prop="maxResult">
          <el-input-number v-model="testForm.maxResult" :min="1" :max="10" :step="1"/>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitTestForm">确 定</el-button>
        <el-button @click="cancelTest">取 消</el-button>
      </div>
      <div style="max-height: 500px; overflow: auto" v-show="matchList.length > 0">
        <el-table v-loading="loadingMatch" :data="matchList">
          <el-table-column label="分段内容" align="center" prop="segment"/>
          <el-table-column label="得分" align="center" prop="score"/>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  listKnowledgeBase,
  getKnowledgeBase,
  delKnowledgeBase,
  addKnowledgeBase,
  updateKnowledgeBase,
  match
} from "@/api/ai/knowledgeBase"

export default {
  name: "KnowledgeBase",
  data() {
    return {
      openTest: false,
      // 遮罩层
      loading: true,
      loadingMatch: false,
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
      // 知识库表格数据
      knowledgeBaseList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        name: null,
        createBy: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        name: [
          {required: true, message: "知识库名称不能为空", trigger: "blur"}
        ],
      },
      testTitle: "",
      testForm: {},
      testRules: {
        content: [
          {required: true, message: "问题不能为空", trigger: "blur"}
        ],
        minScore: [
          {required: true, message: "最小得分不能为空", trigger: "blur"}
        ],
        maxResult: [
          {required: true, message: "展示结果不能为空", trigger: "blur"}
        ]
      },
      matchList: []
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询知识库列表 */
    getList() {
      this.loading = true
      listKnowledgeBase(this.queryParams).then(response => {
        this.knowledgeBaseList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    // 表单重置
    reset() {
      this.form = {
        id: null,
        name: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null,
        remark: null
      }
      this.resetForm("form")
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加知识库"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getKnowledgeBase(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改知识库"
      })
    },
    handleTest(row) {
      this.resetTest()
      this.testTitle = "命中测试-" + row.name
      this.testForm.kbId = row.id
      this.openTest = true
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateKnowledgeBase(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addKnowledgeBase(this.form).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('是否确认删除知识库编号为"' + ids + '"的数据项？').then(function () {
        return delKnowledgeBase(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {
      })
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('ai/knowledgeBase/export', {
        ...this.queryParams
      }, `knowledgeBase_${new Date().getTime()}.xlsx`)
    },
    submitTestForm() {
      this.$refs["testForm"].validate(valid => {
        if (valid) {
          this.loadingMatch = true
          match(this.testForm).then(response=>{
            this.loadingMatch = false
            this.matchList = response.data
            if (this.matchList.length === 0) {
              this.$modal.alertError("未找到匹配项")
            }
          })
        }
      })
    },
    cancelTest() {
      this.openTest = false
      this.resetTest()
    },
    resetTest() {
      this.testForm = {
        content: null,
        minScore: 0.70,
        maxResult: 3
      }
      this.matchList = []
      this.resetForm("testForm")
    },
  }
}
</script>
