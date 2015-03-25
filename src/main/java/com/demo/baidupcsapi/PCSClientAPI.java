package com.demo.baidupcsapi;

import android.os.Handler;
import android.text.TextUtils;

import com.baidu.oauth.BaiduOAuth;
import com.baidu.pcs.BaiduPCSActionInfo;
import com.baidu.pcs.BaiduPCSClient;
import com.baidu.pcs.BaiduPCSStatusListener;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by zl on 15/3/12.
 */
public class PCSClientAPI {


    public final static  String mbApiKey = "L6g70tBRRIXLsY0Z3HwKqlRE";

    public final static String mbRootPath = "/apps/pcstest_oauth";


    private static PCSClientAPI pcsClientAPI;

    private ThreadPoolExecutor threadPoolExecutor=new ThreadPoolExecutor(3,5,2, TimeUnit.MINUTES,new LinkedBlockingQueue<Runnable>());

    private Handler handler;
    private BaiduPCSClient api;

    public static PCSClientAPI getInstance(){
        if(pcsClientAPI == null){
            synchronized (PCSClientAPI.class){
                pcsClientAPI=new PCSClientAPI();
            }
        }
        return pcsClientAPI;
    }


    private PCSClientAPI(){
        handler=new Handler();
    }

    public void shutdown(){
        try{
            if(threadPoolExecutor != null){
                threadPoolExecutor.shutdown();
            }
        }catch (Exception e){

        }
    }

    public boolean setOAuthResponse(BaiduOAuth.BaiduOAuthResponse response){
        if(null != response && !TextUtils.isEmpty(response.getAccessToken())){
            api=new BaiduPCSClient(response.getAccessToken());
            return true;
        }
        return false;
    }

    public String getAccessToken(){
        return (api != null?api.accessToken():null);
    }

    public void setAccessToken(String token){
        api=new BaiduPCSClient(token);
    }

    public boolean isLogin(){
        return api!=null;
    }

    public BaiduPCSClient getBaiduPCSClientAPI(){
        return api;
    }

    public void getQuota(PCSCallBack<BaiduPCSActionInfo.PCSQuotaResponse> callBack){
        threadPoolExecutor.execute(new APIRequest<BaiduPCSActionInfo.PCSQuotaResponse>(callBack) {
            @Override
            public BaiduPCSActionInfo.PCSQuotaResponse request() {
                return api.quota();
            }
        });
    }

    public void list(final String path,final String by,final String order, PCSCallBack<BaiduPCSActionInfo.PCSListInfoResponse> callBack){
        threadPoolExecutor.execute(new APIRequest<BaiduPCSActionInfo.PCSListInfoResponse>(callBack) {
            @Override
            public BaiduPCSActionInfo.PCSListInfoResponse request() {
                return api.list(path,by,order);
            }
        });
    }

    public void search(final String path,final String key,final boolean recursive ,PCSCallBack<BaiduPCSActionInfo.PCSListInfoResponse> callBack){
        threadPoolExecutor.execute(new APIRequest<BaiduPCSActionInfo.PCSListInfoResponse>(callBack) {
            @Override
            public BaiduPCSActionInfo.PCSListInfoResponse request() {
                return api.search(path,key,recursive);
            }
        });
    }

    public void mkdir(final String path,PCSCallBack<BaiduPCSActionInfo.PCSFileInfoResponse> callBack){
        threadPoolExecutor.execute(new APIRequest<BaiduPCSActionInfo.PCSFileInfoResponse>(callBack) {
            @Override
            public BaiduPCSActionInfo.PCSFileInfoResponse request() {
                return api.makeDir(path);
            }
        });
    }

    public void imageStream(PCSCallBack<BaiduPCSActionInfo.PCSListInfoResponse> callBack){
        threadPoolExecutor.execute(new APIRequest<BaiduPCSActionInfo.PCSListInfoResponse>(callBack) {
            @Override
            public BaiduPCSActionInfo.PCSListInfoResponse request() {
                return api.imageStream();
            }
        });

    }

    public void videoStream(PCSCallBack<BaiduPCSActionInfo.PCSListInfoResponse> callBack){
        threadPoolExecutor.execute(new APIRequest<BaiduPCSActionInfo.PCSListInfoResponse>(callBack) {
            @Override
            public BaiduPCSActionInfo.PCSListInfoResponse request() {
                return api.videoStream();
            }
        });

    }

    public void deleteFile(final String path, PCSCallBack<BaiduPCSActionInfo.PCSSimplefiedResponse> callBack){
        threadPoolExecutor.execute(new APIRequest<BaiduPCSActionInfo.PCSSimplefiedResponse>(callBack) {
            @Override
            public BaiduPCSActionInfo.PCSSimplefiedResponse request() {
                return api.deleteFile(path);
            }
        });
    }


    public void rename(final String oldPath,final String newName,PCSCallBack<BaiduPCSActionInfo.PCSFileFromToResponse> callBack){
        threadPoolExecutor.execute(new APIRequest<BaiduPCSActionInfo.PCSFileFromToResponse>(callBack) {
            @Override
            public BaiduPCSActionInfo.PCSFileFromToResponse request() {
                return api.rename(oldPath,newName);
            }
        });
    }

    private abstract class APIRequest<T> implements Runnable{

        private PCSCallBack<T> callBack;
        private APIRequest(PCSCallBack<T> callBack){
            this.callBack=callBack;
        }

        public abstract T request();

        @Override
        public void run() {
            try{
             final T t=request();
                if(callBack != null){
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callBack.call(t);
                        }
                    });
                }
            }catch (Exception e){
                e.printStackTrace();
                if(callBack != null){
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callBack.call(null);
                        }
                    });
                }
            }
        }
    }



    public static interface PCSCallBack<T>{
        void call(T t);
    }

}
