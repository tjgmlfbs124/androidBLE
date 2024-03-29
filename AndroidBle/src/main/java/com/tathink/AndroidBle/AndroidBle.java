package com.tathink.AndroidBle;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/* TODO
    1. BleDevice 객체를 하나로 통일
    2. HashTable이 아닌 TaDevice객체가 스스로 상태 관리
 */
public class AndroidBle {

    private boolean mScanning; // 스캐닝
    private Handler mHandler; // 핸들러
    private BluetoothAdapter mBluetoothAdapter; // 블루투스 어뎁터
    private BluetoothManager mBluetoothManager;

    //private Activity mActivity;
    private Context mContext;
    private FragmentManager fragManager;
    private BluetoothGatt mBluetoothGatt;

    private Map<String, BluetoothDevice> scannedDeviceMap; // 주소 디바이스
    private Map<String, BluetoothGatt> connectedDeviceMap;
    private Map<String, UUID> connectUUIDMap;
    private Map<String, BluetoothGattService> connectedGattService;
    private Map<String, BluetoothGattCharacteristic> connectedGattCharacteristic;
    private List<BleDevice> connectingDeviceList;

    private long mScanPeriod = 5000; // 탐색시간

    private ScanCallBack mScanCallBack = null;
    private ReadCallBack mReadCallBack = null;
    private DisconnectedCallBack mDisconnectedCallBack = null;

    // 블루투스 연결 요청코드
    private static final int REQUEST_ENABLE_BT = 1001;

    // 위치권한 요청코드
    private static final int REQUEST_CODE_PERMISSON_LOCATION = 2001;

    public AndroidBle(Context context, BluetoothManager bleManager) {
        mHandler = new Handler();
        //mActivity = activity;
        mContext = context;


        // 디바이스 BLE지원 검사
        /*if (!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(activity, "BLE를 지원하지 않습니다.", Toast.LENGTH_SHORT).show();
            activity.finish();
        }*/

        // 블루투스 어뎁터 생성
        //mBluetoothManager = (BluetoothManager)activity.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothManager = bleManager;
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if(mBluetoothAdapter == null) {
            //Toast.makeText(activity, "블루투스를 지원하지 않습니다.", Toast.LENGTH_SHORT).show();
            //activity.finish();
            Log.i("@ckw", "블루투스를 지원하지 않는 기기");
        }

        // 블루투스 켜져있는지 확인
        if(!mBluetoothAdapter.isEnabled()) {
            /* // 블루투스 꺼져있으면 온 요청
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mActivity.startActivity(enableBtIntent);
             */
            //Toast.makeText(activity, "블루투스가 꺼져있습니다.", Toast.LENGTH_SHORT).show();
            Log.i("@ckw", "블루투스가 꺼져있습니다");
        }

        // HashMap 초기화
        scannedDeviceMap = new HashMap<String, BluetoothDevice>();
        connectedDeviceMap = new HashMap<String, BluetoothGatt>();
        connectUUIDMap = new HashMap<String, UUID>();
        connectedGattCharacteristic = new HashMap<String, BluetoothGattCharacteristic>();
        connectingDeviceList = new ArrayList<>();

        // 위치정보 검사
        locationPermission();
    }

    public boolean scan(long scanPeriod, ScanCallBack callBack) {
        mScanning = true;
        mScanPeriod = scanPeriod;
        mScanCallBack = callBack;

        // 블루투스가 켜져있는지 확인
        if(!mBluetoothAdapter.isEnabled()) {
            /* // 블루투스 켜기 요청
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
             */
            //Toast.makeText(mActivity, "블루투스가 꺼져있습니다.", Toast.LENGTH_SHORT).show();
            Log.i("@ckw", "블루투스가 꺼져있습니다.");
            mScanning = false;
            return false;
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScanning = false;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                Log.i("@ckw", "탐색 완료");
            }
        }, mScanPeriod);

        mBluetoothAdapter.startLeScan(mLeScanCallback);
        return true;
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
        String address;
        String deviceName;

        @Override // 디바이스가 스캔 됐을 때
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            address = device.getAddress();
            deviceName = device.getName();

            if(!scannedDeviceMap.containsKey(address)) {
                scannedDeviceMap.put(address, device);
                mScanCallBack.onScan(new BleDevice(address, deviceName), rssi);
            }

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

    public boolean connect(BleDevice bleDevice, UUID uuid) {
        // @ckw 스켄중 연결 가능
        BluetoothDevice mDevice;

        if(bleDevice.address == null|| uuid == null || mBluetoothAdapter == null) {
            Log.i("@ckw", "연결할 데이터가 없습니다.");
            return false;
        }
        // 연결 하려는 디바이스가 스켄되지 않았음
        if( !scannedDeviceMap.containsKey(bleDevice.address) ) {
            Log.i("@ckw", "잘못된 연결 주소"+ bleDevice.address);
            return false;
        }

        if( connectedDeviceMap.containsKey(bleDevice.address) ) {

            Log.i("@ckw", "이미 연결된 주소");
            return false;
        }
        // TODO 이미 연결 시도중인 디바이스

        mDevice = scannedDeviceMap.get(bleDevice.address);
        int connectionState = mBluetoothManager.getConnectionState(mDevice, BluetoothProfile.GATT);
        if(connectionState == BluetoothProfile.STATE_CONNECTED) {
            Log.i("@ckw", "이미 연결된 주소");
            return false;
        }

        //connectingDeviceList
        for(BleDevice tempDevice : connectingDeviceList) {
            if(tempDevice.address == bleDevice.address) {
                Log.i("@ckw", "이미 연결 시도중인 주소");
                return false;
            }
        }

        if(connectionState == BluetoothProfile.STATE_DISCONNECTED) {
            mBluetoothGatt = mDevice.connectGatt(mContext, false, mGattCallback);
            Log.i("@ckw", "연결 시도");
            connectingDeviceList.add(new BleDevice(bleDevice.address, bleDevice.name));

            if(connectUUIDMap.containsKey(bleDevice.address)) {
                connectUUIDMap.remove(bleDevice.address);
            }

            connectUUIDMap.put(bleDevice.address, uuid);
            return true;
        } else {
            Log.i("@ckw", "잘못된 연결 시도");
            return false;
        }
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        // 연결 상태가 변경되면 호출됨
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            BluetoothDevice device = gatt.getDevice();
            String address = device.getAddress();
            gatt.discoverServices();

            if(newState == BluetoothProfile.STATE_CONNECTED) {
                // 연결 연결됨
                if(!connectedDeviceMap.containsKey(address)) {
                    connectedDeviceMap.put(address, gatt);
                }
                Log.i("@ckw", "연결 완료.");

                for (BleDevice tempDevice: connectingDeviceList) {
                    if(tempDevice.address == address) {
                        connectingDeviceList.remove(tempDevice);
                        break;
                    }
                }
            } else if(newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("@ckw", "연결 끊김");

                mDisconnectedCallBack.onDisconnected(new BleDevice(address, gatt.getDevice().getName() ));

                if(connectedDeviceMap.containsKey(address)) {
                    BluetoothGatt _bluetoothGatt = connectedDeviceMap.get(address);
                    if(_bluetoothGatt != null) {
                        _bluetoothGatt.close();
                        _bluetoothGatt = null;
                    }
                    connectedDeviceMap.remove(address);
                }

                if(connectUUIDMap.containsKey(address)) {
                    connectUUIDMap.remove(address);
                }

                if(connectedGattCharacteristic.containsKey(address)) {
                    connectedGattCharacteristic.remove(address);
                }

                for (BleDevice tempDevice: connectingDeviceList) {
                    if(tempDevice.address == address) {
                        connectingDeviceList.remove(tempDevice);
                        break;
                    }
                }

            }
        }

        // BLE 서비스 탐색되면 호출
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            BluetoothDevice tempDevice = gatt.getDevice();
            if(status == BluetoothGatt.GATT_SUCCESS) {
                if(connectUUIDMap.containsKey(tempDevice.getAddress() )) {
                    UUID tempUUID = connectUUIDMap.get(tempDevice.getAddress());
                    BluetoothGattService tempGattService = gatt.getService(tempUUID);

                    // Notification 기능 켜기
                    Log.i("@ckw", "Notification 기능"); // 송수신
                    gatt.setCharacteristicNotification(tempGattService.getCharacteristic(tempUUID), true);
                    if(!connectedGattCharacteristic.containsKey(tempDevice.getAddress())) {
                        connectedGattCharacteristic.put(tempDevice.getAddress()
                                ,tempGattService.getCharacteristic(tempUUID));
                    }
                }
            } else {
                Log.i("@ckw", "onServicesDiscovered received: "+status);
            }
        }

        // 특성읽기 (데이터 송신?)
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if(status == BluetoothGatt.GATT_SUCCESS) {

            }
        }

        // 특성 변경됨 (데이터 수신 사용)
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            BluetoothDevice tempBluetoothDevice = gatt.getDevice();
            final byte[] data = characteristic.getValue();
            if(data !=null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data) {
                    stringBuilder.append(String.format("%02X", byteChar));
                }
            }

            BleDevice tempDevice =
                    new BleDevice(tempBluetoothDevice.getAddress(), tempBluetoothDevice.getName());
            mReadCallBack.onData(tempDevice, new String(data) );
        }
    };

    public void readData(ReadCallBack callback) {
        mReadCallBack = callback;
        // 디바이스마다 개별 콜백?
    }

    public void writeData(BleDevice device, String data) {
        if(mBluetoothAdapter == null) {
            Log.i("@ckw", "블루투스 가 설정되지 않음");
            return;
        }

        String address = device.address;
        BluetoothGatt tempBluetoothGatt = connectedDeviceMap.get(address);

        if(!connectedDeviceMap.containsKey(address)){
            Log.i("@ckw", "연결되지 않은 장비에 송신 시도");
            return;
        }

        if(tempBluetoothGatt != null) {
            BluetoothGattCharacteristic tempCharacteristic = connectedGattCharacteristic.get(address);
            if(tempCharacteristic != null) {
                if (tempCharacteristic.PROPERTY_WRITE_NO_RESPONSE > 0 ) {
                    byte[] buffer = data.getBytes();
                    tempCharacteristic.setValue(buffer);
                    tempBluetoothGatt.writeCharacteristic(tempCharacteristic);
                } else {
                    Log.i("@ckw", "write 기능이 없음.");
                }
            } else {
                Log.i("@ckw", "Gatt 특성이 없음.");
            }
        } else {
            Log.i("@ckw", "Gatt 가 설정되지 않음.");
        }
    }

    public void disConnected(DisconnectedCallBack callBack) {
        mDisconnectedCallBack = callBack;
    }

    public void disConnect(BleDevice device) {
        if(connectedDeviceMap.containsKey(device.address)) {
            BluetoothGatt tempGatt = connectedDeviceMap.get(device.address);
            if(tempGatt != null) {
                tempGatt.disconnect();
                tempGatt = null;
            }
            connectedDeviceMap.remove(device.address);
        } else {
            Log.i("@ckw", "연결되지 않은 장비");
            return;
        }

        if (connectUUIDMap.containsKey(device.address)) {
            connectUUIDMap.remove(device.address);
        }
        if(connectedGattCharacteristic.containsKey(device.address)){
            connectedGattCharacteristic.remove(device.address);
        }
        Log.i("@ckw", "연결 끊기");

    }

    public interface ScanCallBack {
        void onScan(final BleDevice device, int rssi);
    }

    public interface ReadCallBack {
        void onData(final BleDevice device, String data);
    }

    public interface DisconnectedCallBack {
        void onDisconnected(final BleDevice device);
    }

    private void locationPermission() {
        // 위치 권한 검사
        final int permissionCheck = ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION);
        if(permissionCheck == PackageManager.PERMISSION_DENIED) {
            /* // 위치 권한 요청
            ActivityCompat.requestPermissions(mActivity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_PERMISSON_LOCATION);
             */
            //Toast.makeText(mActivity, "위치 권한이 없습니다.",Toast.LENGTH_SHORT).show();
            Log.i("@ckw", "위치 권한이 없습니다.");
        }

        // 위치 켜져있는지 검사
        /* 액티비티 미사용으로 검사 불가능
        LocationManager locationManager = (LocationManager)mActivity.getSystemService(Context.LOCATION_SERVICE);
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            //위치 켜기 요청
            //Intent locationOption = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            //mActivity.startActivity(locationOption);

            //Toast.makeText(mActivity, "위치 정보가 꺼져있습니다.", Toast.LENGTH_SHORT);
        }*/
    }

}

