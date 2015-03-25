package com.demo.baidupcsapi;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.pcs.BaiduPCSActionInfo;

import java.io.File;
import java.util.Date;
import java.util.List;


public class BaiduPCSFilesActivity extends BaseActivity implements View.OnClickListener{


    private ListView listView;
    private TextView tvParentView;
    private String currentPath;
    private String type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_baidu_pcsfiles);
        listView= (ListView) findViewById(R.id.list_view);
        currentPath=PCSClientAPI.mbRootPath;
        if(getIntent() == null){
            finish();
            return;
        }
        type= getIntent().getStringExtra("type");
        if("apk".equals(type)){
            listApks();
        }else {
            listFiles(currentPath);
            tvParentView=new TextView(this);
            tvParentView.setText("上一级");
        }
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(position == 0 && !PCSClientAPI.mbRootPath.equals(currentPath)){
                    currentPath=FileUtils.getParent(currentPath);
                    listFiles(currentPath);
                    return;
                }

                BaiduPCSActionInfo.PCSCommonFileInfo item = (BaiduPCSActionInfo.PCSCommonFileInfo) listView.getAdapter().getItem(position);
                if(item != null){
                    if(item.isDir){
                        listFiles(item.path);
                    }else {
                        showCommonFileInfo(item);
                    }
                }
            }
        });
    }

    private Dialog dialog;
    private void showCommonFileInfo(BaiduPCSActionInfo.PCSCommonFileInfo item){
        if (item == null)return;
        StringBuilder sb=new StringBuilder();
        sb.append("path:").append(item.path).append("\n");
        sb.append("创建时间:").append(new Date(item.cTime * 1000).toLocaleString()).append("\n");
        sb.append("修改时间:").append(new Date(item.cTime * 1000).toLocaleString()).append("\n");
        sb.append("MD5:").append(item.blockList).append("\n");
        sb.append("大小:").append(Formatter.formatFileSize(BaiduPCSFilesActivity.this, item.size));
        dialog = new Dialog(this);
        RelativeLayout layout= (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.dialog_info,null);
        layout.findViewById(R.id.btn_download).setOnClickListener(this);
        layout.findViewById(R.id.btn_delete).setOnClickListener(this);
        layout.findViewById(R.id.btn_copy).setOnClickListener(this);
        layout.findViewById(R.id.btn_rename).setOnClickListener(this);
        layout.setTag(item);
        TextView tv= (TextView) layout.findViewById(R.id.tv_content);
        tv.setText(sb.toString());
        dialog.setContentView(layout);
        dialog.show();


    }



    @Override
    public void onClick(View v) {
        BaiduPCSActionInfo.PCSCommonFileInfo item=null;
        try{
           item = (BaiduPCSActionInfo.PCSCommonFileInfo) ((View)(v.getParent()).getParent()).getTag();;
        }catch (Exception e){

        }

        switch (v.getId()){
            case R.id.btn_download:
                if(item != null)
                new PCSUpOrDownFileTask("down").execute(item.path, Environment.getExternalStorageDirectory()+"/Download/"+FileUtils.getName(item.path));

                break;
            case R.id.btn_delete:
                if(item != null)
                    delete(item.path);
                break;
            case R.id.btn_copy:
                break;
            case R.id.btn_rename:
                rename(item);
                break;
        }
    }

    private void delete(String path){
        PCSClientAPI.getInstance().deleteFile(path,new PCSClientAPI.PCSCallBack<BaiduPCSActionInfo.PCSSimplefiedResponse>() {
            @Override
            public void call(BaiduPCSActionInfo.PCSSimplefiedResponse pcsSimplefiedResponse) {
                if(pcsSimplefiedResponse != null && pcsSimplefiedResponse.errorCode == 0){
                    Toast.makeText(context,"删除成功",Toast.LENGTH_LONG).show();
                    referList();
                    if(dialog != null && dialog.isShowing()){
                        dialog.dismiss();
                    }
                }else {
                    Toast.makeText(context,"删除失败 ！！！",Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void rename(final BaiduPCSActionInfo.PCSCommonFileInfo item){
        final StringBuilder sb=new StringBuilder();
        sb.append(FileUtils.getName(item.path));
        showEditDialog(sb).setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (!TextUtils.isEmpty(sb)) {
                    PCSClientAPI.getInstance().rename(item.path, sb.toString(), new PCSClientAPI.PCSCallBack<BaiduPCSActionInfo.PCSFileFromToResponse>() {
                        @Override
                        public void call(BaiduPCSActionInfo.PCSFileFromToResponse pcsFileFromToResponse) {
                            if (pcsFileFromToResponse != null && pcsFileFromToResponse.status.errorCode == 0) {
                                referList();
                                StringBuilder sb = new StringBuilder();
                                sb.append("重命名成功！！");
                                showMessage(sb.toString());
                            } else {
                                Toast.makeText(context, "重命名成功失败", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            }
        }).show();
    }

    private void listApks(){
        PCSClientAPI.getInstance().search(PCSClientAPI.mbRootPath,"apk",true,new PCSClientAPI.PCSCallBack<BaiduPCSActionInfo.PCSListInfoResponse>() {
            @Override
            public void call(BaiduPCSActionInfo.PCSListInfoResponse pcsListInfoResponse) {
                if(pcsListInfoResponse != null && pcsListInfoResponse.status.errorCode == 0){
                    Log.d("test","list apks  "+pcsListInfoResponse.list);
                    if(pcsListInfoResponse.list != null){
                        listView.setAdapter(new FileListAdapter(pcsListInfoResponse.list));
                    }else {
                        Log.e("test","none file ");
                    }
                }else {
                    Log.e("test"," list error ");
                }
            }
        });
    }



    private void listFiles(final String path){
        PCSClientAPI.getInstance().list(path,"name","asc",new PCSClientAPI.PCSCallBack<BaiduPCSActionInfo.PCSListInfoResponse>() {
            @Override
            public void call(BaiduPCSActionInfo.PCSListInfoResponse pcsListInfoResponse) {
                if(pcsListInfoResponse != null){
                    if(PCSClientAPI.mbRootPath.equals(path)){
                        if(tvParentView != null){
                            listView.removeHeaderView(tvParentView);
                        }
                    }else {
                        if(tvParentView != null){
                            if(listView.getHeaderViewsCount() == 0)
                            listView.addHeaderView(tvParentView);
                        }
                    }

                    currentPath=path;
                    if(pcsListInfoResponse.list != null){
                        listView.setAdapter(new FileListAdapter(pcsListInfoResponse.list));
                    }
                    Log.d("test", "list file " + pcsListInfoResponse.list);
                }else {
                    Log.e("test"," list error ");
                }
            }
        });
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if("apk".equals(type))
            return false;
        getMenuInflater().inflate(R.menu.menu_baidu_pcsfiles, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_mkdir:
                test_mkdir();
                return true;
            case R.id.action_upload:
                upload();
                return true;
            case R.id.action_refer:
                referList();
                return true;
            case R.id.action_delete_dir:
                delete(currentPath);
                listFiles(FileUtils.getParent(currentPath));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void referList(){
        if("apk".equals(type)){
            listApks();
        }else {
            listFiles(currentPath);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)  {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    Log.d("test", "File Uri: " + uri.toString());
                    String path = FileUtils.getPath(context, uri);
                    new PCSUpOrDownFileTask("up").execute(path,currentPath);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void upload(){
        Intent intent=new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.",  Toast.LENGTH_SHORT).show();
        }
    }


    private void test_mkdir(){
        final StringBuilder stringBuilder=new StringBuilder();
        showEditDialog(stringBuilder).setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (!TextUtils.isEmpty(stringBuilder)) {
                    PCSClientAPI.getInstance().mkdir(currentPath + "/" + stringBuilder.toString(), new PCSClientAPI.PCSCallBack<BaiduPCSActionInfo.PCSFileInfoResponse>() {
                        @Override
                        public void call(BaiduPCSActionInfo.PCSFileInfoResponse pcsFileInfoResponse) {
                            if (pcsFileInfoResponse != null && pcsFileInfoResponse.status.errorCode == 0) {
                                listFiles(currentPath);
                                StringBuilder sb = new StringBuilder();
                                sb.append("path:" + pcsFileInfoResponse.commonFileInfo.path).append("创建成功！");
                                sb.append("\n time:").append(new Date(pcsFileInfoResponse.commonFileInfo.cTime).toLocaleString());
                                showMessage(sb.toString());
                            } else {
                                Log.e("test", "文件夹 " + stringBuilder + " 创建失败" + pcsFileInfoResponse.status.errorCode + "  --> " + pcsFileInfoResponse.status.message);
                                Toast.makeText(context, "文件夹 " + stringBuilder + " 创建失败", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            }
        }).show();

    }




    class FileListAdapter extends BaseAdapter{

        private List<BaiduPCSActionInfo.PCSCommonFileInfo> data;

        FileListAdapter(List<BaiduPCSActionInfo.PCSCommonFileInfo> data){
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

            TextView textView=new TextView(BaiduPCSFilesActivity.this);
            BaiduPCSActionInfo.PCSCommonFileInfo info=getItem(position);
            StringBuilder sb=new StringBuilder();
            if(info.isDir){
                sb.append('d');
            }else {
                sb.append('-');
            }
            sb.append("     ").append(FileUtils.getName(info.path)).append("     ");

            if(info.isDir){
                //sb.append(info.size);
            }else {
                sb.append(Formatter.formatFileSize(BaiduPCSFilesActivity.this,info.size));
            }
            textView.setText(sb);
            return textView;
        }
    }


}
