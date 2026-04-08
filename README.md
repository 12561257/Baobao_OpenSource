<div align="center">

# 🥗 BaoBao FatLoss — 智能减脂 AI 助理

**一个有长期记忆的 AI 私人减脂教练——它不只是计算，更是真正认识你。**

[![Android](https://img.shields.io/badge/Platform-Android%2024%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![LLM](https://img.shields.io/badge/AI-OpenAI%20Compatible%20API-412991?logo=openai&logoColor=white)](https://platform.openai.com)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

[功能演示](#-核心功能) · [架构设计](#-系统架构) · [快速开始](#-快速开始) · [技术栈](#-技术栈)

</div>

---

## 📖 项目简介

BaoBao FatLoss 是一款 **原生 Android AI 减脂助理应用**，融合了多模态 AI、三层记忆系统和传统营养算法，实现了远超传统健康 App 的智能化体验。

与市面上的健康应用不同，本项目的核心创新在于：

| 传统健康 App 的痛点 | 本项目的解决方案 |
|---|---|
| 手动搜索食物，录入繁琐 | ✅ **多模态拍照/文字识别**，AI 自动分析营养成分 |
| 每次对话从零开始，无记忆 | ✅ **三层记忆架构**，AI 真正"认识"并记住你 |
| 课表死板，不感知用户状态 | ✅ **动态计划调整算法**，自动计算每日热量预算 |
| 仅限固定 AI 服务商 | ✅ **OpenAI 兼容 API**，支持豆包、DeepSeek 等任意服务商 |

---

## ✨ 核心功能

### 🤖 AI 智能对话（核心）
- **上下文感知对话**：AI 结合当日热量账本、用户画像和历史记忆进行个性化回复
- **结构化指令解析**：AI 回复中自动识别并执行 `[FOOD_LOG]`、`[EXERCISE]`、`[DELETE_FOOD]`、`[MEMORY_ACTION]` 等 Agent 动作标记
- **多模态食物识别**：在聊天页面直接发送食物照片，AI 自动估算热量并记入账本

### 📸 食物拍照识别
- 独立拍照/相册入口，专为饮食记录场景优化
- 图片自动压缩（≤ 1280px）防止 OOM，安全转为 Base64 后发送给大模型
- AI 返回结构化 JSON（食物名、热量、碳水/蛋白质/脂肪）并通过正则兜底解析

### 📊 饮食记录与热量管理
- 按早/午/晚/加餐分类管理，支持按日期翻页查看历史
- 文字模糊输入 + AI 分析（输入"一碗米粉"即可获得完整营养估算）
- 实时热量进度追踪（已摄入 / 运动消耗 / 今日余量）

### 🧠 三层 AI 记忆系统（OpenClaw 架构）
```
Layer 1 — 静态属性记忆：用户生理信息、目标体重、AI 人设
Layer 2 — 短期工作区记忆：最近 15 轮对话 + 当日日记摘要
Layer 3 — 长期经验记忆：含向量嵌入的语义检索（余弦相似度 ≥ 0.70）
```
AI 会从对话中主动提炼记忆（如"用户不吃香菜"），并通过 `MEMORY_ACTION` 指令进行增/改/删操作，避免重复记录。

### 📈 进度追踪
- 体重曲线记录，可视化减脂进度
- 昨日 AI 总结：每日自动生成一份 AI 点评（`DailyNote`）

### ⚙️ 灵活的 AI 服务配置
- 支持任意 **OpenAI 兼容**服务商（豆包、DeepSeek、本地 Ollama 等）
- API Key 使用 **AES-256-GCM 加密**存储（`EncryptedSharedPreferences`）
- 内置 Embedding 服务（可降级：Embedding 失败时不影响主聊天功能）

### 🌍 国际化支持
- 完整支持**简体中文 / English** 双语切换，可从设置页面随时变更

---

## 🏗️ 系统架构

```
┌──────────────────────────────────────────────────────┐
│                   UI Layer (Jetpack Compose)          │
│  Home · Camera · MealLog · AiChat · Progress         │
│  Profile · MemoryManagement · ApiKeySetup            │
└──────────────────────────┬───────────────────────────┘
                           │ ViewModel (MVVM)
┌──────────────────────────▼───────────────────────────┐
│                  Domain / Repository Layer            │
│                                                      │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────┐  │
│  │ AiRepository │   │MemoryManager │   │ Calorie  │  │
│  │ (Agent 核心)  │   │ (三层记忆)   │   │  Engine  │  │
│  └──────┬───────┘   └──────────────┘   └──────────┘  │
│         │ DoubaoApiService (OkHttp)                   │
│         │ → OpenAI Chat Completions 格式              │
│         │ → Embeddings (RAG 向量检索)                 │
└─────────┼────────────────────────────────────────────┘
          │
┌─────────▼────────────────────────────────────────────┐
│                    Data Layer (Room DB)               │
│                                                      │
│  UserProfile · DailyLedger · FoodLog                │
│  ChatMessage · UserMemory · WeightRecord            │
│  DailyNote                                          │
└──────────────────────────────────────────────────────┘
```

### Agent 动作解析流程

```
用户发送消息（文字 / 图片）
     │
     ▼
MemoryManager.getEnhancedContext()  ← 构建三层记忆上下文
     │
     ▼
DoubaoApiService.chat()             ← 调用 LLM（OpenAI 兼容格式）
     │
     ▼
parseActionsFromReply()             ← 正则解析 Agent 动作标记
     │
     ├── [FOOD_LOG]    → 写入 FoodLog + 更新 DailyLedger
     ├── [EXERCISE]    → 更新 burned_calories
     ├── [DELETE_FOOD] → 模糊/精确匹配删除食物记录
     └── [MEMORY_ACTION] → 增/改/删 UserMemory (向量化存储)
```

---

## 🛠️ 技术栈

| 层级 | 技术 | 用途 |
|---|---|---|
| **UI** | Kotlin + Jetpack Compose | 现代声明式原生界面 |
| **架构** | MVVM + Repository Pattern | 关注点分离，状态驱动 |
| **导航** | Navigation Compose | 类型安全的页面路由 |
| **本地数据库** | Room (SQLite) + Flow | 离线优先，响应式数据流 |
| **网络** | OkHttp | 轻量 HTTP 客户端 |
| **AI 接入** | OpenAI 兼容 API | 支持 豆包/DeepSeek/Gemini/etc |
| **向量检索** | 本地余弦相似度计算 | 无需外部向量数据库的语义搜索 |
| **图片处理** | Coil + Bitmap 手动压缩 | 防 OOM 的安全图像处理 |
| **安全存储** | EncryptedSharedPreferences | AES-256 加密 API Key |
| **序列化** | kotlinx.serialization | JSON 解析 |
| **数据脱糖** | core-library-desugaring | java.time API 兼容 Android 24+ |

---

## 🚀 快速开始

### 前置要求

- Android Studio Ladybug 或更新版本
- JDK 21
- Android SDK 35（minSdk 24）

### 获取 AI API Key

本应用支持任何兼容 OpenAI Chat Completions 格式的 API 服务商。推荐选项：

- **豆包（火山方舟）**：[https://www.volcengine.com/product/ark](https://www.volcengine.com/product/ark)
- **DeepSeek**：[https://platform.deepseek.com](https://platform.deepseek.com)
- **OpenAI**：[https://platform.openai.com](https://platform.openai.com)

> 如需详细的 API 配置教程，可参考：[配置指南](https://zcntd77twma2.feishu.cn/wiki/U9ABwZqsti54rQkjq7mcqVX7nOh?from=from_copylink)

### 安装步骤

1. **克隆仓库**
   ```bash
   git clone https://github.com/YOUR_USERNAME/Baobao_OpenSource.git
   cd Baobao_OpenSource
   ```

2. **使用 Android Studio 打开项目**
   - File → Open → 选择项目根目录

3. **构建并运行**
   - 连接 Android 设备（API 24+）或启动模拟器
   - 点击 Run，或执行：
   ```bash
   ./gradlew assembleDebug
   ```

4. **初始配置**
   - 首次启动时填写 API 配置（服务商 Base URL + API Key + 模型 ID）
   - 前往个人中心完善身体数据（身高、体重、目标体重、目标日期）
   - 开始与 AI 助理对话！

---

## 📁 项目结构

```
app/src/main/java/com/baobao/fatloss/
├── data/
│   ├── local/
│   │   ├── dao/            # Room DAO 接口
│   │   ├── entity/         # 数据实体类
│   │   ├── ApiKeyStore.kt  # 加密 API Key 存储
│   │   └── AppDatabase.kt  # Room 数据库定义
│   ├── model/
│   │   └── CalorieCalc.kt  # BMR/TDEE 计算工具
│   ├── remote/
│   │   └── DoubaoApiService.kt  # OpenAI 兼容 API 客户端
│   └── repository/
│       ├── AiRepository.kt      # AI 核心逻辑（Agent）
│       ├── MemoryManager.kt     # 三层记忆管理器
│       └── ...                  # 其他 Repository
├── navigation/             # 路由与导航图
├── ui/
│   ├── aichat/             # AI 对话页面
│   ├── camera/             # 拍照识别页面
│   ├── home/               # 首页（热量总览）
│   ├── meallog/            # 饮食记录页面
│   ├── memory/             # 记忆管理页面
│   ├── onboarding/         # 初始化引导
│   ├── profile/            # 个人中心
│   ├── progress/           # 进度追踪
│   ├── setup/              # API 配置页面
│   ├── components/         # 公共 UI 组件
│   └── theme/              # Material 3 主题
├── viewmodel/              # 各页面 ViewModel（8个）
├── MainActivity.kt
├── FitAssistantApp.kt
└── CrashHandler.kt
```

---

## 🔑 关键技术亮点

### 1. Agentic AI 设计模式
AI 回复不只是文字——LLM 输出结构化的"动作标记"，应用层解析并自动执行：
```
[FOOD_LOG]{"meal":"lunch","foods":[{"name":"米粉","calories":650,...}]}[/FOOD_LOG]
```
这一设计使 AI 成为真正的"行动者"，而非仅仅的"回答者"。

### 2. 本地向量语义检索（无需外部 Vector DB）
`MemoryManager` 在 Room DB 中存储 Embedding 向量（JSON 格式），检索时在内存中计算余弦相似度，实现了轻量级的 RAG 架构：
```kotlin
.filter { cosineSimilarity(queryEmbedding, it.embedding) > 0.70 }
.sortedByDescending { score }
.take(5)
```

### 3. 开放的 API 兼容层
`DoubaoApiService` 遵循 OpenAI API 规范，支持 Chat、Vision（多模态）、Embeddings 三种调用，只需修改 `baseUrl` 即可切换服务商，实现了与具体 LLM 厂商的解耦。

### 4. 离线优先 + 隐私保护架构
所有用户数据（饮食照片、健康数据、对话历史）存储在本地 Room DB。API Key 使用 `EncryptedSharedPreferences` + AES-256-GCM 加密，断网可查阅历史。

---

## 🗺️ 路线图

- [x] AI 智能对话（Agent 动作解析）
- [x] 多模态食物识别（拍照 + 文字）
- [x] 三层记忆系统（含本地向量检索）
- [x] 热量账本 + 动态预算计算
- [x] 进度追踪（体重记录）
- [x] API 厂商自由切换
- [x] 国际化（中文 / English）
- [ ] 运动记录详情（组数 / 重量）
- [ ] 周进度自动复盘推送
- [ ] Widget 桌面小组件

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建你的特性分支（`git checkout -b feature/AmazingFeature`）
3. 提交更改（`git commit -m 'Add some AmazingFeature'`）
4. 推送到分支（`git push origin feature/AmazingFeature`）
5. 开启 Pull Request

---

## 📄 许可证

本项目使用 MIT 许可证，详见 [LICENSE](LICENSE)。

---

<div align="center">

**如果这个项目对你有帮助，请给一个 ⭐ Star！**

Made with ❤️ by Yang Hong · Powered by AI-Assisted Development

</div>
