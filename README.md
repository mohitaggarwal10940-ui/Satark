# SATARK (सतर्क)
### Multilingual Investor Scam & Financial Fraud Safety Assistant

SATARK is a full-stack fraud detection system designed to protect investors and everyday citizens from financial fraud, unregistered advisory schemes, fake SEBI registration claims, and high-risk investment messages across SMS, WhatsApp, Telegram, and social platforms.

---

## 📁 Repository Structure
```
Satark/
├── android/            # Native Android Client (Jetpack Compose + Material 3)
│   ├── app/            # Android Application module
│   └── gradle/         # Gradle version catalogs & wrappers
└── backend/            # Spring Boot REST API & Analysis Engine
    ├── src/main/java/  # Controllers, Services, DTOs & Models
    └── src/main/resources/
```

---

## 🚀 Key Features
- **On-Device OCR (ML Kit)**: Extracts financial claims and promotional text from uploaded screenshots and camera photos offline.
- **Multilingual Voice Input (Speech-to-Text)**: Allows users to speak suspicious messages in **8 regional Indian languages** (English, Hindi, Tamil, Telugu, Bengali, Marathi, Gujarati, and Kannada).
- **Voice Safety Readouts (Text-to-Speech)**: Audio explanation read aloud in the user's chosen regional language.
- **Concern Rating & Risk Signals**: Clear 0–100 concern score with broken-down claims, warning badges (Urgency, Guaranteed Returns, Impersonation), and official regulatory links.
- **Zero AI Glitches / Production Architecture**: Clean architecture with Kotlin Coroutines, StateFlow, Retrofit 2, Spring Boot backend, and Material 3 design.

---

## 🛠️ Backend Architecture (Lead: @1amol2)
The backend is built with **Spring Boot** located under `backend/`.
- **Base Endpoint**: `POST /api/analyze`
- **Request DTO** (`AnalysisRequest`): `inputType`, `text`, `language`
- **Response DTO** (`AnalysisResponse`): `analysisId`, `riskScore`, `riskLevel`, `claims`, `riskSignals`, `evidence`, `explanation`, `recommendedActions`

---

## 🧠 AI, NLP & LLM Risk Analysis Engine (Lead: @Bivan11-tech)
The AI/NLP engine processes suspicious messages through the following stages:

### 1. NLP Claim Extraction Layer
Extract structured, falsifiable claims from the input text:
- **Registration & Regulatory Claims**: E.g., *"SEBI registered"*, *"Govt approved"*, *"RBI licensed partner"*.
- **Guaranteed Returns & Profits**: E.g., *"30% monthly return"*, *"100% risk-free profits"*, *"Double your money in 15 days"*.
- **Urgency & Exclusivity Tactics**: E.g., *"Only 3 spots left"*, *"Transfer within 10 minutes"*, *"VIP insider leak"*.

### 2. Risk Scoring & Concern Calibration
- **Concern Score (0–100)**: Formulated as an independent risk/concern score based on scam heuristics and deceptive patterns (never framed as a probability percentage).
- **Risk Levels**:
  - `80–100`: `VERY_HIGH` (Critical red flags like guaranteed returns + unverified credentials + urgency)
  - `60–79`: `HIGH` (Significant unverified regulatory claims or pressure tactics)
  - `35–59`: `MEDIUM` (Ambiguous promotions or missing required disclosures)
  - `0–34`: `LOW` (Standard informational or verified communications)

### 3. Multilingual LLM Output
- The LLM prompts must generate the `explanation`, `warningSignals`, and `recommendedActions` in the target regional language requested (`language` field: `en`, `hi`, `ta`, `te`, `bn`, `mr`, `gu`, `kn`).
- Ensure native phrasing aligns with Indian financial fraud terminology (e.g. Cyber Crime Helpline 1930, official SEBI SCORES portal).

---

## 📱 Tech Stack
- **Client**: Android (Kotlin, Jetpack Compose, Material 3, minSdk 24, targetSdk 35)
- **Backend**: Java / Spring Boot, Maven/Gradle, REST APIs
- **AI / NLP**: LLM Claim Extraction, Heuristic Risk Evaluation & Prompt Engineering
- **OCR Engine**: Google ML Kit Text Recognition (`play-services-mlkit-text-recognition`)
- **Speech & Audio**: Android Native `RecognizerIntent` & `TextToSpeech` (8 Indian Languages)

---

## 👥 Contributors & Core Team
- **Mohit Aggarwal** ([@mohitaggarwal10940-ui](https://github.com/mohitaggarwal10940-ui)) - Project Lead & Android Client Engineering
- **Amol** ([@1amol2](https://github.com/1amol2)) - Backend Architecture, Database & REST API Infrastructure
- **Bivan** ([@Bivan11-tech](https://github.com/Bivan11-tech)) - AI, NLP, LLMs & Financial Risk Analysis Engine
