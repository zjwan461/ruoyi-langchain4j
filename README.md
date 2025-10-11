## 平台简介

* 前端采用Vue、Element UI。
* 后端采用Spring Boot、Spring Security、Redis & Jwt、websocket、Langchain4j、pgvector
* 权限认证使用Jwt，支持多终端认证系统。
* 支持加载动态权限菜单，多方式轻松权限控制。
* 高效率开发，使用代码生成器可以一键生成前后端代码。
* 感谢[RuoYi-Vue](https://github.com/yangzongzhuan/RuoYi-Vue)。

#### 基础环境部署文档与ruoyi-vue一致:https://doc.ruoyi.vip/ruoyi-vue/
#### 基础环境部署文档与ruoyi-vue一致:https://doc.ruoyi.vip/ruoyi-vue/
#### 基础环境部署文档与ruoyi-vue一致:https://doc.ruoyi.vip/ruoyi-vue/

#### 本地RAG功能依赖pgvector，本地部署pgvector推荐使用docker,请参考项目中的[docker-compose-pgvector.yml](./docker-compose-pgvector.yml)

#### 本地RAG功能依赖pgvector，本地部署pgvector推荐使用docker,请参考项目中的[docker-compose-pgvector.yml](./docker-compose-pgvector.yml)

#### 本地RAG功能依赖pgvector，本地部署pgvector推荐使用docker,请参考项目中的[docker-compose-pgvector.yml](./docker-compose-pgvector.yml)

#### 重要的事情说三遍

>  参考资料👇
>
>  1. 若依框架: [http://www.ruoyi.vip](http://www.ruoyi.vip/)
>  2. Langchain4j框架：[教程 | LangChain4j 中文文档](https://docs.langchain4j.info/category/教程)
>  3. ollama：官网[Ollama](https://ollama.com/)；教程[Ollama本地模型部署+API接口调试超详细指南-阿里云开发者社区](https://developer.aliyun.com/article/1656872)

## 最新更新
<p>随手 star ⭐是一种美德。 你们的star就是我的动力</p>

v1.0

1.  新增AI模型管理，（包含大语言模型`LLM`和文档向量模型`Embedding`）；支持`ollama`和兼容`OpenAI`格式的模型提供商；支持本地运行向量模型（[shibing624_text2vec-base-chinese · 模型库](https://modelscope.cn/models/zjwan461/shibing624_text2vec-base-chinese/files)）；支持在线下载本地向量模型功能。
2.  新增本地知识库功能，支持文档分段和手动调整分段内容功能 。
3.  新增智能体管理。智能体支持多知识库 ；系统提示词；用户提示词模板功能；记忆功能；客户端限流功能；
4.  新增AI聊天功能。支持深度思考UI渲染；支持删除聊天历史功能。
5.  新增推送服务功能。支持异步下载本地向量模型结果推送；支持异步文档分段向量化处理结果推送。
6.  新增向量模型维度检测功能。
7.  新增知识库重向量功能



### 测试账号

> 管理账号：admin / admin123
>

## 内置功能

1.  用户管理：用户是系统操作者，该功能主要完成系统用户配置。
2.  部门管理：配置系统组织机构（公司、部门、小组），树结构展现支持数据权限。
3.  岗位管理：配置系统用户所属担任职务。
4.  菜单管理：配置系统菜单，操作权限，按钮权限标识等。
5.  角色管理：角色菜单权限分配、设置角色按机构进行数据范围权限划分。
6.  字典管理：对系统中经常使用的一些较为固定的数据进行维护。
7.  参数管理：对系统动态配置常用参数。
8.  通知公告：系统通知公告信息发布维护。
9.  操作日志：系统正常操作日志记录和查询；系统异常信息日志记录和查询。
10.  登录日志：系统登录日志记录查询包含登录异常。
11.  在线用户：当前系统中活跃用户状态监控。
12.  定时任务：在线（添加、修改、删除)任务调度包含执行结果日志。
13.  代码生成：前后端代码的生成（java、html、xml、sql）支持CRUD下载 。
14.  系统接口：根据业务代码自动生成相关的api接口文档。
15.  服务监控：监视当前系统CPU、内存、磁盘、堆栈等相关信息。
16.  在线构建器：拖动表单元素生成相应的HTML代码。
17.  连接池监视：监视当前系统数据库连接池状态，可进行分析SQL找出系统性能瓶颈。
18.  **AI工具箱：管理AI模型、知识库、智能体**

## 在线体验

- admin/admin123  
- 陆陆续续收到一些打赏，为了更好的体验已用于演示服务器升级。谢谢各位小伙伴。

演示地址：http://204.141.218.130/
文档地址：

## 演示图

<table>
    <tr>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913005255.png" width="1700"/></td>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913005626.png" width="1700"/></td>
    </tr>
    <tr>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913005758.png" width="1700"/></td>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913005843.png" width="1700"/></td>
    </tr>
    <tr>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913005928.png" width="1700"/></td>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913010015.png" width="1700"/></td>
    </tr>
	<tr>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913010059.png" width="1700"/></td>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913010129.png" width="1700"/></td>
    </tr>	 
    <tr>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913010242.png" width="1700"/></td>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913010305.png" width="1700"/></td>
    </tr>
	<tr>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913010408.png" width="1700"/></td>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913010454.png" width="1700"/></td>
    </tr>
	<tr>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913010627.png" width="1700"/></td>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913010656.png" width="1700"/></td>
    </tr>
</table>



## RuoYi-Langchain4j交流群

QQ群： 暂无