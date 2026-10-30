package com.llf.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.core.content.FileProvider;

import java.io.File;

public class MainActivity extends Activity {
    private static final int REQ_FILE = 77, REQ_CAM_PERM = 78;
    private WebView web;
    private ValueCallback<Uri[]> fileCb;
    private Uri camUri;
    private String url;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        url = getString(R.string.app_url);
        web = new WebView(this);
        web.setBackgroundColor(Color.BLACK);
        setContentView(web, new ViewGroup.LayoutParams(-1, -1));

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) {
                    v.loadDataWithBaseURL(url,
                        "<body style='background:#161616;color:#fff;font-family:sans-serif;text-align:center;padding:60px 20px'>"
                        + "<h2>No connection</h2><p>Please check your signal.</p>"
                        + "<button style='font-size:18px;padding:12px 24px' onclick='location.href=\"" + url + "\"'>Try again</button></body>",
                        "text/html", "utf-8", url);
                }
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCb != null) fileCb.onReceiveValue(null);
                fileCb = cb;

                boolean wantCamera = p.isCaptureEnabled();
                if (wantCamera && Build.VERSION.SDK_INT >= 23
                        && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAM_PERM);
                    fileCb.onReceiveValue(null);   // user taps the button again after allowing
                    fileCb = null;
                    return true;
                }

                Intent intent;
                if (wantCamera) {
                    try {
                        File f = File.createTempFile("llf_", ".jpg", getCacheDir());
                        camUri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".fileprovider", f);
                        intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, camUri);
                        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception e) {
                        fileCb.onReceiveValue(null); fileCb = null; return true;
                    }
                } else {
                    camUri = null;
                    Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                    pick.addCategory(Intent.CATEGORY_OPENABLE);
                    pick.setType("image/*");
                    intent = Intent.createChooser(pick, "Choose photo");
                }
                try {
                    startActivityForResult(intent, REQ_FILE);
                } catch (Exception e) {
                    fileCb.onReceiveValue(null); fileCb = null;
                }
                return true;
            }
        });

        if (b != null) web.restoreState(b); else web.loadUrl(url);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_FILE || fileCb == null) return;
        Uri[] out = null;
        if (res == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) out = new Uri[]{data.getData()};
            else if (camUri != null) out = new Uri[]{camUri};
        }
        fileCb.onReceiveValue(out);
        fileCb = null;
    }

    @Override protected void onSaveInstanceState(Bundle o) { super.onSaveInstanceState(o); web.saveState(o); }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
