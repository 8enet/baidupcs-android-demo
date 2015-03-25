package com.demo.baidupcsapi;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.baidu.pcs.BaiduPCSStatusListener;

/**
 * Created by zl on 15/3/13.
 */
public class BaseActivity extends ActionBarActivity {

    protected static final int FILE_SELECT_CODE=4;
    protected Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context=this;
    }

    protected void showMessage(String msg){
        new AlertDialog.Builder(this).setMessage(msg).show();
    }

    protected AlertDialog.Builder showEditDialog(final StringBuilder stringBuilder){
        try {
            final EditText editText = new EditText(this);
            editText.setText(stringBuilder);
            editText.setSelection(stringBuilder.length());
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(editText);
            builder.setCancelable(false);
            builder.setPositiveButton("确认",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            stringBuilder.setLength(0);
                            stringBuilder.append(editText.getText().toString());
                        }
                    });
            builder.setNegativeButton("取消",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            stringBuilder.setLength(0);
                        }
                    });

            return builder;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }




   protected class PCSUpOrDownFileTask extends AsyncTask<String,Long,Void> {

        private ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd=new ProgressDialog(context);
            pd.setIndeterminate(false);
            pd.setCancelable(false);
            pd.setTitle("进度");
            //pd.setMessage(msg);
            pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            pd.show();
        }

        private String type;
        PCSUpOrDownFileTask(String t){
            this.type=t;
        }


        @Override
        protected Void doInBackground(String... params) {
            if("down".equals(type)){

                String source = params[0];
                String target = params[1];
                Log.d("test","source --> "+source);
                Log.d("test","target --> "+target);
                PCSClientAPI.getInstance().getBaiduPCSClientAPI().downloadFileFromStream(source, target, new BaiduPCSStatusListener(){
                    @Override
                    public void onProgress(long bytes, long total) {
                        publishProgress(bytes,total);
                    }

                    @Override
                    public long progressInterval(){
                        return 1000;
                    }

                });

            }else if("up".equals(type)){
                String tmpFile=params[0];
                String dir=PCSClientAPI.mbRootPath;
                if(params.length ==2){
                    dir=params[1];
                }
                Log.d("test", " ----->> uploadFile  " + tmpFile);
                PCSClientAPI.getInstance().getBaiduPCSClientAPI().uploadFile(tmpFile,dir  + "/"+FileUtils.getName(params[0]), new BaiduPCSStatusListener(){

                    @Override
                    public void onProgress(long bytes, long total) {
                        publishProgress(bytes,total);
                    }

                    @Override
                    public long progressInterval(){
                        return 1000;
                    }
                });
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Long... values) {
            super.onProgressUpdate(values);
            pd.setMax(values[1].intValue());
            pd.setProgress(values[0].intValue());
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if(pd != null && pd.isShowing()){
                pd.setMessage("完成！！");
                pd.setCancelable(true);
                Toast.makeText(context,"成功",Toast.LENGTH_LONG).show();
            }
        }
    }


}
