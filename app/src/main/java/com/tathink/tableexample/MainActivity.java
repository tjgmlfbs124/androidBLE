package com.tathink.tableexample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.tathink.table.TaBle;
import com.tathink.table.TaDevice;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    TaDevice toConnectDevice;
    TaDevice toConnectDevice2;
    final static String uuidStr = "0000FFE1-0000-1000-8000-00805F9B34FB";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button connectBtn = (Button)this.findViewById(R.id.connectBtn);
        Button writeBtn = (Button)this.findViewById(R.id.writeBtn);
        Button disconnectBtn = (Button)this.findViewById(R.id.disconnectBtn);

        Button connectBtn2 = (Button)this.findViewById(R.id.connectBtn2);
        Button writeBtn2 = (Button)this.findViewById(R.id.writeBtn2);
        Button disconnectBtn2 = (Button)this.findViewById(R.id.disconnectBtn2);


        final TaBle myBle = new TaBle(this);

        myBle.scan(3000, new TaBle.ScanCallBack() {
            // 디바이스가 스캔 될 때 마다 호출됨.
            @Override
            public void onScan(TaDevice device, int rssi) {
                //"50:8C:B1:66:0C:B3"
                String mName = device.name;
                String mAddress = device.address;
                Log.i("@ckw", "scanned device: "+mName+"//addr: "+mAddress);
                if(mAddress.equals("50:8C:B1:66:0C:B3") ) {
                    Log.i("@ckw", "keep1 :"+mName);
                    toConnectDevice = new TaDevice(mAddress, mName);
                }
                if(mAddress.equals("50:8C:B1:6A:7F:D7")) {
                    Log.i("@ckw", "keep2 :"+mName);
                    toConnectDevice2 = new TaDevice(mAddress, mName);
                }
            }
        });

        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBle.connect(toConnectDevice, UUID.fromString(uuidStr));
            }
        });
        writeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBle.writeData(toConnectDevice, "hello robot1");
            }
        });
        disconnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBle.disConnect(toConnectDevice);
            }
        });

        connectBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBle.connect(toConnectDevice2, UUID.fromString(uuidStr));
            }
        });
        writeBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBle.writeData(toConnectDevice2, "hello robot2");
            }
        });
        disconnectBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBle.disConnect(toConnectDevice2);
            }
        });

        // 데이터를 받을 때 마다 리턴됨
        myBle.readData(new TaBle.ReadCallBack() {
            @Override
            public void onData(TaDevice device, String data) {
                Log.i("@ckw", "readData: "+data);
            }
        });

        myBle.disConnected(new TaBle.DisconnectedCallBack() {
            @Override
            public void onDisconnected(TaDevice device) {
                Log.i("@ckw", "disconnected:"+device.name);
            }
        });
    }
}
