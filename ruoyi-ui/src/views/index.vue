<template>
  <div class="app-container home">
    <el-row :gutter="20">
      <el-col :sm="24" :lg="12" style="padding-left: 20px">
        <h2 v-text="systemName"></h2>
        <p v-html="showInfo"></p>
        <p>

        </p>
      </el-col>

      <el-col :sm="24" :lg="12" style="padding-left: 50px">
        <el-row>
          <el-col :span="12">
            <h2>技术选型</h2>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="6">
            <h4>后端技术</h4>
            <ul>
              <li>SpringBoot</li>
              <li>Spring Security</li>
              <li>JWT</li>
              <li>MyBatis</li>
              <li>Druid</li>
              <li>Fastjson</li>
              <li>...</li>
            </ul>
          </el-col>
          <el-col :span="6">
            <h4>前端技术</h4>
            <ul>
              <li>Vue</li>
              <li>Vuex</li>
              <li>Element-ui</li>
              <li>Axios</li>
              <li>Sass</li>
              <li>Quill</li>
              <li>...</li>
            </ul>
          </el-col>
        </el-row>
      </el-col>
    </el-row>
    <el-divider />
    <el-row :gutter="20">
      <el-col :xs="24" :sm="24" :md="12" :lg="8">
        <el-card class="update-log">
          <div slot="header" class="clearfix">
            <span>最近的任务</span>
          </div>
          <div class="body">
            <p v-for="item in recentTask" :key="item.id"><a href="/requirement/task">{{ item.name }}</a></p>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="12" :lg="8">
        <el-card class="update-log">
          <div slot="header" class="clearfix">
            <span>更新日志</span>
          </div>
          <el-collapse accordion>
            <el-collapse-item title="v0.0.2 - 2025-08-20">
              <ol>
                <li>发布v0.0.2版本，首页信息展示</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v0.0.1 - 2025-08-11">
              <ol>
                <li>发布v0.0.1版本</li>
              </ol>
            </el-collapse-item>
          </el-collapse>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="12" :lg="8">
        <el-card class="update-log">
          <div slot="header" class="clearfix">
            <span>我创建的任务</span>
          </div>
          <div class="body">
            <p v-for="item in myTask" :key="item.id"><a href="/requirement/task">{{ item.name }}</a></p>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { queryMyTask, queryRecentTask } from '../api/home/home.js'
export default {
  name: "Index",
  data () {
    return {
      // 版本号
      version: "3.9.0",
      myTask: [],
      recentTask: [],
      showInfo: '',
      systemName: ''
    }
  },
  created () {
    this.getMyTask()
    this.getRecentTask()
    this.getConfigKey("homePage.desc.showInfo").then(response => {
      this.showInfo = response.msg
    })
    this.getConfigKey("homePage.system.name").then(response => {
      this.systemName = response.msg
    })
  },
  methods: {
    goTarget (href) {
      window.open(href, "_blank")
    },
    getMyTask () {
      queryMyTask().then(res => {
        this.myTask = res.data
      })
    },
    getRecentTask () {
      queryRecentTask().then(res => {
        this.recentTask = res.data
      })
    }
  }
}
</script>

<style scoped lang="scss">
.home {
  blockquote {
    padding: 10px 20px;
    margin: 0 0 20px;
    font-size: 17.5px;
    border-left: 5px solid #eee;
  }
  hr {
    margin-top: 20px;
    margin-bottom: 20px;
    border: 0;
    border-top: 1px solid #eee;
  }
  .col-item {
    margin-bottom: 20px;
  }

  ul {
    padding: 0;
    margin: 0;
  }

  font-family: "open sans", "Helvetica Neue", Helvetica, Arial, sans-serif;
  font-size: 13px;
  color: #676a6c;
  overflow-x: hidden;

  ul {
    list-style-type: none;
  }

  h4 {
    margin-top: 0px;
  }

  h2 {
    margin-top: 10px;
    font-size: 26px;
    font-weight: 100;
  }

  p {
    margin-top: 10px;

    b {
      font-weight: 700;
    }
  }

  .update-log {
    ol {
      display: block;
      list-style-type: decimal;
      margin-block-start: 1em;
      margin-block-end: 1em;
      margin-inline-start: 0;
      margin-inline-end: 0;
      padding-inline-start: 40px;
    }
  }
}
</style>

