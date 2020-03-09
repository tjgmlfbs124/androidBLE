package com.tathink.AndroidBle;

public class BleDevice {
    public String address = "";
    public String name = "";
    public BleDevice(String address, String deviceName) {
        this.address = address;
        this.name = deviceName;
    }
}
