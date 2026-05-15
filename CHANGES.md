# HealthBot Change Log

---

## 2026-05-15 22:15 · LLM 多轮问诊与智能路由

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
