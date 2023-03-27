package com.example.gaodemapdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    // 声明一个集合，在后面的代码中用来存储用户拒绝授权的权限
    private List<String> mPermissionList = new ArrayList<>();
    public static Helmet helmet = new Helmet();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        helmet.setContext(MainActivity.this);
        helmet.init();
        initPermissions();
        //扫描蓝牙设备
        helmet.searchBtDevice();
    }

    protected void onDestroy() {
        super.onDestroy();

        //注销广播接收
        unregisterReceiver(helmet.bleBroadcastReceiver);
    }

    // 动态申请权限
    private void initPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 版本大于等于 Android12 时
            // 只包括蓝牙这部分的权限，其余的需要什么权限自己添加
            mPermissionList.add(Manifest.permission.BLUETOOTH_SCAN);
            mPermissionList.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            mPermissionList.add(Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            // Android 版本小于 Android12 及以下版本
            mPermissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            mPermissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (mPermissionList.size() > 0) {
            ActivityCompat.requestPermissions(this, mPermissionList.toArray(new String[0]), 1001);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 有权限没有通过
        boolean hasPermissionDismiss = false;
        if (1001 == requestCode) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == -1) {
                    hasPermissionDismiss = true;
                    break;
                }
            }
        }
        if (hasPermissionDismiss) {
            // 有权限未通过的处理
            Log.d(TAG, "有权限未通过");
        } else {
            //权限全部通过的处理
            Log.d(TAG, "权限全部通过");
        }
    }

    public void jumpSearchActivity(View view) {
        startActivity(new Intent(this, SearchActivity.class));
    }


}