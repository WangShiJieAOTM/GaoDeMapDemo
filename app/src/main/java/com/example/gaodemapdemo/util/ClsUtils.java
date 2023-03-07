package com.example.gaodemapdemo.util;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ClsUtils {

    public static BluetoothDevice remoteDevice = null;

    /**
     * 与设备配对 参考源码：platform/packages/apps/Settings.git
     * /Settings/src/com/android/settings/bluetooth/CachedBluetoothDevice.java
     */
    @SuppressWarnings("unchecked")
    static public boolean createBond(@SuppressWarnings("rawtypes") Class btClass, BluetoothDevice btDevice)
            throws Exception {
        Method createBondMethod = btClass.getMethod("createBond");
        Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);
        return returnValue.booleanValue();
    }

    //自动配对设置Pin值
    static public boolean autoBond(Class btClass, BluetoothDevice device, String strPin) throws Exception {
        Method autoBondMethod = btClass.getMethod("setPin",new Class[]{byte[].class});
        Boolean result = (Boolean)autoBondMethod.invoke(device,new Object[]{strPin.getBytes()});
        return result;
    }

    static public  void setPairingConfirmation(BluetoothDevice device){
        try {
            @SuppressLint("SoonBlockedPrivateApi") Field field = device.getClass().getDeclaredField("sService");
            field.setAccessible(true);

            Object service = field.get(device);
            Method method = service.getClass().getDeclaredMethod("setPairingConfirmation",
                    BluetoothDevice.class, boolean.class);
            method.setAccessible(true);
            method.invoke(service, device, true);

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    /**
     * 与设备解除配对 参考源码：platform/packages/apps/Settings.git
     * /Settings/src/com/android/settings/bluetooth/CachedBluetoothDevice.java
     */
    @SuppressWarnings("unchecked")
    static public boolean removeBond(Class btClass, BluetoothDevice btDevice)
            throws Exception {
        Method removeBondMethod = btClass.getMethod("removeBond");
        Boolean returnValue = (Boolean) removeBondMethod.invoke(btDevice);
        return returnValue.booleanValue();
    }

    @SuppressWarnings("unchecked")
    static public boolean setPin(Class btClass, BluetoothDevice btDevice,
                                 String str) throws Exception {
        try {
            Method removeBondMethod = btClass.getDeclaredMethod("setPin",
                    new Class[]
                            {byte[].class});
            Boolean returnValue = (Boolean) removeBondMethod.invoke(btDevice,
                    new Object[]
                            {str.getBytes()});
        } catch (SecurityException e) {
            // throw new RuntimeException(e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // throw new RuntimeException(e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return true;

    }

    // 取消用户输入
    //cancelPairingUserInput（）取消用户输入密钥框，
    // 个人觉得一般情况下不要和
    // setPin（setPasskey、setPairingConfirmation、 setRemoteOutOfBandData）一起用，
    // 这几个方法都会remove掉map里面的key:value（也就是互斥的 ）
    @SuppressWarnings("unchecked")
    static public boolean cancelPairingUserInput(Class btClass,
                                                 BluetoothDevice device)

            throws Exception {
        Method createBondMethod = btClass.getMethod("cancelPairingUserInput");
        // cancelBondProcess()
        Boolean returnValue = (Boolean) createBondMethod.invoke(device);

        return returnValue.booleanValue();
    }

    // 取消配对
    @SuppressWarnings("unchecked")
    static public boolean cancelBondProcess(Class btClass,
                                            BluetoothDevice device)
            throws Exception {
        Method createBondMethod = btClass.getMethod("cancelBondProcess");
        Boolean returnValue = (Boolean) createBondMethod.invoke(device);
        return returnValue.booleanValue();
    }

    /**
     * @param clsShow
     */
    @SuppressWarnings("unchecked")
    static public void printAllInform(Class clsShow) {
        try {
            // 取得所有方法
            Method[] hideMethod = clsShow.getMethods();
            int i = 0;
            for (; i < hideMethod.length; i++) {
                //Log.e("method name", hideMethod.getName() + ";and the i is:"
                //      + i);
            }
            // 取得所有常量
            Field[] allFields = clsShow.getFields();
            for (i = 0; i < allFields.length; i++) {
                //Log.e("Field name", allFields.getName());
            }
        } catch (SecurityException e) {
            // throw new RuntimeException(e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // throw new RuntimeException(e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Clears the internal cache and forces a refresh of the services from the	 * remote device.
     */
    public static boolean refreshDeviceCache(BluetoothGatt mBluetoothGatt) {
        if (mBluetoothGatt != null) {
            try {
                BluetoothGatt localBluetoothGatt = mBluetoothGatt;
                Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
                if (localMethod != null) {
                    boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
                    return  bool;
                }
            } catch (Exception localException) {
                Log.i("Config", "An exception occured while refreshing device");
            }
        }
        return false;
    }
}
