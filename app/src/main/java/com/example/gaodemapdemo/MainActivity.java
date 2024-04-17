package com.example.gaodemapdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.amap.api.maps.AMapException;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.navi.AMapNavi;
import com.amap.api.navi.AMapNaviListener;
import com.amap.api.navi.AMapNaviViewListener;
import com.amap.api.navi.AmapNaviPage;
import com.amap.api.navi.AmapNaviParams;
import com.amap.api.navi.AmapNaviType;
import com.amap.api.navi.INaviInfoCallback;
import com.amap.api.navi.ParallelRoadListener;
import com.amap.api.navi.enums.AMapNaviParallelRoadStatus;
import com.amap.api.navi.model.AMapCalcRouteResult;
import com.amap.api.navi.model.AMapLaneInfo;
import com.amap.api.navi.model.AMapModelCross;
import com.amap.api.navi.model.AMapNaviCameraInfo;
import com.amap.api.navi.model.AMapNaviCross;
import com.amap.api.navi.model.AMapNaviLocation;
import com.amap.api.navi.model.AMapNaviRouteNotifyData;
import com.amap.api.navi.model.AMapNaviTrafficFacilityInfo;
import com.amap.api.navi.model.AMapServiceAreaInfo;
import com.amap.api.navi.model.AimLessModeCongestionInfo;
import com.amap.api.navi.model.AimLessModeStat;
import com.amap.api.navi.model.NaviInfo;
import com.example.gaodemapdemo.util.ToastUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class MainActivity extends Activity implements INaviInfoCallback, AMapNaviListener, AMapNaviViewListener, ParallelRoadListener {

    private static final String TAG = "MainActivity";
    // 声明一个集合，在后面的代码中用来存储用户拒绝授权的权限
    private List<String> mPermissionList = new ArrayList<>();
    public static Helmet helmet = new Helmet();

    protected AMapNavi mAMapNavi;
    public String navi_retainTime;
    public String navi_retainDistance;
    public String navi_roadName;
    public String navi_next_roadName;
    public String navi_iconType;
    public String navi_distance;
    public String navi_speed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        helmet.setContext(MainActivity.this);
        helmet.init();
        privacyCompliance();
        initPermissions();
        check_location();
        //扫描蓝牙设备
        helmet.searchBtDevice();
        try {
            mAMapNavi = AMapNavi.getInstance(this);
            mAMapNavi.addAMapNaviListener(this);
            mAMapNavi.addParallelRoadListener(this);
            mAMapNavi.setUseInnerVoice(true, true);
        } catch (AMapException e) {
            e.printStackTrace();
        }

    }

    public void check_location() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this,android.R.style.Theme_Material_Light_Dialog);
            builder.setTitle("GPS未开启");
            builder.setMessage("请开启GPS以使用该应用程序。");
            builder.setPositiveButton("去开启", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            });
            builder.setNegativeButton("取消", null);
            builder.show();
        }
    }

    /**
     * 返回键处理事件
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            System.exit(0);// 退出程序
        }
        return super.onKeyDown(keyCode, event);
    }

    public void start_navi() {
        AmapNaviParams params = new AmapNaviParams(null, null, null, AmapNaviType.RIDE);
        params.setUseInnerVoice(true);
        AmapNaviPage.getInstance().showRouteActivity(this, params, MainActivity.this);
    }

    protected void onDestroy() {
        super.onDestroy();

        //注销广播接收
        unregisterReceiver(helmet.bleBroadcastReceiver);
//        mAMapNaviView.onDestroy();
        //since 1.6.0 不再在naviview destroy的时候自动执行AMapNavi.stopNavi();请自行执行
        if (mAMapNavi != null) {
            mAMapNavi.stopNavi();
            mAMapNavi.destroy();
        }
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

    private void privacyCompliance() {
        MapsInitializer.updatePrivacyShow(MainActivity.this, true, true);
        MapsInitializer.updatePrivacyAgree(MainActivity.this, true);
    }

    public void jumpSearchActivity(View view) {
        if (helmet.curConnState) {
            start_navi();
        } else {
//            start_navi();
            ToastUtil.show(this, "尚未连接到头盔设备,请确保设备已开机或稍等");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
//        mAMapNaviView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
//        mAMapNaviView.onPause();
//        停止导航之后，会触及底层stop，然后就不会再有回调了，但是讯飞当前还是没有说完的半句话还是会说完
//        mAMapNavi.stopNavi();
    }

    @Override
    public void onInitNaviFailure() {
        Toast.makeText(this, "init navi Failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onInitNaviSuccess() {

    }

    @Override
    public void onLocationChange(AMapNaviLocation location) {
        //当前位置回调
        //Log.d("Speed:", String.valueOf(location.getSpeed()));
        navi_speed = String.valueOf(location.getSpeed());
    }

    @Override
    public void onArriveDestination(boolean b) {

    }

    @Override
    public void onStartNavi(int i) {

    }

    @Override
    public void onTrafficStatusUpdate() {

    }

    @Override
    public void onCalculateRouteSuccess(int[] ints) {

    }

    @Override
    public void notifyParallelRoad(int i) {

    }

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo[] aMapNaviTrafficFacilityInfos) {

    }

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo aMapNaviTrafficFacilityInfo) {

    }

    @Override
    public void updateAimlessModeStatistics(AimLessModeStat aimLessModeStat) {

    }

    @Override
    public void updateAimlessModeCongestionInfo(AimLessModeCongestionInfo aimLessModeCongestionInfo) {

    }

    @Override
    public void onPlayRing(int i) {

    }

    @Override
    public void onCalculateRouteSuccess(AMapCalcRouteResult aMapCalcRouteResult) {

    }

    @Override
    public void onCalculateRouteFailure(AMapCalcRouteResult aMapCalcRouteResult) {

    }

    @Override
    public void onNaviRouteNotify(AMapNaviRouteNotifyData aMapNaviRouteNotifyData) {

    }

    @Override
    public void onGpsSignalWeak(boolean b) {

    }

    @Override
    public void onCalculateRouteFailure(int i) {

    }

    @Override
    public void onReCalculateRouteForYaw() {

    }

    @Override
    public void onReCalculateRouteForTrafficJam() {

    }

    @Override
    public void onStopSpeaking() {

    }

    @Override
    public void onReCalculateRoute(int i) {

    }

    @Override
    public void onExitPage(int i) {

    }

    @Override
    public void onStrategyChanged(int i) {

    }

    @Override
    public void onArrivedWayPoint(int i) {

    }

    @Override
    public void onGpsOpenStatus(boolean b) {

    }

    @Override
    public void onMapTypeChanged(int i) {

    }

    @Override
    public void onNaviViewShowMode(int i) {

    }

    @Override
    public void onNaviDirectionChanged(int i) {

    }

    @Override
    public void onDayAndNightModeChanged(int i) {

    }

    @Override
    public void onBroadcastModeChanged(int i) {

    }

    @Override
    public void onScaleAutoChanged(boolean b) {

    }

    @Override
    public View getCustomMiddleView() {
        return null;
    }

    @Override
    public View getCustomNaviView() {
        return null;
    }

    @Override
    public View getCustomNaviBottomView() {
        return null;
    }

    @Override
    public void onGetNavigationText(int type, String text) {
    }

    @Override
    public void onGetNavigationText(String s) {

    }

    @Override
    public void onEndEmulatorNavi() {

    }

    @Override
    public void onArriveDestination() {

    }

    @Override
    public void onNaviSetting() {

    }

    @Override
    public void onNaviCancel() {
        finish();
    }

    @Override
    public boolean onNaviBackClick() {
        return false;
    }

    @Override
    public void onNaviMapMode(int i) {

    }

    @Override
    public void onNaviTurnClick() {

    }

    @Override
    public void onNextRoadClick() {

    }

    @Override
    public void onScanViewButtonClick() {

    }

    @Override
    public void onLockMap(boolean b) {

    }

    @Override
    public void onNaviViewLoaded() {

    }

    @Override
    public void onNaviInfoUpdate(NaviInfo naviinfo) {
        navi_distance = String.valueOf(naviinfo.getCurStepRetainDistance());
        navi_roadName = naviinfo.getCurrentRoadName();
        navi_next_roadName = naviinfo.getNextRoadName();
        navi_iconType = String.valueOf(naviinfo.getIconType());
        navi_retainTime = String.valueOf(naviinfo.getPathRetainTime());
        navi_retainDistance = String.valueOf(naviinfo.getPathRetainDistance());
        //导航过程中的信息更新，请看NaviInfo的具体说明
        Log.d("RetainTime:", navi_retainTime);
        Log.d("RetainDistance:", navi_retainDistance);
        Log.d("NextRoadName:", navi_roadName);
        Log.d("Type:", navi_iconType);
        Log.d("Distance:", navi_distance);
        Log.d("Speed:", navi_speed);
        if (helmet.curConnState) {
            String sendMsg = navi_retainTime +
                    "," + navi_retainDistance +
                    "," + navi_next_roadName +
                    "," + navi_iconType +
                    "," + navi_distance +
                    "," + navi_speed;
            if (helmet.bleManager != null) {
                helmet.bleManager.sendMessage(sendMsg);  //以16进制字符串形式发送数据
            }
        }
    }

    @Override
    public void updateCameraInfo(AMapNaviCameraInfo[] aMapNaviCameraInfos) {

    }

    @Override
    public void updateIntervalCameraInfo(AMapNaviCameraInfo aMapNaviCameraInfo, AMapNaviCameraInfo aMapNaviCameraInfo1, int i) {

    }

    @Override
    public void onServiceAreaUpdate(AMapServiceAreaInfo[] aMapServiceAreaInfos) {

    }

    @Override
    public void showCross(AMapNaviCross aMapNaviCross) {

    }

    @Override
    public void hideCross() {

    }

    @Override
    public void showModeCross(AMapModelCross aMapModelCross) {

    }

    @Override
    public void hideModeCross() {

    }

    @Override
    public void showLaneInfo(AMapLaneInfo[] aMapLaneInfos, byte[] bytes, byte[] bytes1) {

    }

    @Override
    public void showLaneInfo(AMapLaneInfo aMapLaneInfo) {

    }

    @Override
    public void hideLaneInfo() {

    }

    @Override
    public void notifyParallelRoad(AMapNaviParallelRoadStatus aMapNaviParallelRoadStatus) {

    }
}