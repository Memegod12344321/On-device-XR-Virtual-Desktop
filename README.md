# AI Debate Stream Bot

Android-only YouTube live-chat debate bot.

## Current UI
- Google sign-in button
- Masked AI API-key field
- OpenAI/Groq auto-detection and live model loading
- YouTube live-chat connection button
- Stream overlay button that opens the chat-only landscape overlay
- Android built-in TTS for live chat messages

## MVP
- Polls a YouTube live chat using the YouTube Data API v3.
- Accepts `!topic Tyler defend Minecraft, William defend Roblox` commands.
- Generates alternating Tyler/William arguments with Gemini.
- Speaks each argument with Android's built-in Text-to-Speech.
- Shows a clear disconnected/offline state when the device loses network access.
- No camera, contacts, SMS, accessibility service, or microphone permission is used.

## Setup
1. Build/install the debug APK from the GitHub Actions artifact.
2. Enter your YouTube API key, live video ID, and AI API key in the app. Keys are stored locally and are never committed to this repository.
3. Start the bot/overlay.

### Important
This version includes the chat/TTS controller and chat-only overlay. It does not pretend to be a complete RTMP encoder. YouTube's actual outgoing livestream transport still requires a separate streaming layer.

## Privacy
The app requests only INTERNET and notification/foreground-service capabilities needed by the bot. It does not request camera, microphone, contacts, SMS, location, or storage permissions.
