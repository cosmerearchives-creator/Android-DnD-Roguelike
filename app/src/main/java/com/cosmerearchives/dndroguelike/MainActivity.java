package com.cosmerearchives.dndroguelike;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.content.FileProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String REPO = "cosmerearchives-creator/Android-DnD-Roguelike";
    private static final String API = "https://api.github.com/repos/" + REPO + "/releases/latest";
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private TextView status;
    private Button check;
    private boolean busy;
    private boolean awaitingPermission;
    private File readyApk;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = (int)(24 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad * 2, pad, pad);
        root.setBackgroundColor(Color.rgb(21,23,32));
        scroll.addView(root);
        TextView title = text("Android DnD\nRoguelike", 30);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        root.addView(text("v" + BuildConfig.VERSION_NAME, 16));
        TextView intro = text("Your adventure starts here.", 20);
        intro.setPadding(0,pad,0,pad);
        root.addView(intro);
        PlayerCharacter existing=PlayerCharacter.load(this);
        Button create = new Button(this);
        create.setText(existing!=null&&!existing.completed?"Resume character creation":"Create High Elf Fighter");
        create.setOnClickListener(v -> startActivity(new Intent(this, CharacterCreatorActivity.class)));
        root.addView(create);
        Button explore = new Button(this);
        explore.setText("Explore Wildlands (isometric test)");
        explore.setOnClickListener(v -> startActivity(new Intent(this, OutdoorActivity.class)));
        root.addView(explore);
        Button play = new Button(this);
        play.setText("Classic combat arena");
        play.setOnClickListener(v -> startActivity(new Intent(this, GameActivity.class)));
        root.addView(play);
        check = new Button(this);
        check.setText("Check for updates");
        check.setOnClickListener(v -> checkUpdates());
        root.addView(check);
        status = text("Ready", 16);
        status.setPadding(0,pad,0,pad);
        root.addView(status);
        setContentView(scroll);
        checkUpdates();
    }

    private TextView text(String value, int size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(Color.rgb(233,228,244));
        return view;
    }

    private void ui(Runnable action) {
        runOnUiThread(() -> { if (!isFinishing() && !isDestroyed()) action.run(); });
    }

    private void finishWork(String message) {
        ui(() -> { busy=false; check.setEnabled(true); status.setText(message); });
    }

    private void checkUpdates() {
        if (busy) return;
        busy=true;
        check.setEnabled(false);
        status.setText("Checking for updates…");
        worker.execute(() -> {
            try {
                JSONObject release = new JSONObject(read(API));
                if (release.optBoolean("draft") || release.optBoolean("prerelease")) {
                    finishWork("No stable update available."); return;
                }
                JSONArray assets = release.getJSONArray("assets");
                String manifestUrl=null;
                for(int i=0;i<assets.length();i++) {
                    JSONObject asset=assets.getJSONObject(i);
                    if ("update.json".equals(asset.getString("name"))) manifestUrl=asset.getString("browser_download_url");
                }
                if (manifestUrl==null) throw new IOException("Latest release has no update information.");
                JSONObject update = new JSONObject(read(manifestUrl));
                int code=update.getInt("versionCode");
                String name=update.getString("versionName");
                String url=update.getString("apkUrl");
                String hash=update.getString("sha256");
                if (!release.getString("tag_name").equals("v"+name)) throw new IOException("Release version mismatch.");
                if (!url.startsWith("https://github.com/"+REPO+"/releases/download/")) throw new IOException("Unexpected APK source.");
                boolean listed=false;
                for(int i=0;i<assets.length();i++) if(url.equals(assets.getJSONObject(i).getString("browser_download_url"))) listed=true;
                if (!listed || !hash.matches("[0-9a-fA-F]{64}")) throw new IOException("Invalid update information.");
                if (code<=BuildConfig.VERSION_CODE) { finishWork("You’re up to date."); return; }
                ui(() -> {
                    busy=false;
                    check.setEnabled(true);
                    status.setText("Version "+name+" is available.");
                    new AlertDialog.Builder(this).setTitle("Update available")
                        .setMessage("Download version "+name+"?")
                        .setNegativeButton("Later",null)
                        .setPositiveButton("Download",(dialog,which)->download(url,hash,code)).show();
                });
            } catch (NotPublished e) { finishWork("No releases published yet."); }
            catch (Exception e) { finishWork("Couldn’t check for updates. Check your connection and try again."); }
        });
    }

    private HttpURLConnection connect(String address) throws IOException {
        if (!address.startsWith("https://")) throw new IOException("HTTPS required.");
        HttpURLConnection connection=(HttpURLConnection)new URL(address).openConnection();
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(30000);
        connection.setRequestProperty("User-Agent","Android-DnD-Roguelike/"+BuildConfig.VERSION_NAME);
        connection.setRequestProperty("Accept","application/vnd.github+json");
        return connection;
    }

    private String read(String address) throws IOException {
        HttpURLConnection c=connect(address);
        try {
            int response=c.getResponseCode();
            if(response==404 && address.equals(API)) throw new NotPublished();
            if(response!=200) throw new IOException("HTTP "+response);
            try(InputStream input=c.getInputStream(); ByteArrayOutputStream output=new ByteArrayOutputStream()) {
                byte[] buffer=new byte[8192]; int n;
                while((n=input.read(buffer))!=-1) {
                    output.write(buffer,0,n);
                    if(output.size()>1024*1024) throw new IOException("Response too large.");
                }
                return output.toString("UTF-8");
            }
        } finally { c.disconnect(); }
    }

    private void download(String address,String expectedHash,int expectedCode) {
        if(busy) return;
        busy=true;
        check.setEnabled(false);
        status.setText("Downloading update…");
        worker.execute(() -> {
            File folder=new File(getCacheDir(),"updates");
            File partial=new File(folder,"update.part");
            File target=new File(folder,"update.apk");
            try {
                if(!folder.exists() && !folder.mkdirs()) throw new IOException("Cannot create download folder.");
                HttpURLConnection c=connect(address);
                try {
                    if(c.getResponseCode()!=200) throw new IOException("Download failed.");
                    long length=c.getContentLengthLong();
                    MessageDigest digest=MessageDigest.getInstance("SHA-256");
                    long total=0; int last=-1;
                    try(InputStream in=c.getInputStream(); OutputStream out=new FileOutputStream(partial)) {
                        byte[] buffer=new byte[65536]; int n;
                        while((n=in.read(buffer))!=-1) {
                            if(Thread.currentThread().isInterrupted()) throw new IOException("Cancelled.");
                            total+=n;
                            if(total>200L*1024*1024) throw new IOException("APK too large.");
                            digest.update(buffer,0,n); out.write(buffer,0,n);
                            int percent=length>0 ? (int)(total*100/length) : 0;
                            if(percent!=last) {
                                last=percent; final int p=percent;
                                ui(()->status.setText(length>0 ? "Downloading update: "+p+"%" : "Downloading update…"));
                            }
                        }
                    }
                    if(length>0 && length!=total) throw new IOException("Incomplete download.");
                    StringBuilder hash=new StringBuilder();
                    for(byte b:digest.digest()) hash.append(String.format("%02x",b & 255));
                    if(!expectedHash.equalsIgnoreCase(hash.toString())) throw new IOException("Checksum mismatch.");
                } finally { c.disconnect(); }
                validateApk(partial,expectedCode);
                if(target.exists() && !target.delete()) throw new IOException("Cannot replace old download.");
                if(!partial.renameTo(target)) throw new IOException("Cannot save APK.");
                ui(()->{
                    readyApk=target; busy=false; check.setEnabled(true);
                    status.setText("Download verified. Ready to install.");
                    install();
                });
            } catch(Exception e) {
                if(partial.exists()) partial.delete();
                finishWork("Update download failed. Please try again.");
            }
        });
    }

    @SuppressWarnings("deprecation")
    private void validateApk(File apk,int expectedCode) throws Exception {
        PackageManager pm=getPackageManager();
        int flags=Build.VERSION.SDK_INT>=28 ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
        PackageInfo candidate=pm.getPackageArchiveInfo(apk.getAbsolutePath(),flags);
        PackageInfo installed=pm.getPackageInfo(getPackageName(),flags);
        if(candidate==null || !getPackageName().equals(candidate.packageName)) throw new IOException("Wrong package.");
        long code=Build.VERSION.SDK_INT>=28 ? candidate.getLongVersionCode() : candidate.versionCode;
        if(code!=expectedCode || code<=BuildConfig.VERSION_CODE) throw new IOException("Wrong version.");
        Signature[] wanted=Build.VERSION.SDK_INT>=28 ? installed.signingInfo.getApkContentsSigners() : installed.signatures;
        Signature[] actual=Build.VERSION.SDK_INT>=28 ? candidate.signingInfo.getApkContentsSigners() : candidate.signatures;
        if(wanted==null || actual==null || !Arrays.equals(wanted,actual)) throw new IOException("Signing identity mismatch.");
    }

    private void install() {
        if(readyApk==null || !readyApk.exists()) return;
        if(!getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(this).setTitle("Allow app updates")
                .setMessage("Enable ‘Allow from this source’ for Android DnD Roguelike, then return here to install.")
                .setNegativeButton("Later",(d,w)->status.setText("Update downloaded. Check again when you’re ready."))
                .setPositiveButton("Open settings",(d,w)-> {
                    awaitingPermission=true;
                    startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())));
                }).show();
            return;
        }
        try {
            Uri uri=FileProvider.getUriForFile(this,getPackageName()+".files",readyApk);
            Intent intent=new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri,"application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
            status.setText("Finish installing in Android’s installer.");
        } catch(Exception e) {
            status.setText("Couldn’t open installer. Please try again.");
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if(awaitingPermission) {
            awaitingPermission=false;
            if(getPackageManager().canRequestPackageInstalls()) install();
            else status.setText("Installation permission wasn’t enabled. Check again to retry.");
        }
    }
    @Override protected void onDestroy() { worker.shutdownNow(); super.onDestroy(); }
    private static class NotPublished extends IOException {}
}
