

<div align="center">

# RuoYi-Langchain4j

**Plataforma de desarrollo rápido de IA empresarial integrada con LangChain4j sobre RuoYi-Vue**

[![RuoYi-Vue](https://img.shields.io/badge/Base-RuoYi--Vue-blue)](https://github.com/yangzongzhuan/RuoYi-Vue)
[![LangChain4j](https://img.shields.io/badge/AI-LangChain4j-green)](https://docs.langchain4j.info/)
[![MCP](https://img.shields.io/badge/Protocol-MCP-9cf)](https://modelcontextprotocol.io/)
[![Skills](https://img.shields.io/badge/AI-Skills-orange)]()
[![Spring Boot](https://img.shields.io/badge/SpringBoot-2.x-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-2.x-success)](https://vuejs.org/)
[![License](https://img.shields.io/badge/license-MIT-orange)](./LICENSE)

> Frontend Vue / Element UI · Backend Spring Boot / Spring Security / Redis & Jwt / WebSocket / LangChain4j / PgVector / Dify Sandbox / **MCP** / **Skills**

[Inicio rápido](#-inicio-rápido) · [Prueba en línea](#-prueba-en-línea) · [Capturas de pantalla](#-capturas-de-pantalla) · [Apoyar el proyecto](#-apoyar-el-proyecto)

</div>

---

## 📖 Introducción al proyecto

RuoYi-Langchain4j es una versión extendida que / **PgVector**、 **[Dify](https://dify.ai/)** y el protocolo **[MCP (Protocolo de Contexto de Modelo)](https://modelcontextprotocol.io/) 的扩展版本。
Mientras conserva todas las capacidades originales de RuoYi (permisos, monitoreo, tareas programadas, generación de código, etc.), introduce capacidades de IA de una sola plataforma como **Gestión de modelos de IA / Base de conocimientos local / Agentes de múltiples tipos / Conversación de IA / Mercado de herramientas MCP / Sistema de habilidades Skills**., con el objetivo de ayudar a los desarrolladores a construir rápidamente aplicaciones empresariales de IA a un costo mínimo.

A partir de la versión 1.1, la plataforma incorpora tres tipos de agentes: **Agentes de tipo Dify**, **Agentes nativos que llaman a herramientas Java locales** y **Agentes nativos que llaman a Dify Sandbox para ejecutar herramientas Python**.
A partir de la versión 1.2, se introduce además el **Mercado de herramientas MCP** y el **Sistema de habilidades Skills**: los agentes pueden adjuntar herramientas expuestas por cualquier servidor MCP (`sse` / `stdio` / `streamable-http`), o bien vincularse a directorios de habilidades Skills, permitiendo que el LLM active automáticamente `activate_skill` antes de continuar respondiendo en los escenarios correspondientes.

### 🧩 Stack tecnológico

| / **Categoría** | **Tecnología** |
| --- | --- |
| **Frontend** | Vue, Element UI, Axios, Vue Router, Vuex, Editor de código Monaco |
| **Backend** | Spring Boot, Spring Security, MyBatis-Plus, Redis, Jwt, WebSocket |
| **Framework IA** | [LangChain4j](https://docs.langchain4j.info/) + [langchain4j-mcp](https://docs.langchain4j.info/) (compatible con protocolos Ollama / OpenAI) |
| **Ecosistema Dify** | Integración de aplicaciones [Dify](https://dify.ai/) + sandbox de ejecución de código [dify-sandbox](https://github.com/langgenius/dify-sandbox) |
| **MCP** | **Protocolo de herramientas [Model Context Protocol](https://modelcontextprotocol.io/) (`sse` / `stdio` / `streamable-http`)** |
| **Skills** | **Gestión de directorio de habilidades basada en `agent_skill`, soporta tipos `create` / `import`, se añade automáticamente al System Prompt** |
| **Orquestación de Agentes Múltiples** | **[ **AntV X6](https://x6.antv.antgroup.com/) lienzo visual + nodos de Flow, colaboración en serie de múltiples agentes** |
| **Base de datos vectorial** | [PgVector](https://github.com/pgvector/pgvector) (basado en PostgreSQL) |
| **Despliegue** | Docker, Docker Compose, Nginx |

### ⭐ Características principales

- 🤖 **Gestión de modelos de IA**: Gestión unificada de modelos de lenguaje grande (LLM) y modelos vectoriales de texto (Embedding), compatible con protocolos Ollama y OpenAI, permite descarga **Gestión de modelos de IA**: Gestión unificada de modelos de lenguaje grande (LLM) y modelos vectoriales de texto (Embedding), compatible con protocolos Ollama y OpenAI, permite descarga **Gestión de modelos de IA**: Gestión unificada de modelos de lenguaje grande (LLM) y modelos vectoriales de texto (Embedding), compatible con protocolos Ollama y OpenAI, permite la descarga/carga de modelos vectoriales locales.
- 📚 **Base de conocimientos local**: Permite segmentación automática de documentos, ajuste manual de segmentos, vectorización y revectorización.
- 🧠 **Agentes de múltiples tipos**: Tres formas integradas de agentes: Agente nativo LangChain4j, proxy de aplicación Dify y extensiones de llamada de herramientas para ambos.
- 💬 **Conversación IA**: Permite salida en streaming, renderizado de UI de pensamiento profundo (Chain-of-Thought) y gestión de conversaciones históricas.
- 🔔 **Push asíncrono**: Envía resultados de tareas de larga duración como descarga de vectores o procesamiento de segmentación de documentos mediante WebSocket.
- 🛠️ **Capacidades completas de RuoYi**: Usuario/rol/menú/dicciones/tareas programadas/generación de código/monitoreo de servicios listos para usar.
- 🔐 **Autenticación JWT multiplataforma**: Menú de permisos dinámicos + permisos de datos + control a nivel de botones.
- 🪄 **Agentes de tipo Dify**: Administa de manera unificada las aplicaciones Dify como Chatflow / Workflow / Agent mediante la plataforma [Dify](https://dify.ai/), las conversaciones y el mapeo de sesiones se gestionan localmente mediante `dify_session_mapping`, salida en streaming + recuperación de conversaciones históricas con un clic. La puerta de entrada está en [AI 工具箱 → Dify API 管理].
- ☕️ **Llamada de herramientas Java locales por agentes nativos**: Los agentes nativos (LangChain4j) soportan la adjunción de herramientas Java de tipo `builtin` (basadas en la anotación `dev.langchain4j.agent.tool.Tool`). En `JToolProvider`, se escanean los métodos `@Tool` por reflexión y se registran en LangChain4j, logrando la llamada de herramientas LLM ↔ métodos de negocio locales.
- 🐍 **Ejecución de herramientas Python en Dify Sandbox por agentes nativos**: Los agentes nativos soportan la adjunción de herramientas Python de tipo `sandbox`. `SandboxToolExecutor` ensambla automáticamente los parámetros de la LLM en una llamada a función Python según los metadatos de `parameters`, la envía a un sandbox [dify-sandbox](https://github.com/langgenius/dify-sandbox) para su ejecución y devuelve el resultado `print(result)`. **Desde v1.2 se incluye un editor de código Monaco**（`ruoyi-ui/src/components/Editor/codeEditor.vue`），在线编辑 Python 函数代码更友好。
- 🔌 **Mercado de herramientas MCP (nuevo en v1.2)**: Los agentes nativos soportan la adjunción de herramientas de servidores [Model Context Protocol](https://modelcontextprotocol.io/). En [AI 工具箱 → MCP] se mantiene `agent_mcp_service`（`url` + `transport` + `authToken`） y `agent_mcp_tool` 子级（`name` / `description` / `parameters` / `metadata`）， con tres modos de transporte: `sse` / `stdio` / `streamable-http`; `JToolProvider` se carga con langchain4j-mcp y se registra en el agente, la LLM puede llamar directamente a las herramientas MCP remotas. Lado del agente se vinculan herramientas MCP mediante `agent_mcp_tool_mapping` (relación muchos a muchos), ya cuenta con un interceptador de registro de peticiones OkHttp（`McpRequestLoggingInterceptor`）para facilitar la depuración.
- 📜 **Sistema de habilidades Skills (nuevo en v1.2)**: En [AI 工具箱 → Skill] se mantienen habilidades `agent_skill`（`skill_name` / `skill_description` / `skill_content` / `skill_type`）， soportando dos tipos: `create` (creación del sistema) e `import` (importación desde el directorio `skills/` del repositorio); Los agentes adjuntan habilidades mediante `agent_skill_mapping` (relación muchos a muchos), la plataforma añade la información de la habilidad según el template `Constants.SKILL_APPEND_SYSTEM_MESSAGE` al System Prompt, indicando al modelo que active la habilidad mediante la herramienta `activate_skill` antes de continuar respondiendo en los escenarios coincidentes.
- 🕸 **Orquestación de múltiples agentes / Agent Flow (nuevo en v1.3)**: En [AI 工具箱 → 多智能体] arrastra múltiples nodos de agentes mediante el lienzo visual **[AntV X6](https://x6.antv.antgroup.com/) 可视化画布拖拽多个智能体节点，拼接为多智能体协作链/树状 Flow（`agent_flow`）；后端 `FlowTreeParser` 把 X6 `flowJson` 解析为节点树，按节点切换执行智能体并通过 `AgentFlowChatServiceImpl` 产出流式响应，节点间可独立设置 `promptTemplate` / `flowMemory` / `showReasoning`。每个 Flow 生成独立 `visit_url`，支持单客户端每日访问限流（`day_lmt_per_client`）。
- 📊 **Visibilidad de llamadas de herramientas y recuperación**: Registra automáticamente `agent_tool_invoke`（结果 de llamada de herramienta） con `agent_kb_invoke`（segmentos recuperados de la base de conocimientos）， vinculado al ID del mensaje para facilitar la depuración y auditoría; el frontend puede observar el proceso de llamada en tiempo real mediante los eventos SSE `TOOL_CALL` / `TOOL_CALLED` / `RETRIEVED`.

---

## ⚠️ Notas importantes

> 1. **Despliegue del entorno base** es el mismo que RuoYi-Vue, consulte consulta la documentación oficial: <https://doc.ruoyi.vip/ruoyi-vue/>
> 2. **La función RAG depende de PgVector**，本地推荐使用 Docker 部署（项目已附带 `docker-compose-middleware.yml`）。 -> **La función RAG depende de PgVector**, se recomienda desplegarlo localmente con Docker (el proyecto ya incluye `docker-compose-middleware.yml`).
> 3. **La conversación de IA requiere configurar previamente el modelo de lenguaje grande y el modelo vectorial disponible en [AI 工具箱 → 模型管理]**.
> 4. **Los agentes de tipo Dify** requieren configurar previamente el prefijo URL y la clave API de los servicios Dify externos en [AI 工具箱 → Dify API 管理]. Se recomienda desplegar [Dify](https://github.com/langgenius/dify) mediante Docker Compose localmente.
> 5. **Las herramientas Python (tipo Sandbox)** dependen del servicio externo [dify-sandbox](https://github.com/langgenius/dify-sandbox). Después del despliegue, configure las direcciones y la clave API relacionadas con `ai.sandbox.*` en `application.yml`. Este repositorio `sandbox/conf/config.yaml` ya incluye una plantilla de configuración de dify-sandbox.
> 6. **Herramientas MCP** requieren mantener un servidor MCP externo (`url` + `transport`), `sse` / `streamable-http` debe garantizar que el backend pueda acceder a la dirección HTTP del servicio MCP, `stdio` debe asegurar que el proceso MCP pueda iniciarse en el entorno host.
> 7. **Habilidades Skills** soportan dos formas de entrada: crear directamente en [AI 工具箱 → Skill] o mantener `SKILL.md` en el directorio `skills/` del repositorio y luego seleccionar `import` para sincronizar en la base de datos; todas las habilidades adjuntas a los agentes se inyectarán automáticamente en el prompt del sistema mediante `SKILL_APPEND_SYSTEM_MESSAGE`.
> 8. **Migración de base de datos (ejecución automática de Flyway)**: Este proyecto integra **[Flyway](https://flywaydb.org/)**. Al iniciar `ruoyi-admin`, ejecutará automáticamente todos los scripts en `ruoyi-admin/src/main/resources/db/migration/` en orden de versión (`V1__init_database.sql` → `V2__create_tool_table.sql` → `V3__create_skill_table.sql` → `V4__create_mcp_table.sql` → `V5__create_agent_flow_table.sql` → `V6__create_attachment_table.sql` → `V7__add_trace_id_to_chat_tables.sql` → `V8__add_web_search_to_ai_agent_tables.sql`), cubriendo la inicialización de la base de datos `agent_tool` / `agent_tool_mapping` / `agent_tool_invoke` / `agent_kb_invoke`), Skill / MCP table & dictionary, Agent Flow (`agent_flow` / `chat_flow_message`), attachment table (`attachment`), chat log trace_id field & web search related fields, no need to manually execute SQL.
> 9. **Orquestación de múltiples agentes (Agent Flow)**: Desde v1.3 se añaden las tablas `agent_flow` y `chat_flow_message`, además la columna `description` en la tabla `ai_agent`, y el submenú de AI 工具箱子菜单统一重命名为「工具管理 / 多智能体 / Skill管理 / MCP管理」，全部纳入 `V5__create_agent_flow_table.sql` 由 Flyway 自动迁移。

---

## 🆕 Actualizaciones más recientes

> ⭐ **Dar una estrella es un gesto de virtud** ¡vuestras estrellas son mi motivación para seguir manteniendo el proyecto!

### v1.4 (Búsqueda web / Registro de chat / Gestión de archivos adjuntos / Correcciones de langchain4j)

1. 🆕 Integración de **búsqueda web Tavily**：En [AI 工具箱 → 智能体管理] se añaden los campos `web_search_enabled`, `tavily_result_count`, `tavily_source_urls`, soportando la integración de la búsqueda web de Tavily en el canal RAG a través de un reproductor de contenido, logrando la búsqueda web directa y enriquecimiento del prompt de chat.
2. 🆕 **Gestión de registros de chat (Trace ID)**: Se añade el campo `trace_id` para rastrear mensajes de chat entre solicitudes, y una nueva página de menú [Registro de chat] para consultar detalles de chat y contexto de rondas de Flow por trace_id; se añaden los módulos `ChatLogController`, `ChatLogQuery`, `ChatLogDetail`, `FlowRoundContext.traceId` en el backend.
3. 🐛 **Soluciona el conflicto entre la consulta de la base de conocimientos y la entrada multimodal**: Corrige el bug de no poder activar context.retrieval (consulta de base de conocimientos) y pasar múltiples contenidos (como múltiples imágenes) simultáneamente, refactoriza los componentes `JAiServices` y `JContentInjector` para soportar la inyección de contenido mixto.
4. 🆕 **Capacidades de archivos adjuntos de chat y análisis de documentos**: Se añaden la tabla `attachment` y los servicios `IAttachmentService`, `AttachmentServiceImpl`, soportando la carga de imágenes / vista previa de documentos y generación de descripciones en el chat; integración del servicio de análisis de documentos doc-parser (soporta PDF / XLS, etc.), se perfeccionan `DocumentParseClient` y los componentes frontend `DocPreview.vue` / `ImagePreview.vue`.
5. 🔧 **Optimiza la estrategia de programación de Flow de múltiples agentes**: Agent Flow cambia de un modo selector a una ejecución secuencial en lote ordenado, mejorando la lógica de flujo de nodos `FlowTool` y el mecanismo de seguridad de hilos `FlowRoundContext`.
6. 🔧 **Optimiza la respuesta en streaming SSE**: Extrae el contexto del emisor SSE a la clase base para la distribución unificada de eventos, corrige el problema de truncamiento anticipado de SSE; añade un efecto de carga de archivos en la página de chat frontend.
7. 🆕 **Scripts de migración de base de datos**: `V6__create_attachment_table.sql` (tabla de adjuntos), `V7__add_trace_id_to_chat_tables.sql` (campo trace_id), `V8__add_web_search_to_ai_agent_tables.sql` (campo de búsqueda web).
8. 🐳 **Optimización de la imagen Docker**: Se añade el prefijo ghcr.io para optimizar la extracción del servicio de análisis de documentos, se actualiza la configuración de docker-compose.

### v1.3 (Orquestación de múltiples agentes / Agent Flow)

1. 🆕 **Orquestación de múltiples agentes (Agent Flow)**: En [AI 工具箱 → 多智能体] arrastra múltiples nodos de agentes mediante el lienzo visual [AntV X6](https://x6.antv.antgroup.com/) para / 多智能体协作链 / 树状 Flow（`agent_flow`）。后端 `FlowTreeParser` 将 X6 `flowJson` 解析为节点树，由 `AgentFlowChatServiceImpl` 按节点调度智能体并产出流式响应，支持节点独立设置 `promptTemplate` / `flowMemory` / `showReasoning`。
2. 🆕 **Diálogo con Flow y URL de acceso independiente**: Cada Flow genera automáticamente un enlace de acceso `visit_url`, el frontend puede / `ruoyi-ui/src/views/ai/agent/aiFlowChat.vue` dialogar con todo el Flow; soporta `day_lmt_per_client` (límite de acceso diario por cliente).
3. 🆕 **Registro de chat de Flow** tabla `chat_flow_message`: Registra los mensajes generados por cada nodo de agente en cada Flow, incluyendo `agent_flow_id` / `agent_id` / `agent_name` / `session_id` / `client_id`, facilitando la depuración y auditoría.
4. 🆕 **Lienzo visual AntV X6**: Se integran `@antv/x6` + `@antv/x6-plugin-dnd` / `snapline` / `history`, arrastrar, guardar, cargar `flowJson` en línea; ruta de menú `ai/agent_flow_edit/index/:id`.
5. 🆕 **Script de migración de base de datos** `sql/V5__create_agent_flow_table.sql`: Incluye las nuevas tablas `agent_flow`, `chat_flow_message`, la columna `ai_agent.description`补充, y el submenú de AI 工具箱子菜单统一重命名 (Agent Tools → 工具管理、Agent Flow → 多智能体、Skill → Skill管理、MCP → MCP管理).
6. 🆕 **Módulos backend**: `ruoyi-ai-core` añade `component/flow` (`FlowTreeParser`, `AgentSpecAdaptor` y DTO), `service/IAgentFlowService`, `controller/AgentFlowController`, `domain/AgentFlow` / `ChatFlowMessage`; `ruoyi-ai-chat` añade `AgentFlowChatServiceImpl` y `controller/AIFlowChatController`.
7. 🆕 **`FlowTool`**: `ruoyi-ai-core/.../tool/FlowTool.java` proporciona la entrada de herramientas de flujo de nodos para la programación de Flow.
8. 🔧 **Unificación de rutas de menú**: Las subrutas de menú de AI 工具箱 usanAI 工具箱下子菜单统一使用中文名（多智能体 / 工具管理 / Skill管理 / MCP管理），并同步 Gitee 链接路径。

### v1.2 (Mercado de herramientas MCP y sistema de habilidades Skills)

1. 🆕 **Mercado de herramientas MCP**: En [AI 工具箱 → MCP] se mantiene uniformemente `agent_mcp_service` (dirección del servicio / `transport` / `authToken`) y el hijo `agent_mcp_tool` (nombre / descripción / parámetros / metadatos). Soporta tres modos de transporte `sse` / `stdio` / `streamable-http` (diccionario `ai_mcp_transport`), a nivel inferior carga las herramientas MCP remotas a `JToolProvider` mediante [langchain4j-mcp](https://docs.langchain4j.info/), los agentes nativos pueden llamar directamente. Lado del agente a través de `agent_mcp_tool_mapping` muchos a muchos vincula las herramientas MCP.
2. 🆕 **Sistema de habilidades Skills**: En [AI 工具箱 → Skill] se mantienen habilidades `agent_skill`, soportando dos diccionarios `ai_skill_type` `create` (creación del sistema) e `import` (importación desde el directorio `skills/` del repositorio). Los agentes adjuntan habilidades a través de `agent_skill_mapping` muchos a muchos, la plataforma conectará la lista de habilidades en el template `Constants.SKILL_APPEND_SYSTEM_MESSAGE` al System Prompt, indicando al modelo que active la habilidad mediante la herramienta `activate_skill` antes de continuar respondiendo.
3. 🆕 **Herramienta de sincronización de GitHub Skills**: `GitHubSkillsSyncUtil` soporta la importación al directorio `skills/` (como `skills/json/SKILL.md`) a la base de datos con un clic, manteniendo la definición de habilidades y el código en la misma fuente.
4. 🆕 **Interceptador de registro de peticiones MCP**: `McpRequestLoggingInterceptor` basado en interceptador OkHttp registro de peticiones / respuestas MCP, facilitando la depuración.
5. 🆕 **Scripts de migración de base de datos** `sql/V3__create_skill_table.sql`, `sql/V4__create_mcp_table.sql`, incluyen toda la estructura de tablas, menús y datos de diccionario, por favor continúe ejecutando en orden sobre la base de datos v1.1.
6. 🆕 **Editor de código Monaco**: La página Agent Tools (tipo sandbox) integra el editor de código Monaco (`ruoyi-ui/src/components/Editor/codeEditor.vue`), editando en línea el código de funciones Python más amigable.
7. 🔧 **Corrige el conflicto de versión de paquetes jar** tras la引入 `langchain4j-mcp`.

### v1.1 (Herramientización de agentes & Integración de Dify)

1. 🆕 **Agentes de tipo Dify**: En [AI 工具箱 → Dify API 管理] se mantienen uniformemente los servicios Dify externos, a través de `agentType=dify` se puede gestionar de una sola vez Chatflow / Workflow / Agent aplicaciones, las conversaciones y sesiones históricas se mapean localmente mediante `dify_session_mapping`, salida en streaming en toda la cadena.
2. 🆕 **Agentes nativos llaman a herramientas Java locales**: Los agentes soportan la adjunción de herramientas de tipo `builtin`, el backend escanea por reflexión los métodos de anotación `dev.langchain4j.agent.tool.Tool` y los registra en LangChain4j, los modelos grandes pueden llamar automáticamente los métodos de negocio de Java locales (ya incluye el ejemplo de la herramienta de tiempo `time_tool`).
3. 🆕 **Agentes nativos llaman a Dify Sandbox para ejecutar herramientas Python**: Los agentes soportan la adjunción de herramientas de tipo `sandbox`, ensambla automáticamente los parámetros de LLM en `func_name(args)` según los metadatos de `parameters` y los envía al sandbox [dify-sandbox](https://github.com/langgenius/dify-sandbox) para ejecutar el código Python, devolviendo el texto `print(result)` al LLM.
4. 🆕 **Gestión de herramientas**: Se añaden las tablas `agent_tool`, `agent_tool_mapping` y el menú correspondiente (AI 工具箱 → Agent Tools), soportando dos tipos de herramientas `builtin` / `sandbox`, se añade el diccionario `ai_tool_type`, y se preconfigura la herramienta interna `time_tool`.
5. 🆕 **Gestión de API de Dify**: Se añaden las tablas `dify_api`, `dify_session_mapping` y el menú correspondiente, se mantiene uniformemente la integración **Gestión de API de Dify**: Se añaden las tablas `dify_api`, `dify_session_mapping` y el menú correspondiente, se mantiene uniformemente la integración de servicios Dify externos.
6. 🆕 **Visibilidad de llamadas de herramientas y recuperación**: Se añaden las dos tablas de registro `agent_tool_invoke`, `agent_kb_invoke`, se envían tres eventos SSE `TOOL_CALL` / `TOOL_CALLED` / `RETRIEVED` en la respuesta de chat, el frontend puede mostrar en tiempo real el proceso de llamada de herramientas.
7. 🆕 **Scripts de migración de base de datos** `sql/V2__create_tool_table.sql`, incluyen toda la estructura de tablas y datos de diccionario.
8. 🆕 **Recursos de despliegue de sandbox**, el directorio del repositorio `sandbox/` proporciona la plantilla de configuración de inicio de dify-sandbox y la explicación de dependencias.

### v1.0

1. 🆕 **Gestión de modelos de IA** (LLM / Embedding), compatible con protocolos Ollama y OpenAI, compatible con el modelo vectorial local ([shibing624_text2vec-base-chinese](https://modelscope.cn/models/zjwan461/shibing624_text2vec-base-chinese/files)), compatible con la descarga en línea del modelo vectorial local.
2. 🆕 **Función de base de conocimientos local**, compatible con la segmentación de documentos y el ajuste manual de contenido de segmentación.
3. 🆕 **Gestión de agentes**, compatible con la adjunción de múltiples bases de conocimientos, prompts del sistema, plantillas de prompts de usuario, función de memoria, límite de clientes.
4. 🆕 **Función de chat de IA**, compatible con el renderizado de UI de pensamiento profundo, compatible con la eliminación del historial de chat.
5. 🆕 **Servicio de push**, compatible con el empuje asíncrono de resultados de modelos vectoriales locales, empuje de resultados de procesamiento de vectorización de segmentación de documentos.
6. 🆕 **Función de detección de dimensiones de modelos vectoriales**.
7. 🆕 **Función de revectorización de base de conocimientos**.

---

## 🪄 Detalle de nuevas capacidades de v1.2: Mercado de herramientas MCP y sistema de habilidades Skills

En v1.2, sobre las tres formas de agentes (`origin` / `dify`), se introduce **MCP (Protocolo de Contexto de Modelo) Mercado de herramientas MCP** y el **Sistema de habilidades Skills**, se pueden mantener de forma independiente en [AI 工具箱 → MCP] y [AI 工具箱 → Skill], y adjuntar a cualquier agente nativo según sea necesario en [Gestión de Agentes].

| **Capacidad** | **Acceso** | **Diccionario** | **Tablas relacionadas** | **Uso típico** |
| --- | --- | --- | --- | --- |
| **Mercado de herramientas MCP** | AI 工具箱 → MCP | `ai_mcp_transport` (`sse` / `stdio` / `streamable-http`) | `agent_mcp_service`, `agent_mcp_tool`, `agent_mcp_tool_mapping` | Llamada a herramientas expuestas por servidores MCP remotos (como GitHub / Base de datos / Navegador / Servicios desarrollados) |
| **Sistema de habilidades Skills** | AI 工具箱 → Skill | `ai_skill_type` (`create` / `import`) | `agent_skill`, `agent_skill_mapping` | Extraer prompts de dominio reutilizables / plantillas de flujo en habilidades, activadas automáticamente por la LLM |

### 1. Sistema de habilidades Skills

Ruta de implementación:
- Entidad y mapeo: `ruoyi-ai-core/.../domain/AgentSkill.java`, `AgentSkillMapping.java`
- Servicio: `ruoyi-ai-core/.../service/impl/AgentSkillServiceImpl.java`
- Herramienta de sincronización: `ruoyi-ai-core/.../util/GitHubSkillsSyncUtil.java`
- Frontend: `ruoyi-ui/src/views/ai/skill/index.vue`, `ruoyi-ui/src/api/ai/skill.js`
- Plantilla de prompt: `SKILL_APPEND_SYSTEM_MESSAGE` en `ruoyi-ai-core/.../util/Constants.java`

- En [AI 工具箱 → Skill] selecciona `create` (creación del sistema) o `import` (importación desde `SKILL.md` en el directorio `skills/` del repositorio) para mantener la definición de habilidad (`skill_name` / `skill_description` / `skill_content` / `skill_type`).
- En [Gestión de Agentes], al editar un agente, vincula varias habilidades a través de `agent_skill_mapping` (muchos a muchos); después de guardar, la plataforma conectará la lista de habilidades vinculadas al System Prompt según la siguiente plantilla.

```text
You have access to the following skills:

<skill_name_1>: <skill_description_1>
<skill_name_2>: <skill_description_2>
...

When the user's request relates to one of these skills, activate it first
using the `activate_skill` tool before proceeding.
```

- El modelo, en escenarios coincidentes, llamará primero a la herramienta `activate_skill` para activar la habilidad, luego continuará respondiendo, logrando así la consolidación de prompts de dominio / SOPs en activos reutilizables.

> **Convención de directorio**: Colocar `SKILL.md` (y directorios personalizados como `skills/json/SKILL.md`) en el directorio `skills/` del repositorio como la versión de código fuente de la habilidad, las habilidades de tipo `import` sincronizarán el contenido de estos archivos a la base de datos.

### 2. Mercado de herramientas MCP

Ruta de implementación:
- Entidad y mapeo: `ruoyi-ai-core/.../domain/AgentMcpService.java`, `AgentMcpTool.java`, `AgentMcpToolMapping.java`
- Servicio: `ruoyi-ai-core/.../service/impl/AgentMcpServiceImpl.java`
- Capa de control: `ruoyi-ai-core/.../controller/AgentMcpServiceController.java`
- Carga de herramientas: `ruoyi-ai-core/.../tool/JToolProvider.java`
- Componentes MCP: `ruoyi-ai-core/.../component/mcp/JHttpMcpTransport.java`, `McpRequestLoggingInterceptor.java`
- Frontend: `ruoyi-ui/src/views/ai/mcp/index.vue`, `ruoyi-ui/src/api/ai/mcp.js`

- En [AI 工具箱 → MCP] crea un servicio MCP: rellena `mcp_service_name`, `mcp_url`, `mcp_transport` (`sse` / `stdio` / `streamable-http`), `auth_token` opcional.
- A través de la capacidad "Herramienta de sincronización" extrae la lista de herramientas del servidor MCP remoto, se guarda en `agent_mcp_tool` (contiene `mcp_tool_name` / `mcp_tool_description` / `mcp_tool_parameters` / `mcp_tool_metadata`).
- En [Gestión de Agentes], al editar un agente, vincula herramientas MCP a través de `agent_mcp_tool_mapping` (muchos a muchos); después de guardar, `JToolProvider` seleccionará el cliente adecuado según el `transport`:
  - `sse` / `streamable-http`: Llama a la interfaz HTTP MCP a través de `JHttpMcpTransport` + OkHttp personalizado, y habilita `McpRequestLoggingInterceptor` para imprimir el registro de solicitud / respuesta.
  - `stdio`: Fork directamente el proceso MCP externo, se comunica con el servidor MCP a través de E/S estándar (requiere que el entorno host pueda iniciar el proceso).
- Una vez cargadas, las herramientas MCP se registrarán automáticamente como `ToolSpecification` de LangChain4j, los agentes nativos (`agentType=origin`) podrán llamarlas según sea necesario durante el proceso de inferencia de la LLM.

> **Nota de dependencias**: Desde v1.2 se introduce la dependencia `langchain4j-mcp` y se actualiza Spring Boot a `2.7.18`. Si encuentras conflictos de paquetes jar, confirma que las versiones de la serie `langchain4j-*` son uniformes.

### 3. Agentes nativos llaman a herramientas Java locales (continúa en v1.1)

Ruta de implementación: `ruoyi-ai-core/.../tool/JToolProvider.java`

- En [Agent Tools] crea una nueva herramienta, `toolType=builtin`, `clazz` rellena el nombre de clase completo del Spring Bean que contiene la anotación `@dev.langchain4j.agent.tool.Tool`.
- La plataforma carga la clase por reflexión en `JToolProvider.provideTools`, escanea todos los métodos de anotación `@Tool`, usa `ToolSpecifications.toolSpecificationFrom(method)` de LangChain4j para generar la especificación de la herramienta y la registra.
- Durante el proceso de inferencia, la LLM llamará automáticamente a las herramientas coincidentes, la plataforma registra cada llamada en la tabla `agent_tool_invoke` (asociada al `messageId`).

```java
// 示例：com.ruoyi.ai.tool.TimeTool（已内置）
public class TimeTool {
    @Tool("获取当前时间，格式 yyyy-MM-dd HH:mm:ss")
    public String currentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
```

### 4. Agentes nativos llaman a Dify Sandbox para ejecutar herramientas Python (continúa en v1.1)

Ruta de implementación: `ruoyi-ai-core/.../tool/SandboxToolExecutor.java`

- En [Agent Tools] crea una nueva herramienta, `toolType=sandbox`, `code` rellena la definición de función Python, `parameters` rellena la definición de parámetros JSON (como `[{"name":"x","type":"int"}]`), desde v1.2 la página integra el editor de código Monaco.
- `SandboxToolExecutor` ensamblará la llamada a función como `func_name(arg1=value1, arg2='value2')` según los parámetros reales que pase la LLM, y añadirá `print(result)` para enviarlo a dify-sandbox.
- Después de la ejecución, dify-sandbox devuelve la salida `print` (que es la representación en cadena de `result`), inyectando el resultado de la llamada de herramienta al contexto de la LLM.

```python
# 示例：calc_circle_area.py（code 字段）
import math
def calc_circle_area(radius):
    return math.pi * radius * radius
```

> **Sugerencia de despliegue**: Inicia el servicio sandbox según la documentación先按 [dify-sandbox](https://github.com/langgenius/dify-sandbox) 官方文档启动沙箱服务（仓库 `sandbox/conf/config.yaml` 已附模板），后端 `application.yml` 中配置 `ai.sandbox.url-prefix` 与 `ai.sandbox.api-key`.

### 5. Agentes de tipo Dify (continúa en v1.1)

Ruta de implementación: Módulo `ruoyi-ai-dify` + `ruoyi-ai-chat/.../controller/DifyChatController.java`

- En [AI 工具箱 → Dify API 管理] mantiene el `urlPrefix` y `apiKey` de los servicios Dify externos, un agente se vincula a una aplicación Dify.
- El backend mapea el `sessionId` local a `conversation_id` de Dify a través de `dify_session_mapping`, y [SSE 转发 Dify 流式响应](ruoyi-ai/ruoyi-ai-chat/src/main/java/com/ruoyi/ai/controller/DifyChatController.java:80)转发 Dify 流式响应。
- Soporta la consulta paginada localizada de la lista de sesiones y mensajes históricos, el frontend está listo para usar.

---

## 🚀 Inicio rápido

### I. Entorno base

| **Componente** | **Requisito de versión** | **Notas** |
| --- | --- | --- |
| JDK | 1.8+ | Entorno de ejecución backend |
| Maven | 3.6+ | Construcción del proyecto |
| Node.js | 14+ | Construcción frontend |
| MySQL | 5.7+ / 8.0 | Base de datos de negocio |
| Redis | 5.0+ | Caché |
| PostgreSQL + PgVector | 14+ | Base de datos vectorial (RAG obligatorio) |
| Dify (Opcional) | 0.6+ | Solo necesario si se usan agentes de tipo Dify |
| dify-sandbox (Opcional) | latest | Solo necesario si se usan herramientas Python de tipo `sandbox` |
| Servidor MCP (Opcional) | — | Solo necesario si se usan herramientas de tipo MCP, soporta `sse` / `stdio` / `streamable-http` |

> Para la instalación y configuración del entorno base, consulte la documentación oficial de RuoYi: <https://doc.ruoyi.vip/ruoyi-vue/>

### II. Despliegue con Docker (recomendado)

`docker-compose.yml` ya指定了 `ruoyi-admin` 与 `ruoyi-ui` 服务分别指定ó `Dockerfile-admin` 与 `Dockerfile-ui`，**只需一条命令即可同时拉起中间件（MySQL / Redis / PgVector 等）并自动构建前后端镜像**：

```bash
docker compose -f docker-compose.yml up -d --build
```

> - No es necesario ejecutar manualmente los scripts en `build.sh` o `bin/`; `--build` hará que Compose construya automáticamente las imágenes frontend y backend según el Dockerfile.
> - Después de que el contenedor backend se inicie, **Flyway ejecutará automáticamente** todos los scripts `V1` ~ `V8` en `ruoyi-admin/src/main/resources/db/migration/` para completar la creación de tablas e inicialización de diccionarios.
> - Los usuarios de Windows PowerShell / CMD pueden usar directamente `docker compose -f docker-compose.yml up -d --build`.

### III. Modo de desarrollo local

El proyecto ya integra **[Flyway](https://flywaydb.org/)**, al iniciar el backend `ruoyi-admin` 会自动扫描并按版本号顺序执行 `ruoyi-admin/src/main/resources/db/migration/` 下所有迁移脚本（`V1__init_database.sql` → `V2__create_tool_table.sql` → `V3__create_skill_table.sql` → `V4__create_mcp_table.sql` → `V5__create_agent_flow_table.sql` → `V6__create_attachment_table.sql` → `V7__add_trace_id_to_chat_tables.sql` → `V8__add_web_search_to_ai_agent_tables.sql`），**无需手动执行任何 SQL**.首次启动前只需在 MySQL 中手动创建一个空库（库名与 `application-druid.yml` / `application.yml` 中的 `url` 保持一致），Flyway 会自动建表与初始化业务数据。

```bash
# (Opcional) Crea manualmente una base de datos vacía en MySQL antes de la primera ejecución
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS `ry-vue` DEFAULT CHARACTER SET utf8mb4;"

# Inicia el backend (Flyway ejecutará automáticamente todos los scripts V1~V8 en db/migration)
mvn spring-boot:run -pl ruoyi-admin
cd ruoyi-ui
npm install
npm run dev
```

---

## 🧱 Funciones integradas

| # | **Módulo** | **Descripción** |
| --- | --- | --- |
| 1 | Gestión de usuarios | Gestión de operadores del sistema |
| 2 | Gestión de departamentos | Estructura organizativa en árbol, soporta permisos de datos |
| 3 | Gestión de cargos | Configuración de cargos del usuario |
| 4 | Gestión de menús | Menús, permisos de operación, permisos de botones |
| 5 | Gestión de roles | Roles de menú y permisos de rango de datos |
| 6 | Gestión de diccionarios | Mantenimiento de datos fijos |
| 7 | Gestión de parámetros | Configuración de parámetros dinámicos del sistema |
| 8 | Anuncios | Mantenimiento de publicación de anuncios |
| 9 | Registro de operaciones | Registro de operaciones normales / anormales |
| 10 | Registro de inicio de sesión | Registro de inicio de sesión y errores |
| 11 | Usuarios en línea | Monitoreo de usuarios activos |
| 12 | Tareas programadas | Programación de tareas en línea y registro de ejecución |
| 13 | Generación de código | Generación de código CRUD frontend/backend con un clic |
| 14 | Interfaz del sistema | Generación automática de documentación API (Swagger) |
| 15 | Monitoreo de servicios | Monitoreo de CPU / memoria / disco / pila |
| 16 | Constructor en línea | Generación de HTML arrastrando formularios |
| 17 | Monitoreo de connection pool | Estado de connection pool de Druid y análisis de SQL |
| 18 | 🤖 **AI 工具箱** | **Gestión unificada de modelos de IA / base de conocimientos / agentes / API de Dify / Agent Tools** |

AI 工具箱下包含：

- **Gestión de modelos**: Mantenimiento de modelos LLM / Embedding (protocolo Ollama / OpenAI).
- **Gestión de base de conocimientos**: Segmentación de documentos, vectorización, ajuste manual de segmentación.
- **Gestión de agentes**: Agentes de múltiples tipos (`origin` / `dify`), soporta prompt del sistema, memoria, adjuntos de base de conocimientos, **adjunto de herramientas MCP**, **adjunto de habilidades Skills**, adjunto de Agent Tools, límite de clientes.
- **Agentes múltiples** (nuevo en v1.3): Basado en el lienzo visual [AntV X6](https://x6.antv.antgroup.com/), conecta múltiples nodos de agentes a un Flow, genera `visit_url` independiente y proporciona una página de diálogo dedicada (`aiFlowChat.vue`), soporta memoria compartida en Flow / plantillas de prompt / pensamiento profundo / límite de acceso diario por cliente.
- **Gestión de herramientas** (antes Agent Tools): Gestiona dos tipos de herramientas `builtin` (Herramienta Java) y `sandbox` (Herramienta Python), la página de herramientas `sandbox` integra el editor de código Monaco, se puede adjuntar directamente a cualquier agente.
- **Gestión de Skills** (nuevo en v1.2): Mantiene habilidades `agent_skill`, soporta dos tipos `create` / `import`, soporta importación con un clic desde `SKILL.md` en el directorio `skills/` del repositorio, la lista de habilidades se conectará al System Prompt y se activará mediante la herramienta `activate_skill`.
- **Gestión de MCP** (nuevo en v1.2): Mantiene `agent_mcp_service` y el hijo `agent_mcp_tool`, soporta tres modos de transporte `sse` / `stdio` / `streamable-http`, sincroniza automáticamente la lista de herramientas MCP remotas, se puede adjuntar a cualquier agente nativo.
- **Gestión de API de Dify**: Mantiene uniformemente la integración de servicios Dify externos (prefijo URL + API Key).
- **Chat de IA**: Diálogo en streaming, renderizado de UI de pensamiento profundo, gestión de conversaciones históricas.
- **Diálogo con Flow**: Desde v1.3, genera una página de diálogo independiente para cada Agent Flow, registra `chat_flow_message`, se puede usar para entregar enlaces de aplicaciones multiagente independientes.

---

## 🌐 Prueba en línea

- Cuenta por defecto: `admin / admin123`
- Dirección de la demo: <http://204.141.218.130>
- Dirección de la documentación: consulte la sección [Notas importantes](#-notas-importantes)

> Las donaciones recibidas por el servidor de la demo se han utilizado para actualizaciones, ¡Gracias a todos los compañeros por su apoyo ❤️

---

## 🖼 Capturas de pantalla

<table>
    <tr>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913005255.png" alt="演示图-1" width="1700"/></td>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913005626.png" alt="演示图-2" width="1700"/></td>
    </tr>
    <tr>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913005758.png" alt="演示图-3" width="1700"/></td>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913005843.png" alt="演示图-4" width="1700"/></td>
    </tr>
    <tr>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913005928.png" alt="演示图-5" width="1700"/></td>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913010015.png" alt="演示图-6" width="1700"/></td>
    </tr>
    <tr>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913010059.png" alt="演示图-7" width="1700"/></td>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913010129.png" alt="演示图-8" width="1700"/></td>
    </tr>
    <tr>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913010242.png" alt="演示图-9" width="1700"/></td>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20250913010305.png" alt="演示图-10" width="1700"/></td>
    </tr>
    <tr>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20260704001023.png" alt="演示图-11" width="1700"/></td>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20260704001058.png" alt="演示图-12" width="1700"/></td>
    </tr>
    <tr>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20260704001134.png" alt="演示图-13" width="1700"/></td>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20260704000922.png" alt="演示图-14" width="1700"/></td>
    </tr>
    <tr>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20260712234728.png" alt="演示图-15" width="1700"/></td>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20260712234912.png" alt="演示图-16" width="1700"/></td>
    </tr>
    <tr>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20260725001320.png" alt="演示图-17" width="1700"/></td>
        <td><img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/20260725001539.png" alt="演示图-18" width="1700"/></td>
    </tr>
</table>

---

## ❤️ Apoya el proyecto

El código fuente de RuoYi-Langchain4j es de código abierto y gratuito, si crees que este proyecto te ha ayudado, ¡bienvenido a apoyarlo a través de las siguientes formas:

- 🌹 Recomiénd **RuoYi-Langchain4j** ` - El código fuente de RuoYi-Langchain4j es de código abierto y gratuito, si crees que este proyecto te ha ayudado, ¡bienvenido a apoyarlo a través de las siguientes formas:
- 🌹 Recomienda o comparte con tus amigos
- ⭐ Dale una estrella en la esquina superior derecha de la página del proyecto
- ☕️ Haz una donación a través del código QR de abajo, invita al autor a un café

<div align="center">
  <img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/%E6%94%AF%E4%BB%98%E5%AE%9D%E6%94%B6%E6%AC%BE%E7%A0%81.jpg" alt="Código de cobro de Alipay" width="280" />
  &nbsp;&nbsp;&nbsp;
  <img src="https://jerrysu232.oss-cn-shenzhen.aliyuncs.com/img/%E5%BE%AE%E4%BF%A1%E6%94%B6%E6%AC%BE%E7%A0%81.jpg" alt="Código de cobro de WeChat" width="280" />
</div>

---

## 📚 Referencias

- Framework RuoYi: <http://www.ruoyi.vip>
- Documentación de RuoYi-Vue: <https://doc.ruoyi.vip/ruoyi-vue/>
- Documentación en chino de LangChain4j: <https://docs.langchain4j.info/category/教程>
- Módulo LangChain4j MCP: <https://docs.langchain4j.info/category/tools/mcp>
- Sitio web oficial de Ollama: <https://ollama.com/>
- Tutorial de despliegue local de Ollama: <https://developer.aliyun.com/article/1656872>
- Repositorio oficial de Dify: <https://github.com/langgenius/dify>
- Repositorio oficial de dify-sandbox: <https://github.com/langgenius/dify-sandbox>
- Sitio web oficial del Model Context Protocol: <https://modelcontextprotocol.io/>
- SDK Java de MCP: <https://github.com/modelcontextprotocol/java-sdk>
- Colección de ejemplos de servidores MCP: <https://github.com/modelcontextprotocol/servers>
- Introducción a las habilidades de Anthropic: <https://docs.anthropic.com/en/docs/agents-and-tools/agent-skills/overview>
- Motor de edición de gráficos AntV X6: <https://x6.antv.antgroup.com/>
- Plugin Dnd de AntV X6: <https://x6.antv.antgroup.com/api/plugins/dnd>
- Plugin History de AntV X6: <https://x6.antv.antgroup.com/api/plugins/history>

---

## 📄 Licencia de código abierto

Este proyecto se basa en la [MIT License](./LICENSE) de código abierto.
