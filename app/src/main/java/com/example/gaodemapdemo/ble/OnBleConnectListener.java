package com.example.gaodemapdemo.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * 4.0蓝牙连接监听
 */
public interface OnBleConnectListener {

    void onConnecting(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice);   //正在连接
    void onConnectSuccess(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, int status);  //连接成功
    void onConnectFailure(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, String exception, int status);  //连接失败
    void onDisConnecting(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice); //正在断开
    void onDisConnectSuccess(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, int status); // 断开连接

    void onServiceDiscoverySucceed(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, int status);  //发现服务成功
    void onServiceDiscoveryFailed(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, String message);  //发现服务失败
    void onReceiveMessage(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, BluetoothGattCharacteristic characteristic, byte[] msg);      //收到消息
    void onReceiveError(String errorMsg);  //接收数据出错
    void onWriteSuccess(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, byte[] msg);        //写入成功
    void onWriteFailure(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, byte[] msg, String errorMsg);        //写入失败

    void onReadRssi(BluetoothGatt bluetoothGatt, int Rssi, int status);             //成功读取到连接信号强度

    void onMTUSetSuccess(String successMTU, int newMtu);  //MTU设置成功
    void onMTUSetFailure(String failMTU);    //MTU设置失败
}
