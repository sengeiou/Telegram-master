package org.telegram.utils;

import android.app.Activity;
import android.util.Log;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.zhy.http.okhttp.OkHttpUtils;

import org.telegram.ui.LaunchActivity;

import java.util.Date;

import okhttp3.Response;


public class MemberStatusUtil {
    private Integer MemberStatus;
    private String msg;
    private String EndTime;
    private long end_timeStamp;
    private Activity activity;
    public static boolean expire; //true 为已到期   false 为未到期

    public MemberStatusUtil(Activity activity) {
        this.activity = activity;
    }

    public MemberStatusUtil() {
    }

    public void runGetJson(){
        ProxyUtil.okGetProxys();
    }

    //修改  添加请求后台 获取json数据
    public  Thread httptogetThread = new Thread(new Runnable() {
        @Override
        public void run() {
            String url = TelephConfig.memberStatus_url + TelephConfig.Imei;

            try{
                Response response = OkHttpUtils.get()
                        .url(url)
                        .tag(this)
                        .build()
                        .connTimeOut(20000)
                        .readTimeOut(20000)
                        .writeTimeOut(20000)
                        .execute();
                String result = response.body().string();//4.获得返回结果
                if (result == null) {
                    return;
                } else {
                    try {
                        JSONObject resultjson = JSONObject.parseObject(result);
                        JSONObject dataJson = resultjson.getJSONObject("data");
                        MemberStatus = dataJson.getInteger("status");
                        msg = dataJson.getString("msg");
                        EndTime = dataJson.getString("end_at");
                        end_timeStamp = dataJson.getLong("end_timestamp");
                        if (MemberStatus == 0) {
                            expire = true;
                        } else {
                            expire = false;
                        }
                        if (activity != null && activity instanceof LaunchActivity) {
                            long mills = (end_timeStamp * 1000 + 500) - new Date().getTime();
                            activity.runOnUiThread(() -> ((LaunchActivity) activity).floatShow(mills));
                        }
                        runGetJson();
                    } catch (JSONException e) {
                        Log.e("MemberStatusUtil ERR", e.getMessage());
                    }
                }
            }catch (Exception e){
                Log.e("MemberStatusUtil ERR", e.getMessage());
                return;
            }
        }
    });

}