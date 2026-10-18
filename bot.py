import telebot
import yt_dlp
import os
from flask import Flask
from threading import Thread

# ==========================================
# 1. SETUP SERVER PALSU (FLASK) BUAT RENDER
# ==========================================
app = Flask(__name__)

@app.route('/')
def home():
    return "Bot Telegram Lagi Jalan Cuy!"

def run():
    # Render butuh port untuk Web Service
    app.run(host='0.0.0.0', port=8080)

def keep_alive():
    t = Thread(target=run)
    t.start()

# ==========================================
# 2. SETUP BOT TELEGRAM
# ==========================================
# MASUKKAN TOKEN BOT LO DI SINI
TOKEN = os.environ.get('TOKEN')
bot = telebot.TeleBot(TOKEN)

@bot.message_handler(commands=['start'])
def send_welcome(message):
    bot.reply_to(message, "Halo cuy! Kirim link video/foto dari sosmed, nanti gw downloadin.")

@bot.message_handler(func=lambda message: True)
def handle_message(message):
    url = message.text
    if not url.startswith('http'):
        bot.reply_to(message, "Kirim link yang bener cuy (harus diawali http/https).")
        return

    msg = bot.reply_to(message, "Sabar ya cuy, lagi diproses...")

    ydl_opts = {
        'outtmpl': 'downloaded_media.%(ext)s',
        'format': 'best', 
        'noplaylist': True,
        'quiet': True
    }

    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=True)
            filename = ydl.prepare_filename(info)

        # Kirim file kembali ke Telegram
        with open(filename, 'rb') as file:
            bot.send_document(message.chat.id, file)
        
        # Hapus file dari server setelah dikirim supaya storage nggak penuh
        os.remove(filename)
        bot.delete_message(message.chat.id, msg.message_id)

    except Exception as e:
        bot.edit_message_text(f"Waduh gagal cuy. Pastikan link bisa diakses publik atau file terlalu besar.\nError: {str(e)}", message.chat.id, msg.message_id)

# ==========================================
# 3. JALANKAN SEMUANYA
# ==========================================
if __name__ == "__main__":
    keep_alive() # Nyalain server web Flask
    print("Bot sedang berjalan...")
    bot.polling(none_stop=True) # Nyalain bot Telegram
