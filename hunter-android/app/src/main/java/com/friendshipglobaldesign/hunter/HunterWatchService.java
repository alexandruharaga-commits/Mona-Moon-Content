package com.friendshipglobaldesign.hunter;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HunterWatchService extends JobService {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public boolean onStartJob(JobParameters params) {
        executor.execute(() -> {
            try {
                checkForNewAlerts();
            } catch (Exception ignored) {
            } finally {
                jobFinished(params, false);
            }
        });
        return true;
    }

    private void checkForNewAlerts() throws Exception {
        SharedPreferences prefs = getSharedPreferences("hunter_prefs", MODE_PRIVATE);
        if (!prefs.getBoolean("alerts_enabled", false)) return;

        HttpURLConnection connection =
                (HttpURLConnection) new URL(MainActivity.STATUS_URL).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("Cache-Control", "no-cache");

        int code = connection.getResponseCode();
        if (code < 200 || code >= 300) {
            connection.disconnect();
            return;
        }

        StringBuilder body = new StringBuilder();
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
        }
        connection.disconnect();

        JSONObject root = new JSONObject(body.toString());
        JSONArray alerts = root.optJSONArray("lastAlerts");
        if (alerts == null) return;

        persistHistory(prefs, alerts);

        Set<String> currentKeys = new HashSet<>();
        for (int i = 0; i < alerts.length(); i++) {
            JSONObject alert = alerts.optJSONObject(i);
            if (alert != null) currentKeys.add(keyOf(alert));
        }

        if (!prefs.getBoolean("alerts_baselined", false)) {
            prefs.edit()
                    .putStringSet("notified_keys", currentKeys)
                    .putBoolean("alerts_baselined", true)
                    .apply();
            return;
        }

        Set<String> oldKeys =
                new HashSet<>(prefs.getStringSet("notified_keys", new HashSet<>()));

        JSONObject newest = null;
        int newCount = 0;

        for (int i = 0; i < alerts.length(); i++) {
            JSONObject alert = alerts.optJSONObject(i);
            if (alert == null) continue;

            String key = keyOf(alert);
            if (!oldKeys.contains(key)) {
                if (newest == null) newest = alert;
                newCount++;
            }
        }

        prefs.edit().putStringSet("notified_keys", currentKeys).apply();

        if (newest != null && newCount > 0) {
            showNotification(newest, newCount);
        }
    }

    private void persistHistory(SharedPreferences prefs, JSONArray alerts) {
        try {
            LinkedHashMap<String, JSONObject> merged = new LinkedHashMap<>();
            for (int i = 0; i < alerts.length(); i++) {
                JSONObject a = alerts.optJSONObject(i);
                if (a != null) {
                    String k = keyOf(a);
                    if (!k.isEmpty()) merged.put(k, a);
                }
            }
            JSONArray old = new JSONArray(prefs.getString("notification_history_json", "[]"));
            for (int i = 0; i < old.length() && merged.size() < 50; i++) {
                JSONObject a = old.optJSONObject(i);
                if (a != null) {
                    String k = keyOf(a);
                    if (!k.isEmpty() && !merged.containsKey(k)) merged.put(k, a);
                }
            }
            JSONArray out = new JSONArray();
            int count = 0;
            for (Map.Entry<String, JSONObject> e : merged.entrySet()) {
                if (count++ >= 50) break;
                out.put(e.getValue());
            }
            prefs.edit().putString("notification_history_json", out.toString()).apply();
        } catch (Exception ignored) {}
    }

    private String keyOf(JSONObject alert) {
        String link = alert.optString("link", "");
        String title = alert.optString("title", "");
        return !link.isEmpty() ? link : title;
    }

    private void showNotification(JSONObject alert, int count) {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    MainActivity.ALERT_CHANNEL,
                    "HUNTER ALPHA opportunities",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("New crypto opportunities found by HUNTER ALPHA");
            manager.createNotificationChannel(channel);
        }

        Intent launchIntent = new Intent(this, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        launchIntent.putExtra("open_notifications", true);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                4401,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = alert.optString("title", "New crypto opportunity");
        int score = alert.optInt("score", 0);
        String status = alert.optString("sourceVerification", "UNVERIFIED");
        String source = alert.optString("sourceHost", alert.optString("source", "source"));

        String text = count > 1
                ? count + " new opportunities. Top: " + title + " · " + score + "/100"
                : title + " · " + score + "/100 · " + status + " · " + source;

        Notification.Builder builder =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? new Notification.Builder(this, MainActivity.ALERT_CHANNEL)
                        : new Notification.Builder(this);

        builder.setSmallIcon(R.drawable.ic_hunter_notify)
                .setContentTitle("HUNTER ALPHA · New opportunity")
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        manager.notify(4401, builder.build());
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
