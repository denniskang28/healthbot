# HealthBot Change Log

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
