package com.fml.sqlitexp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private WebView web;
    private ValueCallback<Uri[]> picker;
    private byte[] pending;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        web.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void saveFile(final String name, String b64, final String mime) {
                pending = Base64.decode(b64, Base64.DEFAULT);
                runOnUiThread(new Runnable() {
                    public void run() {
                        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        i.addCategory(Intent.CATEGORY_OPENABLE);
                        i.setType(mime);
                        i.putExtra(Intent.EXTRA_TITLE, name);
                        startActivityForResult(i, 2);
                    }
                });
            }
        }, "Android");
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (picker != null) picker.onReceiveValue(null);
                picker = cb;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                startActivityForResult(i, 1);
                return true;
            }
        });
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == 1 && picker != null) {
            Uri[] r = null;
            if (res == RESULT_OK && data != null && data.getData() != null) r = new Uri[]{data.getData()};
            picker.onReceiveValue(r);
            picker = null;
        } else if (req == 2) {
            if (res == RESULT_OK && data != null && data.getData() != null && pending != null) {
                try (OutputStream o = getContentResolver().openOutputStream(data.getData())) {
                    o.write(pending);
                    Toast.makeText(this, "Saved", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Save FAILED: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Save cancelled - NOT saved", Toast.LENGTH_LONG).show();
            }
            pending = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
