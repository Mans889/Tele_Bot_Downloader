package com.mans.videodownloader;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.File;

public class BotService extends Service {
    public static final String ACTION_START = "com.mans.videodownloader.START";
    private static final String CHANNEL_ID = "mans_bot_service";
    private Thread botThread;
    private volatile boolean stopping = false;

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) { stopSelf(); return START_NOT_STICKY; }
        if (ACTION_START.equals(intent.getAction())) {
            String token = intent.getStringExtra("token");
            if (token == null || token.trim().isEmpty()) { stopSelf(); return START_NOT_STICKY; }
            stopping = false;
            startForeground(1001, makeNotification("Bot sedang berjalan"));
            if (botThread == null || !botThread.isAlive()) {
                botThread = new Thread(() -> runBot(token), "MansTelegramBot");
                botThread.start();
            }
        }
        return START_NOT_STICKY;
    }

    private void runBot(String token) {
        try {
            if (!Python.isStarted()) Python.start(new AndroidPlatform(this));
            File cookies = new File(getFilesDir(), "cookies.txt");
            PyObject module = Python.getInstance().getModule("bot");
            module.callAttr("run_bot", token, cookies.getAbsolutePath(), getFilesDir().getAbsolutePath());
        } catch (Throwable t) {
            android.util.Log.e("MansBot", "Bot gagal berjalan", t);
            stopSelf();
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Man's Downloader", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Status layanan bot Telegram");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification makeNotification(String message) {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        return b.setContentTitle("Man's Video Downloader")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setOngoing(true)
                .build();
    }

    @Override public void onDestroy() {
        stopping = true;
        try {
            if (Python.isStarted()) Python.getInstance().getModule("bot").callAttr("request_stop");
        } catch (Throwable ignored) {}
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
