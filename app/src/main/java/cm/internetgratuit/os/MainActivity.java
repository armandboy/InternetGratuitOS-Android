package cm.internetgratuit.os;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String PREFS = "igos_mobile";
    private static final String PREF_SERVER = "server_url";
    private static final int FILE_CHOOSER = 4101;
    private SharedPreferences prefs;
    private WebView webView;
    private ProgressBar progress;
    private TextView status;
    private ValueCallback<Uri[]> fileCallback;
    private String serverUrl = "";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(8,20,21));
        getWindow().setNavigationBarColor(Color.rgb(8,20,21));
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        serverUrl = AppConfig.normalizeBaseUrl(prefs.getString(PREF_SERVER, ""));
        buildUi(); configureWebView(); requestNotificationPermission();
        if (serverUrl.isEmpty()) showServerDialog(true); else openRoute("/sav-dashboard");
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private TextView label(String text, int sizeSp, int color) {
        TextView v = new TextView(this); v.setText(text); v.setTextSize(sizeSp); v.setTextColor(color);
        v.setGravity(Gravity.CENTER_VERTICAL); v.setPadding(dp(12), dp(8), dp(12), dp(8)); return v;
    }
    private Button navButton(String text, String route) {
        Button b = new Button(this); b.setText(text); b.setTextSize(11); b.setAllCaps(false); b.setTextColor(Color.WHITE);
        b.setBackgroundColor(Color.rgb(15,34,52)); b.setPadding(dp(10),0,dp(10),0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(48));
        lp.setMargins(dp(3),dp(3),dp(3),dp(3)); b.setLayoutParams(lp); b.setOnClickListener(v -> openRoute(route)); return b;
    }
    private void buildUi() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(8,20,21)); setContentView(root);
        LinearLayout top = new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(6),dp(3),dp(6),dp(3)); top.setBackgroundColor(Color.rgb(8,20,21));
        TextView title = label("InternetGratuitOS • SAV",15,Color.WHITE); top.addView(title,new LinearLayout.LayoutParams(0,dp(46),1));
        top.addView(navButton("Accueil","/sav-dashboard")); Button settingsBtn=navButton("Serveur",""); settingsBtn.setOnClickListener(v->showServerDialog(false)); top.addView(settingsBtn); root.addView(top);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); progress.setMax(100); progress.setVisibility(View.GONE); root.addView(progress,new LinearLayout.LayoutParams(-1,dp(3)));
        status=label("",10,Color.rgb(156,176,198)); status.setPadding(dp(10),0,dp(10),0); status.setVisibility(View.GONE); root.addView(status,new LinearLayout.LayoutParams(-1,dp(24)));
        FrameLayout holder=new FrameLayout(this); webView=new WebView(this); holder.addView(webView,new FrameLayout.LayoutParams(-1,-1)); root.addView(holder,new LinearLayout.LayoutParams(-1,0,1));
        HorizontalScrollView scroll=new HorizontalScrollView(this); scroll.setHorizontalScrollBarEnabled(false); LinearLayout nav=new LinearLayout(this); nav.setOrientation(LinearLayout.HORIZONTAL); nav.setGravity(Gravity.CENTER); nav.setPadding(dp(3),dp(2),dp(3),dp(2)); nav.setBackgroundColor(Color.rgb(8,20,21));
        nav.addView(navButton("Vue d’ensemble","/sav-dashboard")); nav.addView(navButton("Chats","/chat-support")); nav.addView(navButton("Visiteurs","/chat-visitors")); nav.addView(navButton("Clients","/chat-clients")); nav.addView(navButton("Tickets","/sav-tickets")); scroll.addView(nav); root.addView(scroll,new LinearLayout.LayoutParams(-1,dp(56)));
    }
    private void configureWebView() {
        WebSettings s=webView.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setDatabaseEnabled(true); s.setMediaPlaybackRequiresUserGesture(false); s.setAllowFileAccess(true); s.setAllowContentAccess(true); s.setLoadWithOverviewMode(true); s.setUseWideViewPort(true); s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE); s.setUserAgentString(s.getUserAgentString()+" InternetGratuitOS-Android/1.0");
        CookieManager.getInstance().setAcceptCookie(true); CookieManager.getInstance().setAcceptThirdPartyCookies(webView,true); webView.setKeepScreenOn(true);
        webView.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req){ Uri u=req.getUrl(); if("http".equalsIgnoreCase(u.getScheme())||"https".equalsIgnoreCase(u.getScheme())) return false; try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception ignored){} return true; }
            @Override public void onPageFinished(WebView view,String url){ super.onPageFinished(view,url); status.setText(url); status.setVisibility(View.VISIBLE); }
        });
        webView.setWebChromeClient(new WebChromeClient(){
            @Override public void onProgressChanged(WebView view,int p){ progress.setProgress(p); progress.setVisibility(p<100?View.VISIBLE:View.GONE); }
            @Override public boolean onShowFileChooser(WebView w,ValueCallback<Uri[]> callback,FileChooserParams params){ if(fileCallback!=null) fileCallback.onReceiveValue(null); fileCallback=callback; try{startActivityForResult(params.createIntent(),FILE_CHOOSER); return true;}catch(Exception e){fileCallback=null; Toast.makeText(MainActivity.this,"Sélecteur de fichier indisponible",Toast.LENGTH_SHORT).show(); return false;} }
        });
        webView.setDownloadListener((url,ua,cd,mime,len)->startDownload(url,ua,cd,mime));
    }
    private void startDownload(String url,String ua,String cd,String mime){ try{ DownloadManager.Request r=new DownloadManager.Request(Uri.parse(url)); r.addRequestHeader("User-Agent",ua); String cookie=CookieManager.getInstance().getCookie(url); if(cookie!=null) r.addRequestHeader("Cookie",cookie); r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); String name=android.webkit.URLUtil.guessFileName(url,cd,mime); r.setTitle(name); r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,name); ((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(r); Toast.makeText(this,"Téléchargement lancé",Toast.LENGTH_SHORT).show(); }catch(Exception e){Toast.makeText(this,"Téléchargement impossible",Toast.LENGTH_SHORT).show();} }
    private void showServerDialog(boolean mandatory){ final EditText input=new EditText(this); input.setSingleLine(true); input.setText(serverUrl); input.setHint("https://mon-serveur.example ou http://192.168.x.x"); int pad=dp(20); FrameLayout box=new FrameLayout(this); box.setPadding(pad,0,pad,0); box.addView(input); AlertDialog d=new AlertDialog.Builder(this).setTitle("Serveur InternetGratuitOS").setMessage("Entre l’adresse accessible depuis le téléphone. Exemple : URL Tailscale/HTTPS ou IP locale du PC Laragon.").setView(box).setPositiveButton("Connecter",null).setNegativeButton(mandatory?"Quitter":"Annuler",(x,w)->{if(mandatory)finish();}).create(); d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String normalized=AppConfig.normalizeBaseUrl(input.getText().toString()); if(normalized.isEmpty()){input.setError("Adresse serveur invalide");return;} serverUrl=normalized; prefs.edit().putString(PREF_SERVER,serverUrl).apply(); d.dismiss(); openRoute("/sav-dashboard");})); d.setCanceledOnTouchOutside(!mandatory); d.setCancelable(!mandatory); d.show(); }
    private void openRoute(String path){ if(serverUrl.isEmpty()){showServerDialog(true);return;} String url=AppConfig.route(serverUrl,path); if(!url.isEmpty()) webView.loadUrl(url); }
    private void requestNotificationPermission(){ if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7001); }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){ super.onActivityResult(requestCode,resultCode,data); if(requestCode==FILE_CHOOSER&&fileCallback!=null){Uri[] results=null; if(resultCode==RESULT_OK&&data!=null&&data.getData()!=null)results=new Uri[]{data.getData()}; fileCallback.onReceiveValue(results); fileCallback=null;} }
    @Override public void onBackPressed(){ if(webView!=null&&webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
}
