# Android BLE 통신 라이브러리

- 작업 최소 API : 18 
- 컴파일,타겟 버전 : 29

## BLE 모듈 사용
- 최상의 경로의 table.aar 파일을 임포트 하여 사용

- table/ 폴더를 임포트 하여 사용

## 장치 사용 권한
 - 블루투스
 - 위치권한

## Usage
``` java
// Manifest
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>

// Activity
TaBle myBle = new TaBle(activity);

// scanPeriod: 스캔할 시간
// ScanCallBack(): 장치가 스캔 될 때 콜백됨.
myBle.scan(scanPeriod, TaBle.ScanCallBack() {
    @Override
    public void onScan(TaDevice device, int rssi) {
        ...
    }
})

// 디바이스와 UUID로 서비스, 특성 연결
myBle.connect(TaDevice, UUID); 

// 디바이스에 String 타입 데이터 전송 (최대 20Byte)
myBle.writeData(TaDevice, String); 

// 디바이스연결 해제
myBle.disConnect(TaDevice);

// 디바이스로 부터 온 데이터를 읽음
myBle.readData(new TaBle.ReadCallBack() {
    @Override
    public void onData(TaDevice, String) {
        ...
    }
});

// 디바이스와 연결이 끊겼을 때 호출됨
myBle.disConnected(new Table.DisconnectedCallBack() {
    @Override
    public void onDisconnected(TaDevice device) {
        ...
    }
});

```