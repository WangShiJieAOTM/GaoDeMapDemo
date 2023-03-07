package com.example.gaodemapdemo;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import com.amap.api.maps.model.LatLng;
import com.amap.api.navi.AMapNaviView;
import com.amap.api.navi.AMapNaviViewOptions;
import com.amap.api.navi.enums.NaviType;
import com.amap.api.navi.model.AMapCalcRouteResult;
import com.amap.api.navi.model.NaviLatLng;

public class NaviActivity extends BaseActivity {

    private double startLatitude = 0;
    private double startLongitude = 0;
    private double endLatitude = 0;
    private double endLongitude = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navi);
        Intent i = this.getIntent();
        startLatitude = i.getDoubleExtra("startLatitude", 0);
        startLongitude = i.getDoubleExtra("startLongitude", 0);
        endLatitude = i.getDoubleExtra("endLatitude", 0);
        endLongitude = i.getDoubleExtra("endLongitude", 0);
        mAMapNaviView = (AMapNaviView) findViewById(R.id.navi_view);
        mAMapNaviView.onCreate(savedInstanceState);
        mAMapNaviView.setAMapNaviViewListener(this);
        AMapNaviViewOptions options = mAMapNaviView.getViewOptions();
        options.setCarBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.bike_topview));
        options.setAutoLockCar(true);
        options.setAutoDisplayOverview(true);
        options.setSettingMenuEnabled(true);
        options.setAfterRouteAutoGray(true);
        mAMapNaviView.setViewOptions(options);
    }

    @Override
    public void onInitNaviSuccess() {
        super.onInitNaviSuccess();
        mAMapNavi.calculateRideRoute(new NaviLatLng(startLatitude, startLongitude), new NaviLatLng(endLatitude, endLongitude));
    }

    @Override
    public void onCalculateRouteSuccess(AMapCalcRouteResult aMapCalcRouteResult) {
        super.onCalculateRouteSuccess(aMapCalcRouteResult);
        mAMapNavi.startNavi(NaviType.GPS);
    }
}
