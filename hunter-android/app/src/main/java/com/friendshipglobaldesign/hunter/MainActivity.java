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
        getWindow().setStatusBarColor(Color.rgb(3,5,6));
        getWindow().setNavigationBarColor(Color.rgb(3,5,6));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(3,5,6));
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setUserAgentString(s.getUserAgentString() + " HUNTER-ALPHA/0.3");

        webView.addJavascriptInterface(new HunterNative(), "HunterNative");
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleExternal(request.getUrl().toString());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
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
            webView.post(() -> webView.evaluateJavascript("window.hunterReturned&&window.hunterReturned();", null));
            return true;
        }
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)
                || "file".equalsIgnoreCase(scheme) || "about".equalsIgnoreCase(scheme)) return false;
        try {
            if (url.startsWith("intent://")) {
                Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                startActivity(intent);
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            }
            return true;
        } catch (Exception e) {
            Toast.makeText(this, "Nu pot deschide aplicația pentru acest link.", Toast.LENGTH_LONG).show();
            return true;
        }
    }

    private void handleReturnIntent(Intent intent) {
        if (intent == null || intent.getData() == null) return;
        if ("hunter".equalsIgnoreCase(intent.getData().getScheme()) && webView != null) {
            webView.postDelayed(() -> webView.evaluateJavascript("window.hunterReturned&&window.hunterReturned();", null), 350);
        }
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleReturnIntent(intent);
    }

    public class HunterNative {
        @JavascriptInterface public String listWallets() { return prefs.getString("wallets", "[]"); }

        @JavascriptInterface public boolean addWallet(String label, String address) {
            String l = label == null ? "" : label.trim();
            String a = address == null ? "" : address.trim();
            if (l.length() < 1 || a.length() < 20 || a.length() > 160 || a.contains(" ")) {
                toast("Introdu un nume și o adresă publică validă.");
                return false;
            }
            String low = a.toLowerCase();
            if (low.contains("seed") || low.contains("private") || low.contains("secret")) {
                toast("Nu introduce seed phrase sau private key.");
                return false;
            }
            try {
                JSONArray arr = new JSONArray(prefs.getString("wallets", "[]"));
                JSONObject w = new JSONObject();
                w.put("label", l); w.put("address", a); w.put("source", "manual");
                arr.put(w);
                prefs.edit().putString("wallets", arr.toString()).apply();
                toast("Adresă publică adăugată.");
                return true;
            } catch (Exception e) { return false; }
        }

        @JavascriptInterface public void removeWallet(int index) {
            try {
                JSONArray arr = new JSONArray(prefs.getString("wallets", "[]"));
                JSONArray next = new JSONArray();
                for (int i=0;i<arr.length();i++) if (i != index) next.put(arr.get(i));
                prefs.edit().putString("wallets", next.toString()).apply();
            } catch (Exception ignored) {}
        }

        @JavascriptInterface public boolean saveConnectedWallet(String label, String account, String chain) {
            if (account == null || account.trim().length() < 20) return false;
            try {
                JSONObject o = new JSONObject();
                o.put("label", label == null ? "OKX Wallet" : label);
                o.put("account", account.trim());
                o.put("chain", chain == null ? "" : chain);
                o.put("connectedAt", System.currentTimeMillis());
                prefs.edit().putString("connected_wallet", o.toString()).apply();
                toast("Wallet conectat. Hunter nu păstrează cheile private.");
                return true;
            } catch (Exception e) { return false; }
        }

        @JavascriptInterface public String getConnectedWallet() { return prefs.getString("connected_wallet", "{}"); }
        @JavascriptInterface public void clearConnectedWallet() {
            prefs.edit().remove("connected_wallet").apply();
            toast("Wallet deconectat.");
        }

        @JavascriptInterface public void openExternal(String url) {
            try {
                Uri u = Uri.parse(url);
                String s = u.getScheme();
                if (!"https".equalsIgnoreCase(s) && !"http".equalsIgnoreCase(s)) {
                    toast("Link invalid."); return;
                }
                startActivity(new Intent(Intent.ACTION_VIEW, u));
            } catch (Exception e) { toast("Nu pot deschide linkul."); }
        }

        @JavascriptInterface public void toast(String message) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, message == null ? "" : message, Toast.LENGTH_LONG).show());
        }

        @JavascriptInterface public void notice(String title, String message) {
            runOnUiThread(() -> new AlertDialog.Builder(MainActivity.this)
                    .setTitle(title == null ? "HUNTER ALPHA" : title)
                    .setMessage(message == null ? "" : message)
                    .setPositiveButton("OK", null).show());
        }
    }

    @Override public void onBackPressed() {
        if (webView != null) webView.evaluateJavascript("window.hunterBack&&window.hunterBack();", null);
        else super.onBackPressed();
    }
}
