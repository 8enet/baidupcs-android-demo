package com.demo.baidupcsapi.bean;

import org.json.JSONObject;

/**
 * Created by zl on 15/3/16.
 */
public class BaiduQRLoginRespInfo {
    public String deviceCode;

    public String userCode;

    public String verificationUrl;

    public String qrcodeUrl;

    public int expiresIn;

    public int interval;

    public static BaiduQRLoginRespInfo parseJson(String jsonObject){
        BaiduQRLoginRespInfo info=new BaiduQRLoginRespInfo();
        try{
            JSONObject json=new JSONObject(jsonObject);
            info.deviceCode=json.optString("device_code");
            info.userCode=json.optString("user_code");
            info.verificationUrl=json.optString("verification_url");
            info.qrcodeUrl=json.optString("qrcode_url");
            info.expiresIn=json.optInt("expires_in");
            info.interval=json.optInt("interval");
        }catch (Exception e){

        }
        return info;
    }

}
