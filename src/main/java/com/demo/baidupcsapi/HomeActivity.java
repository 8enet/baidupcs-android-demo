package com.demo.baidupcsapi;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.oauth.BaiduOAuth;
import com.baidu.pcs.BaiduPCSClient;
import com.demo.baidupcsapi.bean.BaiduQRLoginRespInfo;
import com.github.kevinsawicki.http.HttpRequest;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class HomeActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
                .threadPriority(Thread.NORM_PRIORITY - 2)
                .denyCacheImageMultipleSizesInMemory()
                .discCacheFileNameGenerator(new Md5FileNameGenerator())
                .tasksProcessingOrder(QueueProcessingType.LIFO)
                .writeDebugLogs()
                .build();
        ImageLoader.getInstance().init(config);
        show();

    }





    private void show(){
        SharedPreferences baiduPcs = getSharedPreferences("baidu_pcs", Context.MODE_PRIVATE);
        String token=baiduPcs.getString("access_token",null);
        //String refToken=baiduPcs.getString("refresh_token",null);
        String name=baiduPcs.getString("user_name",null);
        if(!TextUtils.isEmpty(token)&&!TextUtils.isEmpty(name)){
            PCSClientAPI.getInstance().setAccessToken(token);
            final Button button= (Button) findViewById(R.id.button);
            final Button btnName= (Button) findViewById(R.id.user_name);
            button.setText("清除登录信息");
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getSharedPreferences("baidu_pcs", Context.MODE_PRIVATE).edit().clear().commit();
                    btnName.setVisibility(View.GONE);
                    button.setText("登录");
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            login();
                        }
                    });
                }
            });
            btnName.setVisibility(View.VISIBLE);
            btnName.setText("已登录："+name+"    下一步");
            btnName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(HomeActivity.this,MainActivity.class));
                }
            });
        }
    }

    public void onClick(View view){
        login();
    }



    private void login(){

        BaiduOAuth oauthClient = new BaiduOAuth();
        oauthClient.startOAuth(this, PCSClientAPI.mbApiKey, new String[]{"basic", "netdisk"}, new BaiduOAuth.OAuthListener() {
            @Override
            public void onException(String msg) {
                Toast.makeText(getApplicationContext(), "Login failed " + msg, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onComplete(BaiduOAuth.BaiduOAuthResponse response) {
                if(PCSClientAPI.getInstance().setOAuthResponse(response)){
                    Toast.makeText(getApplicationContext(), "Login success !! User name:" + response.getUserName(), Toast.LENGTH_SHORT).show();
                    saveOauth(response);
                    show();
                    //startActivity(new Intent(HomeActivity.this,MainActivity.class));
                }
            }
            @Override
            public void onCancel() {
                Toast.makeText(getApplicationContext(), "Login cancelled", Toast.LENGTH_SHORT).show();
            }
        });


        if(true)
            return;

        final String ak="NvLAKPT9fk0YScdlgdrt6ZrC";
        final String sk="iWd2F5vPac3I6R249wtB4dOGzg6uqezb";

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    HttpRequest reqCode = HttpRequest.get("https://openapi.baidu.com/oauth/2.0/device/code?client_id=" + ak + "&response_type=device_code&scope=basic,netdisk", null, true);
                    if(reqCode.ok()){
                        final BaiduQRLoginRespInfo loginRespInfo=BaiduQRLoginRespInfo.parseJson(reqCode.body());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showQrcode(loginRespInfo.qrcodeUrl);
                            }
                        });

                        long st= SystemClock.elapsedRealtime();

                        for (;;){
                            if((SystemClock.elapsedRealtime() - st )<loginRespInfo.expiresIn*1000){
                                JSONObject json=null;
                                try {
                                    StringBuilder sb = new StringBuilder("https://openapi.baidu.com/oauth/2.0/token?grant_type=device_token");
                                    sb.append("&code=").append(loginRespInfo.deviceCode).append("&client_id=").append(ak);
                                    sb.append("&client_secret=").append(sk);
                                    HttpRequest request = HttpRequest.get(sb.toString(), null, false);
                                    Log.d("test", "request     --> " + request.code());
                                    if(!request.ok()){
                                        SystemClock.sleep(2000);
                                        continue;
                                    }else {
                                        json=new JSONObject(request.body());
                                    }
                                }catch (Exception e){
                                    e.printStackTrace();
                                }

                                Log.d("test", "request     -->  json" + json );
                                if (json != null) {
                                    BaiduOAuth.BaiduOAuthResponse response = new BaiduOAuth.BaiduOAuthResponse();
                                    response.setAccessToken(json.optString("access_token"));
                                    response.setRefreshToken(json.optString("refresh_token"));
                                    //response.setUserName();
                                    HttpRequest httpRequest = HttpRequest.get("https://openapi.baidu.com/rest/2.0/passport/users/getLoggedInUser?access_token=" + response.getAccessToken(), null, false);
                                    JSONObject jsonUser=new JSONObject(httpRequest.body());
                                    response.setUserName(jsonUser.optString("uname"));

                                    saveOauth(response);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if(dialog != null && dialog.isShowing())
                                                dialog.dismiss();
                                            show();
                                        }
                                    });

                                    break;
                                }
                            }else {
                                break;
                            }
                        }

                    }
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        }).start();
    }

    private Dialog dialog;
    private void showQrcode(String path){
        dialog = new Dialog(this);
        ImageView img=new ImageView(this);
        ImageLoader.getInstance().displayImage(path,img);
        dialog.setContentView(img);
        dialog.show();
    }


    private void saveOauth(BaiduOAuth.BaiduOAuthResponse response){
        SharedPreferences baiduPcs = getSharedPreferences("baidu_pcs", Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = baiduPcs.edit();
        edit.putString("access_token",response.getAccessToken());
        edit.putString("user_name",response.getUserName());
        edit.apply();
        edit.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
