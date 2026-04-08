# 技术规格书 (Technical Specification)

**项目名称**：智能减脂私人助理 — AI-Powered Diet & Fitness Assistant  
**文档版本**：v1.0.0  
**创建日期**：April 1, 2026  
**作者**：Yang Hong  
**状态**：`草稿 DRAFT`

---

## 目录

1. [产品愿景与市场定位（PM 视角）](#1-产品愿景与市场定位pm-视角)  
2. [用户画像与核心场景（PM 视角）](#2-用户画像与核心场景pm-视角)  
3. [功能优先级矩阵（PM 视角）](#3-功能优先级矩阵pm-视角)  
4. [系统架构设计（CTO 视角）](#4-系统架构设计cto-视角)  
5. [技术栈选型（CTO 视角）](#5-技术栈选型cto-视角)  
6. [数据库与数据模型（CTO 视角）](#6-数据库与数据模型cto-视角)  
7. [AI 记忆系统架构（CTO 视角）](#7-ai-记忆系统架构cto-视角)  
8. [核心 API 接口规范（CTO 视角）](#8-核心-api-接口规范cto-视角)  
9. [动态计划调整算法（CTO 视角）](#9-动态计划调整算法cto-视角)  
10. [开发路线图与交付里程碑](#10-开发路线图与交付里程碑)  
11. [风险评估与缓解策略](#11-风险评估与缓解策略)  
12. [附录：名词解释](#12-附录名词解释)

---

## 1. 产品愿景与市场定位（PM 视角）

### 1.1 一句话产品定义

> **"一个有长期记忆的 AI 私人减脂教练——它不只是计算，更是真正认识你。"**

### 1.2 市场痛点分析

| 现有产品类别 | 核心痛点 |
|---|---|
| 薄荷健康 / MyFitnessPal | 录入繁琐，依赖庞大数据库，用户主动操作成本极高，无个性化记忆 |
| Keep / Nike Training | 课表死板，不感知用户实际体重进度，无饮食联动 |
| 通用 AI 聊天（GPT/Gemini） | 无持久记忆，每次对话从零开始，无法追踪长期目标 |
| **本产品** | ✅ 多模态拍照录入 + ✅ 跨会话长期记忆 + ✅ 动态目标追踪 ＝ **降维打击** |

### 1.3 产品差异化核心

本产品定位为"超级版"健康管理工具，其竞争优势建立在三个维度：

```
传统算法（BMR/TDEE）  →  定义"底薪"：提供精确的热量账本基准
AI 多模态能力        →  降低录入门槛：拍照即记录，语音即输入
长期记忆系统         →  建立情感连接：它记得你、了解你、陪伴你
```

---

## 2. 用户画像与核心场景（PM 视角）

### 2.1 主要用户画像（Persona）

| 属性 | 描述 |
|---|---|
| **姓名** | 杨弘（代表核心用户群） |
| **年龄 / 身份** | 20-25 岁，在校大生 / 初入职场 |
| **身体目标** | 当前体重 79 kg，目标于 2026 年 6 月底前减至目标体重 |
| **痛点** | 课程/工作繁忙、饮食不规律、缺乏持续坚持的动力 |
| **行为特征** | 习惯在学校食堂或外卖场景就餐，不愿意手动搜索每种食物 |
| **技术素养** | 中高，能接受 App 新功能，偏好自然语言交互 |

### 2.2 核心使用场景（User Story）

**场景 A — 拍照吃饭（最高频）**
> 杨弘站在学校食堂前，点了一碗米粉。他打开 App，拍下照片，问："这碗我能吃完吗？"  
> 助理秒回："杨弘，这碗粉约 650 大卡，你今天还有 400 大卡额度。建议少喝汤，或者下午在操场加练 30 分钟来抵消。"

**场景 B — 进度复盘（每周日）**
> 助理主动推送："这周有两次聚餐，但你周三多跑了 5 公里，整体进度仍在 6 月达标轨道上。下周建议保持，你做得很棒！"

**场景 C — 动态调整（计划落后时）**
> 系统检测到连续 3 天热量超标，助理发送通知："检测到本周进度稍落后，我已为你重新计算了下周预算，每天减少 150 大卡就能保住 6 月达标。要继续吗？"

**场景 D — 环境感知（久坐提醒）**
> 杨弘赶作业坐了 4 小时，助理提示："今天久坐时间偏长，晚上的深蹲我帮你简化成了 15 分钟室内燃脂，就在寝室做，怎么样？"

---

## 3. 功能优先级矩阵（PM 视角）

> **优先级定义**：`P0` = MVP 必须有 | `P1` = 第一版迭代 | `P2` = 未来规划

### 3.1 功能清单

| 模块 | 功能 | 优先级 | 说明 |
|---|---|---|---|
| **饮食记录** | 拍照多模态识别热量 | P0 | 核心差异化功能 |
| **饮食记录** | 语音/文字模糊录入 | P0 | 降低使用门槛 |
| **热量管理** | BMR / TDEE 自动计算 | P0 | 传统基础功能，必须扎实 |
| **热量管理** | 今日热量进度条（已摄入 / 剩余 / 运动消耗） | P0 | 首屏核心信息 |
| **AI 助理** | 自然语言问答（"还能吃什么？"） | P0 | 区别于普通 App 的灵魂 |
| **AI 助理** | 基于历史记忆的个性化建议 | P0 | OpenClaw 架构核心 |
| **进度追踪** | 体重 / 体脂率 / 围度记录与折线图 | P1 | 长期坚持的视觉反馈 |
| **动态计划** | 自动调整热量预算与运动建议 | P1 | 系统智能化的体现 |
| **打卡系统** | 月历式打卡，情绪价值 | P1 | 提升用户留存 |
| **通知推送** | 每日提醒、周复盘推送 | P1 | 主动触达用户 |
| **运动记录** | 训练组数 / 次数 / 重量记录 | P2 | 后续版本补全 |
| **关系感应** | 结合日程表（约会欺骗餐额度等） | P2 | 高阶个性化功能 |
| **环境感知** | 根据天气建议室内/室外训练 | P2 | 需接入天气 API |

---

## 4. 系统架构设计（CTO 视角）

### 4.1 整体架构图

```
┌──────────────────────────────────────────────────────┐
│                   CLIENT LAYER (Android)             │
│                                                      │
│             Android App (Kotlin + Compose)           │
│   ┌──────────────────────────────────────────────┐   │
│   │  UI: 热量进度条 + AI 聊天气泡 + 拍照入口     │   │
│   └──────────────┬───────────────────────────────┘   │
└──────────────────┼───────────────────────────────────┘
                   │ 
┌──────────────────▼───────────────────────────────────┐
│                  CORE SERVICE LAYER                  │
│   ┌──────────────────┐   ┌──────────────────────────┐ │
│   │  Vision Service  │   │    Agent Service         │ │
│   │  (Gemini API)    │   │  (Prompt Engineering +   │ │
│   │  多模态图像识别   │   │   Memory Retrieval)      │ │
│   └──────────────────┘   └──────────────────────────┘ │
│                                                      │
│   ┌──────────────────┐   ┌──────────────────────────┐ │
│   │  Calorie Engine  │   │   Schedule Engine        │ │
│   │  BMR/TDEE 计算   │   │   动态计划调整算法        │ │
│   └──────────────────┘   └──────────────────────────┘ │
└──────────────────────┬───────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────┐
│                    DATA LAYER                        │
│                                                      │
│  ┌─────────────────┐   ┌────────────────────────┐   │
│  │     Room DB     │   │    Memory & Logs       │   │
│  │ （用户/账本配置）│   │ （对话/笔记/食物历史）  │   │
│  └─────────────────┘   └────────────────────────┘   │
└──────────────────────────────────────────────────────┘
```

### 4.2 数据流（关键路径）

**拍照识别完整流程：**

```
用户拍照或选择相册
    │
    ▼
Android 图像按限度压缩并转 Base64 (协程 IO 线程)
    │
    ▼
GeminiApiService: /generateContent
    │
    ├─► 提供系统约束 Prompt (包含当日余额、当前用户信息)
    │
    ├─► 将图片+文字混合打包请求
    │
    ├─► 调用 Netlify Proxy 转发至真正的 Gemini API
    │
    └─► 剥离解释文本与动作标记 [FOOD_LOG]，落库后渲染气泡卡片
```

---

## 5. 技术栈选型（CTO 视角）

### 5.1 确定技术栈

| 层级 | 技术选型 | 选型理由 |
|---|---|---|
| **前端 / 移动端** | Android (Kotlin + Jetpack Compose) | 原生体验，极致性能；现代声明式 UI 构建速度快。 |
| **大语言模型** | Google Gemini 3.1 Flash API | 原生支持图像+文字多模态输入；响应速度快、成本低。开发期通过自定义 Netlify 代理转发。 |
| **本地数据库** | Room (SQLite 抽象层) | 离线优先架构（Local-First），安全管理个人隐私饮食数据，通过 Flow 响应式驱动 UI 更新。 |
| **持久层架构** | MVVM + Repository Pattern | 各层职责清晰，ViewModel 控制 UI 状态，Repository 连接远程与本地数据池。 |
| **HTTP 客户端** | OkHttp | 原生且成熟的网络库，处理超时的复杂请求和手动构造 JSON Body 更加轻量。 |
| **图片处理与加载** | Coil + Activity Result API | 原生支持读取相册与调用相机返回 URI，缩略与压缩操作完全本地进行，降低内存问题风险。 |

### 5.2 开发环境要求

| 阶段 | 工具链 | 备注 |
|---|---|---|
| **核心开发** | Android Studio Ladybug/Koala + Kotlin SDK | 需 Android SDK 33-35 支持 |
| **测试** | Android 模拟器或实体测试机 | 测试多模态交互要求网络能访问代理端 |

---

## 6. 数据库与数据模型（CTO 视角）

### 6.1 用户生理与目标表 `user_profile`

```sql
CREATE TABLE user_profile (
    user_id         VARCHAR(36)    PRIMARY KEY,  -- UUID
    name            VARCHAR(50)    NOT NULL,
    height_cm       INT            NOT NULL,
    initial_weight  DECIMAL(5,2)   NOT NULL,     -- kg，初始录入
    current_weight  DECIMAL(5,2)   NOT NULL,     -- kg，实时更新
    target_weight   DECIMAL(5,2)   NOT NULL,     -- kg，目标体重
    target_date     DATE           NOT NULL,     -- 目标达成日期
    activity_level  SMALLINT       NOT NULL DEFAULT 2,
                                                 -- 1=久坐 2=轻度 3=中度 4=重度
    base_bmr        DECIMAL(7,2),               -- 系统自动计算，Mifflin-St Jeor
    base_tdee       DECIMAL(7,2),               -- base_bmr × 活动系数
    created_at      TIMESTAMP      DEFAULT NOW(),
    updated_at      TIMESTAMP      DEFAULT NOW()
);
```

> **BMR 计算公式（Mifflin-St Jeor，男性）：**  
> `BMR = 10 × 体重(kg) + 6.25 × 身高(cm) − 5 × 年龄 + 5`

### 6.2 传统热量账本表 `daily_ledger`

```sql
CREATE TABLE daily_ledger (
    ledger_id          SERIAL         PRIMARY KEY,
    user_id            VARCHAR(36)    REFERENCES user_profile(user_id),
    date               DATE           NOT NULL,
    daily_budget       DECIMAL(7,2)   NOT NULL,   -- 当日总热量预算（TDEE - 缺口）
    consumed_calories  DECIMAL(7,2)   DEFAULT 0,  -- 已摄入热量（实时累加）
    burned_calories    DECIMAL(7,2)   DEFAULT 0,  -- 运动消耗热量
    net_remaining      DECIMAL(7,2)   GENERATED ALWAYS AS
                       (daily_budget - consumed_calories + burned_calories) STORED,
    UNIQUE(user_id, date)
);
```

### 6.3 饮食记录表 `food_log`

```sql
CREATE TABLE food_log (
    log_id           SERIAL         PRIMARY KEY,
    user_id          VARCHAR(36)    REFERENCES user_profile(user_id),
    ledger_id        INT            REFERENCES daily_ledger(ledger_id),
    meal_type        VARCHAR(10),                   -- breakfast/lunch/dinner/snack
    food_description TEXT           NOT NULL,        -- AI 识别的食物描述
    estimated_cal    DECIMAL(7,2)   NOT NULL,        -- 估算热量 (大卡)
    carbs_g          DECIMAL(6,2),                   -- 碳水 (克)
    protein_g        DECIMAL(6,2),                   -- 蛋白质 (克)
    fat_g            DECIMAL(6,2),                   -- 脂肪 (克)
    image_url        TEXT,                            -- 照片存储路径（可选）
    ai_comment       TEXT,                            -- AI 给出的个性化点评
    input_method     VARCHAR(10)    DEFAULT 'photo', -- photo/voice/text
    logged_at        TIMESTAMP      DEFAULT NOW()
);
```

---

## 7. AI 记忆系统架构（CTO 视角）

本系统参考 **OpenClaw Agent** 的多层记忆架构，实现真正有"经验"的 AI 助理。

### 7.1 三层记忆模型

```
┌────────────────────────────────────────────────────┐
│           LAYER 1 — 静态属性记忆                    │
│           Static Memory (Room DB)                  │
│                                                    │
│  • 用户生理信息（身高 / BMR / 目标日期）             │
│  • 更新频率：低（由用户主动修改或 AI 学习后写入）    │
└────────────────────────────────────────────────────┘
                          │
┌────────────────────────▼───────────────────────────┐
│           LAYER 2 — 短期工作区记忆                  │
│           Short-term Context (Room DB)             │
│                                                    │
│  • 当天对话历史（最近 20 轮）                       │
│  • 当日已摄入食物摘要与当日日记 (DailyNote)        │
│  • 生存周期：24 小时或随时间滚动清空                 │
└────────────────────────────────────────────────────┘
                          │
┌────────────────────────▼───────────────────────────┐
│           LAYER 3 — 长期经验记忆                    │
│           Long-term Memory (UserMemoryDao)         │
│                                                    │
│  • 归类文本存储：偏好(preference)/习惯(habit)       │
│  • 示例条目：                                       │
│    - "用户不吃香菜和冬瓜"                           │
│  • 每次 AI 响应后通过指令清洗和去重，自动积累        │
└────────────────────────────────────────────────────┘
```

### 7.2 Agent 检索逻辑（伪代码）

```python
class FitnessAgent:
    def __init__(self, user_id: str):
        self.profile     = PostgresDB.get_profile(user_id)    # Layer 1
        self.short_mem   = RedisCache.get_context(user_id)    # Layer 2
        self.vector_db   = ChromaDB(collection=user_id)       # Layer 3

    def generate_advice(self, user_query: str, meal_analysis: dict) -> str:
        # Step 1: 获取数值基准（传统算法）
        ledger        = PostgresDB.get_today_ledger(self.profile.user_id)
        remaining_cal = ledger.net_remaining

        # Step 2: 检索长期记忆（向量相似度搜索）
        relevant_memories = self.vector_db.query(
            query_texts=[user_query],
            n_results=3
        )

        # Step 3: 组装 LLM Prompt
        system_prompt = self._build_system_prompt(
            profile        = self.profile,
            remaining_cal  = remaining_cal,
            memories       = relevant_memories,
            meal_analysis  = meal_analysis,
            context        = self.short_mem
        )

        # Step 4: 调用大模型生成"人话"建议
        advice = gemini_client.generate(system_prompt + user_query)

        # Step 5: 将本次交互写入短期记忆，关键信息写入向量库
        self._update_memories(user_query, advice, meal_analysis)
        return advice

    def _build_system_prompt(self, **kwargs) -> str:
        return f"""
        你是杨弘的专属减脂助理，你非常了解他。
        【基础信息】身高 {kwargs['profile'].height_cm}cm，当前体重 {kwargs['profile'].current_weight}kg，
        目标日期 {kwargs['profile'].target_date}。
        【今日热量账本】今天还剩 {kwargs['remaining_cal']:.0f} 大卡可用。
        【关于他的记忆】{kwargs['memories']}
        【当前他的提问】{kwargs['context']}
        请用简洁、温暖、像朋友一样的语气给出建议。
        """
```

### 7.3 每周反思机制（Reflection Loop）

```
每周日 23:00 定时任务触发
    │
    ▼
计算本周实际摄入均值 vs 目标预算
    │
    ├─► 若目标进度达标（≥ 90%）→ 生成鼓励报告，存入 ChromaDB
    │
    └─► 若进度落后（< 90%）→ 计算下周调整预算，生成改进建议
                            → 存入 ChromaDB 并推送通知给用户
```

---

## 8. 核心 API 接口规范（CTO 视角）

**基础 URL**：`https://api.your-domain.com/api/v1`  
**认证方式**：`Bearer Token`（JWT）  
**内容类型**：`application/json`

---

### `POST /log_food` — 多模态饮食录入

**功能**：接收食物图片或文字描述，识别热量，自动写入账本并返回 AI 点评。

**请求体：**

```json
{
  "user_id":      "uuid-string",
  "image_base64": "data:image/jpeg;base64,...",  // 可选
  "text_desc":    "刚吃了两个包子，一碗稀饭",      // 可选，二者至少有一
  "meal_type":    "lunch"
}
```

**成功响应 `200`：**

```json
{
  "success":           true,
  "food_items": [
    { "name": "猪肉包子", "qty": 2, "est_cal": 320 },
    { "name": "白粥",     "qty": 1, "est_cal": 120 }
  ],
  "total_estimated_cal": 440,
  "macros": {
    "carbs_g":   65.0,
    "protein_g": 18.0,
    "fat_g":     9.0
  },
  "remaining_cal_today": 360,
  "ai_comment": "杨弘，这顿早饭约 440 大卡，刚好合适！今天还剩 360 大卡，晚餐可以吃一份清淡的鱼或鸡胸肉。"
}
```

---

### `GET /daily_sync?user_id={uid}&date={YYYY-MM-DD}` — 日度状态同步

**功能**：客户端打开时调用，获取今日热量账本全貌，用于渲染首屏进度条。

**成功响应 `200`：**

```json
{
  "date":              "2026-04-01",
  "daily_budget":      1850.0,
  "consumed_calories": 1200.0,
  "burned_calories":   200.0,
  "net_remaining":     850.0,
  "progress_pct":      64.9,
  "days_to_target":    90,
  "weekly_status":     "on_track"  // "on_track" | "behind" | "ahead"
}
```

---

### `POST /agent_chat` — AI 记忆交互

**功能**：接受用户自然语言提问，综合热量账本 + 长期记忆，返回个性化建议。

**请求体：**

```json
{
  "user_id": "uuid-string",
  "message": "晚上还能吃个汉堡吗？",
  "session_id": "optional-session-id"
}
```

**成功响应 `200`：**

```json
{
  "reply": "虽然今天预算只剩 300 大卡，但因为昨天你多消耗了 300，这周前三天表现完美，我帮你借了点'信用额度'。去吃吧，但明天记得恢复清淡！",
  "action_type": "allowance_granted",
  "updated_remaining": 300
}
```

---

## 9. 动态计划调整算法（CTO 视角）

### 9.1 日度热量预算计算

```python
def calculate_daily_budget(profile: UserProfile, date: date) -> float:
    """
    每日动态热量预算
    = TDEE - 减脂缺口 + 运动额外消耗（前一天由传感器/输入获取）
    """
    days_remaining  = (profile.target_date - date).days
    weight_to_lose  = profile.current_weight - profile.target_weight
    weekly_deficit  = (weight_to_lose * 7700) / (days_remaining / 7)
    daily_deficit   = min(weekly_deficit / 7, 750)  # 上限 750 大卡，防止过激

    return profile.base_tdee - daily_deficit
```

### 9.2 进度追踪与自动调整

```python
def check_and_adjust(user_id: str) -> AdjustmentResult:
    """
    每日定时任务：检测连续超标并触发调整
    """
    recent_3_days   = get_recent_ledger(user_id, days=3)
    overage_days    = sum(1 for d in recent_3_days if d.consumed_calories > d.daily_budget * 1.05)

    if overage_days >= 3:
        # 连续 3 天超标，下周每日预算减少 100-200 大卡
        reduction = min(overage_days * 50, 200)
        new_budget = calculate_daily_budget(...) - reduction
        send_agent_notification(
            user_id = user_id,
            message = f"检测到近 3 天超标，下周每日预算调整为 {new_budget:.0f} 大卡。"
                      f"我们按计划每天再减 {reduction:.0f} 大卡，6 月底还来得及！"
        )
        return AdjustmentResult(adjusted=True, new_budget=new_budget)

    return AdjustmentResult(adjusted=False)
```

---

## 10. 开发路线图与交付里程碑

```
Timeline ─────────────────────────────────────────────────────────────────►

Week 1-2         Week 3-4          Week 5-6          Week 7-8        Week 9+
   │                 │                 │                 │               │
   ▼                 ▼                 ▼                 ▼               ▼
┌──────────┐   ┌──────────┐   ┌──────────────┐   ┌──────────┐   ┌──────────┐
│ 阶段一   │   │ 阶段二   │   │  阶段三      │   │ 阶段四   │   │ 阶段五   │
│  基础建设 │   │ AI 接入  │   │  记忆系统    │   │ Web 联调 │   │ Android  │
│          │   │          │   │              │   │          │   │  打包    │
│ FastAPI  │   │ Gemini   │   │  ChromaDB    │   │ Flutter  │   │ APK 发布 │
│ 骨架搭建  │   │ 多模态   │   │  向量存储    │   │ Web 端   │   │ App 上架 │
│ SQLite/  │   │ API 接入  │   │  Agent 类   │   │ 完整跑通  │   │ 测试打包 │
│ PG 初始化 │   │ /log_food│   │ /agent_chat │   │          │   │          │
│ BMR/TDEE │   │ 测试     │   │  周反思任务  │   │          │   │          │
└──────────┘   └──────────┘   └──────────────┘   └──────────┘   └──────────┘
   ✅ MVP           ✅ AI            ✅ Memory         ✅ Web          📱 App
   Backend        Vision           Brain            Launch         Launch
```

### 交付物清单

| 里程碑 | 交付物 | 验证方式 |
|---|---|---|
| 阶段一 | FastAPI 项目骨架 + DB Schema + BMR 计算工具类 | `pytest` 单元测试通过 |
| 阶段二 | Gemini 图像识别服务 + `/log_food` 接口 | Postman 上传图片返回正确 JSON |
| 阶段三 | ChromaDB 向量存储 + Agent 类 + `/agent_chat` 接口 | 记忆检索准确率 > 80% |
| 阶段四 | Flutter Web 端完整 UI + 前后端联调 | Chrome 浏览器跑通全流程演示 |
| 阶段五 | Android `.apk` 打包 + 手机真机测试 | 真机拍照→识别→建议 全流程 ≤ 5s |

---

## 11. 风险评估与缓解策略

| 风险 | 等级 | 缓解策略 |
|---|---|---|
| **API 调用安全性(裸奔)** | 🔴 高 | 本地使用代理地址方便联调，但未来应允许配置自定义 API Key 保护请求流量 |
| **热量估算不准确影响用户信任** | 🟡 中 | 在 UI 中明标"AI 估算值，仅供参考"；支持用户手动修正，修正值存入记忆 |
| **手机相册图片选取导致 OOM (内存溢出)** | 🔴 高 | 使用协程在 IO 线程加载 Bitmap 并严格缩放至 1024 像素以下才准转 Base64 |
| **多模态 JSON 解析脆弱性** | 🔴 高 | 对大模型的任意输出正则提取 `\{.*\}` 与针对 Markdown Code Block 去皮处理 |
| **用户隐私（饮食照片、健康数据）** | 🔴 高 | 优先纯本地 Room 数据库存储与执行管理，脱机网络也可保护隐私 |

---

## 12. 附录：名词解释

| 术语 | 解释 |
|---|---|
| **BMR** | 基础代谢率（Basal Metabolic Rate），人体在完全静止状态下每日消耗的最低热量 |
| **TDEE** | 每日总消耗（Total Daily Energy Expenditure），结合活动量后的实际总消耗热量 |
| **热量缺口** | TDEE - 实际摄入热量；每缺口 7700 大卡约减脂 1 kg |
| **多模态** | AI 同时处理多种类型输入（图像 + 文字）的能力 |
| **Jetpack Compose** | Google 开发的现代原生声明式 UI 工具包，简化 Android 界面开发 |
| **Coroutine/Flow** | Kotlin 异步编程的基石，完美搭配 Room 获取本地库的实时变化通知 |
| **Room DB** | Android Jetpack 库中封装良好的 SQLite 数据库客户端组件 |

---

*文档由 AI 辅助生成 · 最终解释权归项目负责人所有*  
*Technical Specification v1.1.0 · 2026-04-04*
