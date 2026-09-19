package br.com.jstech.privado;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String HOME_BASE_URL = "https://jstech.local/";

    private WebView webView;
    private TextView domainView;
    private ProgressBar progressBar;
    private View fullScreenView;
    private WebChromeClient.CustomViewCallback fullScreenCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().setStatusBarColor(Color.rgb(6, 11, 22));
        getWindow().setNavigationBarColor(Color.rgb(6, 11, 22));
        buildInterface();
        configurePrivateWebView();
        startFreshPrivateSession();
    }

    private void buildInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(6, 11, 22));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), dp(6), dp(8), dp(6));
        bar.setBackgroundColor(Color.rgb(11, 20, 36));

        Button back = toolButton("‹", "Voltar");
        Button home = toolButton("⌂", "Central");
        Button reload = toolButton("↻", "Atualizar");
        Button clear = toolButton("✕", "Limpar sessão");
        Button exit = toolButton("Sair", "Limpar e sair");

        domainView = new TextView(this);
        domainView.setText("Sessão privada");
        domainView.setTextColor(Color.rgb(203, 213, 225));
        domainView.setTextSize(12);
        domainView.setGravity(Gravity.CENTER);
        domainView.setSingleLine(true);
        domainView.setPadding(dp(8), 0, dp(8), 0);

        bar.addView(back, new LinearLayout.LayoutParams(dp(42), dp(42)));
        bar.addView(home, new LinearLayout.LayoutParams(dp(42), dp(42)));
        bar.addView(domainView, new LinearLayout.LayoutParams(0, dp(42), 1f));
        bar.addView(reload, new LinearLayout.LayoutParams(dp(42), dp(42)));
        bar.addView(clear, new LinearLayout.LayoutParams(dp(42), dp(42)));
        bar.addView(exit, new LinearLayout.LayoutParams(dp(58), dp(42)));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(6, 11, 22));

        root.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));
        root.addView(progressBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));
        root.addView(webView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        back.setOnClickListener(v -> {
            if (webView.canGoBack()) webView.goBack();
            else loadHomePage();
        });
        home.setOnClickListener(v -> loadHomePage());
        reload.setOnClickListener(v -> webView.reload());
        clear.setOnClickListener(v -> clearPrivateSession(false));
        exit.setOnClickListener(v -> clearPrivateSession(true));
    }

    private Button toolButton(String text, String description) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(text.length() > 2 ? 11 : 20);
        button.setTextColor(Color.WHITE);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setAllCaps(false);
        button.setContentDescription(description);
        button.setPadding(0, 0, 0, 0);
        return button;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configurePrivateWebView() {
        WebView.setWebContentsDebuggingEnabled(false);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(false);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setSaveFormData(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setGeolocationEnabled(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) settings.setSafeBrowsingEnabled(true);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) ->
                Toast.makeText(this, "Downloads estão bloqueados no modo restrito.", Toast.LENGTH_LONG).show());

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleNavigation(view, request.getUrl());
            }

            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleNavigation(view, Uri.parse(url));
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                showDomain(url);
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                showDomain(url);
                super.onPageFinished(view, url);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel();
                Toast.makeText(MainActivity.this, "Conexão insegura bloqueada.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    Toast.makeText(MainActivity.this, "Este serviço não abriu dentro do aplicativo.", Toast.LENGTH_LONG).show();
                }
                super.onReceivedError(view, request, error);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (fullScreenView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                fullScreenView = view;
                fullScreenCallback = callback;
                FrameLayout decor = (FrameLayout) getWindow().getDecorView();
                decor.addView(view, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                webView.setVisibility(View.GONE);
            }

            @Override
            public void onHideCustomView() {
                hideFullScreenVideo();
            }
        });
    }

    private boolean handleNavigation(WebView view, Uri uri) {
        String scheme = uri.getScheme();
        if ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) {
            view.loadUrl(uri.toString());
            return true;
        }
        Toast.makeText(this, "Este tipo de link está bloqueado no modo restrito.", Toast.LENGTH_LONG).show();
        return true;
    }

    private void loadHomePage() {
        try {
            java.io.InputStream input = getAssets().open("home.html");
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            input.close();
            webView.loadDataWithBaseURL(HOME_BASE_URL, output.toString("UTF-8"), "text/html", "UTF-8", null);
        } catch (Exception error) {
            Toast.makeText(this, "Não foi possível abrir a central JSTech.", Toast.LENGTH_LONG).show();
        }
    }

    private void showDomain(String url) {
        try {
            String host = Uri.parse(url).getHost();
            if ("jstech.local".equalsIgnoreCase(host)) domainView.setText("🔒 JSTech Privado");
            else domainView.setText(host == null ? "Sessão privada" : "🔒 " + host);
        } catch (Exception ignored) {
            domainView.setText("Sessão privada");
        }
    }

    private void startFreshPrivateSession() {
        webView.clearHistory();
        webView.clearCache(true);
        webView.clearFormData();
        WebStorage.getInstance().deleteAllData();
        CookieManager manager = CookieManager.getInstance();
        manager.removeAllCookies(removed -> {
            manager.flush();
            runOnUiThread(this::loadHomePage);
        });
    }

    private void clearPrivateSession(boolean closeAfter) {
        webView.stopLoading();
        webView.loadUrl("about:blank");
        webView.clearHistory();
        webView.clearCache(true);
        webView.clearFormData();
        WebStorage.getInstance().deleteAllData();
        CookieManager manager = CookieManager.getInstance();
        manager.removeAllCookies(removed -> {
            manager.flush();
            runOnUiThread(() -> {
                if (closeAfter) finishAndRemoveTask();
                else {
                    Toast.makeText(this, "Sessão local apagada.", Toast.LENGTH_SHORT).show();
                    loadHomePage();
                }
            });
        });
    }

    private void hideFullScreenVideo() {
        if (fullScreenView == null) return;
        FrameLayout decor = (FrameLayout) getWindow().getDecorView();
        decor.removeView(fullScreenView);
        fullScreenView = null;
        webView.setVisibility(View.VISIBLE);
        if (fullScreenCallback != null) fullScreenCallback.onCustomViewHidden();
        fullScreenCallback = null;
    }

    @Override
    public void onBackPressed() {
        if (fullScreenView != null) hideFullScreenVideo();
        else if (webView.canGoBack()) webView.goBack();
        else clearPrivateSession(true);
    }

    @Override
    protected void onPause() {
        webView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.clearHistory();
            webView.clearCache(true);
            webView.removeAllViews();
            webView.destroy();
        }
        WebStorage.getInstance().deleteAllData();
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
