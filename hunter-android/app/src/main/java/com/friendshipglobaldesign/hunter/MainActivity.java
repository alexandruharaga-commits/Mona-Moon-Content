package com.friendshipglobaldesign.hunter;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.biometrics.BiometricPrompt;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

public class MainActivity extends Activity {
    public static final String STATUS_URL = "https://alalex.app.n8n.cloud/webhook/hunter/status";
    public static final String SCAN_URL = "https://alalex.app.n8n.cloud/webhook/hunter/scan";
    public static final String ALERT_CHANNEL = "hunter_alpha_alerts";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 101;
    private static final String INSTALL_STATUS_ACTION = "com.friendshipglobaldesign.hunter.INSTALL_STATUS";

    private WebView webView;
    private SharedPreferences prefs;
    private boolean openNotificationsAfterLoad = false;\n    private String notificationKeyAfterLoad = "";

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
        s.setUserAgentString(s.getUserAgentString()+" HUNTER-ALPHA/0.6.0");

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

        handleInstallStatus(getIntent());
    }

    @Override protected void onResume(){
        super.onResume();
        String pending=prefs.getString("pending_update_apk","");
        if(!pending.isEmpty() && canInstallPackages()){
            prefs.edit().remove("pending_update_apk").apply();
            File f=new File(pending);
            if(f.exists())installApk(f);
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

    private boolean canInstallPackages(){
        return Build.VERSION.SDK_INT<26 || getPackageManager().canRequestPackageInstalls();
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
            if(!"https".equalsIgnoreCase(uri.getScheme())){toastUi("HUNTER opens HTTPS links only.");return;}
            startActivity(new Intent(Intent.ACTION_VIEW,uri));
        }catch(Exception e){toastUi("Cannot open this link.");}
    }

    private void readLaunchIntent(Intent intent){
        if(intent!=null&&intent.getBooleanExtra("open_notifications",false))openNotificationsAfterLoad=true;
    }

    private void dispatchNotificationOpen(){
        if(!openNotificationsAfterLoad||webView==null)return;
        openNotificationsAfterLoad=false;
        final String key=notificationKeyAfterLoad;
        notificationKeyAfterLoad="";
        webView.postDelayed(()->{
            if(key!=null&&!key.isEmpty()){
                webView.evaluateJavascript(
                    "window.hunterOpenNotificationFromNative&&window.hunterOpenNotificationFromNative("+JSONObject.quote(key)+");",
                    null
                );
            }else{
                webView.evaluateJavascript(
                    "window.hunterOpenNotificationsFromNative&&window.hunterOpenNotificationsFromNative();",
                    null
                );
            }
        },350);
    }

    @Override protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);setIntent(intent);readLaunchIntent(intent);dispatchNotificationOpen();handleInstallStatus(intent);
    }

    private void handleInstallStatus(Intent intent){
        if(intent==null||!INSTALL_STATUS_ACTION.equals(intent.getAction()))return;
        int status=intent.getIntExtra(PackageInstaller.EXTRA_STATUS,PackageInstaller.STATUS_FAILURE);
        if(status==PackageInstaller.STATUS_PENDING_USER_ACTION){
            Intent confirm=intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if(confirm!=null){
                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try{startActivity(confirm);}catch(Exception e){toastUi("Android could not open the update confirmation.");}
            }
        }else if(status==PackageInstaller.STATUS_SUCCESS){
            toastUi("HUNTER ALPHA update installed.");
        }else{
            String msg=intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
            toastUi("Update was not installed"+(msg==null?".":": "+msg));
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==NOTIFICATION_PERMISSION_REQUEST){
            boolean granted=grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED;
            prefs.edit().putBoolean("alerts_enabled",granted).putBoolean("alerts_baselined",false).apply();
            if(granted){
                boolean scheduled=HunterJobs.schedule(this);
                toastUi(scheduled?"HUNTER alerts enabled.":"Alerts enabled; background watcher will retry next time Hunter opens.");
            }else{
                HunterJobs.cancel(this);
                toastUi("Notification permission was not granted.");
            }
            if(webView!=null)webView.postDelayed(()->webView.evaluateJavascript("window.renderAlerts&&window.renderAlerts();",null),250);
        }
    }

    private String downloadText(String urlText) throws Exception{
        URL url=new URL(urlText);
        if(!"https".equalsIgnoreCase(url.getProtocol()))throw new IllegalArgumentException("HTTPS required");
        HttpURLConnection c=(HttpURLConnection)url.openConnection();
        c.setConnectTimeout(10000);c.setReadTimeout(20000);c.setRequestProperty("Cache-Control","no-cache");
        int code=c.getResponseCode();if(code<200||code>=300){c.disconnect();throw new Exception("HTTP "+code);}
        StringBuilder sb=new StringBuilder();
        try(BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8))){
            String line;while((line=r.readLine())!=null)sb.append(line.trim());
        }finally{c.disconnect();}
        return sb.toString();
    }

    private String sha256(byte[] data) throws Exception{
        MessageDigest md=MessageDigest.getInstance("SHA-256");
        byte[] d=md.digest(data);StringBuilder s=new StringBuilder();
        for(byte b:d)s.append(String.format(Locale.US,"%02x",b));
        return s.toString();
    }

    private long currentVersionCode(){
        try{
            PackageInfo p=getPackageManager().getPackageInfo(getPackageName(),0);
            return Build.VERSION.SDK_INT>=28?p.getLongVersionCode():p.versionCode;
        }catch(Exception e){return 0;}
    }

    private void postUpdateStatus(String message){
        final String m=message==null?"":message;
        runOnUiThread(()->{
            if(webView!=null)webView.evaluateJavascript("window.hunterUpdateStatus&&window.hunterUpdateStatus("+JSONObject.quote(m)+");",null);
        });
    }

    private void requestInstallPermission(File apk){
        prefs.edit().putString("pending_update_apk",apk.getAbsolutePath()).apply();
        try{
            Intent i=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName()));
            startActivity(i);
            toastUi("Allow HUNTER ALPHA to install updates, then return to the app.");
        }catch(Exception e){toastUi("Open Android settings and allow HUNTER ALPHA to install unknown apps.");}
    }

    private void installApk(File apk){
        if(!canInstallPackages()){requestInstallPermission(apk);return;}
        new Thread(()->{
            PackageInstaller.Session session=null;
            try{
                PackageInstaller installer=getPackageManager().getPackageInstaller();
                PackageInstaller.SessionParams params=new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                params.setAppPackageName(getPackageName());
                int sessionId=installer.createSession(params);
                session=installer.openSession(sessionId);
                try(InputStream in=new FileInputStream(apk);OutputStream out=session.openWrite("base.apk",0,apk.length())){
                    byte[] buf=new byte[65536];int n;
                    while((n=in.read(buf))>0)out.write(buf,0,n);
                    session.fsync(out);
                }
                Intent callback=new Intent(MainActivity.this,MainActivity.class).setAction(INSTALL_STATUS_ACTION);
                PendingIntent pending=PendingIntent.getActivity(MainActivity.this,sessionId,callback,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE);
                session.commit(pending.getIntentSender());
                session.close();session=null;
                postUpdateStatus("Android is verifying the signed Hunter update…");
            }catch(Exception e){
                postUpdateStatus("Update install failed: "+e.getMessage());
                toastUi("Could not start the update installer.");
                try{if(session!=null)session.close();}catch(Exception ignored){}
            }
        }).start();
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
                        toastUi("Invalid or insecure external link.");return;
                    }
                    String status=sourceStatus==null||sourceStatus.trim().isEmpty()?"UNVERIFIED":sourceStatus;
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Open external source?")
                        .setMessage("Domain: "+host+"\nSource status: "+status+"\n\nVerify the domain before connecting a wallet, signing a message or approving any transaction.")
                        .setNegativeButton("CANCEL",null)
                        .setPositiveButton("OPEN",(d,w)->openHttps(target))
                        .show();
                }catch(Exception e){toastUi("Cannot verify this link.");}
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

        @JavascriptInterface public String getNotificationHistory(){return prefs.getString("notification_history_json","[]");}
        @JavascriptInterface public void clearNotificationHistory(){prefs.edit().remove("notification_history_json").apply();}
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
                    toastUi(scheduled?"HUNTER alerts enabled.":"Alerts enabled; background scheduling will retry automatically.");
                    if(webView!=null)webView.postDelayed(()->webView.evaluateJavascript("window.renderAlerts&&window.renderAlerts();",null),250);
                }catch(Throwable t){
                    prefs.edit().putBoolean("alerts_enabled",false).apply();
                    HunterJobs.cancel(MainActivity.this);
                    toastUi("Could not enable background alerts. Hunter remains usable.");
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
                    prompt.authenticate(new android.os.CancellationSignal(),getMainExecutor(),new BiometricPrompt.AuthenticationCallback(){
                        @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result){
                            super.onAuthenticationSucceeded(result);
                            if(webView!=null)webView.evaluateJavascript("window.hunterBiometricSuccess&&window.hunterBiometricSuccess();",null);
                        }
                        @Override public void onAuthenticationError(int code,CharSequence err){
                            super.onAuthenticationError(code,err);
                            if(code!=BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED&&code!=BiometricPrompt.BIOMETRIC_ERROR_CANCELED)toast(String.valueOf(err));
                        }
                    });
                }catch(Throwable t){toast("Biometric authentication is not available on this device.");}
            });
        }

        @JavascriptInterface public void installUpdate(String manifestUrl){
            final String manifest=manifestUrl==null?"":manifestUrl.trim();
            new Thread(()->{
                try{
                    postUpdateStatus("Downloading signed update metadata…");
                    JSONObject m=new JSONObject(downloadText(manifest));
                    long code=m.optLong("versionCode",0);
                    if(code<=currentVersionCode()){postUpdateStatus("HUNTER ALPHA is already up to date.");return;}
                    JSONArray parts=m.optJSONArray("parts");
                    if(parts==null||parts.length()==0){postUpdateStatus("Update package is not published yet.");return;}
                    String expected=m.optString("sha256","").toLowerCase(Locale.US);
                    StringBuilder encoded=new StringBuilder();
                    for(int i=0;i<parts.length();i++){
                        String part=parts.optString(i,"");
                        URL u=new URL(part);
                        if(!"https".equalsIgnoreCase(u.getProtocol())||!"raw.githubusercontent.com".equalsIgnoreCase(u.getHost())){
                            throw new SecurityException("Update source is not approved");
                        }
                        postUpdateStatus("Downloading update "+(i+1)+"/"+parts.length()+"…");
                        encoded.append(downloadText(part));
                    }
                    byte[] apk=Base64.decode(encoded.toString(),Base64.DEFAULT);
                    String actual=sha256(apk);
                    if(expected.isEmpty()||!actual.equals(expected))throw new SecurityException("SHA-256 verification failed");
                    File dir=getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                    if(dir==null)dir=getCacheDir();
                    File out=new File(dir,"HUNTER-ALPHA-update.apk");
                    try(FileOutputStream fos=new FileOutputStream(out)){fos.write(apk);fos.flush();}
                    postUpdateStatus("Update verified. Preparing Android installer…");
                    runOnUiThread(()->installApk(out));
                }catch(Exception e){
                    postUpdateStatus("Update failed: "+e.getMessage());
                    toastUi("Hunter update could not be prepared.");
                }
            }).start();
        }

        @JavascriptInterface public void resetNativeState(){prefs.edit().clear().apply();HunterJobs.cancel(MainActivity.this);}
        @JavascriptInterface public void exitApp(){runOnUiThread(MainActivity.this::finishAndRemoveTask);}
        @JavascriptInterface public void toast(String message){toastUi(message);}
    }

    private void toastUi(String message){
        String m=message==null?"":message;
        runOnUiThread(()->Toast.makeText(MainActivity.this,m,Toast.LENGTH_LONG).show());
    }

    @Override public void onBackPressed(){
        if(webView!=null)webView.evaluateJavascript("window.hunterBack&&window.hunterBack();",null);
        else finishAndRemoveTask();
    }
}
