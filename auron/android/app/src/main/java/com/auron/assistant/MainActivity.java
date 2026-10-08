package com.auron.assistant;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String HOME =
            "https://alexandruharaga-commits.github.io/Mona-Moon-Content/auron/";
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(6, 6, 6));
        getWindow().setNavigationBarColor(Color.rgb(6, 6, 6));

        webView = new WebView(this);
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setUserAgentString(settings.getUserAgentString() + " AURON-Android/0.3");

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl());
            }

            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(Uri.parse(url));
            }
        });

        if (savedInstanceState == null) webView.loadUrl(HOME);
        else webView.restoreState(savedInstanceState);
    }

    private boolean handleUrl(Uri uri) {
        if (uri == null) return false;
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();

        if ("https".equals(scheme) &&
            ("alexandruharaga-commits.github.io".equals(host) ||
             "alalex.app.n8n.cloud".equals(host))) {
            return false;
        }

        if ("http".equals(scheme) || "https".equals(scheme) || "market".equals(scheme)) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (Exception e) {
                Toast.makeText(this, "Nu pot deschide linkul extern.", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
