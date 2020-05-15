# Android BLE 통신 라이브러리

- 작업 최소 API : 18 
- 컴파일,타겟 버전 : 29

## 라이브러리를 종속성으로 추가
1. 다음 두 가지 방법 중 하나로 라이브러리를 프로젝트에 추가합니다. 
    - AndroidBle.aar 파일을 추가합니다.  
        1. FIle > New > New Module 을 선택합니다.  
        2. Import .JAR/.AAR Package를 클릭한 후 Next를 클릭합니다.  
        3. 'AndroidBle-debug.aar' 파일의 위치를 입력한 후 Finish를 클릭합니다.
    - 라이브러리 모듈을 프로젝트로 가져옵니다  
        1. File > New > Import Module을 클릭합니다.  
        2. 라이브러리 모듈 디렉토리의 위치를 입력한 후 Finish를 클릭

2. settings.gradle 파일의 상단에 라이브러리가 표시되는지 확인합니다.
```
include ':app', ':AndroidBle'
```
3. 앱 모듈의 build.gradle 파일을 열고 dependencies 블록에 새 줄을 추가합니다.
```
dependencies {
    implementation project(":AndroidBle")
}
```
4. 'File > Sync Project with Gradle Files' 을 클릭합니다.

## 장치 사용 권한
: 위치와 블루투스가 켜져있어야 합니다.
 - 블루투스
 - 위치권한

## Usage

- Manifest.xml
``` xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
```

- Activity.java
``` java
import com.tathink.AndroidBle.AndroidBle;
import com.tathink.AndroidBle.BleDevice;

BluetoothManager myBluetoothManager = (BluetoothManager)activity.getSystemservice(Context.BLUETOOTH_SERVICE); // 블루투스매니저
AndroidBle myBle = new AndroidBle(Context, myBluetoothManager);

// scanPeriod: 스캔할 시간
// ScanCallBack(): 장치가 스캔 될 때 콜백됨.
myBle.scan(scanPeriod, new AndroidBle.ScanCallBack() {
    @Override
    public void onScan(BleDevice device, int rssi) {
        ...
    }
})

// 디바이스와 UUID로 서비스, 특성 연결
myBle.connect(BleDevice, UUID); 

// 디바이스에 String 타입 데이터 전송 (최대 20Byte)
myBle.writeData(BleDevice, String); 

// 디바이스연결 해제
myBle.disConnect(BleDevice);

// 디바이스로 부터 온 데이터를 읽음
myBle.readData(new AndroidBle.ReadCallBack() {
    @Override
    public void onData(BleDevice, String) {
        ...
    }
});

// 디바이스와 연결이 끊겼을 때 호출됨
myBle.disConnected(new AndroidBle.DisconnectedCallBack() {
    @Override
    public void onDisconnected(BleDevice device) {
        ...
    }
});

```