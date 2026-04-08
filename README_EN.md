<div align="center">

# 🥗 BaoBao FatLoss — Intelligent AI Weight Loss Assistant

**[English](./README_EN.md) | [中文](./README.md)**

**An AI private coach with long-term memory — It doesn't just calculate; it truly knows you.**

[![Android](https://img.shields.io/badge/Platform-Android%2024%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com)
[![LLM](https://img.shields.io/badge/AI-OpenAI%20Compatible%20API-412991?logo=openai&logoColor=white)](https://platform.openai.com)
[![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)

[Demo](#-core-features) · [Architecture](#-system-architecture) · [Quick Start](#-quick-start) · [Tech Stack](#-tech-stack)

</div>

---

## 📖 Introduction

**BaoBao FatLoss** (饱饱减脂) is a **native Android AI weight loss assistant** that integrates multimodal AI, a three-layer memory system, and traditional nutritional algorithms to achieve an intelligent experience far beyond traditional health apps.

Unlike typical health apps, the core innovations of this project are:

| Traditional Health App Pain Points | Our Solution |
|---|---|
| Manual food search, tedious entry | ✅ **Multimodal Photo/Text Recognition**, AI automatically analyzes nutrients |
| Starts from scratch every time, no memory | ✅ **Three-Layer Memory Architecture**, the AI truly "knows" and remembers you |
| Rigid plans, unaware of user status | ✅ **Dynamic Plan Adjustment**, automatically calculates daily calorie budgets |
| Limited to specific AI providers | ✅ **OpenAI Compatible API**, supports Doubao, DeepSeek, etc. |

---

## ✨ Core Features

### 🤖 AI Intelligent Chat (Core)
- **Context-Aware Dialogue**: AI provides personalized replies by combining the daily calorie ledger, user profile, and historical memory.
- **Structured Instruction Parsing**: Automatically identifies and executes Agent actions like `[FOOD_LOG]`, `[EXERCISE]`, `[DELETE_FOOD]`, and `[MEMORY_ACTION]` from AI replies.
- **Multimodal Food Recognition**: Send food photos directly in chat; AI estimates calories and logs them.

### 📸 Food Photo Recognition
- Dedicated photo/gallery entry optimized for diet logging.
- Automatic image compression (≤ 1280px) to prevent OOM, safely converted to Base64 for LLM.
- AI returns structured JSON (food name, calories, macros) with regex fallback parsing.

### 📊 Diet Logging & Calorie Management
- Categorized management (Breakfast/Lunch/Dinner/Snack) with date-based history browsing.
- Fuzzy text input + AI analysis (input "a bowl of rice noodles" to get full nutritional estimates).
- Real-time calorie tracking (Consumed / Burned / Remaining).

### 🧠 Three-Layer AI Memory System (OpenClaw Architecture)
```
Layer 1 — Static Attribute Memory: Physiological info, target weight, AI persona.
Layer 2 — Short-term Workspace: Recent 15 dialogue rounds + daily summary.
Layer 3 — Long-term Experience Memory: Semantic retrieval via Vector Embeddings (Cosine Similarity ≥ 0.70).
```
The AI actively extracts memory (e.g., "User doesn't eat cilantro") and performs CRUD operations via `MEMORY_ACTION` commands.

### ⚙️ Flexible AI Configuration
- Supports any **OpenAI Compatible** provider (Doubao, DeepSeek, local Ollama, etc.).
- API Keys stored with **AES-256-GCM encryption** (`EncryptedSharedPreferences`).

---

## 🏗️ System Architecture

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
│  │ (Agent Core)  │   │ (Memory Sys) │   │  Engine  │  │
│  └──────┬───────┘   └──────────────┘   └──────────┘  │
│         │ DoubaoApiService (OkHttp)                   │
│         │ → OpenAI Chat Completions Format            │
│         │ → Embeddings (RAG Retrieval)                │
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

---

## 🚀 Quick Start

### Prerequisites

- Android Studio Ladybug or newer
- JDK 21
- Android SDK 35 (minSdk 24)

### Get an AI API Key

This app supports any API provider compatible with the OpenAI Chat Completions format. Recommended:

- **Doubao (Volcengine)**: [https://www.volcengine.com/product/ark](https://www.volcengine.com/product/ark)
- **DeepSeek**: [https://platform.deepseek.com](https://platform.deepseek.com)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/12561257/Baobao_OpenSource.git
   cd Baobao_OpenSource
   ```
2. **Open with Android Studio**
3. **Build & Run**
   ```bash
   ./gradlew assembleDebug
   ```

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit an Issue or Pull Request.

---

## 📄 License

This project is licensed under the GPL-3.0 License - see the [LICENSE](LICENSE) file for details.

---

<div align="center">

**If this project helps you, please give it a ⭐ Star!**

Made with ❤️ by Yang Hong · Powered by AI-Assisted Development

</div>
