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
