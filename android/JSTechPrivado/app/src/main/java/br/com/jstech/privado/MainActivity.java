package br.com.jstech.privado;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
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

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String HOME_BASE_URL = "https://jstech.local/";
    private static final String UPDATE_JSON_URL =
            "https://raw.githubusercontent.com/juliovianadeoliveira-source/jstech-prime-site/main/updates/version.json";

    private WebView webView;
    private TextView domainView;
    private ProgressBar progressBar;
    private View fullScreenView;
    private WebChromeClient.CustomViewCallback fullScreenCallback;

    private long updateDownloadId = -1L;
    private String updateExpectedSha256 = "";
    private String pendingUpdateUrl;
    private String pendingUpdateSha256;
    private BroadcastReceiver updateDownloadReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().setStatusBarColor(Color.rgb(6, 11, 22));
        getWindow().setNavigationBarColor(Color.rgb(6, 11, 22));

        registerUpdateReceiver();
        buildInterface();
        configurePrivateWebView();
        startFreshPrivateSession();
        checkForUpdates(false);
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
        Button update = toolButton("↑", "Verificar atualização");
        Button reload = toolButton("↻", "Atualizar página");
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
        bar.addView(update, new LinearLayout.LayoutParams(dp(42), dp(42)));
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
        update.setOnClickListener(v -> checkForUpdates(true));
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
                Toast.makeText(this, "Downloads de sites estão bloqueados no modo privado.", Toast.LENGTH_LONG).show());

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
        Toast.makeText(this, "Este tipo de link está bloqueado no modo privado.", Toast.LENGTH_LONG).show();
        return true;
    }

    private void checkForUpdates(boolean manual) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(UPDATE_JSON_URL + "?t=" + System.currentTimeMillis());
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(7000);
                connection.setReadTimeout(7000);
                connection.setUseCaches(false);
                connection.setRequestProperty("Accept", "application/json");

                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) throw new Exception("HTTP " + code);

                String json = readAll(connection.getInputStream());
                JSONObject object = new JSONObject(json);

                int remoteCode = object.optInt("versionCode", 0);
                String remoteName = object.optString("versionName", "");
                String apkUrl = object.optString("apkUrl", "").trim();
                String sha256 = object.optString("sha256", "").trim();
                String notes = object.optString("notes", "Nova versão disponível.");

                runOnUiThread(() -> {
                    if (remoteCode > getCurrentVersionCode() && !apkUrl.isEmpty()) {
                        showUpdateDialog(remoteName, notes, apkUrl, sha256);
                    } else if (manual) {
                        Toast.makeText(
                                MainActivity.this,
                                "Você já está na versão mais recente (" + getCurrentVersionName() + ").",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
            } catch (Exception error) {
                if (manual) {
                    runOnUiThread(() -> Toast.makeText(
                            MainActivity.this,
                            "Não foi possível verificar atualização agora.",
                            Toast.LENGTH_LONG
                    ).show());
                }
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private void showUpdateDialog(String versionName, String notes, String apkUrl, String sha256) {
        String title = versionName.isEmpty()
                ? "Nova atualização disponível"
                : "JSTech Privado " + versionName;

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(notes + "\n\nA atualização será instalada por cima do aplicativo atual.")
                .setPositiveButton("Atualizar agora", (dialog, which) ->
                        prepareUpdateDownload(apkUrl, sha256))
                .setNegativeButton("Depois", null)
                .show();
    }

    private void prepareUpdateDownload(String apkUrl, String sha256) {
        if (apkUrl == null || !apkUrl.startsWith("https://")) {
            Toast.makeText(this, "Endereço de atualização inválido.", Toast.LENGTH_LONG).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !getPackageManager().canRequestPackageInstalls()) {
            pendingUpdateUrl = apkUrl;
            pendingUpdateSha256 = sha256;

            Intent permissionIntent = new Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + getPackageName())
            );
            startActivity(permissionIntent);
            Toast.makeText(
                    this,
                    "Ative 'Permitir desta fonte' para o JSTech Privado e volte ao aplicativo.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        startUpdateDownload(apkUrl, sha256);
    }

    private void startUpdateDownload(String apkUrl, String sha256) {
        try {
            File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (dir != null) {
                File old = new File(dir, "JSTech-Privado-update.apk");
                if (old.exists()) old.delete();
            }

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
            request.setTitle("JSTech Privado");
            request.setDescription("Baixando atualização...");
            request.setMimeType("application/vnd.android.package-archive");
            request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            );
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            request.setDestinationInExternalFilesDir(
                    this,
                    Environment.DIRECTORY_DOWNLOADS,
                    "JSTech-Privado-update.apk"
            );

            DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            updateExpectedSha256 = sha256 == null ? "" : sha256.trim().toLowerCase(Locale.ROOT);
            updateDownloadId = manager.enqueue(request);

            Toast.makeText(this, "Baixando atualização...", Toast.LENGTH_SHORT).show();
        } catch (Exception error) {
            Toast.makeText(this, "Não foi possível iniciar a atualização.", Toast.LENGTH_LONG).show();
        }
    }

    private void registerUpdateReceiver() {
        updateDownloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
                if (id == updateDownloadId) installDownloadedUpdate();
            }
        };

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(updateDownloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(updateDownloadReceiver, filter);
        }
    }

    private void installDownloadedUpdate() {
        try {
            DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            Uri apkUri = manager.getUriForDownloadedFile(updateDownloadId);
            if (apkUri == null) {
                Toast.makeText(this, "Falha ao baixar a atualização.", Toast.LENGTH_LONG).show();
                return;
            }

            if (!updateExpectedSha256.isEmpty()) {
                String actual = sha256(apkUri);
                if (!updateExpectedSha256.equalsIgnoreCase(actual)) {
                    manager.remove(updateDownloadId);
                    Toast.makeText(
                            this,
                            "A atualização não passou na verificação de segurança.",
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }
            }

            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(apkUri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(install);
        } catch (Exception error) {
            Toast.makeText(
                    this,
                    "Não foi possível abrir o instalador da atualização.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private String sha256(Uri uri) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) throw new Exception("Arquivo indisponível");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
        }

        StringBuilder out = new StringBuilder();
        for (byte b : digest.digest()) out.append(String.format(Locale.ROOT, "%02x", b));
        return out.toString();
    }

    private String readAll(InputStream input) throws Exception {
        try (InputStream in = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) output.write(buffer, 0, read);
            return output.toString("UTF-8");
        }
    }

    private void loadHomePage() {
        try {
            InputStream input = getAssets().open("home.html");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
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
        if (webView != null) webView.onResume();

        if (pendingUpdateUrl != null &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                getPackageManager().canRequestPackageInstalls()) {
            String url = pendingUpdateUrl;
            String sha = pendingUpdateSha256;
            pendingUpdateUrl = null;
            pendingUpdateSha256 = null;
            startUpdateDownload(url, sha);
        }
    }

    @Override
    protected void onDestroy() {
        try {
            if (updateDownloadReceiver != null) unregisterReceiver(updateDownloadReceiver);
        } catch (Exception ignored) {
        }

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


    private long getCurrentVersionCode() {
        try {
            android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) return info.getLongVersionCode();
            return info.versionCode;
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private String getCurrentVersionName() {
        try {
            android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName == null ? "" : info.versionName;
        } catch (Exception ignored) {
            return "";
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
