package com.friendshipglobaldesign.johntrade;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final String MONITOR_URL = "https://alalex.app.n8n.cloud/webhook/john-monitor";
    private static final String REMOTE_URL = "https://alalex.app.n8n.cloud/webhook/john-remote";
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(11, 13, 16));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(24, 20, 24, 14);
        header.setBackgroundColor(Color.rgb(11, 13, 16));

        TextView title = new TextView(this);
        title.setText("JOHN Trade");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        header.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Monitor + Telecomandă • PAPER / LIVE SIGNAL");
        subtitle.setTextColor(Color.rgb(151, 162, 175));
        subtitle.setTextSize(12);
        header.addView(subtitle);

        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(11, 13, 16));
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setBuiltInZoomControls(false);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        root.addView(webView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(8, 8, 8, 12);
        nav.setBackgroundColor(Color.rgb(14, 18, 24));

        Button home = navButton("HOME");
        Button monitor = navButton("MONITOR");
        Button remote = navButton("REMOTE");
        Button okx = navButton("OKX");

        nav.addView(home, weight());
        nav.addView(monitor, weight());
        nav.addView(remote, weight());
        nav.addView(okx, weight());

        home.setOnClickListener(v -> showHome());
        monitor.setOnClickListener(v -> webView.loadUrl(MONITOR_URL));
        remote.setOnClickListener(v -> webView.loadUrl(REMOTE_URL));
        okx.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.okx.com/"));
            startActivity(i);
        });

        root.addView(nav, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
        showHome();
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private Button navButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(11);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        return b;
    }

    private void showHome() {
        String html = "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>" +
                "<style>body{font-family:system-ui;background:#0b0d10;color:#f4f7fb;margin:0;padding:20px}" +
                ".card{background:#151922;border:1px solid #252c36;border-radius:18px;padding:18px;margin:12px 0}" +
                ".badge{display:inline-block;background:#12351f;color:#7dffa0;border:1px solid #2f7d46;border-radius:999px;padding:7px 10px;font-size:11px;font-weight:800}" +
                "h1{font-size:32px;margin:14px 0 5px}.muted{color:#97a2af;line-height:1.5}.big{font-size:28px;font-weight:900}</style></head>" +
                "<body><span class='badge'>JOHN APP v0.1</span><h1>JOHN Trade</h1>" +
                "<p class='muted'>Aplicația dedicată pentru monitorul și telecomanda lui John.</p>" +
                "<div class='card'><div class='muted'>STATUS</div><div class='big'>READY</div><p class='muted'>Folosește MONITOR pentru piață și REMOTE pentru control.</p></div>" +
                "<div class='card'><div class='muted'>REAL MONEY</div><p class='muted'>Fondurile rămân în OKX. Aplicația nu stochează chei API, Secret Key sau Passphrase. Ordinele reale trebuie confirmate în OKX.</p></div>" +
                "<div class='card'><div class='muted'>SAFETY</div><p class='muted'>Withdraw / Transfer nu sunt necesare pentru John. Păstrează-le dezactivate pentru orice integrare API.</p></div>" +
                "</body></html>";
        webView.loadDataWithBaseURL("https://john.local/", html, "text/html", "UTF-8", null);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
