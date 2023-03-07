package com.example.gaodemapdemo.permission;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * 封装权限申请
 */
public class PermissionRequest extends Activity {

    private static final String TAG = "PermissionRequest";
    private static final int REQUEST_PERMISSION_CODE = 1;  //默认请求权限的requestCode为1
    private PermissionListener mListener;

    /**
     * 请求申请权限
     * 默认请求权限的requestCode为1
     * @param permissions  要申请的权限数组
     * @param permissionListener 权限申请结果监听者
     */
    public void requestRuntimePermission(Context context, String[] permissions, PermissionListener permissionListener){
        mListener = permissionListener;
        //存放permissions中当前未被授予的权限
        List<String> permissionList = new ArrayList<>();
        //遍历权限数组，检测所需权限是否已被授予，若该权限尚未授予，添加到permissionList中
        for (String permission : permissions) {
            if(ContextCompat.checkSelfPermission(context,permission) != PackageManager.PERMISSION_GRANTED){
                if(!permissionList.contains(permission)){
                    permissionList.add(permission);
                }
            }
        }

        if(!permissionList.isEmpty()){
            //有权限尚未授予，去授予权限
            ActivityCompat.requestPermissions((Activity) context,permissionList.toArray(new String[permissionList.size()]),REQUEST_PERMISSION_CODE);
        }else{
            //权限都被授予了
            if(mListener != null){
                mListener.onGranted();  //权限都被授予了回调
                Log.d(TAG,"权限都授予了");
            }
        }
    }

    /**
     * 申请权限结果返回
     * @param requestCode  请求码
     * @param permissions  所有申请的权限集合
     * @param grantResults 权限申请的结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch(requestCode){
            case REQUEST_PERMISSION_CODE:
                if(grantResults.length > 0){ //有权限申请
                    //存储被用户拒绝的权限
                    List<String> deniedPermissionList = new ArrayList<>();
                    //有权限被拒绝，分类出被拒绝的权限
                    for (int i = 0; i < grantResults.length; i++) {
                        String permission = permissions[i];
                        int grantResult = grantResults[i];
                        if(grantResult != PackageManager.PERMISSION_GRANTED){
                            if(!deniedPermissionList.contains(permission)){
                                deniedPermissionList.add(permission);
                            }
                        }
                    }

                    if(deniedPermissionList.isEmpty()){
                        //没有被拒绝的权限
                        if(mListener != null){
                            mListener.onGranted();
                            Log.d(TAG,"权限都授予了");
                        }
                    }else{
                        //有被拒绝的权限
                        if(mListener != null){
                            mListener.onDenied(deniedPermissionList);
                            Log.e(TAG,"有权限被拒绝了");
                        }
                    }
                }
                break;
        }
    }
}
