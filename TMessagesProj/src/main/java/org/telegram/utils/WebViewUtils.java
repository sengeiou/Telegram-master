package org.telegram.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.WebviewActivity;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class WebViewUtils extends BaseFragment {

    public WebView mWebView;
    public static TLRPC.User user;
    private static final String TAG = WebViewUtils.class.getSimpleName();
    private String webUrl;
    private String title;

    public WebViewUtils(String memberView_url, String l_title) {
        webUrl = memberView_url;
        title = l_title;
    }

    public class JsJavaBridge {
        @JavascriptInterface
        public String getImei() {
            return TelephConfig.Imei;
        }

        @JavascriptInterface
        public void setMemberStatus() {
            new MemberStatusUtil(getParentActivity()).httptogetThread.start();
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
        public String getNewAppFileUrl(){
            return TelephConfig.newAppFile_url;
        }

        @JavascriptInterface
        public void reload(){
            if (mWebView != null){
                mWebView.reload();
            }
        }
    }

    @Override
    public View createView(Context context) {
        mWebView = new WebView(context);
        actionBar.setAllowOverlayTitle(true);
        int titleRes ;
        switch (title){
            case "pay":
                titleRes = R.string.Pay;
                break;
            case "InvitedTitle":
                titleRes = R.string.InvitedTitle;
                break;
            case "SearchCN":
                titleRes = R.string.SearchCN;
                break;
            default:
                titleRes = R.string.AppName;
        }
        actionBar.setTitle(LocaleController.getString(title, titleRes));
        try {
            user = UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser();
        }catch (Exception e){ }
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    if (mWebView.canGoBack()){
                        mWebView.goBack();
                    }else {
                        finishFragment(false);
                    }
                }
            }
        });

        //与js交互
        mWebView.getSettings().setJavaScriptEnabled(true);
        // 设置允许JS弹窗
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mWebView.addJavascriptInterface(new JsJavaBridge(), "$App");
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);  //设置 缓存模式
        // 开启 DOM storage API 功能
        mWebView.getSettings().setAppCacheEnabled(true);
        mWebView.getSettings().setDatabaseEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        if (Build.VERSION.SDK_INT >= 19) {
            mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        if (Build.VERSION.SDK_INT >= 21) {
            mWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptThirdPartyCookies(mWebView, true);
        }

        mWebView.setWebViewClient(new WebViewClient() {
            String referer = webUrl;

            private boolean isInternalUrl(String url) {
                if (TextUtils.isEmpty(url)) {
                    return false;
                }
                Uri uri = Uri.parse(url);
                if ("appjs".equals(uri.getScheme())) {
                    String parameter = url.substring(21);
                    Log.i(TAG, "parameter url="+parameter);
                    getToGroup(parameter);
                    return true;
                } else {
                    if (url.startsWith("weixin://") || url.startsWith("alipays://") || url.startsWith("tg://")) {
                        try {
                            // 如下方案可在非微信内部WebView的H5页面中调出微信支付
                            Map<String, String> extraHeaders = new HashMap<>();
                            extraHeaders.put("Referer", referer);
                            mWebView.loadUrl(url, extraHeaders);
                            referer = url;

                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(url));
                            getParentActivity().startActivity(intent);

                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    }
                }

                return false;
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                Log.i(TAG, "onLoadResource url="+url);
                if (isInternalUrl(url)) {
                    return;
                }
                super.onLoadResource(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView webview, String url) {
                Log.i(TAG, "intercept url="+url);
                return isInternalUrl(url) || super.shouldOverrideUrlLoading(webview, url);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Log.e(TAG, "onPageStarted");
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                String title = view.getTitle();
                Log.e(TAG, "onPageFinished WebView title=" + title);
                super.onPageFinished(view, url);
                //安卓调用js方法。注意需要在 onPageFinished 回调里调用
                if (TelephConfig.Imei != null && TelephConfig.Imei != "") {
                    mWebView.post(() -> mWebView.loadUrl("javascript:ftest('" + TelephConfig.Imei + "')"));
                }
            }

        });

        mWebView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            // TODO: 处理下载事件
            downloadByBrowser(url);
        });

        frameLayout.addView(mWebView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        return frameLayout;
    }

    public void getToGroup(String parameter){
        if (parameter.startsWith("@")) parameter = parameter.replace("@", "https://t.me/");
        Browser.openUrl(getParentActivity(), parameter);
    }

    private void downloadByBrowser(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(Uri.parse(url));
        getParentActivity().startActivity(intent);
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        mWebView.setLayerType(View.LAYER_TYPE_NONE, null);
        try {
            ViewParent parent = mWebView.getParent();
            if (parent != null) {
                ((FrameLayout) parent).removeView(mWebView);
            }
            mWebView.stopLoading();
            mWebView.loadUrl("about:blank");
            mWebView.destroy();
            mWebView = null;
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    @Override
    protected void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        if (isOpen && !backward && mWebView != null) {
            mWebView.loadUrl(webUrl);
        }
    }

}
