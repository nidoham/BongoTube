package bd.nidoham.bongo;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import bd.nidoham.bongo.cache.UserSessionManager;

public class OnboardActivity extends AppCompatActivity {

    private static final String TAG = "OnboardActivity";
    private WebView webView;
    private ProgressBar progressBar;
    private UserSessionManager sessionManager;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboard);

        webView = findViewById(R.id.webViewLogin);
        progressBar = findViewById(R.id.progressBar);
        sessionManager = new UserSessionManager(this);

        // লগইন করার জন্য YouTube/Google Accounts পেজ লোড করা হচ্ছে
        String loginUrl = "https://accounts.google.com/ServiceLogin?service=youtube&uilel=3&passive=true&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26app%3Ddesktop%26hl%3Den%26next%3D%252F&hl=en";

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);

                Log.d(TAG, "Page finished loading: " + url);

                // যখন ব্যবহারকারী সফলভাবে লগইন করে YouTube.com এ ফিরে আসবে
                if (url.toLowerCase().contains("youtube.com")) {
                    String cookies = CookieManager.getInstance().getCookie(url);

                    if (cookies != null && (cookies.contains("SID=") || cookies.contains("SSID="))) {
                        Log.d(TAG, "Login successful. Cookies found.");
                        
                        // কুকি সেভ করা হচ্ছে
                        sessionManager.saveSession(cookies);

                        Toast.makeText(OnboardActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                        
                        // লগইন সম্পন্ন হলে এই অ্যাক্টিভিটি বন্ধ করে দিন
                        finish(); 
                    }
                }
            }
        });

        webView.loadUrl(loginUrl);
    }
}