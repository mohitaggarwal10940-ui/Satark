package com.dev.satark.data.repository

import com.dev.satark.data.model.AnalysisResponse
import com.dev.satark.data.model.AnalyzeRequest
import com.dev.satark.data.model.Claim
import com.dev.satark.data.model.Evidence
import com.dev.satark.data.model.RiskSignal
import com.dev.satark.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AnalysisRepository(
    private val useMockDataSource: Boolean = false
) {

    companion object {
        fun createMockResponse(
            inputPreview: String = "",
            languageCode: String = "en",
            isHindi: Boolean = false
        ): AnalysisResponse {
            val id = "satark-" + System.currentTimeMillis().toString().takeLast(6)
            val lang = if (isHindi) "hi" else languageCode.lowercase()

            return when (lang) {
                "hi" -> AnalysisResponse(
                    analysisId = id,
                    riskScore = 82,
                    riskLevel = "VERY_HIGH",
                    claims = listOf(
                        Claim(claimText = "सेबी पंजीकृत सलाहकार", category = "पंजीकरण दावा"),
                        Claim(claimText = "30% मासिक रिटर्न की गारंटी", category = "गारंटीड-रिटर्न दावा")
                    ),
                    riskSignals = listOf(
                        RiskSignal(title = "गारंटीड रिटर्न", description = "संदेश अवास्तविक निश्चित रिटर्न का वादा करता है।", severity = "CRITICAL"),
                        RiskSignal(title = "भुगतान अनुरोध", description = "संदेश आपसे तुरंत धन हस्तांतरित करने को कहता है।", severity = "HIGH"),
                        RiskSignal(title = "जल्दबाजी व दबाव", description = "संदेश बिना सोचे-समझे तुरंत निर्णय लेने का दबाव बनाता है।", severity = "MEDIUM")
                    ),
                    evidence = listOf(
                        Evidence(
                            claim = "पंजीकरण दावा",
                            status = "UNVERIFIED",
                            details = "आधिकारिक सेबी रिकॉर्ड में इस सलाहकार के पंजीकरण की पुष्टि के लिए उपलब्ध जानकारी अपर्याप्त थी।",
                            sourceUrl = "https://www.sebi.gov.in"
                        )
                    ),
                    explanation = "इस संदेश में अपंजीकृत निवेश योजनाओं में पाए जाने वाले कई चेतावनी संकेत हैं, जिनमें असत्यापित नियामक साख, तुरंत पैसे भेजने का दबाव और अवास्तविक गारंटीड रिटर्न के वादे शामिल हैं।",
                    recommendedActions = listOf(
                        "स्वतंत्र रूप से सत्यापित होने तक कोई भी धनराशि ट्रांसफर न करें।",
                        "मूल संदेश, स्क्रीनशॉट, यूपीआई आईडी और बैंक विवरण सुरक्षित रखें।",
                        "आधिकारिक सेबी वेबसाइट पर जाकर मध्यस्थ का पंजीकरण नंबर अवश्य जांचें।"
                    )
                )

                "ta" -> AnalysisResponse(
                    analysisId = id,
                    riskScore = 82,
                    riskLevel = "VERY_HIGH",
                    claims = listOf(
                        Claim(claimText = "செபி பதிவு பெற்ற ஆலோசகர்", category = "பதிவு கோரிக்கை"),
                        Claim(claimText = "மாதந்தோறும் 30% உத்தரவாத லாபம்", category = "உத்தரவாத வருமான கோரிக்கை")
                    ),
                    riskSignals = listOf(
                        RiskSignal(title = "உத்தரவாத வருமானம்", description = "செய்தி உத்தரவாதமான அசாதாரண லாபத்தை உறுதியளிக்கிறது.", severity = "CRITICAL"),
                        RiskSignal(title = "பண பரிமாற்ற கோரிக்கை", description = "உடனடியாக பணம் மாற்றுமாறு கோருகிறது.", severity = "HIGH"),
                        RiskSignal(title = "அவசர அழுத்தம்", description = "சிந்திக்க நேரமின்ற உடனடியாக முடிவெடுக்க அழுத்தம் தருகிறது.", severity = "MEDIUM")
                    ),
                    evidence = listOf(
                        Evidence(
                            claim = "பதிவு கோரிக்கை",
                            status = "UNVERIFIED",
                            details = "செபி அதிகாரப்பூர்வ பதிவேடுகளில் இந்த ஆலோசகரின் பதிவை உறுதிப்படுத்த போதிய ஆதாரங்கள் இல்லை.",
                            sourceUrl = "https://www.sebi.gov.in"
                        )
                    ),
                    explanation = "இந்த செய்தியில் பதிவு செய்யப்படாத போலி முதலீட்டு மோசடிகளில் காணப்படும் பல முக்கிய எச்சரிக்கை சமிக்ஞைகள் உள்ளன, இதில் அங்கீகரிக்கப்படாத உரிமைகோரல்கள் மற்றும் சாத்தியமற்ற உத்தரவாத லாபங்கள் அடங்கும்.",
                    recommendedActions = listOf(
                        "முழுமையாக சரிபார்க்கும் வரை எந்த பணப் பரிவர்த்தனையும் செய்யாதீர்கள்.",
                        "அசல் செய்தி, ஸ்கிரீன்ஷாட், யுபிஐ ஐடி மற்றும் வங்கி விவரங்களை பாதுகாப்பாக சேமிக்கவும்.",
                        "அங்கீகரிக்கப்பட்ட செபி போர்ட்டல் மூலம் ஆலோசகரின் பதிவு எண்ணை உறுதிப்படுத்தவும்."
                    )
                )

                "te" -> AnalysisResponse(
                    analysisId = id,
                    riskScore = 82,
                    riskLevel = "VERY_HIGH",
                    claims = listOf(
                        Claim(claimText = "సెబి నమోదిత సలహాదారు", category = "నమోదు దావా"),
                        Claim(claimText = "నెలకు 30% హామీ రాబడి", category = "హామీ రాబడి దావా")
                    ),
                    riskSignals = listOf(
                        RiskSignal(title = "హామీ రాబడి", description = "సందేశం అవాస్తవ స్థిర లాభాలను వాగ్దానం చేస్తుంది.", severity = "CRITICAL"),
                        RiskSignal(title = "చెల్లింపు అభ్యర్థన", description = "వెంటనే డబ్బు బదిలీ చేయాలని కోరుతోంది.", severity = "HIGH"),
                        RiskSignal(title = "అత్యవసర ఒత్తిడి", description = "ఆలోచించే సమయం లేకుండా తక్షణ నిర్ణయం తీసుకోవాలని ఒత్తిడి చేస్తోంది.", severity = "MEDIUM")
                    ),
                    evidence = listOf(
                        Evidence(
                            claim = "నమోదు దావా",
                            status = "UNVERIFIED",
                            details = "అధికారిక సెబి రికార్డులలో ఈ సలహాదారు నమోదును ధృవీకరించడానికి ఆధారాలు లేవు.",
                            sourceUrl = "https://www.sebi.gov.in"
                        )
                    ),
                    explanation = "ఈ సందేశంలో నమోదుకాని పెట్టుబడి మోసాలలో కనిపించే తీవ్రమైన హెచ్చరిక సంకేతాలు ఉన్నాయి, వీటిలో ధృవీకరించబడని నియంత్రణ ఆధారాలు మరియు అవాస్తవ హామీ లాభాలు ఉన్నాయి.",
                    recommendedActions = listOf(
                        "పూర్తిగా ధృవీకరించబడే వరకు ఎటువంటి డబ్బును బదిలీ చేయవద్దు.",
                        "అసలు సందేశం, స్క్రీన్ షాట్, యుపిఐ ఐడి మరియు చెల్లింపు వివరాలను భద్రపరుచుకోండి.",
                        "అధికారిక సెబి వెబ్‌సైట్ ద్వారా సలహాదారు గుర్తింపును స్వయంగా తనిఖీ చేయండి."
                    )
                )

                "bn" -> AnalysisResponse(
                    analysisId = id,
                    riskScore = 82,
                    riskLevel = "VERY_HIGH",
                    claims = listOf(
                        Claim(claimText = "সেবি নিবন্ধিত উপদেষ্টা", category = "নিবন্ধন দাবি"),
                        Claim(claimText = "প্রতি মাসে ৩০% নিশ্চিত রিটার্ন", category = "নিশ্চিত রিটার্ন দাবি")
                    ),
                    riskSignals = listOf(
                        RiskSignal(title = "নিশ্চিত রিটার্ন", description = "বার্তাটিতে অবাস্তব স্থির মুনাফার প্রতিশ্রুতি দেওয়া হয়েছে।", severity = "CRITICAL"),
                        RiskSignal(title = "অর্থ প্রদানের অনুরোধ", description = "অবিলম্বে টাকা পাঠানোর জন্য বলা হচ্ছে।", severity = "HIGH"),
                        RiskSignal(title = "জরুরি তাগিদ ও চাপ", description = "চিন্তা করার সুযোগ না দিয়ে দ্রুত পদক্ষেপ নিতে চাপ সৃষ্টি করছে।", severity = "MEDIUM")
                    ),
                    evidence = listOf(
                        Evidence(
                            claim = "নিবন্ধন দাবি",
                            status = "UNVERIFIED",
                            details = "অফিসিয়াল সেবি রেকর্ডে এই উপদেষ্টার নিবন্ধনের বৈধতা যাচাই করার মতো পর্যাপ্ত তথ্য নেই।",
                            sourceUrl = "https://www.sebi.gov.in"
                        )
                    ),
                    explanation = "এই বার্তায় অনিবন্ধিত জাল বিনিয়োগ স্কিমগুলিতে সাধারণত পাওয়া যায় এমন একাধিক বিপজ্জনক সতর্কতা সংকেত রয়েছে।",
                    recommendedActions = listOf(
                        "স্বাধীনভাবে যাচাই না করা পর্যন্ত কোনও টাকা পাঠাবেন না।",
                        "মূল বার্তা, স্ক্রিনশট, ইউপিআই বিবরণ এবং প্রমাণাদি সংরক্ষণ করুন।",
                        "সেবির অফিসিয়াল পোর্টালের মাধ্যমে উপদেষ্টার নিবন্ধন নিশ্চিত করুন।"
                    )
                )

                "mr" -> AnalysisResponse(
                    analysisId = id,
                    riskScore = 82,
                    riskLevel = "VERY_HIGH",
                    claims = listOf(
                        Claim(claimText = "सेबी नोंदणीकृत सल्लागार", category = "नोंदणी दावा"),
                        Claim(claimText = "दरमहा ३०% हमी परतावा", category = "हमी परतावा दावा")
                    ),
                    riskSignals = listOf(
                        RiskSignal(title = "हमी परतावा", description = "संदेश अवास्तविक निश्चित नफ्याचे आश्वासन देतो.", severity = "CRITICAL"),
                        RiskSignal(title = "पैसे पाठवण्याची विनंती", description = "लगेच खात्यावर पैसे पाठवण्यास सांगत आहे.", severity = "HIGH"),
                        RiskSignal(title = "तातडीचा दबाव", description = "विचार न करता त्वरित निर्णय घेण्याचा दबाव निर्माण करत आहे.", severity = "MEDIUM")
                    ),
                    evidence = listOf(
                        Evidence(
                            claim = "नोंदणी दावा",
                            status = "UNVERIFIED",
                            details = "अधिकृत सेबी रेकॉर्डमध्ये या सल्लागाराच्या वैधतेची खात्री करण्यासाठी पुरेशी माहिती उपलब्ध नाही.",
                            sourceUrl = "https://www.sebi.gov.in"
                        )
                    ),
                    explanation = "या संदेशात फसव्या गुंतवणूक योजनांमध्ये आढळणारे अनेक गंभीर धोक्याचे संकेत आहेत, ज्यामध्ये अनधिकृत दावे आणि हमी नफ्याचे आमिष दाखवले आहे.",
                    recommendedActions = listOf(
                        "अधिकृत पडताळणी होईपर्यंत कोणत्याही खात्यावर पैसे पाठवू नका.",
                        "मूळ मेसेज, स्क्रीनशॉट, युपीआय आयडी आणि पुरावे सुरक्षित ठेवा.",
                        "सेबीच्या अधिकृत संकेतस्थळावर जाऊन नोंदणी क्रमांकाची स्वतः खात्री करा."
                    )
                )

                "gu" -> AnalysisResponse(
                    analysisId = id,
                    riskScore = 82,
                    riskLevel = "VERY_HIGH",
                    claims = listOf(
                        Claim(claimText = "સેબી રજિસ્ટર્ડ સલાહકાર", category = "નોંધણી દાવો"),
                        Claim(claimText = "દર મહિને 30% ગેરંટીડ રિટર્ન", category = "ગેરંટીડ રિટર્ન દાવો")
                    ),
                    riskSignals = listOf(
                        RiskSignal(title = "ગેરંટીડ રિટર્ન", description = "આ સંદેશ અવાસ્તવિક નિશ્ચિત વળતરનું વચન આપે છે.", severity = "CRITICAL"),
                        RiskSignal(title = "પેમેન્ટ ટ્રાન્સફર વિનંતી", description = "તરત જ નાણાં મોકલવા માટે દબાણ કરે છે.", severity = "HIGH"),
                        RiskSignal(title = "ઉતાવળ અને દબાણ", description = "વિચારવાનો સમય આપ્યા વિના નિર્ણય લેવા દબાણ કરે છે.", severity = "MEDIUM")
                    ),
                    evidence = listOf(
                        Evidence(
                            claim = "નોંધણી દાવો",
                            status = "UNVERIFIED",
                            details = "સત્તાવાર સેબી રેકોર્ડમાં આ સલાહકારની નોંધણીની પુષ્ટિ કરવા માટે યોગ્ય પુરાવા નથી.",
                            sourceUrl = "https://www.sebi.gov.in"
                        )
                    ),
                    explanation = "આ સંદેશમાં અનરજિસ્ટર્ડ રોકાણ છેતરપિંડીઓમાં જોવા મળતા બહુવિધ ગંભીર ચેતવણી સંકેતો છે, જેમાં ખોટી ઓળખ અને અવાસ્તવિક વળતરનો સમાવેશ થાય છે.",
                    recommendedActions = listOf(
                        "સ્વતંત્ર ચકાસણી ન થાય ત્યાં સુધી કોઈ પૈસા ટ્રાન્સફર કરશો નહીં.",
                        "મૂળ સંદેશ, સ્ક્રીનશૉટ, યુપીઆઈ આઈડી અને ચૂકવણી વિગતો સાચવી રાખો.",
                        "સત્તાવાર સેબી પોર્ટલ દ્વારા સલાહકારની નોંધણી ચકાસો."
                    )
                )

                "kn" -> AnalysisResponse(
                    analysisId = id,
                    riskScore = 82,
                    riskLevel = "VERY_HIGH",
                    claims = listOf(
                        Claim(claimText = "ಸೆಬಿ ನೋಂದಾಯಿತ ಸಲಹೆಗಾರ", category = "ನೋಂದಣಿ ಹಕ್ಕು"),
                        Claim(claimText = "ತಿಂಗಳಿಗೆ 30% ಖಾತರಿಯ ಲಾಭ", category = "ಖಾತರಿಯ ಲಾಭದ ಹಕ್ಕು")
                    ),
                    riskSignals = listOf(
                        RiskSignal(title = "ಖಾತರಿಯ ಲಾಭ", description = "ಸಂದೇಶವು ಅವಾಸ್ತವಿಕ ಸ್ಥಿರ ಲಾಭವನ್ನು ಭರವಸೆ ನೀಡುತ್ತದೆ.", severity = "CRITICAL"),
                        RiskSignal(title = "ಹಣ ವರ್ಗಾವಣೆ ವಿನಂತಿ", description = "ತಕ್ಷಣವೇ ಹಣವನ್ನು ವರ್ಗಾಯಿಸಲು ಕೇಳುತ್ತದೆ.", severity = "HIGH"),
                        RiskSignal(title = "ತುರ್ತು ಒತ್ತಡ", description = "ಯೋಚಿಸಲು ಸಮಯ ನೀಡದೆ ತಕ್ಷಣದ ನಿರ್ಧಾರ ತೆಗೆದುಕೊಳ್ಳಲು ಒತ್ತಡ ಹೇರುತ್ತದೆ.", severity = "MEDIUM")
                    ),
                    evidence = listOf(
                        Evidence(
                            claim = "ನೋಂದಣಿ ಹಕ್ಕು",
                            status = "UNVERIFIED",
                            details = "ಅಧಿಕೃತ ಸೆಬಿ ದಾಖಲೆಗಳಲ್ಲಿ ಈ ಸಲಹೆಗಾರರ ನೋಂದಣಿಯನ್ನು ದೃಢೀಕರಿಸಲು ಯಾವುದೇ ಪುರಾವೆಗಳಿಲ್ಲ.",
                            sourceUrl = "https://www.sebi.gov.in"
                        )
                    ),
                    explanation = "ಈ ಸಂದೇಶದಲ್ಲಿ ನೋಂದಣಿಯಾಗದ ಹೂಡಿಕೆ ವಂಚನೆಗಳಲ್ಲಿ ಕಂಡುಬರುವ ಪ್ರಮುಖ ಎಚ್ಚರಿಕೆಯ ಸಂಕೇತಗಳಿವೆ, ಇದರಲ್ಲಿ ಪರಿಶೀಲಿಸದ ಕ್ಲೈಮ್‌ಗಳು ಮತ್ತು ಅವಾಸ್ತವಿಕ ಲಾಭಗಳು ಸೇರಿವೆ.",
                    recommendedActions = listOf(
                        "ಸ್ವತಂತ್ರವಾಗಿ ಪರಿಶೀಲಿಸುವವರೆಗೆ ಯಾವುದೇ ಹಣವನ್ನು ವರ್ಗಾಯಿಸಬೇಡಿ.",
                        "ಮೂಲ ಸಂದೇಶ, ಸ್ಕ್ರೀನ್‌ಶಾಟ್, ಯುಪಿಐ ಐಡಿ ಮತ್ತು ವಿವರಗಳನ್ನು ಸುರಕ್ಷಿತವಾಗಿರಿಸಿ.",
                        "ಅಧಿಕೃತ ಸೆಬಿ ಪೋರ್ಟಲ್ ಮೂಲಕ ನೋಂದಣಿ ವಿವರಗಳನ್ನು ನೀವೇ ಖಚಿತಪಡಿಸಿಕೊಳ್ಳಿ."
                    )
                )

                else -> AnalysisResponse(
                    analysisId = id,
                    riskScore = 82,
                    riskLevel = "VERY_HIGH",
                    claims = listOf(
                        Claim(claimText = "SEBI registered advisor", category = "Registration claim"),
                        Claim(claimText = "Guaranteed 30% monthly returns", category = "Guaranteed-return claim")
                    ),
                    riskSignals = listOf(
                        RiskSignal(title = "Guaranteed return", description = "The message promises a guaranteed return.", severity = "CRITICAL"),
                        RiskSignal(title = "Payment request", description = "The message asks you to transfer money.", severity = "HIGH"),
                        RiskSignal(title = "Urgency", description = "The message pressures you to act immediately.", severity = "MEDIUM")
                    ),
                    evidence = listOf(
                        Evidence(
                            claim = "Registration claim",
                            status = "UNVERIFIED",
                            details = "The available information was insufficient to verify the registration claim.",
                            sourceUrl = "https://www.sebi.gov.in"
                        )
                    ),
                    explanation = "The message contains multiple warning signals commonly found in unregistered investment schemes, including unverified regulatory credentials, urgency tactics, and unrealistic guaranteed returns.",
                    recommendedActions = listOf(
                        "Do not transfer money until independently verified.",
                        "Preserve the original message, screenshot, payment details, and related evidence.",
                        "Verify important claims through appropriate official sources."
                    )
                )
            }
        }
    }

    suspend fun analyzeContent(
        inputType: String,
        text: String,
        language: String = "en",
        forceMock: Boolean = false
    ): Result<AnalysisResponse> = withContext(Dispatchers.IO) {
        if (useMockDataSource || forceMock) {
            // Simulate brief network delay for mock testing
            delay(1200)
            return@withContext Result.success(createMockResponse(text, languageCode = language))
        }

        try {
            val response = RetrofitClient.apiService.analyzeContent(
                AnalyzeRequest(
                    inputType = inputType,
                    text = text,
                    language = language
                )
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorCode = response.code()
                val errorBody = response.errorBody()?.string() ?: ""
                Result.failure(
                    IOException("Server returned error $errorCode: $errorBody")
                )
            }
        } catch (e: UnknownHostException) {
            Result.failure(IOException("No internet connection. Please verify your network and try again.", e))
        } catch (e: ConnectException) {
            Result.failure(IOException("Cannot connect to SATARK server. Please ensure the backend is running.", e))
        } catch (e: SocketTimeoutException) {
            Result.failure(IOException("Connection timed out. The server took too long to respond.", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
