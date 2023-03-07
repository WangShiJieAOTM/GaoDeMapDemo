package com.example.gaodemapdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.gaodemapdemo.ble.BLEManager;
import com.example.gaodemapdemo.ble.OnBleConnectListener;
import com.example.gaodemapdemo.ble.OnDeviceSearchListener;
import com.example.gaodemapdemo.permission.PermissionListener;
import com.example.gaodemapdemo.permission.PermissionRequest;
import com.example.gaodemapdemo.util.ToastUtil;
import com.example.gaodemapdemo.util.TypeConversion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pub.devrel.easypermissions.EasyPermissions;


@RequiresApi(api = Build.VERSION_CODES.S)
public class BlueToothActivity extends AppCompatActivity {
    private static final String TAG = "BLEMain";
    //请求权限码
    private static final int REQUEST_PERMISSIONS = 9527;

    private static final int CONNECT_SUCCESS = 0x01;
    private static final int CONNECT_FAILURE = 0x02;
    private static final int DISCONNECT_SUCCESS = 0x03;
    private static final int SEND_SUCCESS = 0x04;
    private static final int SEND_FAILURE = 0x05;
    private static final int RECEIVE_SUCCESS = 0x06;
    private static final int RECEIVE_FAILURE = 0x07;
    private static final int START_DISCOVERY = 0x08;
    private static final int STOP_DISCOVERY = 0x09;
    private static final int DISCOVERY_DEVICE = 0x0A;
    private static final int DISCOVERY_OUT_TIME = 0x0B;
    private static final int SELECT_DEVICE = 0x0C;
    private static final int BT_OPENED = 0x0D;
    private static final int BT_CLOSED = 0x0E;

    //bt_patch(mtu).bin
    public static final String SERVICE_UUID = "e95dd91d-251d-470a-a062-fa1922dfa9a8";  //蓝牙通讯服务
    public static final String READ_UUID = "e95d95ee-251d-470a-a062-fa1922dfa9a8";  //读特征
    public static final String WRITE_UUID = "e95d93ee-251d-470a-a062-fa1922dfa9a8";  //写特征
    public static final String HELMET_NAME = "Helmet";
    public static final String HELMET_ADDRESS = "B8:27:EB:20:49:54";
    //动态申请权限
    private String[] requestPermissionArray = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    // 声明一个集合，在后面的代码中用来存储用户拒绝授权的权限
    private List<String> deniedPermissionList = new ArrayList<>();
    private List<String> mPermissionList = new ArrayList<>();

    private Context mContext;
    private BLEManager bleManager;
    private BluetoothDevice curBluetoothDevice;  //当前连接的设备
    private BLEBroadcastReceiver bleBroadcastReceiver;
    //当前设备连接状态
    private boolean curConnState = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blue_tooth);

        mContext = BlueToothActivity.this;

        //动态申请权限（Android 6.0）
        //初始化数据
        initData();
        //注册广播
        initBLEBroadcastReceiver();
        //初始化权限
        initPermissions();
        //扫描蓝牙设备
        searchBtDevice();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //注销广播接收
        unregisterReceiver(bleBroadcastReceiver);
    }

    /**
     * 初始化数据
     */
    private void initData() {
        //初始化ble管理器
        bleManager = new BLEManager();
        if (!bleManager.initBle(mContext)) {
            Log.d(TAG, "该设备不支持低功耗蓝牙");
            Toast.makeText(mContext, "该设备不支持低功耗蓝牙", Toast.LENGTH_SHORT).show();
        } else {
            if (!bleManager.isEnable()) {
                //去打开蓝牙
                bleManager.openBluetooth(mContext, false);
            }
        }
    }

    //    /**
//     * 初始化权限
//     */
//    private void initPermissions() {
//        //Android 6.0以上动态申请权限
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (EasyPermissions.hasPermissions(this, requestPermissionArray)) {
//                Log.d(TAG,"拥有全部权限");
//            } else {
//                //false 无权限
//                EasyPermissions.requestPermissions(this, "需要权限", REQUEST_PERMISSIONS, requestPermissionArray);
//            }
//        }
//    }
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

    /**
     * 注册广播
     */
    private void initBLEBroadcastReceiver() {
        //注册广播接收
        bleBroadcastReceiver = new BLEBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED); //开始扫描
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);//扫描结束
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);//手机蓝牙状态监听
        registerReceiver(bleBroadcastReceiver, intentFilter);
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case START_DISCOVERY:
                    Log.d(TAG, "开始搜索设备...");
                    break;

                case STOP_DISCOVERY:
                    Log.d(TAG, "停止搜索设备...");
                    break;

                case DISCOVERY_DEVICE:  //扫描到设备
                    BLEDevice bleDevice = (BLEDevice) msg.obj;
//                    lvDevicesAdapter.addDevice(bleDevice);
                    if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    if (Objects.equals(bleDevice.getBluetoothDevice().getName(), HELMET_NAME)&& Objects.equals(bleDevice.getBluetoothDevice().getAddress(), HELMET_ADDRESS)) {
                        if (bleManager != null) {
                            bleManager.stopDiscoveryDevice();
                        }
                        Message message = new Message();
                        message.what = SELECT_DEVICE;
                        message.obj = bleDevice.getBluetoothDevice();
                        mHandler.sendMessage(message);
                    }
                    break;

                case SELECT_DEVICE:
                    BluetoothDevice bluetoothDevice = (BluetoothDevice) msg.obj;
                    if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    Log.d(TAG, bluetoothDevice.getName());
                    Log.d(TAG, bluetoothDevice.getAddress());
                    curBluetoothDevice = bluetoothDevice;
                    if (!curConnState) {
                        if (bleManager != null) {
                            bleManager.connectBleDevice(mContext, curBluetoothDevice, 15000, SERVICE_UUID, READ_UUID, WRITE_UUID, onBleConnectListener);
                        }
                    } else {
                        ToastUtil.show(mContext, "头盔设备已连接");
                    }
                    break;

                case CONNECT_FAILURE: //连接失败
                    Log.d(TAG, "连接失败");
                    curConnState = false;
                    break;

                case CONNECT_SUCCESS:  //连接成功
                    Log.d(TAG, "连接成功");
                    curConnState = true;
                    String sendMsg = "wsj1234王世杰 左转";
                    if (bleManager != null) {
                        bleManager.sendMessage(sendMsg);  //以16进制字符串形式发送数据
                    }
                    break;

                case DISCONNECT_SUCCESS:
                    Log.d(TAG, "断开成功");
                    curConnState = false;

                    break;

                case SEND_FAILURE: //发送失败
                    byte[] sendBufFail = (byte[]) msg.obj;
                    String sendFail = TypeConversion.bytes2HexString(sendBufFail, sendBufFail.length);
                    Log.d(TAG, "发送数据失败，长度" + sendBufFail.length + "--> " + sendFail);
                    break;

                case SEND_SUCCESS:  //发送成功
                    byte[] sendBufSuc = (byte[]) msg.obj;
                    String sendResult = TypeConversion.bytes2HexString(sendBufSuc, sendBufSuc.length);
                    Log.d(TAG, "发送数据成功，长度" + sendBufSuc.length + "--> " + sendResult);
                    break;

                case RECEIVE_FAILURE: //接收失败
                    String receiveError = (String) msg.obj;
                    Log.d(TAG, receiveError);
                    break;

                case RECEIVE_SUCCESS:  //接收成功
                    byte[] recBufSuc = (byte[]) msg.obj;
                    String receiveResult = TypeConversion.bytes2HexString(recBufSuc, recBufSuc.length);
                    Log.d(TAG, "接收数据成功，长度" + recBufSuc.length + "--> " + receiveResult);
                    break;

                case BT_CLOSED:
                    Log.d(TAG, "系统蓝牙已关闭");
                    break;

                case BT_OPENED:
                    Log.d(TAG, "系统蓝牙已打开");
                    break;
            }

        }
    };
    //扫描结果回调
    private OnDeviceSearchListener onDeviceSearchListener = new OnDeviceSearchListener() {

        @Override
        public void onDeviceFound(BLEDevice bleDevice) {
            Message message = new Message();
            message.what = DISCOVERY_DEVICE;
            message.obj = bleDevice;
            mHandler.sendMessage(message);
        }

        @Override
        public void onDiscoveryOutTime() {
            Message message = new Message();
            message.what = DISCOVERY_OUT_TIME;
            mHandler.sendMessage(message);
        }
    };
    //连接回调
    private OnBleConnectListener onBleConnectListener = new OnBleConnectListener() {
        @Override
        public void onConnecting(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice) {

        }

        @Override
        public void onConnectSuccess(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, int status) {
            //因为服务发现成功之后，才能通讯，所以在成功发现服务的地方表示连接成功
        }

        @Override
        public void onConnectFailure(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, String exception, int status) {
            Message message = new Message();
            message.what = CONNECT_FAILURE;
            mHandler.sendMessage(message);
        }

        @Override
        public void onDisConnecting(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice) {

        }

        @Override
        public void onDisConnectSuccess(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, int status) {
            Message message = new Message();
            message.what = DISCONNECT_SUCCESS;
            message.obj = status;
            mHandler.sendMessage(message);
        }

        @Override
        public void onServiceDiscoverySucceed(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, int status) {
            //因为服务发现成功之后，才能通讯，所以在成功发现服务的地方表示连接成功
            Message message = new Message();
            message.what = CONNECT_SUCCESS;
            mHandler.sendMessage(message);
        }

        @Override
        public void onServiceDiscoveryFailed(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, String failMsg) {
            Message message = new Message();
            message.what = CONNECT_FAILURE;
            mHandler.sendMessage(message);
        }

        @Override
        public void onReceiveMessage(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, BluetoothGattCharacteristic characteristic, byte[] msg) {
            Message message = new Message();
            message.what = RECEIVE_SUCCESS;
            message.obj = msg;
            mHandler.sendMessage(message);
        }

        @Override
        public void onReceiveError(String errorMsg) {
            Message message = new Message();
            message.what = RECEIVE_FAILURE;
            mHandler.sendMessage(message);
        }

        @Override
        public void onWriteSuccess(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, byte[] msg) {
            Message message = new Message();
            message.what = SEND_SUCCESS;
            message.obj = msg;
            mHandler.sendMessage(message);
        }

        @Override
        public void onWriteFailure(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, byte[] msg, String errorMsg) {
            Message message = new Message();
            message.what = SEND_FAILURE;
            message.obj = msg;
            mHandler.sendMessage(message);
        }

        @Override
        public void onReadRssi(BluetoothGatt bluetoothGatt, int Rssi, int status) {

        }

        @Override
        public void onMTUSetSuccess(String successMTU, int newMtu) {

        }

        @Override
        public void onMTUSetFailure(String failMTU) {

        }
    };

    //////////////////////////////////  搜索设备  /////////////////////////////////////////////////
    private void searchBtDevice() {
        if (bleManager == null) {
            Log.d(TAG, "searchBtDevice()-->bleManager == null");
            return;
        }

        if (bleManager.isDiscovery()) { //当前正在搜索设备...
            bleManager.stopDiscoveryDevice();
        }

//        if (lvDevicesAdapter != null) {
//            lvDevicesAdapter.clear();  //清空列表
//        }

        //开始搜索
        bleManager.startDiscoveryDevice(onDeviceSearchListener, 15000);
    }

    /**
     * 蓝牙广播接收器
     */
    private class BLEBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.equals(action, BluetoothAdapter.ACTION_DISCOVERY_STARTED)) { //开启搜索
                Message message = new Message();
                message.what = START_DISCOVERY;
                mHandler.sendMessage(message);

            } else if (TextUtils.equals(action, BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {//完成搜素
                Message message = new Message();
                message.what = STOP_DISCOVERY;
                mHandler.sendMessage(message);

            } else if (TextUtils.equals(action, BluetoothAdapter.ACTION_STATE_CHANGED)) {   //系统蓝牙状态监听

                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                if (state == BluetoothAdapter.STATE_OFF) {
                    Message message = new Message();
                    message.what = BT_CLOSED;
                    mHandler.sendMessage(message);

                } else if (state == BluetoothAdapter.STATE_ON) {
                    Message message = new Message();
                    message.what = BT_OPENED;
                    mHandler.sendMessage(message);
                }
            }
        }
    }
}

