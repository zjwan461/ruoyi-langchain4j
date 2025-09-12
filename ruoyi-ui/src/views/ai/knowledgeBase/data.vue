<template>
  <div class="app-container">

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd"
          v-hasPermi="['ai:knowledgeBase:list']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate"
          v-hasPermi="['ai:knowledgeBase:list']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete"
          v-hasPermi="['ai:knowledgeBase:list']">删除</el-button>
      </el-col>
    </el-row>
    <el-table v-loading="loading" :data="segmentList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="embeddingId" />
      <el-table-column label="文本分块" align="center" prop="text" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
            v-hasPermi="['ai:knowledgeBase:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
            v-hasPermi="['ai:knowledgeBase:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="pageNum" :limit.sync="pageSize"
      @pagination="listSegment" />

    <el-dialog :title="title" :visible.sync="showDialog" width="1000px" append-to-body>
      <el-row>
        <el-col :span="12">
          <el-form ref="elForm" :model="formData" :rules="rules" size="medium" label-width="100px">
            <el-form-item label="向量模型" prop="embeddingModelId">
              <el-select v-model="formData.embeddingModelId">
                <el-option v-for="item in embeddingModelList" :key="item.id" :value="item.id"
                  :label="item.name"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="最大分段数" prop="maxSegmentSize">
              <el-input-number v-model="formData.maxSegmentSize"></el-input-number>
            </el-form-item>
            <el-form-item label="最大重叠数" prop="maxOverlapSize">
              <el-input-number v-model="formData.maxOverlapSize"></el-input-number>
            </el-form-item>
            <el-form-item label="上传文档" prop="fileList">
              <el-upload ref="file" name="file" :file-list="formData.fileList" :action="fileAction" drag
                :before-upload="fileBeforeUpload" :headers="tokenHeader" :limit="1" :on-error="errorHandle"
                :on-success="successHandle" :on-change="changeHandle" :on-remove="removeHandle">
                <i class="el-icon-upload"></i>
                <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
                <div slot="tip" class="el-upload__tip">只能上传不超过 50MB 的文件</div>
              </el-upload>
            </el-form-item>

            <el-form-item size="large">
              <el-button type="primary" @click="submitForm">提交</el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-col>
        <el-col :span="12" v-if="documentList.length > 0">
          <div style="height: 600px; overflow: scroll;">
            <el-card v-for="item, index in documentList" :key="index" class="documentCard">
              {{ item }}
            </el-card>
          </div>
          <el-button type="primary" @click="agreeSegment">确认</el-button>
        </el-col>
      </el-row>
    </el-dialog>

    <el-dialog title="更新分段" :visible.sync="showUpdate" width="1000px" append-to-body>
      <el-form ref="segmentForm" :model="segmentForm" :rules="segmentRules" size="medium" label-width="100px">
        <el-form-item label="向量模型" prop="embeddingModelId">
          <el-select v-model="segmentForm.embeddingModelId">
            <el-option v-for="item in embeddingModelList" :key="item.id" :value="item.id"
              :label="item.name"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="向量ID" prop="embeddingId">
          <el-input v-model="segmentForm.embeddingId" disabled></el-input>
        </el-form-item>
        <el-form-item label="分段内容" prop="text">
          <el-input type="textarea" rows="20" v-model="segmentForm.text"></el-input>
        </el-form-item>

        <el-form-item size="large">
          <el-button type="primary" @click="submitUpdateForm">提交</el-button>
        </el-form-item>
      </el-form>

    </el-dialog>
  </div>
</template>
<script>
import { getToken } from '@/utils/auth'
import { documentSplit, embedding, segmentQuery, getSegment, delSegment, updateSegment } from '@/api/ai/knowledgeBaseData'
import { listModel } from '@/api/ai/model'
export default {
  components: {},
  props: [],
  data() {
    return {
      showUpdate: false,
      total: 0,
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      showDialog: false,
      title: "",
      segmentList: [],
      pageNum: 1,
      pageSize: 10,
      tokenHeader: null,
      embeddingModelList: [],
      documentList: [],
      formData: {
        embeddingModelId: null,
        fileList: [],
        maxSegmentSize: 300,
        maxOverlapSize: 10,
      },
      rules: {
        embeddingModelId: [{
          required: true,
          message: '请选择向量模型',
          trigger: 'change'
        }],
        maxSegmentSize: [{
          required: true,
          message: '请输入最大分段字数',
          trigger: 'blur'
        }],
        maxOverlapSize: [{
          required: true,
          message: '请输入最大重叠字数',
          trigger: 'blur'
        }],
        fileList: [{
          required: true,
          message: '请先上传文档',
          trigger: 'blur'
        }],
      },
      fileAction: process.env.VUE_APP_BASE_API + '/common/upload',
      segmentForm: {
        embeddingModelId: null,
        embeddingId: null,
        text: ''
      },
      segmentRules: {
        embeddingModelId: [{
          required: true,
          message: '请选择向量模型',
          trigger: 'blur'
        }],
        embeddingId: [{
          required: true,
          message: 'id必填',
          trigger: 'blur'
        }],
        text: [{
          required: true,
          message: '分段内容必填',
          trigger: 'blur'
        }]
      }
    }
  },
  computed: {},
  watch: {},
  created() {
    this.formData.kbId = this.$route.params && this.$route.params.kbId
    this.createHeader()
    this.getEmbedingModel()
    this.listSegment()
  },
  mounted() { },
  methods: {
    submitUpdateForm() {
      this.$refs['segmentForm'].validate(valid => {
        if (!valid) return

        const reqParams = { ...this.segmentForm }
        reqParams.kbId = this.formData.kbId
        this.$modal.loading("修改中...")
        updateSegment(reqParams).then(response => {
          this.$modal.closeLoading()
          this.showUpdate = false
          this.listSegment()
        }).catch(e => {
          this.$modal.closeLoading()
        })
      })
    },

    resetUpdate() {
      this.segmentForm = {
        embeddingId: null,
        embeddingModelId: null,
        text: ''
      }
    },
    reset() {
      this.formData.embeddingModelId = null
      this.formData.setfileList = []
      this.formData.maxSegmentSize = 300
      this.formData.maxOverlapSize = 10
    },
    handleAdd() {
      this.reset()
      this.getConfigKey("ai.model.embedding").then(response => {
        this.formData.embeddingModelId = parseInt(response.msg)
      })
      this.showDialog = true
      this.title = "新增文档分段"
    },
    handleUpdate(row) {
      this.resetUpdate()
      const id = row.embeddingId || this.ids
      getSegment(id).then(response => {
        this.showUpdate = true
        this.segmentForm = response.data
      })
    },
    handleDelete(row) {
      const ids = row.embeddingId || this.ids
      this.$modal.confirm('是否确认删除向量ID为"' + ids + '"的分段？').then(function () {
        return delSegment(ids)
      }).then(() => {
        this.listSegment()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => { })
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.embeddingId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    getEmbedingModel() {
      listModel({ pageNum: 1, pageSize: 1000, type: 1 }).then(response => {
        this.embeddingModelList = response.rows
      })
    },
    createHeader() {
      this.tokenHeader = {
        'Authorization': 'Bearer ' + getToken()
      }
    },
    submitForm() {
      this.$refs['elForm'].validate(valid => {
        if (!valid) return
        documentSplit(this.formData).then(response => {
          this.documentList = response.data
        })
      })
    },
    resetForm() {
      this.$refs['elForm'].resetFields()
    },
    fileBeforeUpload(file) {
      let isRightSize = file.size / 1024 / 1024 < 50
      if (!isRightSize) {
        this.$message.error('文件大小超过 50MB')
      }
      return isRightSize
    },
    errorHandle(err, file, fileList) {
      this.$message.error(err)
    },
    successHandle(response, file, fileList) {
      if (response.code !== 200) {
        this.$message.error(response.msg)
        this.$refs.file.clearFiles()
      }
    },
    changeHandle(file, fileList) {
      this.formData.fileList = fileList
    },
    removeHandle(file, fileList) {
      this.formData.fileList = fileList
    },
    agreeSegment() {
      this.$refs['elForm'].validate(valid => {
        if (!valid) return
        this.$modal.loading("embedding...")
        embedding(this.formData).then(response => {
          this.$modal.closeLoading()
          this.$modal.alertSuccess('处理中，共计' + response.data.segmentSize + '个文档分段，单次处理分段数为' + response.data.batchSize + "。请留意系统推送信息")
          this.showDialog = false
        }).catch(err => {
          this.$modal.closeLoading()
        })
      })
    },
    listSegment() {
      const reqParams = { ...this.formData }
      reqParams.pageNum = this.pageNum
      reqParams.pageSize = this.pageSize
      this.loading = true
      segmentQuery(reqParams).then(response => {
        this.loading = false
        this.segmentList = response.rows
        this.total = response.total
      })
    }

  }
}

</script>
<style>
.el-upload__tip {
  line-height: 1.2;
}

.documentCard {
  margin: 10px 10px;
}
</style>
