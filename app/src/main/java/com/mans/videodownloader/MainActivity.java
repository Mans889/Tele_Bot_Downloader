package com.mans.videodownloader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends Activity {
    private EditText tokenInput;
    private TextView cookieStatus, stateText;
    private SharedPreferences prefs;
    private static final int PICK_COOKIES = 741;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("mans_bot", MODE_PRIVATE);
        buildUi();
        tokenInput.setText(prefs.getString("token", ""));
        refreshState();
    }

    private void buildUi() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(28, 34, 28, 24);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setBackgroundColor(0xFFF5F6F8);

        TextView title = new TextView(this);
        title.setText("⚡ MAN'S VIDEO DOWNLOADER V4");
        title.setTextSize(22);
        title.setTextColor(0xFF111827);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 8, 0, 12);
        layout.addView(title, full());

        TextView desc = new TextView(this);
        desc.setText("Bot Telegram berjalan dari HP Android. HP harus tetap menyala dan terhubung internet.");
        desc.setTextSize(14);
        desc.setTextColor(0xFF374151);
        desc.setGravity(Gravity.CENTER);
        desc.setPadding(0, 0, 0, 18);
        layout.addView(desc, full());

        tokenInput = new EditText(this);
        tokenInput.setSingleLine(true);
        tokenInput.setHint("Token bot dari @BotFather");
        tokenInput.setTextSize(15);
        layout.addView(tokenInput, full());

        Button save = new Button(this);
        save.setText("SIMPAN TOKEN");
        save.setOnClickListener(v -> saveToken());
        layout.addView(save, full());

        Button cookies = new Button(this);
        cookies.setText("IMPORT cookies.txt (Instagram)");
        cookies.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, PICK_COOKIES);
        });
        layout.addView(cookies, full());

        cookieStatus = new TextView(this);
        cookieStatus.setTextSize(13);
        cookieStatus.setTextColor(0xFF374151);
        cookieStatus.setPadding(0, 4, 0, 8);
        layout.addView(cookieStatus, full());

        Button start = new Button(this);
        start.setText("▶ JALANKAN BOT");
        start.setOnClickListener(v -> startBot());
        layout.addView(start, full());

        Button stop = new Button(this);
        stop.setText("■ HENTIKAN BOT");
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, BotService.class));
            stateText.setText("Status: dihentikan");
        });
        layout.addView(stop, full());

        stateText = new TextView(this);
        stateText.setTextSize(14);
        stateText.setTextColor(0xFF065F46);
        stateText.setPadding(0, 14, 0, 8);
        layout.addView(stateText, full());

        TextView note = new TextView(this);
        note.setText("Catatan: cookies Instagram bersifat rahasia. Jangan unggah token/cookies ke GitHub. Video besar mungkin tidak bisa dikirim lewat Bot API.");
        note.setTextSize(12);
        note.setTextColor(0xFF6B7280);
        note.setPadding(0, 12, 0, 0);
        layout.addView(note, full());

        setContentView(layout);
        updateCookieStatus();
    }

    private LinearLayout.LayoutParams full() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void saveToken() {
        String token = tokenInput.getText().toString().trim();
        if (token.isEmpty() || !token.matches("\\d{5,}:\\S{20,}")) {
            new AlertDialog.Builder(this).setMessage("Token tampaknya belum valid. Salin token lengkap dari @BotFather.").setPositiveButton("OK", null).show();
            return;
        }
        prefs.edit().putString("token", token).apply();
        Toast.makeText(this, "Token disimpan di HP.", Toast.LENGTH_SHORT).show();
    }

    private void startBot() {
        saveToken();
        String token = prefs.getString("token", "");
        if (token.isEmpty()) return;
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 902);
        }
        Intent intent = new Intent(this, BotService.class);
        intent.setAction(BotService.ACTION_START);
        intent.putExtra("token", token);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent); else startService(intent);
        stateText.setText("Status: perintah mulai dikirim. Buka Telegram dan kirim /start ke bot.");
    }

    private void refreshState() {
        stateText.setText("Status: siap");
        updateCookieStatus();
    }

    private void updateCookieStatus() {
        File f = new File(getFilesDir(), "cookies.txt");
        if (cookieStatus != null) cookieStatus.setText(f.exists() ? "cookies.txt: sudah diimpor" : "cookies.txt: belum ada (opsional)");
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_COOKIES && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try (InputStream in = getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(new File(getFilesDir(), "cookies.txt"))) {
                if (in == null) throw new Exception("File tidak bisa dibaca");
                byte[] buffer = new byte[8192]; int n;
                while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
                Toast.makeText(this, "cookies.txt berhasil diimpor.", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Gagal impor cookies: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
            updateCookieStatus();
        }
    }
}
