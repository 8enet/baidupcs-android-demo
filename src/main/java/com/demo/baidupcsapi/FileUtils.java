package com.demo.baidupcsapi;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

public class FileUtils {
    public static String getPath(Context context, Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_data" };
            Cursor cursor = null;
 
            try {
                cursor = context.getContentResolver().query(uri, projection,null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
 
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
 
        return null;
    }

    /**
     * 获取文件名
     * @param path 路径
     * @return
     */
    public static String getName(String path){
        if(TextUtils.isEmpty(path)||path.indexOf('/') == -1)
            return path;
        return path.substring(path.lastIndexOf('/')+1);
    }


    /**
     * 根据路径获取上一级路径
     * @param path
     * @return
     */
    public static String getParent(String path) {
        int length = path.length(), firstInPath = 0;

        int index = path.lastIndexOf('/');
        if (index == -1 && firstInPath > 0) {
            index = 2;
        }
        if (index == -1 || path.charAt(length - 1) == '/') {
            return null;
        }
        if (path.indexOf('/') == index
                && path.charAt(firstInPath) == '/') {
            return path.substring(0, index + 1);
        }
        return path.substring(0, index);
    }
}