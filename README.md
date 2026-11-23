# GameVoice AI Free — Android prototype

Android Java prototype for Huawei Pura 80 Pro / EMUI 15. English speech from the TV microphone → LibreTranslate-compatible HTTPS API → Russian Android TTS.

## Build APK without a PC
1. Create a new GitHub repository.
2. Upload **the contents** of this ZIP, including `.github/workflows/android.yml`, preserving folders. GitHub's browser uploader may hide dotfolders; check that the workflow file is present.
3. Open Actions → Build Android APK → Run workflow (or push to main).
4. Download the `GameVoiceAI-Free-APK` artifact, unzip it, install `app-debug.apk` on your phone.
5. Grant microphone permission, ensure a speech recognition service and Russian TTS voice are installed.
6. Enter a working **HTTPS LibreTranslate /translate endpoint** (and key if needed), then press Start.

## Limitations
- **This is a prototype, not a verified working APK.** Compilation and EMUI 15 compatibility have not been tested here.
- No guaranteed free public LibreTranslate server is supplied. A private server or compatible endpoint is required; hosting can cost money. A public server may be rate-limited, unavailable or unsafe for sensitive speech.
- The system SpeechRecognizer may be absent on Huawei without Google services; it can stop listening after pauses, and restart behavior varies by vendor.
- Runs only while app is open; no background microphone foreground service. Audio from phone TTS may be picked up by the microphone and cause feedback. Use low volume and place phone near TV.
- No Vosk or offline translator is bundled in this initial prototype. A fully offline production build would require additional models and engineering.
- The app does not record or store audio; recognized text is transmitted to the configured translation server. API key is saved in app preferences without encryption: avoid sensitive keys.
