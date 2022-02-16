package org.telegram.utils;

import android.content.SharedPreferences;
import android.util.Log;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zhy.http.okhttp.OkHttpUtils;

import org.telegram.messenger.BuildVars;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SharedConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.ProxyListActivity;

import java.util.HashMap;
import java.util.Iterator;

import okhttp3.Response;


public class ProxyUtil {
    public static Object ProxyUtil_TAG = new  ProxyUtil();

    public static class Proxy{
        public Integer id;
        public String address;
        public Integer port;
        public String secret;
        public String name;
        public Integer image;
        public String username;
        public String password;

        public Proxy(Integer id, String address, Integer port, String secret, String name, Integer image, String username, String password) {
            this.id = id;
            this.address = address;
            this.port = port;
            this.secret = secret;
            this.name = name;
            this.image = image;
            if (this.address == null){
                this.address = "";
            }
            if (this.secret == null){
                this.secret = "";
            }
            if (this.name == null){
                this.name = "";
            }
            if (this.username == null){
                this.username = "";
            }
            if (this.password == null){
                this.password = "";
            }
            if (this.image == null){
                this.image = 0;
            }
        }
    }

    public static HashMap<String, Proxy> Proxy_Map = new HashMap<>();
    private static int proxycount;
    private static int conut;

    public static String getProxyName(String address){
        try{
            return Proxy_Map.get(address).name;
        }
        catch (NullPointerException e){
            return null;
        }
    }

    public static Integer getProxyImage(String address){
        try{
            return Proxy_Map.get(address).image;
        }
        catch (NullPointerException e){
            return null;
        }
    }

    public static void setImageId(String address, int i){
        try{
            Proxy_Map.get(address).image = i;
            if(i == 4 && address.equals(SharedConfig.currentProxy.address)){
                useProxy();
            }
        }
        catch (NullPointerException e){
        }

    }

    public static void init(){
        Iterator<SharedConfig.ProxyInfo> iterator = SharedConfig.proxyList.iterator();
        while (iterator.hasNext()) {
            SharedConfig.ProxyInfo info = iterator.next();
            if (info.address != null) {
                iterator.remove();//使用迭代器的删除方法删除
                SharedConfig.saveProxyList();
            }
        }
        SharedConfig.saveProxyList();
        Proxy_Map.clear();
        conut = 0;
    }

    public static void useProxy(){
        boolean useProxy = true;
        if (MemberStatusUtil.expire){
            // 会员到期
            useProxy = false;
            SharedConfig.currentProxy = new SharedConfig.ProxyInfo("", 1080, "", "", "");
        }else {
            //会员未到期
            if (SharedConfig.proxyList.size() <= 0){ return; }
            ProxyUtil.conut = (int)(Math.random()* SharedConfig.proxyList.size());
            SharedConfig.currentProxy  = SharedConfig.proxyList.get(ProxyUtil.conut);
            if (SharedConfig.currentProxy.checking){
                useProxy();
            }
        }
        ProxyListActivity.useProxySettings = useProxy;
        SharedPreferences.Editor editor = MessagesController.getGlobalMainSettings().edit();
        editor.putString("proxy_ip", SharedConfig.currentProxy.address);
        editor.putString("proxy_pass", SharedConfig.currentProxy.password);
        editor.putString("proxy_user", SharedConfig.currentProxy.username);
        editor.putInt("proxy_port", SharedConfig.currentProxy.port);
        editor.putString("proxy_secret", SharedConfig.currentProxy.secret);
        editor.putBoolean("proxy_enabled", useProxy);
        editor.putBoolean("proxy_enabled_calls", false);
        editor.commit();
        ConnectionsManager.setProxySettings(useProxy, SharedConfig.currentProxy.address, SharedConfig.currentProxy.port, SharedConfig.currentProxy.username, SharedConfig.currentProxy.password, SharedConfig.currentProxy.secret);
    }

    public static void okGetProxys(){
        try{
            Response response = OkHttpUtils.get()
                    .url(TelephConfig.proxy_url)
                    .tag(ProxyUtil_TAG)
                    .build()
                    .connTimeOut(20000)
                    .readTimeOut(20000)
                    .writeTimeOut(20000)
                    .execute();
            String result = response.body().string();//4.获得返回结果
            if(result == null && result.startsWith("{")){
                return;
            }else {
                try {
//                    Log.e("res",result);
                    init();
                    JSONObject resultjson = JSONObject.parseObject(result);
                    JSONArray resultAtty = resultjson.getJSONArray("servers");
                    proxycount = resultAtty.size();
                    for (int i = 0; i < proxycount; i++) {
                        JSONObject proxy = resultAtty.getJSONObject(i);
                        Integer id = proxy.getInteger("poxyId");
                        String address = proxy.getString("poxyAddress");
                        Integer port = proxy.getInteger("poxyPort");
                        String poxySecret = proxy.getString("poxySecret");
                        String proxyName = proxy.getString("proxyName");
                        String username = proxy.getString("username");
                        String password = proxy.getString("password");
                        Integer proxyImg = proxy.getInteger("proxyImg");
                        SharedConfig.ProxyInfo info;
                        Proxy proxy1;
                        if (MemberStatusUtil.expire){
                            info = new SharedConfig.ProxyInfo(address, port, "", "", "");
                            info.available = false;
                            proxy1 = new Proxy(id, address, port, "", proxyName+"  ⚓️", 4, username, password);
                        }else {
                            info = new SharedConfig.ProxyInfo(address, port, username, password, poxySecret);
                            proxy1 = new Proxy(id, address, port, poxySecret, proxyName+" \uD83D\uDE80", proxyImg, username, password);
                        }
                        SharedConfig.addProxy(info);
                        Proxy_Map.put(proxy1.address, proxy1);
                    }
                    useProxy();
                } catch (Exception e) {
                    Log.e("Proxy ERR", e.getMessage());
                }
            }
        }catch (Exception e){
            Log.e("Proxy ERR", e.getMessage());
            return;
        }
    }
}