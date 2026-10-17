# Man's Video Downloader V4 — Android APK project

Project Android untuk menjalankan bot Telegram downloader dari HP Android menggunakan Chaquopy + Python + yt-dlp.

## Build APK paling mudah (GitHub Actions)
1. Buat repository baru di GitHub, misalnya `mans-video-downloader-v4`.
2. Upload semua isi folder project ini ke root repository (bukan folder ZIP-nya).
3. Buka tab **Actions**. Jika diminta, aktifkan Actions.
4. Pilih workflow **Build Man's Video Downloader APK** lalu klik **Run workflow**. Bisa juga push ke branch `main`.
5. Setelah workflow hijau, buka hasil run, bagian **Artifacts**, unduh `mans-video-downloader-v4-apk`.
6. Ekstrak ZIP artifact. APK berada sebagai `app-debug.apk`.

## Pemakaian
1. Instal APK di HP Android dan izinkan instalasi dari sumber tersebut bila diminta.
2. Buat bot lewat `@BotFather`, salin token, masukkan ke aplikasi, lalu tekan **SIMPAN TOKEN**.
3. (Opsional) Import `cookies.txt` hasil ekspor dari browser/perangkat milikmu untuk konten Instagram yang akunmu memang berhak akses.
4. Tekan **JALANKAN BOT**. Buka Telegram, cari bot kamu dan kirim `/start`.
5. Kirim URL video yang ingin diunduh.

## Penting
- Jangan commit token Telegram atau `cookies.txt` ke GitHub. Masukkan token hanya di aplikasi.
- HP harus hidup, punya internet, dan Android tidak boleh membatasi aplikasi di background. Matikan battery optimization untuk aplikasi jika diperlukan.
- Ini bukan hosting 24/7: kalau HP mati, aplikasi dipaksa berhenti, atau koneksi putus, bot berhenti.
- Bot API Telegram umumnya membatasi upload bot; aplikasi membatasi file sekitar 49 MiB. Video yang lebih besar tidak dikirim.
- Build belum diuji pada semua perangkat Android. Kalau GitHub Actions gagal, buka log build dan gunakan pesan error untuk perbaikan.
- Gunakan downloader hanya untuk video yang kamu punya hak/izin untuk unduh. Hormati privasi, hak cipta, dan ketentuan platform.
