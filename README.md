# Man's Video Downloader — Android/GitHub Actions starter

This is a starter repository for building an Android APK with GitHub Actions. It is **not yet a working Android port of the original Python bot**. The original script uses `python-telegram-bot`, `yt-dlp`, FFmpeg, and Instagram cookies; these need a tested Android runtime/service integration before the APK can run the bot reliably.

## Build
1. Create a **private** GitHub repository.
2. Upload the contents of this folder to the repository root (including `.github/`).
3. Open **Actions** and run **Build Android APK**.
4. Download the `android-debug-apk` artifact if the workflow succeeds.

## Security
- Never commit your Telegram bot token or `cookies.txt`.
- Treat Instagram cookies as account credentials.
- This starter deliberately does not include secrets or personal cookies.

## Current status
The workflow builds a minimal Android app shell to verify GitHub Actions and Android SDK setup. It does not yet embed/run `VideoDownload_Bot.py`; that requires a separate Android Python-runtime/foreground-service implementation and validation on your device.
