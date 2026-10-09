package com.friendshipglobaldesign.hunter;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    public static final String STATUS_URL = "https://alalex.app.n8n.cloud/webhook/hunter/status";
    public static final String SCAN_URL = "https://alalex.app.n8n.cloud/webhook/hunter/scan";
    public static final String ALERT_CHANNEL = "hunter_alpha_alerts";
    public static final int WATCH_JOB_ID = 42042;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 101;

    private WebView webView;
    private SharedPreferences prefs;
    private boolean openNotificationsAfterLoad = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("hunter_prefs", Context.MODE_PRIVATE);
        createNotificationChannel();
        readLaunchIntent(getIntent());

        getWindow().setStatusBarColor(Color.rgb(3, 5, 6));
        getWindow().setNavigationBarColor(Color.rgb(3, 5, 6));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(3, 5, 6));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setUserAgentString(settings.getUserAgentString() + " HUNTER-ALPHA/0.5.2");

        webView.addJavascriptInterface(new HunterNative(), "HunterNative");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleExternal(request.getUrl().toString());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleExternal(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                dispatchNotificationOpen();
            }
        });

        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");

        if (prefs.getBoolean("alerts_enabled", false) && notificationPermissionGranted()) {
            scheduleWatchJob();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    ALERT_CHANNEL,
                    "HUNTER ALPHA opportunities",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("New crypto opportunities found by HUNTER ALPHA");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private boolean notificationPermissionGranted() {
        return Build.VERSION.SDK_INT < 33
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void scheduleWatchJob() {
        JobScheduler scheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler == null) return;

        JobInfo jobInfo = new JobInfo.Builder(
                WATCH_JOB_ID,
                new ComponentName(this, HunterWatchService.class)
        )
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(15 * 60 * 1000L)
                .setPersisted(true)
                .build();

        scheduler.schedule(jobInfo);
    }

    private void cancelWatchJob() {
        JobScheduler scheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler != null) scheduler.cancel(WATCH_JOB_ID);
    }

    private boolean handleExternal(String url) {
        if (url == null) return false;
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();

        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)
                || "file".equalsIgnoreCase(scheme) || "about".equalsIgnoreCase(scheme)) {
            return false;
        }

        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
            return true;
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open this link.", Toast.LENGTH_LONG).show();
            return true;
        }
    }

    private void openHttps(String url) {
        try {
            Uri uri = Uri.parse(url);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                Toast.makeText(this, "HUNTER opens HTTPS links only.", Toast.LENGTH_LONG).show();
                return;
            }
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open this link.", Toast.LENGTH_LONG).show();
        }
    }

    private void readLaunchIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("open_notifications", false)) {
            openNotificationsAfterLoad = true;
        }
    }

    private void dispatchNotificationOpen() {
        if (openNotificationsAfterLoad && webView != null) {
            openNotificationsAfterLoad = false;
            webView.postDelayed(() -> webView.evaluateJavascript(
                    "window.hunterOpenNotificationsFromNative&&window.hunterOpenNotificationsFromNative();",
                    null
            ), 300);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        readLaunchIntent(intent);
        dispatchNotificationOpen();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            prefs.edit()
                    .putBoolean("alerts_enabled", granted)
                    .putBoolean("alerts_baselined", false)
                    .apply();

            if (granted) {
                scheduleWatchJob();
                Toast.makeText(this, "HUNTER alerts enabled.", Toast.LENGTH_LONG).show();
            } else {
                cancelWatchJob();
                Toast.makeText(this, "Notification permission was not granted.", Toast.LENGTH_LONG).show();
            }

            if (webView != null) {
                webView.postDelayed(() -> webView.evaluateJavascript(
                        "window.renderAlerts&&window.renderAlerts();",
                        null
                ), 250);
            }
        }
    }

    public class HunterNative {
        @JavascriptInterface
        public boolean savePublicWallet(String label, String address) {
            String l = label == null ? "" : label.trim();
            String a = address == null ? "" : address.trim();

            if (a.length() < 20 || a.length() > 160 || a.contains(" ")) {
                toast("Enter a valid public wallet address.");
                return false;
            }

            String lower = a.toLowerCase();
            if (lower.contains("seed") || lower.contains("private")
                    || lower.contains("recovery") || lower.contains("secret")) {
                toast("Never enter a seed phrase or private key.");
                return false;
            }

            try {
                JSONObject wallet = new JSONObject();
                wallet.put("label", l.isEmpty() ? "Public wallet" : l);
                wallet.put("address", a);
                wallet.put("mode", "WATCH_ONLY");
                wallet.put("savedAt", System.currentTimeMillis());
                prefs.edit().putString("public_wallet", wallet.toString()).apply();
                toast("Public wallet saved in watch-only mode.");
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public String getPublicWallet() {
            return prefs.getString("public_wallet", "{}");
        }

        @JavascriptInterface
        public void clearPublicWallet() {
            prefs.edit().remove("public_wallet").apply();
            toast("Public wallet removed.");
        }

        @JavascriptInterface
        public void openWalletProvider(String name) {
            String url;
            String n = name == null ? "" : name.trim();
            switch (n) {
                case "MetaMask":
                    url = "https://metamask.io/";
                    break;
                case "Trust Wallet":
                    url = "https://trustwallet.com/";
                    break;
                case "WalletConnect":
                    url = "https://walletconnect.network/";
                    break;
                case "Coinbase Wallet":
                    url = "https://www.coinbase.com/wallet";
                    break;
                case "OKX Wallet":
                    url = "https://www.okx.com/web3";
                    break;
                default:
                    toast("Wallet provider is not available.");
                    return;
            }
            toast("Secure in-app wallet connection is not enabled in this beta. Opening the official provider page.");
            openHttps(url);
        }

        @JavascriptInterface
        public void openExternalChecked(String url, String sourceStatus) {
            final String target = url == null ? "" : url.trim();

            runOnUiThread(() -> {
                try {
                    Uri uri = Uri.parse(target);
                    String host = uri.getHost();

                    if (!"https".equalsIgnoreCase(uri.getScheme())
                            || host == null || host.trim().isEmpty()) {
                        Toast.makeText(MainActivity.this,
                                "Invalid or insecure external link.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    String status = sourceStatus == null || sourceStatus.trim().isEmpty()
                            ? "UNVERIFIED" : sourceStatus;

                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Open external source?")
                            .setMessage("Link domain: " + host
                                    + "\nSource status: " + status
                                    + "\n\nVerify the domain before connecting a wallet, signing a message or approving a transaction.")
                            .setNegativeButton("CANCEL", null)
                            .setPositiveButton("OPEN", (dialog, which) -> openHttps(target))
                            .show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this,
                            "Cannot verify this link.",
                            Toast.LENGTH_LONG).show();
                }
            });
        }

        @JavascriptInterface
        public void triggerScan() {
            new Thread(() -> {
                boolean ok = false;
                String message;
                HttpURLConnection connection = null;

                try {
                    URL url = new URL(SCAN_URL);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(10000);
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("Accept", "application/json");

                    byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
                    connection.setFixedLengthStreamingMode(body.length);

                    try (OutputStream output = connection.getOutputStream()) {
                        output.write(body);
                    }

                    int code = connection.getResponseCode();
                    ok = code >= 200 && code < 300;
                    message = ok
                            ? "Hunt started. HUNTER is scanning the configured sources."
                            : "Scheduled Hunter is online. Showing the latest scheduled scan until on-demand hunt is enabled.";
                } catch (Exception e) {
                    message = "Scheduled Hunter is online. Showing the latest scheduled scan until on-demand hunt is enabled.";
                } finally {
                    if (connection != null) connection.disconnect();
                }

                final boolean finalOk = ok;
                final String finalMessage = message;

                runOnUiThread(() -> {
                    if (webView != null) {
                        webView.evaluateJavascript(
                                "window.hunterScanResult&&window.hunterScanResult("
                                        + finalOk + ","
                                        + JSONObject.quote(finalMessage)
                                        + ");",
                                null
                        );
                    }
                });
            }).start();
        }

        @JavascriptInterface
        public boolean alertsEnabled() {
            return prefs.getBoolean("alerts_enabled", false);
        }

        @JavascriptInterface
        public boolean notificationPermissionGranted() {
            return MainActivity.this.notificationPermissionGranted();
        }

        @JavascriptInterface
        public void enableAlerts() {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= 33 && !notificationPermissionGranted()) {
                    requestPermissions(
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            NOTIFICATION_PERMISSION_REQUEST
                    );
                    return;
                }

                prefs.edit()
                        .putBoolean("alerts_enabled", true)
                        .putBoolean("alerts_baselined", false)
                        .apply();
                scheduleWatchJob();
                Toast.makeText(MainActivity.this, "HUNTER alerts enabled.", Toast.LENGTH_LONG).show();

                if (webView != null) {
                    webView.postDelayed(() -> webView.evaluateJavascript(
                            "window.renderAlerts&&window.renderAlerts();",
                            null
                    ), 200);
                }
            });
        }

        @JavascriptInterface
        public void disableAlerts() {
            prefs.edit()
                    .putBoolean("alerts_enabled", false)
                    .putBoolean("alerts_baselined", false)
                    .apply();
            cancelWatchJob();
            toast("HUNTER alerts disabled.");
        }

        @JavascriptInterface
        public void openNotificationSettings() {
            runOnUiThread(() -> {
                try {
                    Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                    startActivity(intent);
                } catch (Exception e) {
                    toast("Cannot open notification settings.");
                }
            });
        }

        @JavascriptInterface
        public void resetNativeState() {
            prefs.edit().clear().apply();
            cancelWatchJob();
        }

        @JavascriptInterface
        public void exitApp() {
            runOnUiThread(MainActivity.this::finishAndRemoveTask);
        }

        @JavascriptInterface
        public void toast(String message) {
            String m = message == null ? "" : message;
            runOnUiThread(() -> Toast.makeText(MainActivity.this, m, Toast.LENGTH_LONG).show());
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null) {
            webView.evaluateJavascript(
                    "window.hunterBack&&window.hunterBack();",
                    null
            );
        } else {
            finishAndRemoveTask();
        }
    }
}
