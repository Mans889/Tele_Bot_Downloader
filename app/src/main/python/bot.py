import asyncio
import os
import re
import threading
import time
import logging
from pathlib import Path
from telegram import Update
from telegram.ext import Application, CommandHandler, MessageHandler, ContextTypes, filters
import yt_dlp

logging.basicConfig(level=logging.INFO)
log = logging.getLogger("mans_downloader")
_stop_event = threading.Event()
_loop = None
_app = None

URL_RE = re.compile(r'https?://[^\s<>"]+', re.IGNORECASE)
MAX_TELEGRAM_BYTES = 49 * 1024 * 1024

def request_stop():
    _stop_event.set()
    # Polling loop exits after the current polling interval / next update.

def _clean_url(url):
    return url.rstrip(").,!?;]}'\"")

async def start_cmd(update: Update, context: ContextTypes.DEFAULT_TYPE):
    await update.message.reply_text(
        "⚡ Man's Video Downloader V4 aktif.\n\n"
        "Kirim link video Instagram, Facebook, TikTok, YouTube, Threads, atau X.\n"
        "Perintah: /start, /help, /status.\n"
        "Catatan: hasil unduhan hanya untuk konten yang boleh kamu akses dan gunakan."
    )

async def help_cmd(update: Update, context: ContextTypes.DEFAULT_TYPE):
    await update.message.reply_text(
        "Cara pakai:\n1. Kirim URL video ke chat bot.\n"
        "2. Tunggu proses unduhan.\n3. Bot mengirim file jika ukuran dan format didukung.\n\n"
        "Instagram privat mungkin memerlukan cookies.txt yang diekspor dari akun milikmu. "
        "Jangan bagikan file cookies atau token bot."
    )

async def status_cmd(update: Update, context: ContextTypes.DEFAULT_TYPE):
    await update.message.reply_text("Status: bot aktif dan siap menerima link.")

def _download(url, workdir, cookies_path):
    os.makedirs(workdir, exist_ok=True)
    opts = {
        "outtmpl": os.path.join(workdir, "%(title).80s-%(id)s.%(ext)s"),
        "format": "best[protocol!=m3u8_native]/best",
        "noplaylist": True,
        "quiet": True,
        "no_warnings": True,
        "retries": 2,
        "socket_timeout": 25,
        "max_filesize": MAX_TELEGRAM_BYTES,
    }
    if cookies_path and os.path.isfile(cookies_path) and os.path.getsize(cookies_path) > 0:
        opts["cookiefile"] = cookies_path
    with yt_dlp.YoutubeDL(opts) as ydl:
        info = ydl.extract_info(url, download=True)
        path = ydl.prepare_filename(info)
        if not os.path.exists(path):
            # yt-dlp may change extension after post-processing.
            base = os.path.splitext(path)[0]
            matches = [str(p) for p in Path(workdir).glob(os.path.basename(base) + ".*")]
            if matches: path = matches[0]
        return path, info.get("title", "video")

async def handle_url(update: Update, context: ContextTypes.DEFAULT_TYPE):
    if not update.message or not update.message.text:
        return
    urls = [_clean_url(m.group(0)) for m in URL_RE.finditer(update.message.text)]
    if not urls:
        await update.message.reply_text("Kirim URL video yang valid ya.")
        return
    url = urls[0]
    status = await update.message.reply_text("⏳ Sedang memproses link. Tunggu sebentar...")
    workdir = os.path.join(context.application.bot_data["base_dir"], "downloads")
    cookies = context.application.bot_data["cookies_path"]
    try:
        path, title = await asyncio.to_thread(_download, url, workdir, cookies)
        if not os.path.isfile(path):
            raise RuntimeError("File hasil unduhan tidak ditemukan.")
        size = os.path.getsize(path)
        if size > MAX_TELEGRAM_BYTES:
            await status.edit_text("File berhasil diunduh, tetapi terlalu besar untuk dikirim melalui Bot API (batas aplikasi ini sekitar 49 MB).")
            return
        caption = ("⚡ Man's Video Downloader\n" + title)[:900]
        with open(path, "rb") as media:
            if path.lower().endswith((".mp4", ".m4v", ".mov", ".webm")):
                await update.message.reply_video(video=media, caption=caption, read_timeout=180, write_timeout=180, connect_timeout=30)
            else:
                await update.message.reply_document(document=media, caption=caption, read_timeout=180, write_timeout=180, connect_timeout=30)
        await status.delete()
    except Exception as exc:
        log.exception("Download failed")
        message = str(exc).replace("\n", " ")[:600]
        try:
            await status.edit_text("❌ Gagal memproses link. Pastikan link publik/aksesnya tersedia dan coba lagi.\nDetail: " + message)
        except Exception:
            pass
    finally:
        # Keep only the newest few downloads to limit phone storage.
        try:
            files = sorted(Path(workdir).glob("*"), key=lambda p: p.stat().st_mtime, reverse=True)
            for old in files[10:]:
                if old.is_file():
                    old.unlink(missing_ok=True)
        except Exception:
            pass

async def error_handler(update, context):
    log.error("Telegram handler error", exc_info=context.error)

def run_bot(token, cookies_path, base_dir):
    global _loop, _app
    _stop_event.clear()
    _loop = asyncio.new_event_loop()
    asyncio.set_event_loop(_loop)
    _app = Application.builder().token(token).build()
    _app.bot_data["cookies_path"] = cookies_path
    _app.bot_data["base_dir"] = base_dir
    _app.add_handler(CommandHandler("start", start_cmd))
    _app.add_handler(CommandHandler("help", help_cmd))
    _app.add_handler(CommandHandler("status", status_cmd))
    _app.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, handle_url))
    _app.add_error_handler(error_handler)
    async def main():
        await _app.initialize()
        await _app.start()
        await _app.updater.start_polling(drop_pending_updates=False)
        try:
            while not _stop_event.is_set():
                await asyncio.sleep(1)
        finally:
            await _app.updater.stop()
            await _app.stop()
            await _app.shutdown()
    try:
        _loop.run_until_complete(main())
    finally:
        _loop.close()
        _loop = None
        _app = None
