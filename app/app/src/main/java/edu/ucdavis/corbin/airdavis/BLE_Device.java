package edu.ucdavis.corbin.airdavis;

import android.bluetooth.BluetoothDevice;

// Container class for BluetoothDevice
// stores signal strength along with it (rssi)

public class BLE_Device {
    private BluetoothDevice bluetoothDevice;
    private int rssi;

    public BLE_Device(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    public String getAddress() {
        return bluetoothDevice.getAddress();
    }

    public String getName() {
        return bluetoothDevice.getName();
    }

    public void setRSSI(int rssi) {
        this.rssi = rssi;
    }

    public int getRSSI() {
        return rssi;
    }
}
