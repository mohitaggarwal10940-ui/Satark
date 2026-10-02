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

## 🛠️ Backend Integration Guide (For @1amol2)

The Android frontend is fully configured to talk to your backend service via Retrofit.

### 1. Base URL Configuration
In `app/src/main/java/com/dev/satark/data/remote/RetrofitClient.kt`:
```kotlin
// Android Emulator pointing to your local machine:
const val EMULATOR_BASE_URL = "http://10.0.2.2:8000/"

// Physical Android device over local Wi-Fi:
const val PHYSICAL_DEVICE_BASE_URL = "http://<YOUR_LOCAL_IP>:8000/"
```

### 2. API Contract

#### **Endpoint**: `POST /api/analyze`

#### **Request Body** (`application/json`):
```json
{
  "inputType": "TEXT",
  "text": "Join our VIP group for guaranteed 30% monthly returns! SEBI registered advisor. Limited slots.",
  "language": "hi"
}
```

*Fields*:
- `inputType`: `"TEXT"` or `"SCREENSHOT"`
- `text`: Raw text extracted via OCR, voice dictation, or pasted by user
- `language`: Target language code (`"en"`, `"hi"`, `"ta"`, `"te"`, `"bn"`, `"mr"`, `"gu"`, `"kn"`)

#### **Expected Response Body** (`application/json`):
```json
{
  "analysisId": "satark-829103",
  "riskScore": 85,
  "riskLevel": "VERY_HIGH",
  "claims": [
    {
      "claimText": "SEBI registered advisor",
      "category": "Registration Claim"
    },
    {
      "claimText": "30% monthly returns guaranteed",
      "category": "Guaranteed Return Claim"
    }
  ],
  "riskSignals": [
    {
      "title": "Unrealistic Guaranteed Returns",
      "description": "Promises fixed high returns which violate SEBI regulations.",
      "severity": "CRITICAL"
    },
    {
      "title": "Pressure & Urgency Tactics",
      "description": "Urges immediate deposit and transfer of funds.",
      "severity": "HIGH"
    }
  ],
  "evidence": [
    {
      "claim": "Registration Claim",
      "status": "UNVERIFIED",
      "details": "Not listed on official SEBI intermediary registers.",
      "sourceUrl": "https://www.sebi.gov.in"
    }
  ],
  "explanation": "This message displays severe red flags consistent with unauthorized investment advisories and unregistered schemes.",
  "recommendedActions": [
    "Do not transfer or send any money.",
    "Verify any advisory directly on SEBI's official portal.",
    "Report fraudulent communications to the Cyber Crime Helpline at 1930."
  ]
}
```

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
