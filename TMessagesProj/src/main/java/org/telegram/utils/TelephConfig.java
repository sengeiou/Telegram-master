package org.telegram.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.android.exoplayer2.util.Log;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import org.telegram.messenger.BuildVars;
import org.telegram.ui.LaunchActivity;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import okhttp3.Call;

public class TelephConfig {
    //TODO: refactor

    public static final int TITLE_TYPE_TEXT = 0;
    public static final int TITLE_TYPE_ICON = 1;
    public static final int TITLE_TYPE_MIX = 2;
    public static boolean showProxySponsorChannel = true;
    public static int tabsTitleType = TITLE_TYPE_MIX;

    public static String Imei = null;
    public static long mills;
    public static long end_timeStamp;
    public static File updateFile;
    public static String topImg_url;

    public static  String proxy_url = "http://alijson.teleph.cn/";
    public static String searchView_url = "http://search.teleph.cn/";
    public static String memberView_url = "http://alitest.teleph.cn/";
    public static String invitedView_url = "http://invited.teleph.cn/";
    public static String memberStatus_url = "http://alitest.teleph.cn:9539/xunpay/query/";
    public static String appUpdate_url = "http://update.teleph.cn";
    public static String channel_url = "https://t.me/tgzh_vip";
    public static String status_url = "http://status.teleph.cn";
    public static String newAppFile_url = "";
    public static Map APP_KEY = new HashMap();

    public static void getConfig(){
        OkHttpUtils.get()
                .url("http://config.teleph.cn")
                .build()
                .connTimeOut(20000)
                .readTimeOut(20000)
                .writeTimeOut(20000)
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int i) {
                        android.util.Log.e("Call config ERR", e.getMessage());
                        return;
                    }

                    @Override
                    public void onResponse(String s, int i) {
                        if (!s.startsWith("{") || s == null){
                            return;
                        }
                        JSONObject request_json = JSONObject.parseObject(s);

                        TelephConfig.proxy_url = request_json.getString("proxy_url");
                        TelephConfig.searchView_url =  request_json.getString("searchView_url");
                        TelephConfig.memberView_url = request_json.getString("memberView_url");
                        TelephConfig.invitedView_url = request_json.getString("invitedView_url");
                        TelephConfig.memberStatus_url =  request_json.getString("memberStatus_url");
                        TelephConfig.appUpdate_url =  request_json.getString("appUpdate_url");
                        TelephConfig.channel_url =  request_json.getString("channel_url");
                        TelephConfig.status_url =  request_json.getString("status_url");
                        int up_imgCount = request_json.getInteger("up_imgCount");
                        String up_topUrl = request_json.getString("up_topUrl");
                        JSONArray app_keys = request_json.getJSONArray("app_keys");

                        Random top_random = new Random();
                        int temp = top_random.nextInt(up_imgCount);
                        TelephConfig.topImg_url = up_topUrl + temp + ".png";

                        for (Object jsob : app_keys){
                            String api_id = (String) ((JSONObject)jsob).get("api_id");
                            String api_hash = (String) ((JSONObject)jsob).get("api_hash");
                            TelephConfig.APP_KEY.put(api_id, api_hash);
                        }

                        //        APP_KEY.put("7596339","2e875824bc186ec476c296cda6429d0e");
                        APP_KEY.put("2212709","3c786777d0e11018212ed8b40e2b9767");

                        Set APP_KEY_SET = APP_KEY.entrySet();
                        Random appKey_random = new Random();
                        int appKey_temp = appKey_random.nextInt(APP_KEY_SET.size());
                        List APP_KEY_LIST = Arrays.asList(APP_KEY_SET.toArray());
                        Map.Entry app_str = (Map.Entry)APP_KEY_LIST.get(appKey_temp);
                        BuildVars.APP_ID = Integer.parseInt(app_str.getKey().toString());
                        BuildVars.APP_HASH = app_str.getValue().toString();
                        Log.e("app_str",BuildVars.APP_ID + " : "+BuildVars.APP_HASH);

                    }
                });
    }

}
