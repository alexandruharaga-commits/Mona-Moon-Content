package com.auron.assistant;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.Executor;

public class MainActivity extends FragmentActivity {
    private static final String HOME =
            "https://alexandruharaga-commits.github.io/Mona-Moon-Content/auron/";
    private WebView webView;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

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
        settings.setUserAgentString(settings.getUserAgentString() + " AURON-Android/0.11");

        setupBiometrics();
        webView.addJavascriptInterface(new AuronNativeBridge(), "AuronNative");
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

    private void setupBiometrics() {
        Executor executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        sendBiometricResult(true, "Authenticated");
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        sendBiometricResult(false, errString == null ? "Authentication cancelled" : errString.toString());
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        sendBiometricResult(false, "Biometric not recognized");
                    }
                });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("AURON")
                .setSubtitle("Confirm your identity")
                .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG |
                        BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .setNegativeButtonText("Cancel")
                .build();
    }

    private void sendBiometricResult(boolean ok, String message) {
        if (webView == null) return;
        String safe = message == null ? "" : message.replace("\\", "\\\\").replace("'", "\\'");
        webView.post(() -> webView.evaluateJavascript(
                "window.onAuronBiometric && window.onAuronBiometric(" + ok + ",'" + safe + "')", null));
    }

    private final class AuronNativeBridge {
        @JavascriptInterface
        public boolean canUseBiometrics() {
            int result = BiometricManager.from(MainActivity.this).canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG |
                    BiometricManager.Authenticators.BIOMETRIC_WEAK);
            return result == BiometricManager.BIOMETRIC_SUCCESS;
        }

        @JavascriptInterface
        public void requestBiometric() {
            runOnUiThread(() -> {
                int result = BiometricManager.from(MainActivity.this).canAuthenticate(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG |
                        BiometricManager.Authenticators.BIOMETRIC_WEAK);
                if (result == BiometricManager.BIOMETRIC_SUCCESS) {
                    biometricPrompt.authenticate(promptInfo);
                } else {
                    sendBiometricResult(false, "Biometric authentication is not available on this device");
                }
            });
        }
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
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
