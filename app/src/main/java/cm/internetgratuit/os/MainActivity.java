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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String PREFS="igos_mobile", PREF_SERVER="server_url";
    private static final int FILE_CHOOSER=4101;
    private SharedPreferences prefs; private WebView webView; private ProgressBar progress; private TextView status; private ValueCallback<Uri[]> fileCallback; private String serverUrl="";
    @Override protected void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.rgb(8,20,21));getWindow().setNavigationBarColor(Color.rgb(8,20,21));prefs=getSharedPreferences(PREFS,MODE_PRIVATE);serverUrl=AppConfig.normalizeBaseUrl(prefs.getString(PREF_SERVER,""));buildUi();configureWebView();requestNotificationPermission();if(serverUrl.isEmpty())showServerDialog(true);else openRoute("/sav-dashboard");}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView label(String t,int s,int c){TextView v=new TextView(this);v.setText(t);v.setTextSize(s);v.setTextColor(c);v.setGravity(Gravity.CENTER_VERTICAL);v.setPadding(dp(10),dp(6),dp(10),dp(6));return v;}
    private Button btn(String t){Button b=new Button(this);b.setText(t);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setBackgroundColor(Color.rgb(15,34,52));return b;}
    private void buildUi(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(8,20,21));setContentView(root);LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);TextView title=label("InternetGratuitOS • SAV",15,Color.WHITE);top.addView(title,new LinearLayout.LayoutParams(0,dp(50),1));Button home=btn("Accueil");home.setOnClickListener(v->openRoute("/sav-dashboard"));top.addView(home);Button server=btn("Serveur");server.setOnClickListener(v->showServerDialog(false));top.addView(server);root.addView(top);progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);progress.setVisibility(View.GONE);root.addView(progress,new LinearLayout.LayoutParams(-1,dp(3)));status=label("",10,Color.LTGRAY);status.setVisibility(View.GONE);root.addView(status,new LinearLayout.LayoutParams(-1,dp(22)));FrameLayout holder=new FrameLayout(this);webView=new WebView(this);holder.addView(webView,new FrameLayout.LayoutParams(-1,-1));root.addView(holder,new LinearLayout.LayoutParams(-1,0,1));}
    private void configureWebView(){WebSettings s=webView.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setDatabaseEnabled(true);s.setMediaPlaybackRequiresUserGesture(false);s.setAllowFileAccess(true);s.setAllowContentAccess(true);s.setLoadWithOverviewMode(false);s.setUseWideViewPort(false);s.setSupportZoom(true);s.setBuiltInZoomControls(true);s.setDisplayZoomControls(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);s.setUserAgentString(s.getUserAgentString()+" InternetGratuitOS-Android/2.0");CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(webView,true);webView.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){Uri u=r.getUrl();if("http".equalsIgnoreCase(u.getScheme())||"https".equalsIgnoreCase(u.getScheme()))return false;try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception ignored){}return true;}@Override public void onPageFinished(WebView v,String url){super.onPageFinished(v,url);status.setText(url);status.setVisibility(View.VISIBLE);injectMobileFixes();}});webView.setWebChromeClient(new WebChromeClient(){@Override public void onProgressChanged(WebView v,int p){progress.setProgress(p);progress.setVisibility(p<100?View.VISIBLE:View.GONE);}@Override public boolean onShowFileChooser(WebView w,ValueCallback<Uri[]> cb,FileChooserParams p){if(fileCallback!=null)fileCallback.onReceiveValue(null);fileCallback=cb;try{startActivityForResult(p.createIntent(),FILE_CHOOSER);return true;}catch(Exception e){fileCallback=null;return false;}}});webView.setDownloadListener((url,ua,cd,mime,len)->startDownload(url,ua,cd,mime));}
    private void injectMobileFixes(){String js="(function(){"+
      "var m=document.querySelector('meta[name=viewport]');if(!m){m=document.createElement('meta');m.name='viewport';document.head.appendChild(m);}m.content='width=device-width,initial-scale=1,maximum-scale=5,user-scalable=yes';"+
      "var st=document.getElementById('igosAndroidMobile');if(!st){st=document.createElement('style');st.id='igosAndroidMobile';st.textContent='@media(max-width:900px){body{overflow-x:hidden!important}.sidebar,.app-sidebar,aside[class*=sidebar],nav[class*=sidebar]{z-index:99999!important}.sidebar.open,.sidebar.active,.app-sidebar.open,.app-sidebar.active{display:block!important;visibility:visible!important;transform:translateX(0)!important}button,[role=button],a{touch-action:manipulation}}';document.head.appendChild(st);}"+
      "document.addEventListener('click',function(e){var x=e.target.closest('button,a,[role=button]');if(!x)return;var a=(x.getAttribute('aria-label')||x.title||x.textContent||'').toLowerCase();if(a.indexOf('menu')>=0||a.indexOf('navigation')>=0){var q=document.querySelector('.sidebar,.app-sidebar,aside[class*=sidebar],nav[class*=sidebar]');if(q){q.classList.toggle('open');q.classList.toggle('active');}}},true);"+
      "})();";webView.evaluateJavascript(js,null);}
    private void startDownload(String url,String ua,String cd,String mime){try{DownloadManager.Request r=new DownloadManager.Request(Uri.parse(url));r.addRequestHeader("User-Agent",ua);String c=CookieManager.getInstance().getCookie(url);if(c!=null)r.addRequestHeader("Cookie",c);r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);String n=android.webkit.URLUtil.guessFileName(url,cd,mime);r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,n);((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(r);}catch(Exception e){Toast.makeText(this,"Téléchargement impossible",Toast.LENGTH_SHORT).show();}}
    private void showServerDialog(boolean mandatory){EditText input=new EditText(this);input.setSingleLine(true);input.setText(serverUrl);input.setHint("https://serveur/InternetGratuitOS/public");FrameLayout box=new FrameLayout(this);box.setPadding(dp(20),0,dp(20),0);box.addView(input);AlertDialog d=new AlertDialog.Builder(this).setTitle("Serveur InternetGratuitOS").setMessage("Adresse de base accessible depuis le téléphone.").setView(box).setPositiveButton("Connecter",null).setNegativeButton(mandatory?"Quitter":"Annuler",(x,w)->{if(mandatory)finish();}).create();d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String n=AppConfig.normalizeBaseUrl(input.getText().toString());if(n.isEmpty()){input.setError("Adresse invalide");return;}serverUrl=n;prefs.edit().putString(PREF_SERVER,n).apply();d.dismiss();openRoute("/sav-dashboard");}));d.setCancelable(!mandatory);d.show();}
    private void openRoute(String p){if(serverUrl.isEmpty()){showServerDialog(true);return;}String u=AppConfig.route(serverUrl,p);if(!u.isEmpty())webView.loadUrl(u);}
    private void requestNotificationPermission(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7001);}
    @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==FILE_CHOOSER&&fileCallback!=null){Uri[] z=null;if(c==RESULT_OK&&d!=null&&d.getData()!=null)z=new Uri[]{d.getData()};fileCallback.onReceiveValue(z);fileCallback=null;}}
    @Override public void onBackPressed(){if(webView!=null&&webView.canGoBack())webView.goBack();else super.onBackPressed();}
}
