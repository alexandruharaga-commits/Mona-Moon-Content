package com.friendshipglobaldesign.johntrade;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.view.ViewGroup;
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
    private static final String REMOTE_ACTION_URL = "https://alalex.app.n8n.cloud/webhook/john-remote-action";

    private WebView webView;
    private WebView miniMonitor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.rgb(7, 10, 14));
        getWindow().setNavigationBarColor(Color.rgb(7, 10, 14));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(7, 10, 14));

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            int bottom = insets.getSystemWindowInsetBottom();
            v.setPadding(0, top, 0, bottom);
            return insets;
        });

        root.addView(buildHeader());

        webView = configuredWebView();
        root.addView(webView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // Persistent compact live monitor above the app controls.
        LinearLayout miniWrap = new LinearLayout(this);
        miniWrap.setOrientation(LinearLayout.VERTICAL);
        miniWrap.setPadding(dp(12), dp(6), dp(12), dp(4));

        TextView miniTitle = new TextView(this);
        miniTitle.setText("LIVE MINI MONITOR   •   tap to open full");
        miniTitle.setTextColor(Color.rgb(153, 164, 178));
        miniTitle.setTextSize(10);
        miniTitle.setTypeface(Typeface.DEFAULT_BOLD);
        miniWrap.addView(miniTitle);

        miniMonitor = configuredWebView();
        miniMonitor.setInitialScale(55);
        miniMonitor.getSettings().setLoadWithOverviewMode(true);
        miniMonitor.getSettings().setUseWideViewPort(true);
        miniMonitor.setOnClickListener(v -> webView.loadUrl(MONITOR_URL));
        miniWrap.addView(miniMonitor, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(132)));

        root.addView(miniWrap, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(buildNav());

        setContentView(root);
        root.requestApplyInsets();

        showHome();
        miniMonitor.loadUrl(MONITOR_URL);
    }

    private LinearLayout buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(22), dp(12), dp(22), dp(10));
        header.setBackgroundColor(Color.rgb(7, 10, 14));

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView john = new TextView(this);
        john.setText("JOHN");
        john.setTextColor(Color.rgb(247, 249, 252));
        john.setTextSize(24);
        john.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(john);

        TextView trade = new TextView(this);
        trade.setText("  TRADE");
        trade.setTextColor(Color.rgb(205, 165, 82));
        trade.setTextSize(24);
        trade.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(trade);

        TextView live = new TextView(this);
        live.setText("   ● ONLINE");
        live.setTextColor(Color.rgb(66, 211, 146));
        live.setTextSize(10);
        live.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(live);

        header.addView(row);

        TextView subtitle = new TextView(this);
        subtitle.setText("Command Center • Monitor • Safety • OKX");
        subtitle.setTextColor(Color.rgb(128, 141, 158));
        subtitle.setTextSize(11);
        subtitle.setPadding(0, dp(2), 0, 0);
        header.addView(subtitle);
        return header;
    }

    private WebView configuredWebView() {
        WebView w = new WebView(this);
        w.setBackgroundColor(Color.rgb(7, 10, 14));
        w.getSettings().setJavaScriptEnabled(true);
        w.getSettings().setDomStorageEnabled(true);
        w.getSettings().setBuiltInZoomControls(false);
        w.setWebViewClient(new WebViewClient());
        w.setWebChromeClient(new WebChromeClient());
        return w;
    }

    private LinearLayout buildNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(10), dp(8), dp(10), dp(8));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(15, 20, 28));
        bg.setStroke(dp(1), Color.rgb(34, 42, 55));
        bg.setCornerRadius(dp(18));
        nav.setBackground(bg);

        Button home = navButton("⌂\nHOME");
        Button control = navButton("◆\nCONTROL");
        Button monitor = navButton("◉\nMONITOR");
        Button okx = navButton("↗\nOKX");

        nav.addView(home, weight());
        nav.addView(control, weight());
        nav.addView(monitor, weight());
        nav.addView(okx, weight());

        home.setOnClickListener(v -> showHome());
        control.setOnClickListener(v -> showControls());
        monitor.setOnClickListener(v -> webView.loadUrl(MONITOR_URL));
        okx.setOnClickListener(v -> startActivity(
                new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.okx.com/"))));

        LinearLayout outer = new LinearLayout(this);
        outer.setPadding(dp(12), dp(2), dp(12), dp(10));
        outer.addView(nav, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(72)));
        return outer;
    }

    private int dp(int value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private LinearLayout.LayoutParams weight() {
        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        p.setMargins(dp(3), 0, dp(3), 0);
        return p;
    }

    private Button navButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(9);
        b.setTextColor(Color.rgb(231, 235, 241));
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(2), dp(4), dp(2), dp(4));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(22, 29, 40));
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), Color.rgb(42, 52, 68));
        b.setBackground(bg);
        return b;
    }

    private void showHome() {
        String html =
                "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>" +
                "<style>*{box-sizing:border-box}body{font-family:system-ui;background:#070a0e;color:#f7f9fc;margin:0;padding:16px}" +
                ".hero{background:linear-gradient(145deg,#111722,#0c1118);border:1px solid #263142;border-radius:24px;padding:20px}" +
                ".ey{font-size:10px;letter-spacing:.13em;color:#cda552;font-weight:900}.title{font-size:32px;font-weight:900;margin:10px 0 5px}" +
                ".sub{color:#8e9bad;line-height:1.45;font-size:14px}.grid{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:12px}" +
                ".card{background:#101620;border:1px solid #222d3d;border-radius:18px;padding:15px;min-height:96px}.wide{grid-column:1/-1}" +
                ".k{font-size:10px;color:#7e8b9c;letter-spacing:.08em;font-weight:800}.v{font-size:21px;font-weight:900;margin-top:6px}" +
                ".green{color:#5be4ae}.gold{color:#d7b566}.muted{color:#8b97a7;font-size:12px;line-height:1.4;margin-top:6px}" +
                "</style></head><body>" +
                "<div class='hero'><div class='ey'>JOHN • COMMAND CENTER</div><div class='title'>Control. Verify. Decide.</div>" +
                "<div class='sub'>Tot ce ai nevoie ca să verifici John din telefon: monitor live, telecomandă, status și acces rapid la OKX.</div></div>" +
                "<div class='grid'>" +
                "<div class='card'><div class='k'>SYSTEM</div><div class='v green'>READY</div><div class='muted'>Monitor + Remote disponibile.</div></div>" +
                "<div class='card'><div class='k'>MODE</div><div class='v gold'>PAPER</div><div class='muted'>Live = confirmare manuală.</div></div>" +
                "<div class='card wide'><div class='k'>QUICK CHECK</div><div class='muted'>Folosește CONTROL pentru PAUSE / RESUME / EMERGENCY STOP / RESET. Mini-monitorul de jos rămâne vizibil permanent.</div></div>" +
                "</div></body></html>";
        webView.loadDataWithBaseURL("https://john.local/", html, "text/html", "UTF-8", null);
    }

    private void showControls() {
        String html =
                "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>" +
                "<style>*{box-sizing:border-box}body{font-family:system-ui;background:#070a0e;color:white;margin:0;padding:16px}" +
                "h2{margin:2px 0 4px}.sub{color:#8d99a9;font-size:13px;margin-bottom:14px}.grid{display:grid;grid-template-columns:1fr 1fr;gap:10px}" +
                "form{margin:0}button{width:100%;border:0;border-radius:16px;padding:17px 8px;font-weight:900;color:white;font-size:14px}" +
                ".green{background:#14532d}.amber{background:#8a5a12}.red{background:#8b1f2c}.slate{background:#273244}.blue{background:#1d4ed8}" +
                ".wide{grid-column:1/-1}.note{margin-top:14px;background:#101620;border:1px solid #222d3d;border-radius:18px;padding:14px;color:#99a5b5;font-size:12px;line-height:1.45}" +
                "</style></head><body><h2>JOHN CONTROL</h2><div class='sub'>Comenzi rapide pentru workflow-ul PAPER.</div>" +
                "<div class='grid'>" +
                "<form method='post' action='" + REMOTE_ACTION_URL + "'><input type='hidden' name='action' value='RESUME'><button class='green'>▶ RESUME</button></form>" +
                "<form method='post' action='" + REMOTE_ACTION_URL + "'><input type='hidden' name='action' value='PAUSE'><button class='amber'>⏸ PAUSE</button></form>" +
                "<form method='post' action='" + REMOTE_ACTION_URL + "'><input type='hidden' name='action' value='EMERGENCY_STOP'><button class='red'>🚨 EMERGENCY STOP</button></form>" +
                "<form method='post' action='" + REMOTE_ACTION_URL + "'><input type='hidden' name='action' value='RESET_EMERGENCY'><button class='slate'>↻ RESET STOP</button></form>" +
                "<form class='wide' method='get' action='" + REMOTE_URL + "'><button class='blue'>STATUS / FULL REMOTE</button></form>" +
                "</div><div class='note'>Comenzile de aici controlează modul PAPER al lui John. Pentru bani reali, ordinele rămân cu confirmare manuală în OKX.</div>" +
                "</body></html>";
        webView.loadDataWithBaseURL("https://john.local/", html, "text/html", "UTF-8", null);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
