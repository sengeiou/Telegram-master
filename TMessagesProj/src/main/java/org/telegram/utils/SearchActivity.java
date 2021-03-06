package org.telegram.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.telegram.messenger.BuildVars;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.LaunchActivity;

import java.util.HashMap;
import java.util.Map;

public class SearchActivity extends BaseFragment {

    public class JsJavaBridge {
        private Activity activity;
        private WebView webView;
        public JsJavaBridge(Activity activity, WebView webView) {
            this.activity = activity;
            this.webView = webView;
        }

        @JavascriptInterface
        public String getImei() {
            return TelephConfig.Imei;
        }

        @JavascriptInterface
        public void setMemberStatus() {
            new MemberStatusUtil(activity).httptogetThread.start();
        }

        @JavascriptInterface
        public String getCurrencyUser(){
            if (user != null){
                return user.phone+";"+user.first_name+" "+user.last_name;
            }else {
                return " ; ";
            }
        }

        @JavascriptInterface
        public int getCustomVersion(){
            Log.e("CUSTOM_VERSION", BuildVars.BUILD_VERSION+"");
            return BuildVars.BUILD_VERSION;
        }

        @JavascriptInterface
        public void reload(){
            if (webView != null){
                webView.reload();
            }
        }
    }

    public static WebView webView;
    private WebSettings setting;
    public static TLRPC.User user;
    private LaunchActivity launchActivity;
    private String url = TelephConfig.searchView_url;
    private Context theContext;

    @Override
    public View createView(Context context) {
        this.theContext = context;
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("SearchCN", R.string.SearchCN));
        try {
            user = UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser();
        }catch (Exception e){ }
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    if (webView.canGoBack()){
                        webView.goBack();
                    }else {
                        finishFragment();
                    }
                }
            }
        });
        launchActivity = (LaunchActivity) getParentActivity();
        webView = new WebView(context);

        setting = webView.getSettings();
        setting.setJavaScriptEnabled(true);   //???js??????
        webView.addJavascriptInterface(new JsJavaBridge(launchActivity, webView), "$App");
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            // TODO: ??????????????????
            downloadByBrowser(url);
        });
        return webView;
    }

    private void downloadByBrowser(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(Uri.parse(url));
        launchActivity.startActivity(intent);
    }

    private void init(String imei){
        // ????????????JS??????
        setting.setJavaScriptCanOpenWindowsAutomatically(true);
        webView.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            String referer = url;

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                //????????????js???????????????????????? onPageFinished ???????????????
                if (TelephConfig.Imei != null && TelephConfig.Imei != "") {
                    webView.post(() -> webView.loadUrl("javascript:ftest('" + TelephConfig.Imei + "')"));
                }
            }
            //??????shouldOverrideUrlLoading ??????
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                try {
                    Map<String, String> extraHeaders = new HashMap<>();
                    extraHeaders.put("Referer", referer);
                    view.loadUrl(url, extraHeaders);
                    referer = url;
                    // ?????????????????????????????????WebView???H5???????????????????????????
                    if (url.startsWith("weixin://") || url.startsWith("alipays://")
                            || url.startsWith("tg://") ) {
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        launchActivity.startActivity(intent);
                        return true;
                    }
                }catch (Exception e){
                    return false;
                }


                if(url == null) return false;
                Uri uri = Uri.parse(url);
                /*
                // ??????url????????? = ??????????????? js ??????
                // ???????????????????????????
                 */
                if ( uri.getScheme().equals("appjs")) {
                    // ?????? authority  = ???????????????????????? webview????????????????????????????????????
                    // ????????????url,??????JS????????????Android???????????????
                    if (uri.getAuthority().equals("webview")) {
                        //  ??????3???
                        // ??????JS????????????????????????
                        // ??????????????? key?????????????????????
                        String parameter = uri.getQueryParameter("path");
                        if (parameter.startsWith("http://") || parameter.startsWith("https://")){
                            if (parameter.startsWith("https://t.me") || parameter.startsWith("@")){
                                getToGroup(parameter);
                            }else {
                                webView.loadUrl(parameter);
                            }
                        }else {
                            getToGroup(parameter);
                        }
                    }
                    return true;
                }
                return true;//???????????????????????????????????????
            }

        });

    }

    public void getToGroup(String parameter){
        Browser.openUrl(launchActivity, parameter.replace("@","https://t.me/"));
    }

    public void sendDEVICE_ID(String imei, Activity activity){
        if (webView == null){
            webView = new WebView(theContext);
        }
        setting = webView.getSettings();
        setting.setJavaScriptEnabled(true);   //???js??????
        webView.addJavascriptInterface(new JsJavaBridge(activity, webView), "$App");
        webView.loadUrl(url);
        init(TelephConfig.Imei);
    }

    @Override
    public void onResume() {
        super.onResume();
        webView.onResume();
        //new GetMemberStatus(LaunchActivity.Imei, launchActivity).httptogetThread.start();
        sendDEVICE_ID(TelephConfig.Imei, getParentActivity());
    }

    @Override
    public void onPause() {
        webView.onPause();
        super.onPause();
    }

    @Override
    public void onFragmentDestroy() {
        webView.destroy();
        super.onFragmentDestroy();
    }

}
