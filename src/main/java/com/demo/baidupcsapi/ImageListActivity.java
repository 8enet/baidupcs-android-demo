package com.demo.baidupcsapi;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.baidu.pcs.BaiduPCSActionInfo;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.nio.channels.Pipe;
import java.util.List;


public class ImageListActivity extends BaseActivity {


    private GridView gridView;
    private static int width;

    private String type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_list);
        gridView= (GridView) findViewById(R.id.grid_view);
        width=getResources().getDisplayMetrics().widthPixels;
        width=(width-200)/4;

        type=getIntent().getStringExtra("type");


        if("img".equals(type)) {

            PCSClientAPI.getInstance().imageStream(new PCSClientAPI.PCSCallBack<BaiduPCSActionInfo.PCSListInfoResponse>() {
                @Override
                public void call(BaiduPCSActionInfo.PCSListInfoResponse pcsListInfoResponse) {
                    if (pcsListInfoResponse != null && pcsListInfoResponse.status.errorCode == 0) {
                        Log.d("test","img --> "+pcsListInfoResponse.list);
                        if (pcsListInfoResponse.list != null) {
                            gridView.setAdapter(new ImageAdapter(pcsListInfoResponse.list));
                        }
                    }
                }
            });
        }else {

            PCSClientAPI.getInstance().videoStream(new PCSClientAPI.PCSCallBack<BaiduPCSActionInfo.PCSListInfoResponse>() {
                @Override
                public void call(BaiduPCSActionInfo.PCSListInfoResponse pcsListInfoResponse) {
                    if (pcsListInfoResponse != null && pcsListInfoResponse.status.errorCode == 0) {
                        Log.d("test","video --> "+pcsListInfoResponse.list);
                        if (pcsListInfoResponse.list != null) {
                            gridView.setAdapter(new ImageAdapter(pcsListInfoResponse.list));
                        }
                    }
                }
            });
        }

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if("video".equals(type))
                    return;
                if(gridView.getAdapter() != null){
                    BaiduPCSActionInfo.PCSCommonFileInfo item = (BaiduPCSActionInfo.PCSCommonFileInfo) gridView.getAdapter().getItem(position);

                    if(item != null)
                    showImage(item.path);
                }
            }
        });
    }



    private void showImage(String path){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ImageView img=new ImageView(this);
        String url="https://pcs.baidu.com/rest/2.0/pcs/stream?method=download&access_token="+PCSClientAPI.getInstance().getAccessToken()+"&path="+path;
        ImageLoader.getInstance().displayImage(url,img);
        builder.setView(img);
        builder.show();
    }


    private String getImageURL(String path,int width){
        StringBuilder sb=new StringBuilder();
        sb.append("https://pcs.baidu.com/rest/2.0/pcs/thumbnail?method=generate&access_token=").append(PCSClientAPI.getInstance().getAccessToken());
        sb.append("&path=").append(path).append("&quality=100");

        if("img".equals(type)){
            sb.append("&width=").append(width).append("&height=").append(width);
        }else {
            sb.append("&size=c300_u300");
        }
        return sb.toString();
    }

    class ImageAdapter extends BaseAdapter{

        private List<BaiduPCSActionInfo.PCSCommonFileInfo> data;

        ImageAdapter(List<BaiduPCSActionInfo.PCSCommonFileInfo> data){
            this.data=data;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public BaiduPCSActionInfo.PCSCommonFileInfo getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView img=null;
            if(convertView == null){
                img=new ImageView(ImageListActivity.this);
                img.setMinimumWidth(width);
                img.setMaxHeight(width);
                convertView=img;
            }else {
                img= (ImageView) convertView;
            }

            String url=getImageURL(getItem(position).path,width);

            ImageLoader.getInstance().displayImage(url,img,new SimpleImageLoadingListener(){
                @Override
                public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                    super.onLoadingFailed(imageUri, view, failReason);
                    Log.e("test",imageUri+"   load ERROR !!"+failReason);
                }

                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    super.onLoadingComplete(imageUri, view, loadedImage);
                    ((ImageView)view).setImageBitmap(loadedImage);
                }
            });
            return convertView;
        }
    }
}
