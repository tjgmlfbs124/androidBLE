package com.tathink.table;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class taBle {

    private boolean mScanning; // 스캐닝
    private Handler mHandler; // 핸들러
    private BluetoothAdapter mBluetoothAdapter; // 블루투스 어뎁터

    private Activity mActivity;
    private FragmentManager fragManager;
    private BluetoothGatt mBluetoothGatt;

    private Map<String, BluetoothDevice> scannedDeviceMap;
    private Map<String, BluetoothGatt> connectedDeviceMap;

    private static long mScanPeriod = 5000; // 탐색시간

    private scanCallBack mScanCallBack = null;

    private static final int REQUEST_ENABLE_BT = 1001;

    private static final int REQUEST_CODE_PERMISSON_LOCATION = 2001;

    public taBle(Activity activity) {
        mHandler = new Handler();
        mActivity = activity;

        // 디바이스 BLE지원 검사
        if (!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(activity, "BLE를 지원하지 않습니다.", Toast.LENGTH_SHORT).show();
            activity.finish();
        }

        // 블루투스 어뎁터 생성
        final BluetoothManager bluetoothManager = (BluetoothManager)activity.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if(mBluetoothAdapter == null) {
            Toast.makeText(activity, "블루투스를 지원하지 않습니다.", Toast.LENGTH_SHORT).show();
            activity.finish();
        }

        // 블루투스 켜져있는지 확인
        if(!mBluetoothAdapter.isEnabled()) {
            /* // 블루투스 꺼져있으면 온 요청
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mActivity.startActivity(enableBtIntent);
             */
        }   Toast.makeText(activity, "블루투스가 꺼져있습니다.", Toast.LENGTH_SHORT).show();

        // 위치정보 검사
        locationPermission();


        //@TODO 바인드 서비스?
    }

    public boolean scan(long scanPeriod, scanCallBack callBack) {
        mScanning = true;
        mScanPeriod = scanPeriod;
        mScanCallBack = callBack;
        scannedDeviceMap = new HashMap<String, BluetoothDevice>();

        // 블루투스가 켜져있는지 확인
        if(!mBluetoothAdapter.isEnabled()) {
            /* // 블루투스 켜기 요청
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
             */
            Toast.makeText(mActivity, "블루투스가 꺼져있습니다.", Toast.LENGTH_SHORT).show();
            mScanning = false;
            return false;
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScanning = false;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }, mScanPeriod);

        mBluetoothAdapter.startLeScan(mLeScanCallback);
        return true;
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
        String address;
        String deviceName;
                @Override
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    address = device.getAddress();
                    deviceName = device.getName();
                    scannedDeviceMap.put(address, device);

                    scanCallBackCaller(address, deviceName, rssi);
                }
            };

    public boolean isScanning() {
        return mScanning;
    }
    public void stopScan() {
        if(mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        mScanning = false;
    }

    private void scanCallBackCaller(String address, String deviceName, int rssi) {
        mScanCallBack.onScan(new taDevice(address, deviceName), rssi);
    }

    public boolean connect(String address, UUID uuid) {
        //TODO 연결 시 스켄 정지 해야하는가?
        BluetoothDevice mDevice;

        if(address == null|| uuid == null || mBluetoothAdapter == null) {
            Log.i("@ckw", "잘못된 연결 시도.");
            return false;
        }

        if(!scannedDeviceMap.containsKey(address) ) {
            Log.i("@ckw", "잘못된 연결 주소");
            return false;
        }
        mDevice = scannedDeviceMap.get(address);

        mBluetoothGatt = mDevice.connectGatt(mActivity, false, mGattCallback);

        return true;
    }
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);



            //if(newState ==)
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
        }
    };

    public void readData() {

    }

    public void writeData() {

    }

    public void disConnect() {

    }

    public class taDevice {
        public String mAddress = "";
        public String mDeviceName = "";
        public taDevice(String address, String deviceName) {
            mAddress = address;
            mDeviceName = deviceName;
        }
    }

    public interface scanCallBack {
        void onScan(final taDevice device, int rssi);
    }

    private void locationPermission() {
        // 위치 권한 검사
        final int permissionCheck = ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION);
        if(permissionCheck == PackageManager.PERMISSION_DENIED) {
            /* // 위치 권한 요청
            ActivityCompat.requestPermissions(mActivity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_PERMISSON_LOCATION);
             */
            Toast.makeText(mActivity, "위치 권한이 없습니다.",Toast.LENGTH_SHORT).show();
        }

        // 위치 켜져있는지 검사
        LocationManager locationManager = (LocationManager)mActivity.getSystemService(Context.LOCATION_SERVICE);
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            /* //위치 켜기 요청
            Intent locationOption = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            mActivity.startActivity(locationOption);
             */
            Toast.makeText(mActivity, "위치 정보가 꺼져있습니다.", Toast.LENGTH_SHORT);
        }
    }

}