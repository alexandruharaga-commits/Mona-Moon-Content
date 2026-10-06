package com.friendshipglobaldesign.hunter;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.graphics.drawable.GradientDrawable;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private static final String HUNTER_STATUS_URL = "https://alalex.app.n8n.cloud/webhook/hunter/status";
    private static final String VERSION = "0.1";
    private WebView webView;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("hunter_prefs", Context.MODE_PRIVATE);

        getWindow().setStatusBarColor(Color.rgb(5, 9, 8));
        getWindow().setNavigationBarColor(Color.rgb(5, 9, 8));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(5, 9, 8));

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
            return insets;
        });

        root.addView(buildHeader());

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(5, 9, 8));
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowContentAccess(false);
        webView.addJavascriptInterface(new HunterNative(), "HunterNative");
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        root.addView(webView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(buildNav());

        setContentView(root);
        root.requestApplyInsets();
        showHome();
    }

    private LinearLayout buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20), dp(12), dp(20), dp(10));
        header.setBackgroundColor(Color.rgb(5, 9, 8));

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);

        android.widget.TextView name = new android.widget.TextView(this);
        name.setText("HUNTER");
        name.setTextColor(Color.rgb(239, 247, 243));
        name.setTextSize(25);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(name);

        android.widget.TextView premium = new android.widget.TextView(this);
        premium.setText("  PREMIUM");
        premium.setTextColor(Color.rgb(214, 180, 94));
        premium.setTextSize(12);
        premium.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(premium);

        android.widget.TextView live = new android.widget.TextView(this);
        live.setText("   ● SCOUT");
        live.setTextColor(Color.rgb(33, 208, 122));
        live.setTextSize(10);
        live.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(live);

        header.addView(row);

        android.widget.TextView sub = new android.widget.TextView(this);
        sub.setText("Crypto Opportunity Command Center • Watch-only v" + VERSION);
        sub.setTextColor(Color.rgb(126, 145, 136));
        sub.setTextSize(11);
        sub.setPadding(0, dp(2), 0, 0);
        header.addView(sub);
        return header;
    }

    private LinearLayout buildNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(8), dp(7), dp(8), dp(7));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(11, 18, 15));
        bg.setStroke(dp(1), Color.rgb(31, 54, 43));
        bg.setCornerRadius(dp(18));
        nav.setBackground(bg);

        Button home = navButton("⌂\nHOME");
        Button hunt = navButton("◎\nHUNT");
        Button wallets = navButton("◇\nWALLETS");
        Button execute = navButton("⚡\nEXECUTE");
        Button settings = navButton("⋯\nMORE");

        nav.addView(home, weight());
        nav.addView(hunt, weight());
        nav.addView(wallets, weight());
        nav.addView(execute, weight());
        nav.addView(settings, weight());

        home.setOnClickListener(v -> showHome());
        hunt.setOnClickListener(v -> showHunt());
        wallets.setOnClickListener(v -> showWallets());
        execute.setOnClickListener(v -> showExecute());
        settings.setOnClickListener(v -> showSettings());

        LinearLayout outer = new LinearLayout(this);
        outer.setPadding(dp(10), dp(2), dp(10), dp(10));
        outer.addView(nav, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(70)));
        return outer;
    }

    private LinearLayout.LayoutParams weight() {
        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        p.setMargins(dp(2), 0, dp(2), 0);
        return p;
    }

    private Button navButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(8);
        b.setTextColor(Color.rgb(229, 239, 233));
        b.setGravity(Gravity.CENTER);
        b.setPadding(1, 2, 1, 2);

        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.rgb(15, 26, 21));
        g.setStroke(dp(1), Color.rgb(35, 65, 50));
        g.setCornerRadius(dp(13));
        b.setBackground(g);
        return b;
    }

    private String css() {
        return "<style>*{box-sizing:border-box}body{font-family:system-ui,-apple-system,sans-serif;background:#050908;color:#eff7f3;margin:0;padding:16px}" +
                ".hero{background:linear-gradient(145deg,#0e1b15,#09110e);border:1px solid #1d3c2e;border-radius:24px;padding:20px;box-shadow:0 16px 40px #0008}" +
                ".ey{font-size:10px;letter-spacing:.16em;color:#d6b45e;font-weight:900}.title{font-size:30px;font-weight:950;margin:9px 0 5px}.sub{color:#89a095;line-height:1.45;font-size:13px}" +
                ".grid{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:12px}.card{background:#0b1410;border:1px solid #173326;border-radius:18px;padding:15px;min-height:96px}.wide{grid-column:1/-1}" +
                ".k{font-size:10px;color:#71877c;letter-spacing:.1em;font-weight:900}.v{font-size:20px;font-weight:950;margin-top:6px}.green{color:#21d07a}.gold{color:#d6b45e}.muted{color:#82988d;font-size:12px;line-height:1.45;margin-top:6px}" +
                ".pill{display:inline-block;border:1px solid #22573d;background:#0d2418;color:#4de093;border-radius:999px;padding:7px 10px;font-size:11px;font-weight:900;margin:5px 5px 0 0}" +
                "button{border:0;border-radius:15px;padding:14px 16px;font-weight:900;font-size:13px;background:#21d07a;color:#031008}button.secondary{background:#18241f;color:#e7efe9;border:1px solid #2c483a}" +
                "input{width:100%;background:#07100c;color:#eaf3ee;border:1px solid #254535;border-radius:13px;padding:13px;margin-top:8px}.danger{color:#ff8f99}.ok{color:#43dc8b}.row{display:flex;gap:8px;flex-wrap:wrap;margin-top:12px}" +
                ".op{background:#0b1410;border:1px solid #173326;border-radius:16px;padding:14px;margin-top:10px}.score{font-size:22px;font-weight:950;color:#21d07a}" +
                "</style>";
    }

    private String shell(String body, String script) {
        return "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1'>" +
                css() + "</head><body>" + body + (script == null ? "" : "<script>" + script + "</script>") + "</body></html>";
    }

    private void showHome() {
        String body =
                "<div class='hero'><div class='ey'>HUNTER • CRYPTO OPPORTUNITY INTELLIGENCE</div>" +
                "<div class='title'>Find first. Verify twice.</div>" +
                "<div class='sub'>Hunter caută airdrop-uri, quests, launchpools și rewards. Versiunea 0.1 este construită intenționat fără acces la chei private.</div>" +
                "<div class='row'><span class='pill'>WATCH ONLY</span><span class='pill'>SCORE ≥ 80</span><span class='pill'>NO SEED PHRASES</span></div></div>" +
                "<div class='grid'>" +
                "<div class='card'><div class='k'>HUNTER MODE</div><div class='v green'>SCOUT</div><div class='muted'>Căutare + filtrare.</div></div>" +
                "<div class='card'><div class='k'>EXECUTION</div><div class='v gold'>APPROVAL</div><div class='muted'>Semnătura rămâne la tine.</div></div>" +
                "<div class='card'><div class='k'>MIN SCORE</div><div class='v'>80/100</div><div class='muted'>Doar oportunități bune.</div></div>" +
                "<div class='card'><div class='k'>WALLETS</div><div class='v'>WATCH</div><div class='muted'>Adrese publice only.</div></div>" +
                "<div class='card wide'><div class='k'>ROADMAP</div><div class='muted'>v0.2: WalletConnect + eligibilitate on-chain. v0.3: claim/swap pregătit de Hunter, trimis la wallet pentru confirmare biometrică/PIN. Kill switch + limite gas rămân obligatorii.</div></div>" +
                "</div>";
        webView.loadDataWithBaseURL("https://hunter.local/", shell(body, null), "text/html", "UTF-8", null);
    }

    private void showHunt() {
        String body =
                "<div class='hero'><div class='ey'>LIVE HUNT</div><div class='title'>Opportunities</div>" +
                "<div class='sub'>Încarcă starea agentului Hunter și ultimele oportunități acceptate de filtrul de risc.</div>" +
                "<div class='row'><button onclick='refreshHunter()'>↻ REFRESH</button></div></div>" +
                "<div id='status' class='card' style='margin-top:12px'><div class='k'>STATUS</div><div class='v gold'>CONNECTING…</div></div>" +
                "<div id='ops'></div>";
        String script =
                "const endpoint='" + HUNTER_STATUS_URL + "';" +
                "function esc(s){return String(s??'').replace(/[&<>\"']/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;',\"'\":'&#39;'}[m]));}" +
                "async function refreshHunter(){const st=document.getElementById('status'),ops=document.getElementById('ops');" +
                "st.innerHTML='<div class=\"k\">STATUS</div><div class=\"v gold\">CHECKING…</div>';ops.innerHTML='';" +
                "try{const r=await fetch(endpoint,{cache:'no-store'});if(!r.ok)throw new Error('HTTP '+r.status);const j=await r.json();" +
                "const alerts=Array.isArray(j.lastAlerts)?j.lastAlerts:[];" +
                "st.innerHTML='<div class=\"k\">HUNTER</div><div class=\"v green\">'+esc(j.mode||'ONLINE')+'</div><div class=\"muted\">Seen: '+esc(j.seenCount||0)+' • Last run: '+esc(j.lastRunAt||'—')+'</div>';" +
                "if(!alerts.length){ops.innerHTML='<div class=\"card wide\" style=\"margin-top:10px\"><div class=\"k\">NO ACTIVE ALERTS</div><div class=\"muted\">Hunter nu are momentan oportunități ≥80 în status.</div></div>';return;}" +
                "ops.innerHTML=alerts.map(a=>'<div class=\"op\"><div class=\"score\">'+esc(a.score||'—')+'/100</div><b>'+esc(a.title||'Opportunity')+'</b><div class=\"muted\">'+esc(a.source||'')+' • Risk '+esc(a.risk||'—')+'<br>Cost: '+esc(a.cost||'—')+'<br>'+esc(a.action||'Review official source')+'</div>'+(a.link?'<div class=\"row\"><button class=\"secondary\" onclick=\"location.href=\\\''+esc(a.link)+'\\\'\">OPEN SOURCE</button></div>':'')+'</div>').join('');" +
                "}catch(e){st.innerHTML='<div class=\"k\">HUNTER STATUS</div><div class=\"v gold\">AGENT READY</div><div class=\"muted\">API n8n nu este publicată încă sau nu răspunde. Aplicația rămâne funcțională în watch-only. '+esc(e.message)+'</div>';}}" +
                "refreshHunter();";
        webView.loadDataWithBaseURL("https://hunter.local/", shell(body, script), "text/html", "UTF-8", null);
    }

    private void showWallets() {
        String body =
                "<div class='hero'><div class='ey'>WATCH-ONLY VAULT</div><div class='title'>Wallets</div>" +
                "<div class='sub'>Adaugi doar adrese publice. Hunter nu cere și nu salvează seed phrase, private key sau parola wallet-ului.</div></div>" +
                "<div class='card' style='margin-top:12px'><div class='k'>ADD WATCH WALLET</div>" +
                "<input id='label' placeholder='Label, ex. Main Wallet'><input id='address' placeholder='Public wallet address'>" +
                "<div class='row'><button onclick='addW()'>+ ADD</button></div></div><div id='list'></div>";
        String script =
                "function esc(s){return String(s??'').replace(/[&<>\"']/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;',\"'\":'&#39;'}[m]));}" +
                "function load(){let a=[];try{a=JSON.parse(HunterNative.listWallets())}catch(e){};document.getElementById('list').innerHTML=a.length?a.map((w,i)=>'<div class=\"op\"><b>'+esc(w.label)+'</b><div class=\"muted\">'+esc(w.address)+'</div><div class=\"row\"><button class=\"secondary\" onclick=\"HunterNative.removeWallet('+i+');load()\">REMOVE</button></div></div>').join(''):'<div class=\"card\" style=\"margin-top:10px\"><div class=\"muted\">Niciun wallet watch-only adăugat.</div></div>';}" +
                "function addW(){const l=document.getElementById('label').value.trim(),a=document.getElementById('address').value.trim();const ok=HunterNative.addWallet(l,a);if(ok){document.getElementById('label').value='';document.getElementById('address').value='';load();}}" +
                "load();";
        webView.loadDataWithBaseURL("https://hunter.local/", shell(body, script), "text/html", "UTF-8", null);
    }

    private void showExecute() {
        String body =
                "<div class='hero'><div class='ey'>EXECUTION CONTROL</div><div class='title'>Approve, then sign.</div>" +
                "<div class='sub'>Zona de execuție este pregătită pentru versiunile viitoare, dar v0.1 nu poate muta fonduri.</div></div>" +
                "<div class='grid'>" +
                "<div class='card'><div class='k'>WALLET CONTROL</div><div class='v gold'>LOCKED</div><div class='muted'>WalletConnect în v0.2.</div></div>" +
                "<div class='card'><div class='k'>AUTO SIGN</div><div class='v danger'>OFF</div><div class='muted'>Rămâne oprit.</div></div>" +
                "<div class='card'><div class='k'>GAS LIMIT</div><div class='v'>TBD</div><div class='muted'>Setat înainte de activare.</div></div>" +
                "<div class='card'><div class='k'>KILL SWITCH</div><div class='v ok'>ARMED</div><div class='muted'>Design obligatoriu.</div></div>" +
                "<div class='card wide'><div class='k'>FUTURE FLOW</div><div class='muted'>Hunter detectează → verifică eligibilitatea → simulează cost/gas → pregătește claim/swap → tu confirmi în wallet cu PIN/biometric → abia apoi tranzacția poate fi transmisă.</div></div>" +
                "</div><div class='row'><button onclick=\"HunterNative.notice('Execuția reală va fi activată doar cu wallet confirmation.')\">TEST APPROVAL FLOW</button></div>";
        webView.loadDataWithBaseURL("https://hunter.local/", shell(body, null), "text/html", "UTF-8", null);
    }

    private void showSettings() {
        String body =
                "<div class='hero'><div class='ey'>HUNTER PREMIUM</div><div class='title'>System</div><div class='sub'>Build v" + VERSION + " • Friendship Global Design</div></div>" +
                "<div class='grid'>" +
                "<div class='card wide'><div class='k'>STATUS API</div><div class='muted'>" + HUNTER_STATUS_URL + "</div></div>" +
                "<div class='card'><div class='k'>SECURITY</div><div class='v green'>SAFE</div><div class='muted'>No private keys.</div></div>" +
                "<div class='card'><div class='k'>NETWORK</div><div class='v'>HTTPS</div><div class='muted'>Cleartext disabled.</div></div>" +
                "<div class='card wide'><div class='k'>PRIVACY</div><div class='muted'>Wallet list is stored locally on this phone as watch-only public addresses. Do not enter seed phrases or private keys anywhere in Hunter.</div></div>" +
                "</div>";
        webView.loadDataWithBaseURL("https://hunter.local/", shell(body, null), "text/html", "UTF-8", null);
    }

    private int dp(int value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    public class HunterNative {
        @JavascriptInterface
        public String listWallets() {
            return prefs.getString("wallets", "[]");
        }

        @JavascriptInterface
        public boolean addWallet(String label, String address) {
            if (label == null) label = "";
            if (address == null) address = "";
            label = label.trim();
            address = address.trim();

            if (label.length() < 1 || address.length() < 20 || address.length() > 128 || address.contains(" ")) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Introdu un label și o adresă publică validă.", Toast.LENGTH_SHORT).show());
                return false;
            }

            String lower = address.toLowerCase();
            if (lower.contains("seed") || lower.contains("private") || lower.contains("secret") || lower.split(" ").length > 3) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Nu introduce seed phrase sau private key.", Toast.LENGTH_LONG).show());
                return false;
            }

            try {
                JSONArray arr = new JSONArray(prefs.getString("wallets", "[]"));
                JSONObject w = new JSONObject();
                w.put("label", label);
                w.put("address", address);
                arr.put(w);
                prefs.edit().putString("wallets", arr.toString()).apply();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Watch wallet adăugat.", Toast.LENGTH_SHORT).show());
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
                for (int i = 0; i < arr.length(); i++) if (i != index) next.put(arr.get(i));
                prefs.edit().putString("wallets", next.toString()).apply();
            } catch (Exception ignored) {}
        }

        @JavascriptInterface
        public void notice(String message) {
            runOnUiThread(() -> new AlertDialog.Builder(MainActivity.this)
                    .setTitle("HUNTER")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show());
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
