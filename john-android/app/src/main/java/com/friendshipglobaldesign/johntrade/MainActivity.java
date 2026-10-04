package com.friendshipglobaldesign.johntrade;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.drawable.GradientDrawable;

public class MainActivity extends Activity {

    private static final String MONITOR_URL = "https://alalex.app.n8n.cloud/webhook/john-monitor";
    private static final String REMOTE_URL = "https://alalex.app.n8n.cloud/webhook/john-remote";
    private WebView webView;
    private LinearLayout nav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.rgb(7, 10, 14));
        getWindow().setNavigationBarColor(Color.rgb(7, 10, 14));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(7, 10, 14));

        // Android 15+ edge-to-edge safe area fix.
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            int bottom = insets.getSystemWindowInsetBottom();
            v.setPadding(0, top, 0, bottom);
            return insets;
        });

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(22), dp(14), dp(22), dp(12));
        header.setBackgroundColor(Color.rgb(7, 10, 14));

        LinearLayout brandRow = new LinearLayout(this);
        brandRow.setOrientation(LinearLayout.HORIZONTAL);
        brandRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("JOHN");
        title.setTextColor(Color.rgb(247, 249, 252));
        title.setTextSize(25);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        brandRow.addView(title);

        TextView trade = new TextView(this);
        trade.setText("  TRADE");
        trade.setTextColor(Color.rgb(205, 165, 82));
        trade.setTextSize(25);
        trade.setTypeface(Typeface.DEFAULT_BOLD);
        brandRow.addView(trade);

        TextView status = new TextView(this);
        status.setText("   ● ONLINE");
        status.setTextColor(Color.rgb(66, 211, 146));
        status.setTextSize(11);
        status.setTypeface(Typeface.DEFAULT_BOLD);
        brandRow.addView(status);

        header.addView(brandRow);

        TextView subtitle = new TextView(this);
        subtitle.setText("Smart market intelligence • Monitor • Remote");
        subtitle.setTextColor(Color.rgb(128, 141, 158));
        subtitle.setTextSize(12);
        subtitle.setPadding(0, dp(3), 0, 0);
        header.addView(subtitle);

        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(7, 10, 14));
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setBuiltInZoomControls(false);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        root.addView(webView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(10), dp(8), dp(10), dp(8));

        GradientDrawable navBg = new GradientDrawable();
        navBg.setColor(Color.rgb(15, 20, 28));
        navBg.setStroke(dp(1), Color.rgb(34, 42, 55));
        nav.setBackground(navBg);

        Button home = navButton("⌂\nHOME");
        Button monitor = navButton("◉\nMONITOR");
        Button remote = navButton("◆\nREMOTE");
        Button okx = navButton("↗\nOKX");

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

        LinearLayout.LayoutParams navParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(72));
        navParams.setMargins(dp(12), dp(4), dp(12), dp(10));
        root.addView(nav, navParams);

        setContentView(root);
        root.requestApplyInsets();
        showHome();
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return (int)(value * density + 0.5f);
    }

    private LinearLayout.LayoutParams weight() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        p.setMargins(dp(3), 0, dp(3), 0);
        return p;
    }

    private Button navButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(10);
        b.setTextColor(Color.rgb(225, 231, 239));
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(4), dp(6), dp(4), dp(6));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(22, 29, 40));
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), Color.rgb(42, 52, 68));
        b.setBackground(bg);
        return b;
    }

    private void showHome() {
        String html = "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>" +
                "<style>" +
                "*{box-sizing:border-box}" +
                "body{font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif;background:#070a0e;color:#f7f9fc;margin:0;padding:18px 18px 28px}" +
                ".hero{background:linear-gradient(145deg,#111722,#0c1118);border:1px solid #263142;border-radius:26px;padding:22px;box-shadow:0 18px 45px rgba(0,0,0,.35)}" +
                ".eyebrow{font-size:11px;letter-spacing:.14em;color:#cda552;font-weight:800}" +
                "h1{font-size:36px;line-height:1.05;margin:12px 0 8px;letter-spacing:-.03em}" +
                ".sub{color:#8e9bad;line-height:1.5;font-size:15px}" +
                ".success{display:inline-block;margin-top:14px;background:#0d2b22;border:1px solid #1e634d;color:#5be4ae;padding:8px 11px;border-radius:999px;font-size:11px;font-weight:900;letter-spacing:.04em}" +
                ".grid{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-top:14px}" +
                ".card{background:#101620;border:1px solid #222d3d;border-radius:20px;padding:17px;min-height:118px}" +
                ".wide{grid-column:1/-1}" +
                ".k{font-size:11px;color:#7e8b9c;letter-spacing:.08em;font-weight:800}" +
                ".v{font-size:25px;font-weight:900;margin-top:8px}" +
                ".gold{color:#d7b566}.green{color:#5be4ae}.muted{color:#8b97a7;font-size:13px;line-height:1.45;margin-top:8px}" +
                ".bar{height:8px;background:#1a2230;border-radius:999px;overflow:hidden;margin-top:12px}.fill{width:82%;height:100%;background:linear-gradient(90deg,#2c8f68,#5be4ae)}" +
                "</style></head>" +
                "<body>" +
                "<div class='hero'>" +
                "<div class='eyebrow'>JOHN • MARKET INTELLIGENCE</div>" +
                "<h1>Built to stay ready.</h1>" +
                "<div class='sub'>Monitorizare, control și decizii urmărite într-un singur loc. Design premium, fără promisiuni false de profit.</div>" +
                "<span class='success'>● SYSTEM READY</span>" +
                "</div>" +
                "<div class='grid'>" +
                "<div class='card'><div class='k'>STATUS</div><div class='v green'>ONLINE</div><div class='muted'>Motorul aplicației este pregătit pentru monitor și remote.</div></div>" +
                "<div class='card'><div class='k'>MODE</div><div class='v gold'>PAPER</div><div class='muted'>Live money rămâne cu confirmare manuală în OKX.</div></div>" +
                "<div class='card wide'><div class='k'>SYSTEM HEALTH</div><div class='v'>82%</div><div class='bar'><div class='fill'></div></div><div class='muted'>UI + monitor + remote sunt conectate. Trading cycle trebuie validat separat.</div></div>" +
                "<div class='card wide'><div class='k'>CAPITAL PROTECTION</div><div class='muted'>Cheile API nu sunt stocate în aplicație. Withdraw / Transfer nu sunt necesare. Fondurile rămân la exchange.</div></div>" +
                "</div>" +
                "</body></html>";
        webView.loadDataWithBaseURL("https://john.local/", html, "text/html", "UTF-8", null);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
