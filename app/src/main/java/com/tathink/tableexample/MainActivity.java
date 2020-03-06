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
    TaDevice toConnnectDevice;
    String uuidStr = "0000FFE1-0000-1000-8000-00805F9B34FB";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button connectBtn = (Button)this.findViewById(R.id.connectBtn);
        Button writeBtn = (Button)this.findViewById(R.id.writeBtn);


        final TaBle myBle = new TaBle(this);

        myBle.scan(2000, new TaBle.ScanCallBack() {
            // 디바이스가 스캔 될 때 마다 호출됨.
            @Override
            public void onScan(TaDevice device, int rssi) {
                //"50:8C:B1:66:0C:B3"
                String mName = device.name;
                String mAddress = device.address;
                Log.i("@ckw", "scanned device: "+mName+"//addr: "+mAddress);
                if(mAddress.equals("50:8C:B1:66:0C:B3") ) {
                    Log.i("@ckw", "나의주소 저장");
                    toConnnectDevice = new TaDevice(mAddress, mName);
                }
            }
        });

        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBle.connect(toConnnectDevice.address, UUID.fromString(uuidStr));
            }
        });

        writeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBle.writeData(toConnnectDevice, "hello robot!");
            }
        });

        // 데이터를 받을 때 마다 리턴됨
        myBle.readData(new TaBle.ReadCallBack() {
            @Override
            public void onData(TaDevice device, String data) {
                Log.i("@ckw", "readData: "+data);
            }
        });
    }
}
