package com.friendshipglobaldesign.hunter;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.biometrics.BiometricPrompt;
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
        s.setUserAgentString(s.getUserAgentString()+" HUNTER-ALPHA/0.5.4");

        webView.addJavascriptInterface(new HunterNative(),"HunterNative");
        webView.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request){
                return handleExternal(request.getUrl().toString());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view,String url){return handleExternal(url);}
            @Override public void onPageFinished(WebView view,String url){
                super.onPageFinished(view,url);dispatchNotificationOpen();
            }
        });

        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");

        if(prefs.getBoolean("alerts_enabled",false)&&notificationPermissionGranted()){
            HunterJobs.schedule(this);
        }
    }

    private void createNotificationChannel(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            NotificationChannel c=new NotificationChannel(ALERT_CHANNEL,"HUNTER ALPHA opportunities",NotificationManager.IMPORTANCE_HIGH);
            c.setDescription("New qualified crypto opportunities found by HUNTER ALPHA");
            NotificationManager m=getSystemService(NotificationManager.class);
            if(m!=null)m.createNotificationChannel(c);
        }
    }

    private boolean notificationPermissionGranted(){
        return Build.VERSION.SDK_INT<33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;
    }

    private boolean handleExternal(String url){
        if(url==null)return false;
        Uri uri=Uri.parse(url);String scheme=uri.getScheme();
        if("file".equalsIgnoreCase(scheme)||"about".equalsIgnoreCase(scheme))return false;
        if("http".equalsIgnoreCase(scheme)||"https".equalsIgnoreCase(scheme)){
            try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}
            catch(Exception e){Toast.makeText(this,"Cannot open this link.",Toast.LENGTH_LONG).show();}
            return true;
        }
        try{startActivity(new Intent(Intent.ACTION_VIEW,uri));return true;}
        catch(Exception e){Toast.makeText(this,"Cannot open this link.",Toast.LENGTH_LONG).show();return true;}
    }

    private void openHttps(String url){
        try{
            Uri uri=Uri.parse(url);
            if(!"https".equalsIgnoreCase(uri.getScheme())){Toast.makeText(this,"HUNTER opens HTTPS links only.",Toast.LENGTH_LONG).show();return;}
            startActivity(new Intent(Intent.ACTION_VIEW,uri));
        }catch(Exception e){Toast.makeText(this,"Cannot open this link.",Toast.LENGTH_LONG).show();}
    }

    private void readLaunchIntent(Intent intent){
        if(intent!=null&&intent.getBooleanExtra("open_notifications",false))openNotificationsAfterLoad=true;
    }

    private void dispatchNotificationOpen(){
        if(openNotificationsAfterLoad&&webView!=null){
            openNotificationsAfterLoad=false;
            webView.postDelayed(()->webView.evaluateJavascript("window.hunterOpenNotificationsFromNative&&window.hunterOpenNotificationsFromNative();",null),350);
        }
    }

    @Override protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);setIntent(intent);readLaunchIntent(intent);dispatchNotificationOpen();
    }

    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==NOTIFICATION_PERMISSION_REQUEST){
            boolean granted=grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED;
            prefs.edit().putBoolean("alerts_enabled",granted).putBoolean("alerts_baselined",false).apply();
            if(granted){
                boolean scheduled=HunterJobs.schedule(this);
                Toast.makeText(this,scheduled?"HUNTER alerts enabled.":"Alerts enabled; background watcher will retry next time Hunter opens.",Toast.LENGTH_LONG).show();
            }else{
                HunterJobs.cancel(this);
                Toast.makeText(this,"Notification permission was not granted.",Toast.LENGTH_LONG).show();
            }
            if(webView!=null)webView.postDelayed(()->webView.evaluateJavascript("window.renderAlerts&&window.renderAlerts();",null),250);
        }
    }

    public class HunterNative {
        @JavascriptInterface public boolean savePublicWallet(String label,String address){
            String l=label==null?"":label.trim(),a=address==null?"":address.trim();
            if(a.length()<20||a.length()>160||a.contains(" ")){toast("Enter a valid public wallet address.");return false;}
            String lower=a.toLowerCase();
            if(lower.contains("seed")||lower.contains("private")||lower.contains("recovery")||lower.contains("secret")){
                toast("Never enter a seed phrase or private key.");return false;
            }
            try{
                JSONObject w=new JSONObject();w.put("label",l.isEmpty()?"Public wallet":l);w.put("address",a);w.put("mode","WATCH_ONLY");w.put("savedAt",System.currentTimeMillis());
                prefs.edit().putString("public_wallet",w.toString()).apply();toast("Public wallet saved in watch-only mode.");return true;
            }catch(Exception e){return false;}
        }

        @JavascriptInterface public String getPublicWallet(){return prefs.getString("public_wallet","{}");}
        @JavascriptInterface public void clearPublicWallet(){prefs.edit().remove("public_wallet").apply();toast("Public wallet removed.");}

        @JavascriptInterface public void openWalletProvider(String name){
            String url;String n=name==null?"":name.trim();
            switch(n){
                case "MetaMask":url="https://metamask.io/";break;
                case "Trust Wallet":url="https://trustwallet.com/";break;
                case "WalletConnect":url="https://walletconnect.network/";break;
                case "Coinbase Wallet":url="https://www.coinbase.com/wallet";break;
                case "OKX Wallet":url="https://www.okx.com/web3";break;
                default:toast("Wallet provider is not available.");return;
            }
            toast("Opening the official provider. Hunter does not connect or sign automatically.");
            openHttps(url);
        }

        @JavascriptInterface public void openExternalChecked(String url,String sourceStatus){
            final String target=url==null?"":url.trim();
            runOnUiThread(()->{
                try{
                    Uri uri=Uri.parse(target);String host=uri.getHost();
                    if(!"https".equalsIgnoreCase(uri.getScheme())||host==null||host.trim().isEmpty()){
                        Toast.makeText(MainActivity.this,"Invalid or insecure external link.",Toast.LENGTH_LONG).show();return;
                    }
                    String status=sourceStatus==null||sourceStatus.trim().isEmpty()?"UNVERIFIED":sourceStatus;
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Open external source?")
                        .setMessage("Domain: "+host+"\nSource status: "+status+"\n\nVerify the domain before connecting a wallet, signing a message or approving any transaction.")
                        .setNegativeButton("CANCEL",null)
                        .setPositiveButton("OPEN",(d,w)->openHttps(target))
                        .show();
                }catch(Exception e){Toast.makeText(MainActivity.this,"Cannot verify this link.",Toast.LENGTH_LONG).show();}
            });
        }

        @JavascriptInterface public void triggerScan(){
            new Thread(()->{
                boolean ok=false;String message;HttpURLConnection c=null;
                try{
                    c=(HttpURLConnection)new URL(SCAN_URL).openConnection();
                    c.setRequestMethod("POST");c.setConnectTimeout(8000);c.setReadTimeout(12000);c.setDoOutput(true);
                    c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Accept","application/json");
                    byte[] body="{}".getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(body.length);
                    try(OutputStream out=c.getOutputStream()){out.write(body);}
                    int code=c.getResponseCode();ok=code>=200&&code<300;
                    message=ok?"Fresh hunt started. Hunter is scanning now.":"On-demand scan is not active on the backend yet. Showing the latest scheduled results.";
                }catch(Exception e){message="On-demand scan is not active on the backend yet. Showing the latest scheduled results.";}
                finally{if(c!=null)c.disconnect();}
                final boolean finalOk=ok;final String finalMessage=message;
                runOnUiThread(()->{if(webView!=null)webView.evaluateJavascript("window.hunterScanResult&&window.hunterScanResult("+finalOk+","+JSONObject.quote(finalMessage)+");",null);});
            }).start();
        }

        @JavascriptInterface public boolean alertsEnabled(){return prefs.getBoolean("alerts_enabled",false);}
        @JavascriptInterface public boolean notificationPermissionGranted(){return MainActivity.this.notificationPermissionGranted();}

        @JavascriptInterface public void enableAlerts(){
            runOnUiThread(()->{
                try{
                    if(Build.VERSION.SDK_INT>=33&&!notificationPermissionGranted()){
                        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},NOTIFICATION_PERMISSION_REQUEST);return;
                    }
                    prefs.edit().putBoolean("alerts_enabled",true).putBoolean("alerts_baselined",false).apply();
                    boolean scheduled=HunterJobs.schedule(MainActivity.this);
                    Toast.makeText(MainActivity.this,scheduled?"HUNTER alerts enabled.":"Alerts enabled; background scheduling will retry automatically.",Toast.LENGTH_LONG).show();
                    if(webView!=null)webView.postDelayed(()->webView.evaluateJavascript("window.renderAlerts&&window.renderAlerts();",null),250);
                }catch(Throwable t){
                    prefs.edit().putBoolean("alerts_enabled",false).apply();
                    HunterJobs.cancel(MainActivity.this);
                    Toast.makeText(MainActivity.this,"Could not enable background alerts. Hunter remains usable.",Toast.LENGTH_LONG).show();
                    if(webView!=null)webView.postDelayed(()->webView.evaluateJavascript("window.renderAlerts&&window.renderAlerts();",null),250);
                }
            });
        }

        @JavascriptInterface public void disableAlerts(){
            prefs.edit().putBoolean("alerts_enabled",false).putBoolean("alerts_baselined",false).apply();
            HunterJobs.cancel(MainActivity.this);toast("HUNTER alerts disabled.");
        }

        @JavascriptInterface public void openNotificationSettings(){
            runOnUiThread(()->{
                try{
                    Intent i=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);i.putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName());startActivity(i);
                }catch(Exception e){toast("Cannot open notification settings.");}
            });
        }

        @JavascriptInterface public void startBiometric(){
            runOnUiThread(()->{
                if(Build.VERSION.SDK_INT<28){toast("Biometric login requires Android 9 or newer.");return;}
                try{
                    BiometricPrompt prompt=new BiometricPrompt.Builder(MainActivity.this)
                        .setTitle("HUNTER ALPHA")
                        .setSubtitle("Unlock your local Hunter profile")
                        .setDescription("Authentication stays on this device.")
                        .setNegativeButton("USE EMAIL",getMainExecutor(),(d,w)->{})
                        .build();
                    prompt.authenticate(getCancellationSignal(),getMainExecutor(),new BiometricPrompt.AuthenticationCallback(){
                        @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result){
                            super.onAuthenticationSucceeded(result);
                            if(webView!=null)webView.evaluateJavascript("window.hunterBiometricSuccess&&window.hunterBiometricSuccess();",null);
                        }
                        @Override public void onAuthenticationError(int code,CharSequence err){
                            super.onAuthenticationError(code,err);
                            if(code!=BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED&&code!=BiometricPrompt.BIOMETRIC_ERROR_NEGATIVE_BUTTON)toast(String.valueOf(err));
                        }
                    });
                }catch(Throwable t){toast("Biometric authentication is not available on this device.");}
            });
        }

        private android.os.CancellationSignal getCancellationSignal(){return new android.os.CancellationSignal();}

        @JavascriptInterface public void resetNativeState(){prefs.edit().clear().apply();HunterJobs.cancel(MainActivity.this);}
        @JavascriptInterface public void exitApp(){runOnUiThread(MainActivity.this::finishAndRemoveTask);}
        @JavascriptInterface public void toast(String message){String m=message==null?"":message;runOnUiThread(()->Toast.makeText(MainActivity.this,m,Toast.LENGTH_LONG).show());}
    }

    @Override public void onBackPressed(){
        if(webView!=null)webView.evaluateJavascript("window.hunterBack&&window.hunterBack();",null);
        else finishAndRemoveTask();
    }
}
