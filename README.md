# SATARK (सतर्क)
### Multilingual Investor Scam & Financial Fraud Safety Assistant for Android

SATARK is a native Android application built with Jetpack Compose designed to protect investors and everyday citizens from financial fraud, unregistered advisory schemes, fake SEBI registration claims, and high-risk investment messages across SMS, WhatsApp, Telegram, and social platforms.

---

## 🚀 Key Features
- **On-Device OCR (ML Kit)**: Extracts financial claims and promotional text from uploaded screenshots and camera photos offline.
- **Multilingual Voice Input (Speech-to-Text)**: Allows users to speak suspicious messages in **8 regional Indian languages** (English, Hindi, Tamil, Telugu, Bengali, Marathi, Gujarati, and Kannada).
- **Voice Safety Readouts (Text-to-Speech)**: Audio explanation read aloud in the user's chosen regional language.
- **Concern Rating & Risk Signals**: Clear 0–100 concern score with broken-down claims, warning badges (Urgency, Guaranteed Returns, Impersonation), and official regulatory links.
- **Zero AI Glitches / Production Architecture**: Follows standard Android MVVM with Kotlin Coroutines, StateFlow, Retrofit 2, and Material 3 design.

---



---

## 📱 Tech Stack
- **OS**: Android (minSdk 24, targetSdk 35)
- **UI**: Jetpack Compose + Material 3
- **Networking**: Retrofit 2 + OkHttp 3 + Gson
- **OCR Engine**: Google ML Kit Text Recognition (`play-services-mlkit-text-recognition`)
- **Speech & Audio**: Android Native `RecognizerIntent` & `TextToSpeech`
- **Architecture**: Clean MVVM with Repository Pattern & Kotlin Coroutines

---

## 👥 Contributors & Maintainers
- **Mohit Aggarwal** ([@mohitaggarwal10940-ui](https://github.com/mohitaggarwal10940-ui)) - Frontend & Android Engineering
- **Amol** ([@1amol2](https://github.com/1amol2)) - Backend Architecture & Analysis API
