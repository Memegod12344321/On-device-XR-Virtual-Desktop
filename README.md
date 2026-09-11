# AI Debate Stream Bot

Android-only YouTube live-chat debate bot.

## MVP
- Polls a YouTube live chat using the YouTube Data API v3.
- Accepts `!topic Tyler defend Minecraft, William defend Roblox` commands.
- Generates alternating Tyler/William arguments with Gemini.
- Speaks each argument with Android's built-in Text-to-Speech.
- Shows a clear disconnected/offline state when the device loses network access.
- No camera, contacts, SMS, accessibility service, or microphone permission is used.

## Setup
1. Build/install the debug APK from the GitHub Actions artifact.
2. Enter your YouTube API key, live video ID, and Gemini API key in the app. Keys are stored locally and are never committed to this repository.
3. Start the bot.

### Important
This first version is the bot engine and chat/TTS controller. It does not pretend to be a complete RTMP encoder. YouTube's actual outgoing livestream transport will be added as a separate streaming layer.

## Privacy
The app requests only INTERNET and notification/foreground-service capabilities needed by the bot. It does not request camera, microphone, contacts, SMS, location, or storage permissions.
