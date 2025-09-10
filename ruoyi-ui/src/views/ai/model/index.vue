<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="模型名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入模型名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="base url" prop="baseUrl">
        <el-input v-model="queryParams.baseUrl" placeholder="请输入base url" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="请选择类型" clearable>
          <el-option v-for="dict in dict.type.ai_model_type" :key="dict.value" :label="dict.label"
            :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd"
          v-hasPermi="['ai:model:add']">新增
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate"
          v-hasPermi="['ai:model:edit']">修改
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete"
          v-hasPermi="['ai:model:remove']">删除
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport"
          v-hasPermi="['ai:model:export']">导出
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="modelList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="模型ID" align="center" prop="id" />
      <el-table-column label="模型名称" align="center" prop="name" />
      <el-table-column label="base url" align="center" prop="baseUrl" />
      <el-table-column label="api key" align="center" prop="apiKey" />
      <el-table-column label="temperature" align="center" prop="temperature" />
      <el-table-column label="token最大生成数" align="center" prop="maxOutputToken" />
      <el-table-column label="类型" align="center" prop="type">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.ai_model_type" :value="scope.row.type" />
        </template>
      </el-table-column>
      <el-table-column label="提供商" align="center" prop="provider">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.ai_model_provider" :value="scope.row.provider" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
            v-hasPermi="['ai:model:edit']" :disabled="scope.row.provider === 'local'">修改
          </el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
            v-hasPermi="['ai:model:remove']" :disabled="scope.row.provider === 'local'">删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize"
      @pagination="getList" />

    <!-- 添加或修改模型管理对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="800px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择类型">
            <el-option v-for="dict in dict.type.ai_model_type" :key="dict.value" :label="dict.label"
              :value="parseInt(dict.value)"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="提供商" prop="provider">
          <el-select v-model="form.provider" placeholder="请选择提供商">
            <el-option v-for="dict in dict.type.ai_model_provider" :key="dict.value" :label="dict.label"
              :value="dict.value"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="模型名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入模型名称" />
        </el-form-item>
        <el-form-item label="base url" prop="baseUrl">
          <el-input v-model="form.baseUrl" placeholder="请输入base url" />
        </el-form-item>
        <el-form-item label="api key" prop="apiKey">
          <el-input v-model="form.apiKey" placeholder="请输入api key" />
        </el-form-item>
        <el-form-item label="temperature" prop="temperature">
          <el-input-number v-model="form.temperature" :step="0.1" :max="1.0" :min="0.1" placeholder="请输入temperature" />
        </el-form-item>
        <el-form-item label="token最大生成数" prop="maxOutputToken">
          <el-input-number v-model="form.maxOutputToken" :step="100" :min="200" placeholder="请输入token最大生成数" />
        </el-form-item>

      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listModel, getModel, delModel, addModel, updateModel, checkEmbeddingModel, downloadEmbeddingModel } from "@/api/ai/model"

export default {
  name: "Model",
  dicts: ['ai_model_type', 'ai_model_provider'],
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
      // 模型管理表格数据
      modelList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        name: null,
        baseUrl: null,
        type: null
      },
      // 表单参数
      form: {
        temperature: 0.7,
        maxOutputToken: 2048,
      },
      // 表单校验
      rules: {
        name: [
          { required: true, message: "模型名称不能为空", trigger: "blur" }
        ],
        baseUrl: [
          { required: true, message: "base url不能为空", trigger: "blur" }
        ],
        type: [
          { required: true, message: "类型不能为空", trigger: "change" }
        ],
        provider: [
          { required: true, message: "提供商不能为空", trigger: "change" }
        ]
      }
    }
  },
  created() {
    this.checkEmbeddingModel()
    this.getList()
  },
  methods: {
    checkEmbeddingModel() {
      const alertFlag = sessionStorage.getItem("alert_embedding")
      if (!alertFlag) {
        checkEmbeddingModel().then(response => {
          if (response.msg === "fail") {
            this.$modal.confirm('系统检测到您还没有配置任何embedding模型，是否使用系统默认embedding模型？此操作会从modelscope上下载模型').then(() => {
              return downloadEmbeddingModel()
            }).then(() => {
              this.$modal.msgSuccess("开始下载本地embedding模型，请留意系统推送")
            }).catch(() => {
            })
            sessionStorage.setItem("alert_embedding", "1")
          }
        })
      }
    },
    /** 查询模型管理列表 */
    getList() {
      this.loading = true
      listModel(this.queryParams).then(response => {
        this.modelList = response.rows
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
        baseUrl: null,
        apiKey: null,
        temperature: 0.7,
        maxOutputToken: 2048,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null,
        type: null,
        provider: null,
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
      this.title = "添加模型管理"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getModel(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改模型管理"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.$modal.loading("正在处理中，请稍后...")
          if (this.form.id != null) {
            updateModel(this.form).then(response => {
              this.$modal.closeLoading()
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            }).catch(err => {
              this.$modal.closeLoading()
              this.$modal.msgError(err)
            })
          } else {
            addModel(this.form).then(response => {
              this.$modal.closeLoading()
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            }).catch(err => {
              this.$modal.closeLoading()
              this.$modal.msgError(err)
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('是否确认删除模型管理编号为"' + ids + '"的数据项？').then(function () {
        return delModel(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {
      })
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('ai/model/export', {
        ...this.queryParams
      }, `model_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
