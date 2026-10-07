package com.friendshipglobaldesign.hunter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private WebView webView;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("hunter_prefs", Context.MODE_PRIVATE);

        getWindow().setStatusBarColor(Color.rgb(5, 9, 8));
        getWindow().setNavigationBarColor(Color.rgb(5, 9, 8));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(5, 9, 8));

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setUserAgentString(s.getUserAgentString() + " HUNTER-Premium/0.2");

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
        });

        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
        handleReturnIntent(getIntent());
    }

    private boolean handleExternal(String url) {
        if (url == null) return false;
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();

        if ("hunter".equalsIgnoreCase(scheme)) {
            webView.post(() -> webView.evaluateJavascript(
                    "window.hunterReturned && window.hunterReturned();", null));
            return true;
        }

        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)
                || "file".equalsIgnoreCase(scheme) || "about".equalsIgnoreCase(scheme)) {
            return false;
        }

        try {
            if (url.startsWith("intent://")) {
                Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                try {
                    startActivity(intent);
                } catch (Exception first) {
                    String fallback = intent.getStringExtra("browser_fallback_url");
                    if (fallback != null) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fallback)));
                    } else {
                        throw first;
                    }
                }
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            }
            return true;
        } catch (Exception e) {
            Toast.makeText(this, "Nu pot deschide wallet-ul pentru acest link.", Toast.LENGTH_LONG).show();
            return true;
        }
    }

    private void handleReturnIntent(Intent intent) {
        if (intent == null || intent.getData() == null) return;
        Uri data = intent.getData();
        if ("hunter".equalsIgnoreCase(data.getScheme())) {
            if (webView != null) {
                webView.postDelayed(() -> webView.evaluateJavascript(
                        "window.hunterReturned && window.hunterReturned();", null), 300);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleReturnIntent(intent);
    }

    public class HunterNative {

        @JavascriptInterface
        public String listWallets() {
            return prefs.getString("wallets", "[]");
        }

        @JavascriptInterface
        public boolean addWallet(String label, String address) {
            final String cleanLabel = label == null ? "" : label.trim();
            final String cleanAddress = address == null ? "" : address.trim();

            if (cleanLabel.length() < 1 || cleanAddress.length() < 20
                    || cleanAddress.length() > 160 || cleanAddress.contains(" ")) {
                toast("Introdu un nume și o adresă publică validă.");
                return false;
            }

            String lower = cleanAddress.toLowerCase();
            if (lower.contains("seed") || lower.contains("private") || lower.contains("secret")) {
                toast("Nu introduce seed phrase sau private key.");
                return false;
            }

            try {
                JSONArray arr = new JSONArray(prefs.getString("wallets", "[]"));
                JSONObject w = new JSONObject();
                w.put("label", cleanLabel);
                w.put("address", cleanAddress);
                w.put("source", "manual");
                arr.put(w);
                prefs.edit().putString("wallets", arr.toString()).apply();
                toast("Watch wallet adăugat.");
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public void removeWallet(int index) {
            try {
                JSONArray arr = new JSONArray(prefs.getString("wallets", "[]"));
                JSONArray next = new JSONArray();
                for (int i = 0; i < arr.length(); i++) {
                    if (i != index) next.put(arr.get(i));
                }
                prefs.edit().putString("wallets", next.toString()).apply();
            } catch (Exception ignored) {}
        }

        @JavascriptInterface
        public boolean saveConnectedWallet(String label, String account, String chain) {
            if (account == null || account.trim().length() < 20) return false;
            try {
                JSONObject obj = new JSONObject();
                obj.put("label", label == null ? "OKX Wallet" : label);
                obj.put("account", account.trim());
                obj.put("chain", chain == null ? "" : chain);
                obj.put("connectedAt", System.currentTimeMillis());
                prefs.edit().putString("connected_wallet", obj.toString()).apply();
                toast("OKX Wallet conectat în mod approval-only.");
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public String getConnectedWallet() {
            return prefs.getString("connected_wallet", "{}");
        }

        @JavascriptInterface
        public void clearConnectedWallet() {
            prefs.edit().remove("connected_wallet").apply();
            toast("Wallet deconectat din Hunter.");
        }

        @JavascriptInterface
        public void toast(String message) {
            final String msg = message == null ? "" : message;
            runOnUiThread(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show());
        }

        @JavascriptInterface
        public void notice(String title, String message) {
            final String t = title == null ? "HUNTER" : title;
            final String m = message == null ? "" : message;
            runOnUiThread(() -> new AlertDialog.Builder(MainActivity.this)
                    .setTitle(t)
                    .setMessage(m)
                    .setPositiveButton("OK", null)
                    .show());
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null) {
            webView.evaluateJavascript("window.hunterBack && window.hunterBack();", null);
        } else {
            super.onBackPressed();
        }
    }
}
