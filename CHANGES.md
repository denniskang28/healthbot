# HealthBot Change Log

---

## 2026-05-16 · MEDICAL_LLM 服务商优化：Mock 独立、DeepSeek 默认、服务次数统计

**四项改动：**

**1. Mock Simulation 独立为专属服务商**

Mock 不再是每个 LLM provider 的一个开关，而是一个独立的 `MEDICAL_LLM` 服务商（`mockMode: true`）。通过 Admin 启用它、加一条路由规则指向它即可激活；真实 LLM provider（Claude/GPT-4/DeepSeek）的配置页面只保留 provider、model、API key、system prompt，不再显示 mock 开关。

| 文件 | 改动 |
|------|------|
| `admin/src/pages/ProviderDetail.tsx` | `LlmConfigCard` 按 `existingConfig.mockMode` 分两套 UI：mock provider 只显示脚本选择；真实 provider 只显示 LLM 参数 |

**2. 默认 MEDICAL_LLM 切换为 DeepSeek**

| 文件 | 改动 |
|------|------|
| `backend/config/DataInitializer.java` | DeepSeek Medical 优先级提至 100、enabled=true；Claude AI 降为 80、enabled=false；新增种子路由规则 `Default LLM — DeepSeek`（serviceType=MEDICAL_LLM，condition=`{}`，priority=0）—— 清库重启后自动生效 |

**3. MEDICAL_LLM 服务次数统计**

每次 LLM 给出有效推荐（recommendation 非 null），为当前活跃的 MEDICAL_LLM provider 写一条 `status=COMPLETED` 的 `ServiceRecord`，Admin Providers 列表的服务次数实时更新。

| 文件 | 改动 |
|------|------|
| `backend/service/ProviderRoutingService.java` | 新增 `recordMedicalLlmCompletion(provider, userId)` |
| `backend/controller/ChatController.java` | recommendation 非 null 时调用上述方法 |

**4. 服务商详情页显示服务记录**

原来 MEDICAL_LLM 服务商详情页只渲染 LLM 配置卡片，服务记录表格被 `else` 分支隐藏。现改为两块并列：LLM 配置卡片（仅 MEDICAL_LLM）+ 服务记录表格（所有类型）。

| 文件 | 改动 |
|------|------|
| `admin/src/pages/ProviderDetail.tsx` | 三元改为条件渲染 + 始终显示服务记录表格 |

---

## 2026-05-16 · LLM 配置移入 MEDICAL_LLM 服务商 + 专科路由

**需求：** 把原来几个 LLM provider（Claude、GPT-4、DeepSeek、Mock）做成 MEDICAL_LLM 服务商，LLM 的 provider/model/API key/system prompt 全部在服务商详情页配置；通过路由规则决定用哪个 MEDICAL_LLM 服务商。同时支持按专科路由（如将心内科问诊路由给专科服务商）。

**架构变化：**

```
聊天请求进入 ChatController
  ↓
ProviderRoutingService.selectMedicalLlm(language, specialty)
  → 按路由规则选优先级最高的已启用 MEDICAL_LLM 服务商
  ↓
LlmProxyService.applyProviderConfigJson(provider.config)
  → 把该服务商的 JSON config push 给 llm-service（热重载，无需重启）
  ↓
正常 LLM 调用（使用该服务商的 model/key/mockMode 等配置）
```

**路由条件支持 AND 逻辑：**

| conditionJson | 匹配规则 |
|--------------|---------|
| `{}` | 匹配所有请求 |
| `{"language":"ZH"}` | 仅匹配中文用户 |
| `{"specialty":"CARDIOLOGY"}` | 仅匹配心内科推荐 |
| `{"language":"ZH","specialty":"NEUROLOGY"}` | 中文 + 神经科（两个条件同时满足） |

**种子 MEDICAL_LLM 服务商：**

| 服务商 | 优先级 | 启用 | 说明 |
|--------|--------|------|------|
| Claude AI | 100 | ✓ | Anthropic claude-sonnet-4-6，默认激活 |
| GPT-4 Medical | 80 | - | OpenAI gpt-4o，备用 |
| DeepSeek Medical | 60 | - | DeepSeek deepseek-chat，低成本备用 |
| Mock Simulation | 0 | - | mockMode=true，演示专用，零成本 |

**各组件改动：**

| 文件 | 改动 |
|------|------|
| `backend/config/DataInitializer.java` | 新增 4 个 MEDICAL_LLM 服务商种子（含完整 config JSON）；非 LLM 服务商改用无品牌虚构公司名 |
| `backend/service/ProviderRoutingService.java` | 新增 `selectMedicalLlm(language, specialty)`；`conditionMatches()` 支持 language + specialty AND 逻辑 |
| `backend/service/LlmProxyService.java` | 新增 `applyProviderConfigJson(configJson)` 解析并 push 到 llm-service |
| `backend/controller/ChatController.java` | 每次聊天前调用 `selectMedicalLlm` + `applyProviderConfigJson`；`dispatch()` 传入 specialty |
| `backend/dto/ActionsDto.java` | 新增 `specialty` 字段（LLM 分类结果，用于路由） |
| `llm-service/llm_client.py` | `_CLASSIFY_TOOL` 新增 `specialty` 枚举字段（10 个专科值）；`CLASSIFY_SYSTEM` 新增专科分配规则 |
| `llm-service/models.py` | `ChatResponse` 新增 `specialty` 字段 |
| `llm-service/mock_responses.py` | 各脚本新增 `specialty` 值（RESPIRATORY / NEUROLOGY / ENDOCRINOLOGY） |
| `admin/src/pages/ProviderDetail.tsx` | MEDICAL_LLM 服务商展示 `LlmConfigCard`（Mock 开关、脚本选择、provider/model/key/system prompt 全配置）；保存时 API key 为空则保留现有 key |
| `admin/src/pages/RoutingRules.tsx` | 路由规则服务类型新增 MEDICAL_LLM；条件预设生成所有 language × specialty 组合 |
| `admin/src/components/Layout.tsx` | 移除独立「LLM Config」菜单项（配置已移入服务商详情页） |
| `admin/src/context/LanguageContext.tsx` | 新增 `llmConfigCard` 翻译键 |

---

## 2026-05-16 · 可配置服务商管理 + 路由规则引擎

**需求：** 把医疗大语言模型、线上问诊、线下预约、线上药房做成可配置可替换的服务商，支持多服务商并存、规则引擎按条件路由、Admin 可视化管理。

**整体架构：**

```
LLM 判断 recommendation（ONLINE_CONSULTATION / OFFLINE_APPOINTMENT / MEDICATION）
  ↓
ChatController 调用 ProviderRoutingService.dispatch()
  ↓
规则引擎：按优先级评估 RoutingRule 列表
  ├── 条件匹配（如 language=ZH）且有指定目标 → 选该服务商
  ├── 条件匹配但无指定目标 → fallthrough 到默认
  └── 无匹配规则 → 取优先级最高的已启用服务商
  ↓
记录 ServiceRecord（status=DISPATCHED）
  ↓
ActionsDto 返回 selectedProviderId / selectedProviderName / selectedProviderCompany
```

**服务商类型：**

| 类型 | 说明 |
|------|------|
| `MEDICAL_LLM` | 医疗大语言模型，路由规则决定用哪个 |
| `ONLINE_CONSULTATION` | 线上问诊平台 |
| `OFFLINE_APPOINTMENT` | 线下预约网络 |
| `ONLINE_PHARMACY` | 线上药房 |
| `OTHER` | 其他类型 |

**种子非 LLM 服务商（首次启动自动写入）：**

| 服务商 | 类型 | 公司 | 优先级 |
|--------|------|------|--------|
| MediConnect Online | ONLINE_CONSULTATION | MediConnect Health | 100 |
| CareCloud Doctors | ONLINE_CONSULTATION | CareCloud Medical | 80 |
| HealthNet Hospital Network | OFFLINE_APPOINTMENT | HealthNet Group | 100 |
| Premier Care Centers | OFFLINE_APPOINTMENT | Premier Health | 80 |
| QuickPharm Online | ONLINE_PHARMACY | QuickPharm | 100 |
| MedMart Pharmacy | ONLINE_PHARMACY | MedMart Health | 80 |

**默认路由规则：**

| 规则 | 条件 | 目标服务商 | 优先级 |
|------|------|-----------|--------|
| ZH - Online Consultation | `{"language":"ZH"}` | MediConnect Online | 100 |
| ZH - Offline Appointment | `{"language":"ZH"}` | HealthNet Hospital Network | 100 |
| ZH - Online Pharmacy | `{"language":"ZH"}` | QuickPharm Online | 100 |

**各组件改动：**

| 文件 | 改动 |
|------|------|
| `backend/model/ServiceProvider.java` | **新增** — 服务商实体（name、type、company、description、enabled、priority、config JSON） |
| `backend/model/ServiceRecord.java` | **新增** — 调度记录实体（providerId、userId、serviceType、status、rating、notes） |
| `backend/model/RoutingRule.java` | **新增** — 路由规则实体（serviceType、conditionJson、targetProviderId、priority、enabled） |
| `backend/repository/ServiceProviderRepository.java` | **新增** |
| `backend/repository/ServiceRecordRepository.java` | **新增** — 含 `countByProviderId`、`avgRatingByProviderId` 聚合查询 |
| `backend/repository/RoutingRuleRepository.java` | **新增** |
| `backend/service/ProviderRoutingService.java` | **新增** — `dispatch(recommendation, language, userId)` 实现完整规则引擎 |
| `backend/controller/ProviderController.java` | **新增** — 服务商 CRUD（含启用/禁用、服务记录查询、评分）；路由规则 CRUD |
| `backend/controller/ChatController.java` | 聊天完成后调用 `dispatch()`，将服务商信息写入 `ActionsDto` |
| `backend/dto/ActionsDto.java` | 新增 `selectedProviderId / selectedProviderName / selectedProviderCompany` |
| `backend/service/LlmProxyService.java` | 更新 `ActionsDto` 构造以匹配新字段 |
| `backend/config/DataInitializer.java` | 新增服务商和路由规则种子数据块（`count() == 0` 守卫） |
| `admin/src/pages/Providers.tsx` | **新页面** — 服务商列表（类型 Tag、启用开关、服务次数、评分），支持新增/编辑/删除 |
| `admin/src/pages/ProviderDetail.tsx` | **新页面** — 服务商详情（基本信息 + 服务记录表格 + 评分操作） |
| `admin/src/pages/RoutingRules.tsx` | **新页面** — 路由规则管理（条件配置、目标服务商选择、优先级排序、启用开关） |
| `admin/src/api/types.ts` | 新增 `ServiceProviderDto`、`ServiceRecordDto`、`RoutingRuleDto` |
| `admin/src/api/client.ts` | 新增服务商和路由规则的全套 API 调用 |
| `admin/src/context/LanguageContext.tsx` | 新增 EN/ZH 翻译键（服务商类型、状态、操作按钮等 30+ 个键） |
| `admin/src/App.tsx` | 注册 `/providers`、`/providers/:id`、`/routing-rules` 路由 |
| `admin/src/components/Layout.tsx` | 侧边栏新增「服务商」（ApiOutlined）和「路由规则」（BranchesOutlined）菜单项 |

---

## 2026-05-16 · Android 多环境支持 + 阿里云部署

**需求：** 支持本地模拟器调试和云端真机测试并存，测试人员用自己手机连接阿里云服务器。

**Android 改动：**

| 文件 | 改动 |
|------|------|
| `android/app/build.gradle` | 新增 `productFlavors`：`local`（`10.0.2.2:8080`）和 `cloud`（`YOUR_SERVER_IP:8080`）；开启 `buildConfig true` |
| `android/api/RetrofitClient.kt` | `BASE_URL` 从硬编码改为读 `BuildConfig.BASE_URL` |

**构建方式：**
- 本地调试：Android Studio 选 **localDebug** variant → 连模拟器
- 测试分发：选 **cloudRelease** variant → 生成 APK 分发给测试人员

**阿里云部署架构：**

```
手机/浏览器
  :80   →  Nginx  →  admin/dist（静态文件）
                  →  /api/*, /admin/*  →  Backend :8080
                  →  /ws              →  Backend :8080（WebSocket）
  :8080 →  Backend（Spring Boot，手机直连）
                  →  localhost:8000   →  LLM Service（仅内网，不暴露）
```

**服务器端口策略：**

| 端口 | 开放 | 用途 |
|------|------|------|
| 80 | 公网 | Admin 控制台 |
| 8080 | 公网 | Android 手机直连 Backend |
| 8000 | 仅内网 | LLM Service，不对外暴露 |

**部署方式：** LLM Service 和 Backend 用 `nohup` 或 `screen` 后台运行；Admin 用 `npm run build` 后由 Nginx 托管静态文件。

---

## 2026-05-16 · Admin 登录鉴权 + 密码重置

**需求：** Admin 控制台增加登录保护，初始密码 `12345`，可在系统管理页面重置。

**鉴权流程：**

```
Admin UI 输入密码
  POST /admin/auth/login  →  Backend 验证 SHA-256 hash
                              └── 生成 UUID token 返回
Admin UI 存入 localStorage，后续所有请求自动携带
  Authorization: Bearer <token>  →  AdminAuthFilter 验证
                                     ├── 通过 → 正常处理
                                     └── 失败 → 401，前端清 token 跳回登录页

修改密码
  PUT /admin/auth/password  →  验旧密码 → 更新 hash → 使现有 token 失效 → 前端自动退出
```

**各组件改动：**

| 文件 | 改动 |
|------|------|
| `backend/model/AdminCredential.java` | **新增** — 存密码 SHA-256 hash 的实体 |
| `backend/repository/AdminCredentialRepository.java` | **新增** |
| `backend/service/AdminAuthService.java` | **新增** — 登录生成 UUID token、验 token、改密码（改完 token 失效） |
| `backend/controller/AdminAuthController.java` | **新增** — `POST /admin/auth/login`、`PUT /admin/auth/password` |
| `backend/config/AdminAuthFilter.java` | **新增** — `OncePerRequestFilter`，拦截所有 `/admin/**`，`/admin/auth/` 放行 |
| `backend/config/DataInitializer.java` | 首次启动写入 `12345` 的 hash |
| `admin/src/pages/Login.tsx` | **新增** — 居中登录卡片，密码输入框 |
| `admin/src/pages/Settings.tsx` | **新增** — 系统管理页，修改密码表单，成功后 1.5s 自动退出重新登录 |
| `admin/src/api/client.ts` | 新增 axios 拦截器（注入 Bearer token；401 时清 token 刷新）；新增 `login`、`changePassword` |
| `admin/src/App.tsx` | 未登录渲染 `Login.tsx`；已登录渲染正常布局；新增 `/settings` 路由 |
| `admin/src/components/Layout.tsx` | 新增「系统管理」菜单项；右上角新增带确认弹窗的退出按钮 |
| `admin/src/context/LanguageContext.tsx` | 新增 EN/ZH 翻译键：`logout`、`settings`、`changePassword`、`passwordChanged` 等 |

---

## 2026-05-16 · Android Chatbot 进入时加载历史记录

**需求：** 每次打开 Chatbot 界面时，先展示最近 20 条历史消息，再显示问候语，让用户有完整的对话上下文。

**改动文件：** `android/ChatbotActivity.kt`

| 改动点 | 内容 |
|--------|------|
| `onCreate` | 移除同步添加欢迎语的逻辑，改为调用 `loadHistoryThenGreet()` |
| `loadHistoryThenGreet()` | 新增方法：先调 `GET /api/chat/{userId}/history` 拉取历史消息逐条渲染，加载完（或失败时静默忽略）再追加欢迎语；`result_message` 追加在欢迎语之后 |

API `getChatHistory` 在 `HealthBotApi.kt` 中已存在，无需新增接口。

---

## 2026-05-16 · Bug 修复：推荐后闲聊重复触发路由

**问题：** 超过 3 轮后每条消息都强制走分类流程，用户说"thank you"也会再次弹出预约/购药界面。

**修复方案：** 在 `_CLASSIFY_TOOL` 加第四个选项 `CONTINUE_CHAT`，并在 `CLASSIFY_SYSTEM` 最顶部加优先规则——如果最新消息是致谢、确认或与症状无关的闲聊，选 `CONTINUE_CHAT`；代码收到后返回普通对话响应（`isComplete=False`），不触发任何路由 UI。

**改动文件：** `llm-service/llm_client.py`

| 改动点 | 内容 |
|--------|------|
| `_CLASSIFY_TOOL` enum | 新增 `CONTINUE_CHAT` 选项 |
| `CLASSIFY_SYSTEM` prompt | 顶部新增 `CONTINUE_CHAT` 规则，覆盖致谢 / 确认 / 闲聊场景 |
| `chat()` 函数 | `recommendation == "CONTINUE_CHAT"` 时直接返回 `ChatResponse(content=...)` 不带 `isComplete=True` |

Backend 和 Android 无需改动，本来就只在 `isComplete=True` 时展示路由界面。

---

## 2026-05-16 · Bug 修复：H2 数据库持久化 + 种子数据重复

**问题：** 后端重启后数据全部丢失；即使改为文件存储，种子数据也会在每次重启时重复插入。

| 问题 | 原因 | 修复 |
|------|------|------|
| 重启后数据丢失 | H2 使用内存模式（`mem:`），进程退出即清空；`ddl-auto: create-drop` 关闭时主动删表 | URL 改为 `jdbc:h2:file:./data/healthbotdb`，`ddl-auto` 改为 `update` |
| 种子数据重复插入 | `DataInitializer` 每次启动无条件执行所有 `save()` | 各实体块加 `if (repo.count() == 0)` 判断，首次启动才插入 |

**改动文件：**

| 文件 | 改动 |
|------|------|
| `backend/resources/application.yml` | datasource url 从 `mem:` 改为 `file:./data/healthbotdb`；`ddl-auto` 从 `create-drop` 改为 `update` |
| `backend/.gitignore` | 新增，忽略 `data/` 目录（数据库文件不入库） |
| `backend/config/DataInitializer.java` | Users、Doctors、LlmConfig 三个种子块各加 `count() == 0` 守卫 |

---

## 2026-05-16 · Admin 对话历史查看与清除

**需求：** Admin 可查看任意用户的对话历史，并一键清除（同时重置 Mock 计数器），方便重新演示。

**数据流：**

```
Admin UI 选择用户
  GET /admin/chat-history/{userId}  →  Backend  →  DB 查 chat_messages 表

Admin UI 点击「清除历史」
  DELETE /admin/chat-history/{userId}  →  Backend
                                           ├── DB 删除该用户所有消息
                                           └── DELETE /chat-counter/{userId}  →  llm-service 重置 Mock 计数器
```

**各组件改动：**

| 文件 | 改动 |
|------|------|
| `backend/repository/ChatMessageRepository.java` | 新增 `deleteByUserId(Long userId)` |
| `backend/service/LlmProxyService.java` | 新增 `resetChatCounter(userId)`，调用 llm-service `DELETE /chat-counter/{userId}` |
| `backend/controller/AdminController.java` | 新增 `GET /admin/users`、`GET /admin/chat-history/{userId}`、`DELETE /admin/chat-history/{userId}` |
| `llm-service/main.py` | 新增 `DELETE /chat-counter/{userId}`，清除内存中的 Mock 消息计数 |
| `admin/src/pages/ChatHistory.tsx` | **新页面** — 左侧用户列表，右侧聊天气泡（USER 蓝色右对齐 / AI 灰色左对齐），顶部「清除历史」按钮含确认弹窗 |
| `admin/src/api/types.ts` | 新增 `ChatMessageDto` 接口 |
| `admin/src/api/client.ts` | 新增 `getUsers`、`getChatHistory`、`deleteChatHistory` |
| `admin/src/context/LanguageContext.tsx` | 新增 EN/ZH 翻译键：`chatHistory`、`clearHistory`、`confirmClearHistory`、`historyCleared` 等 |
| `admin/src/App.tsx` | 注册 `/chat-history` 路由 |
| `admin/src/components/Layout.tsx` | 侧边栏新增「对话历史」菜单项（MessageOutlined 图标） |

---

## 2026-05-16 · Bug 修复：Chat 历史记录顺序与重复问题

**问题根因：** `ChatController` 在保存当前用户消息**之后**才查询历史记录，且查询使用降序排列，导致两个并发 bug。

| Bug | 现象 | 修复 |
|-----|------|------|
| 历史顺序反转 | `findTop20ByUserIdOrderByTimestampDesc` 返回最新在前，LLM 收到的对话是倒序的，上下文理解错误 | 查完后 `Collections.reverse()` 变成时间正序 |
| 当前消息重复 | 用户消息先存入 DB，再查历史时已包含在内，`llm_client.py` 又额外 append 一次，LLM 看到最后一条消息出现两次 | 将历史查询移到 `save(userMsg)` 之前，确保当前消息不在 history 里 |

**改动文件：** `backend/controller/ChatController.java`

---

## 2026-05-16 · 新增 DeepSeek LLM 支持

**各组件改动：**

| 文件 | 改动 |
|------|------|
| `llm-service/llm_client.py` | `get_client()` 新增 `deepseek` 分支，使用 OpenAI 兼容 SDK，base_url 设为 `https://api.deepseek.com`；`_call_tool()` 的 OpenAI 路径扩展至 `deepseek` |
| `admin/src/pages/LlmConfig.tsx` | `PROVIDERS` 新增 DeepSeek 选项；`MODEL_PRESETS` 新增 `deepseek-chat` 和 `deepseek-reasoner` |
| `llm-service/CLAUDE.md` | 环境变量表新增 `DEEPSEEK_API_KEY` 说明 |

**集成方式：** DeepSeek 兼容 OpenAI API 格式，复用现有 `openai` 代码路径，仅通过 `base_url` 区分。Admin 切换至 DeepSeek 后填入 API key 即生效，无需重启。

---

## 2026-05-16 00:10 · Admin 实时 LLM 配置 + Mock 模式

**需求：** Admin 控制台可切换 LLM 服务商，配置立即生效无需重启；支持 Mock 模式，确保演示路线可预测；配置持久化，重启后继续生效。

**数据流：**

```
Admin UI (React)
  PUT /admin/llm-config  →  Backend (Spring Boot)
                              └── PUT /config  →  llm-service (FastAPI)
                                                    └── config_manager.update()
                                                          ├── 写 config.json（持久化）
                                                          └── llm_client.reload()（热重载）
```

**各组件改动：**

| 文件 | 改动 |
|------|------|
| `llm-service/config_manager.py` | **新增** — 配置读写单例；`get()` 从 `config.json` 或 `.env` 加载；`update()` 写文件并调 `llm_client.reload()`；`safe_get()` 脱敏 API key 供 Admin 显示 |
| `llm-service/models.py` | 新增 `LlmConfigRequest`、`LlmConfigResponse` 数据模型 |
| `llm-service/main.py` | 新增 `GET /config` 和 `PUT /config` 路由；`POST /chat` 检查 `mockMode`，开启时跳过真实 LLM 调用 |
| `llm-service/llm_client.py` | 移除模块级 `PROVIDER`/`MODEL`/`API_KEY` 常量；改为运行时从 `config_manager` 读取；新增 `reload()` 重置客户端实例 |
| `llm-service/mock_responses.py` | 重写 `MOCK_SCRIPTS` — 三套完整脚本（MEDICATION / ONLINE_CONSULTATION / OFFLINE_APPOINTMENT），每套含各轮回复、结论、处方、推荐医生 ID；`get_mock_chat_response()` 支持脚本参数 |
| `backend/service/LlmProxyService.java` | 新增 `fetchConfig()` 和 `pushConfig()`，代理到 llm-service `/config`；LLM 服务 URL 通过 `@Value` 从 `application.yml` 读取 |
| `backend/controller/AdminController.java` | 新增 `GET /admin/llm-config` 和 `PUT /admin/llm-config`，调用 `LlmProxyService` |
| `admin/src/api/types.ts` | `LlmConfigDto` 新增 `mockMode`、`mockScript` 字段 |
| `admin/src/context/LanguageContext.tsx` | 新增 EN/ZH 翻译键：`mockMode`、`mockModeHint`、`mockScript`、`mockScriptMedication`、`mockScriptOnline`、`mockScriptOffline` |
| `admin/src/pages/LlmConfig.tsx` | 完全重写：顶部 Mock 模式开关（黄色区域）+ 脚本选择器；状态标签显示当前模式；Alert 提示「无需重启」；表单字段与新 DTO 对齐 |

**Mock 脚本设计：**

| 脚本 | 场景 | 推荐路由 |
|------|------|---------|
| `MEDICATION` | 普通感冒，3 轮后推荐购药 | → 线上药房 |
| `ONLINE_CONSULTATION` | 反复头痛，3 轮后推荐线上问诊 | → 在线专家 |
| `OFFLINE_APPOINTMENT` | 不明原因体重下降，3 轮后推荐线下预约 | → 线下预约 |

**验证结果：**
- `GET /config` → 返回当前配置（API key 脱敏为 `****...xxxx`）
- `PUT /config` 切换 Mock 模式 → 立即生效，无需重启 llm-service 或 backend
- Mock 模式下连续 3 轮 chat → `isComplete: true`，`recommendation` 精确匹配所选脚本

---

## 2026-05-15 22:50 · Bug 修复：千问配置 + 中文输入

**完整的数据流：**

```
用户聊天 (3+ 轮)
    ↓
LLM 判断有足够信息 → isComplete=true
    ↓
返回：conclusion（结论文字）+ recommendation（推荐类型）
    ↓
Android 弹出 Bottom Sheet 显示初步诊断
    ↓
ONLINE_CONSULTATION → 选医生 → 视频问诊
OFFLINE_APPOINTMENT → 预约界面
MEDICATION          → 显示处方 → 线上药房
```

**各组件改动：**

| 文件 | 改动 |
|------|------|
| `llm-service/llm_client.py` | 更新 system prompt（多轮收集→初步判断）；`_CHAT_TOOL` schema 增加 `isComplete / conclusion / recommendation / prescription`；`chat()` 解析并返回新字段，`max_tokens` 提至 1500 |
| `llm-service/models.py` | `ChatResponse` 新增 `isComplete`、`conclusion`、`recommendation`、`prescription` |
| `llm-service/mock_responses.py` | 第 3 条消息后返回含完整结论的 mock，无 API key 也可演示全流程 |
| `backend/dto/ActionsDto.java` | 新增 `isComplete / conclusion / recommendation / prescription`；加 `@JsonProperty("isComplete")` 解决 Lombok boolean 命名冲突 |
| `backend/service/LlmProxyService.java` | `chat()` 解析 LLM 返回的新字段并写入 `ActionsDto` |
| `android/api/models/Models.kt` | `ActionsDto` 新增 `isComplete / conclusion / recommendation / prescription` |
| `android/res/layout/bottom_sheet_consult.xml` | 重新设计：初步诊断卡片 + 处方列表区 + 三种动作按钮 |
| `android/ChatbotActivity.kt` | `handleActions()` 拆分为 `showConclusionSheet()`（新流程）和 `showLegacySuggestionSheet()`（旧流程兼容）；新增 `startPharmacyWithPrescription()` |
| `android/res/values/strings.xml` | 新增 7 个字符串（EN） |
| `android/res/values-zh/strings.xml` | 新增 7 个字符串（ZH） |

---

## 2026-05-15 22:50 · Bug 修复：千问配置 + 中文输入

**各组件改动：**

| 文件 | 问题 | 修复 |
|------|------|------|
| `llm-service/models.py` | `ChatResponse` 引用 `Medicine` 时该类尚未定义，启动报 `NameError` | 将 `Medicine` 类移到 `ChatResponse` 之前 |
| `llm-service/main.py` | `load_dotenv()` 在 `import llm_client` 之后执行，`.env` 中的 `PROVIDER` 等变量未生效，始终使用 `anthropic` 默认值 | 将 `load_dotenv()` 移至所有 import 之前 |
| `android/res/layout/activity_chatbot.xml` | `inputType="text"` + `imeOptions="actionSend"` 导致中文拼音输入法在合字阶段触发发送，无法完成选字 | 改为 `inputType="textMultiLine"`，去掉 `imeOptions` |
| `android/ChatbotActivity.kt` | `onEditorActionListener` 监听回车发送，与中文输入法换行冲突 | 移除该监听，发送仅通过 FAB 按钮触发 |

---

## 2026-05-15 23:30 · 修复：LLM 路由不稳定

**问题根因：** 原实现用一次 LLM 调用同时决定「何时结束对话」和「推荐哪条路」，两个决策都不可靠，导致三个 demo 案例经常走错流程。

**修复方案：时机由代码控制，分类用独立调用**

```
原来（不稳定）                      现在（稳定）
──────────────────────────         ──────────────────────────────────
1次LLM调用                          Turn 1-2: 简单对话（_CHAT_RESPONSE_TOOL）
  → 决定isComplete（不稳定）             只生成 responseText，无路由逻辑
  → 决定recommendation（不稳定）    Turn 3:  代码强制触发
                                       Step 1: 生成"已有初步判断"回复
                                       Step 2: 独立分类调用（_CLASSIFY_TOOL）
                                              用明确规则决定推荐类型
```

**分类规则（写入 CLASSIFY_SYSTEM prompt）：**

| 推荐类型 | 触发条件 |
|----------|---------|
| `MEDICATION` | 急性起病（<3天）、症状轻微明确（感冒/轻度过敏/轻微疼痛）、无慢性病史 |
| `OFFLINE_APPOINTMENT` | 不明原因体重下降、夜间盗汗、疑似代谢/激素问题、胸痛、需要体检或化验 |
| `ONLINE_CONSULTATION` | 其余情况（反复发作、中度严重、专科建议但无需立即检查） |

不确定时：ONLINE vs OFFLINE → 选 OFFLINE；MEDICATION vs ONLINE → 选 ONLINE。

**各文件改动：**

| 文件 | 改动 |
|------|------|
| `llm-service/llm_client.py` | 拆分为 `_CHAT_RESPONSE_TOOL`（对话）和 `_CLASSIFY_TOOL`（分类）；`chat()` 重写为两步调用；新增 `CONCLUDE_AFTER_EXCHANGES=3` 常量控制触发时机 |
| `llm-service/mock_responses.py` | mock 触发条件从 `>=3` 改为 `>=2`（0-indexed 计数器对齐第3轮） |

---
