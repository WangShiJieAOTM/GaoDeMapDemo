package com.example.gaodemapdemo;

import static com.example.gaodemapdemo.MainActivity.helmet;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.view.View.OnClickListener;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.AMap.OnMarkerClickListener;
import com.amap.api.maps.AMap.InfoWindowAdapter;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItemV2;
import com.amap.api.services.core.ServiceSettings;
import com.amap.api.services.help.Tip;
import com.amap.api.services.poisearch.PoiResultV2;
import com.amap.api.services.poisearch.PoiSearchV2;
import com.example.gaodemapdemo.overlay.PoiOverlay;

import java.util.ArrayList;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import com.example.gaodemapdemo.util.Constants;
import com.example.gaodemapdemo.util.ToastUtil;

public class SearchActivity extends FragmentActivity implements OnMarkerClickListener, InfoWindowAdapter, PoiSearchV2.OnPoiSearchListener, OnClickListener, AMapLocationListener, LocationSource {
    //请求权限码
    private static final int REQUEST_PERMISSIONS = 9527;
    //POI搜索关键字
    private String mKeyWords = "";
    //搜索进度条
    private ProgressDialog progDialog = null;
    //POI查找返回的结果
    private PoiResultV2 poiResult;
    //搜索返回的当前页
    private int currentPage = 1;
    //POI查询条件类
    private PoiSearchV2.Query query;
    //POI搜索类
    private PoiSearchV2 poiSearch;
    //POI Marker
    private Marker mPoiMarker;

    //搜索关键词显示框
    private TextView mKeywordsTextView;
    //出发按钮
    private ImageView mGoButton;
    //清除按钮
    private ImageView mCleanKeyWords;

    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    //声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;
    //地图控制器
    private AMap mAMap;
    //位置更改监听
    private OnLocationChangedListener mListener;
    //地图显示
    private MapView mapView;
    //现在的位置(定位出的位置)
    private LatLng nowLng = null;
    //目标位置
    private LatLng endLng = null;
    //goLayout  用于隐藏和显示出发按钮
    private RelativeLayout goLayout;


    public static final int REQUEST_CODE = 100;
    public static final int RESULT_CODE_INPUTTIPS = 101;
    public static final int RESULT_CODE_KEYWORDS = 102;

    public void initPrivacy() {
        //隐私声明
        AMapLocationClient.updatePrivacyShow(this, true, true);
        AMapLocationClient.updatePrivacyAgree(this, true);
        //地图隐私政策同意
        MapsInitializer.updatePrivacyShow(this, true, true);
        MapsInitializer.updatePrivacyAgree(this, true);
        //搜索隐私政策同意
        ServiceSettings.updatePrivacyShow(this, true, true);
        ServiceSettings.updatePrivacyAgree(this, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        //初始化点击回调
        initOnclick();
        goLayout = (RelativeLayout) findViewById(R.id.goLayout);
        goLayout.setVisibility(View.INVISIBLE);

        //初始化隐私
        initPrivacy();
        //初始化定位
        try {
            initLocation();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //初始化地图
        initMap(savedInstanceState);

        checkingAndroidVersion();
        mKeyWords = "";
    }

    /**
     * 检查Android版本
     */
    private void checkingAndroidVersion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Android6.0及以上先获取权限再定位
            requestPermission();
        } else {
            //启动定位
            mLocationClient.startLocation();
        }
    }

    /**
     * 动态请求权限
     */
    @AfterPermissionGranted(REQUEST_PERMISSIONS)
    private void requestPermission() {
        String[] permissions = {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (EasyPermissions.hasPermissions(this, permissions)) {
            mLocationClient.startLocation();
        } else {
            //false 无权限
            EasyPermissions.requestPermissions(this, "需要权限", REQUEST_PERMISSIONS, permissions);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //设置权限请求结果
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    private void initOnclick() {
        mCleanKeyWords = (ImageView) findViewById(R.id.clean_keywords);
        mCleanKeyWords.setOnClickListener(this);
        mGoButton = (ImageView) findViewById(R.id.goButton);
        mGoButton.setOnClickListener(this);
        mKeywordsTextView = (TextView) findViewById(R.id.main_keywords);
        mKeywordsTextView.setOnClickListener(this);
    }

    private void initLocation() throws Exception {
        //初始化定位
        mLocationClient = new AMapLocationClient(getApplicationContext());
        //设置定位回调监听
        mLocationClient.setLocationListener(this);
        //初始化AMapLocationClientOption对象
        mLocationOption = new AMapLocationClientOption();
        //设置定位模式为AMapLocationMode.High_Accuracy，高精度模式。
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //获取最近3s内精度最高的一次定位结果：
        //设置setOnceLocationLatest(boolean b)接口为true，启动定位时SDK会返回最近3s内精度最高的一次定位结果。如果设置其为true，setOnceLocation(boolean b)接口也会被设置为true，反之不会，默认为false。
        mLocationOption.setOnceLocationLatest(true);
        //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setNeedAddress(true);
        //设置定位请求超时时间，单位是毫秒，默认30000毫秒，建议超时时间不要低于8000毫秒。
        mLocationOption.setHttpTimeOut(20000);
        //关闭缓存机制，高精度定位会产生缓存。
        mLocationOption.setLocationCacheEnable(false);
        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
    }

    /**
     * 初始化AMap对象
     */
    private void initMap(Bundle savedInstanceState) {
        if (mAMap == null) {
            mapView = findViewById(R.id.map);
            mapView.onCreate(savedInstanceState);
            mAMap = mapView.getMap();
            //设置最小缩放等级为15 ，缩放级别范围为[3, 20]
            mAMap.setMinZoomLevel(15);
            //开启室内地图
            mAMap.showIndoorMap(true);
            //隐藏缩放按钮 默认显示
            mAMap.getUiSettings().setZoomControlsEnabled(false);
            //显示比例尺 默认不显示
            mAMap.getUiSettings().setScaleControlsEnabled(true);
            // 设置定位监听
            mAMap.setLocationSource(this);
            // 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
            mAMap.setMyLocationEnabled(true);
            mAMap.setOnMarkerClickListener(this);
            mAMap.setInfoWindowAdapter(this);
        }
    }

    /**
     * 显示进度框
     */
    private void showProgressDialog() {
        if (progDialog == null) progDialog = new ProgressDialog(this);
        progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progDialog.setIndeterminate(false);
        progDialog.setCancelable(false);
        progDialog.setMessage("正在搜索:\n" + mKeyWords);
        progDialog.show();
    }

    /**
     * 隐藏进度框
     */
    private void dissmissProgressDialog() {
        if (progDialog != null) {
            progDialog.dismiss();
        }
    }

    /**
     * 开始进行poi搜索
     */
    protected void doSearchQuery(String keywords) throws AMapException {
        showProgressDialog();// 显示进度框
        currentPage = 1;
        // 第一个参数表示搜索字符串，第二个参数表示poi搜索类型，第三个参数表示poi搜索区域（空字符串代表全国）
        query = new PoiSearchV2.Query(keywords, "", Constants.DEFAULT_CITY);
        // 设置每页最多返回多少条poiitem
        query.setPageSize(10);
        // 设置查第一页
        query.setPageNum(currentPage);

        poiSearch = new PoiSearchV2(this, query);
        poiSearch.setOnPoiSearchListener(this);
        poiSearch.searchPOIAsyn();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        marker.showInfoWindow();
        return false;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

    @Override
    public View getInfoWindow(final Marker marker) {
        View view = getLayoutInflater().inflate(R.layout.poikeywordsearch_uri, null);
        TextView title = (TextView) view.findViewById(R.id.title);
        title.setText(marker.getTitle());
        TextView snippet = (TextView) view.findViewById(R.id.snippet);
        snippet.setText(marker.getSnippet());
        return view;
    }

    /**
     * 输入提示activity选择结果后的处理逻辑
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CODE_INPUTTIPS && data != null) {
            mAMap.clear();
            Tip tip = data.getParcelableExtra(Constants.EXTRA_TIP);
            if (tip.getPoiID() == null || tip.getPoiID().equals("")) {
                try {
                    doSearchQuery(tip.getName());
                } catch (AMapException e) {
                    e.printStackTrace();
                }
            } else {
                addTipMarker(tip);
            }
            mKeywordsTextView.setText(tip.getName());
            if (!tip.getName().equals("")) {
                mCleanKeyWords.setVisibility(View.VISIBLE);
            }
        } else if (resultCode == RESULT_CODE_KEYWORDS && data != null) {
            mAMap.clear();
            String keywords = data.getStringExtra(Constants.KEY_WORDS_NAME);
            if (keywords != null && !keywords.equals("")) {
                try {
                    doSearchQuery(keywords);
                } catch (AMapException e) {
                    e.printStackTrace();
                }
            }
            mKeywordsTextView.setText(keywords);
            if (!keywords.equals("")) {
                mCleanKeyWords.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * 用 marker展示输入提示list选中数据
     */
    private void addTipMarker(Tip tip) {
        if (tip == null) {
            return;
        }
        mPoiMarker = mAMap.addMarker(new MarkerOptions());
        LatLonPoint point = tip.getPoint();
        if (point != null) {
            LatLng markerPosition = new LatLng(point.getLatitude(), point.getLongitude());
            endLng = markerPosition;
            mPoiMarker.setPosition(markerPosition);
            mAMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markerPosition, 17));
            goLayout.setVisibility(View.VISIBLE);
        }
        mPoiMarker.setTitle(tip.getName());
        mPoiMarker.setSnippet(tip.getAddress());
    }

    /**
     * 点击事件回调方法
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_keywords:
                Intent intent = new Intent(this, InputTipsActivity.class);
                startActivityForResult(intent, REQUEST_CODE);
                break;
            case R.id.clean_keywords:
                mKeywordsTextView.setText("");
                mAMap.clear();
                mCleanKeyWords.setVisibility(View.GONE);
                break;
            case R.id.goButton:
                Intent navi_intent = new Intent(this, NaviActivity.class);
                navi_intent.putExtra("startLatitude", nowLng.latitude);
                navi_intent.putExtra("startLongitude", nowLng.longitude);
                navi_intent.putExtra("endLatitude", endLng.latitude);
                navi_intent.putExtra("endLongitude", endLng.longitude);
                startActivity(navi_intent);
                goLayout.setVisibility(View.INVISIBLE);
                break;
            default:
                break;
        }
    }

    @Override
    public void onPoiSearched(PoiResultV2 result, int rCode) {
        dissmissProgressDialog();// 隐藏对话框
        if (rCode == 1000) {
            if (result != null && result.getQuery() != null) {// 搜索poi的结果
                if (result.getQuery().equals(query)) {// 是否是同一条
                    poiResult = result;
                    // 取得搜索到的poiitems有多少页
                    ArrayList<PoiItemV2> poiItems = poiResult.getPois();// 取得第一页的poiitem数据，页数从数字0开始

                    if (poiItems != null && poiItems.size() > 0) {
                        mAMap.clear();// 清理之前的图标
                        PoiOverlay poiOverlay = new PoiOverlay(mAMap, poiItems);
                        poiOverlay.removeFromMap();
                        poiOverlay.addToMap();
                        poiOverlay.zoomToSpan();
                    } else {
                        ToastUtil.show(SearchActivity.this, R.string.no_result);
                    }
                }
            } else {
                ToastUtil.show(SearchActivity.this, R.string.no_result);
            }
        } else {
            ToastUtil.showerror(this, rCode);
        }
    }

    @Override
    public void onPoiItemSearched(PoiItemV2 poiItemV2, int i) {

    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
                //地址
                String address = aMapLocation.getAddress();
                double latitude = aMapLocation.getLatitude();
                double longitude = aMapLocation.getLongitude();

                nowLng = new LatLng(latitude, longitude);
                //停止定位后,本地定位服务不会被销毁
                mLocationClient.stopLocation();
                if (mListener != null) {
                    mListener.onLocationChanged(aMapLocation);
                }
            } else {
                //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                Log.e("AmapError", "location Error, ErrCode:" + aMapLocation.getErrorCode() + ", errInfo:" + aMapLocation.getErrorInfo());
            }
        }
    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mListener = onLocationChangedListener;
        if (mLocationClient == null) {
            mLocationClient.startLocation();//启动定位
        }
    }

    @Override
    public void deactivate() {
        mListener = null;
        if (mLocationClient != null) {
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
        }
        mLocationClient = null;
    }

    public void location(View view) {
        mAMap.moveCamera(CameraUpdateFactory.changeLatLng(nowLng));
    }
}
