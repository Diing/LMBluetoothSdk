/*
 * The MIT License (MIT)

 * Copyright (c) 2015 LinkMob.cc

 * Author: lujun

 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package co.lujun.lmbluetoothsdk.service;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import co.lujun.lmbluetoothsdk.base.BaseListener;
import co.lujun.lmbluetoothsdk.base.BluetoothLEListener;
import co.lujun.lmbluetoothsdk.base.State;

import static android.content.ContentValues.TAG;

/**
 * Author: lujun(http://blog.lujun.co)
 * Date: 2016-1-21 15:36
 */
@TargetApi(21)
public class BluetoothLEService {

    private BaseListener mBluetoothListener;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mInfoWriteCharacteristic, mInfoNotifyCharacteristic;
    private BluetoothGattCharacteristic mSyncWriteCharacteristic, mSyncNotifyCharacteristic;
    private String serviceUUID;
    private String configUUID;
    private String infoWriteUUID;
    private String infoReadUUID;
    private String syncWriteUUID;
    private String syncReadUUID;

    private int mState;

    private Context mContext;

    public BluetoothLEService(Context context) {
        mState = State.STATE_NONE;
        mContext = context;
    }

    /**
     * Set bluetoothLE listener.
     * @param listener BluetoothLEListener
     */
    public synchronized void setBluetoothLEListener(BaseListener listener) {
        this.mBluetoothListener = listener;
    }

    /**
     * Set the current state of the connection.
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        mState = state;
        if (mBluetoothListener != null){
            mBluetoothListener.onBluetoothServiceStateChanged(state);
        }
    }

    /**
     * Get the current state of connection.
     * Possible return values are STATE_NONE, STATE_LISTEN, STATE_CONNECTING, STATE_CONNECTED,
     * STATE_DISCONNECTED, STATE_UNKNOWN in {@link co.lujun.lmbluetoothsdk.base.State} class.
     * @return the connection state
     */
    public int getState() {
        return mState;
    }

    /**
     * Connect to a GATT server.
     * @param context the context
     * @param device the device
     */
    public void connect(Context context, BluetoothDevice device){
        setState(State.STATE_CONNECTING);
        mBluetoothGatt = device.connectGatt(context, true, mBTGattCallback);
    }

    /**
     * Reconnect to a GATT server.
     */
    public void reConnect(){
        if (mBluetoothGatt != null){
            mBluetoothGatt.connect();
        }
    }

    /**
     * Disconnect the connection.
     */
    public void disConnect(){
        if (mBluetoothGatt != null){
            mBluetoothGatt.disconnect();
        }
    }

    /**
     * Close GATT client.
     */
    public void close(){
        disConnect();
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
        }
        mBluetoothGatt = null;
    }

    public void bond() {
        if (mBluetoothGatt.getDevice().getBondState() == BluetoothDevice.BOND_NONE) {
            final BluetoothDevice device = mBluetoothGatt.getDevice();
            device.createBond();
            Log.e(TAG, "BOND  Required");
        }

    }

    public void unBond() {
        try {
            final BluetoothDevice device = mBluetoothGatt.getDevice();
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unBond(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Write data to remote device via Info characteristic..
     * @param data data to send to the device
     */
    public void write(byte[] data){
        if (mBluetoothGatt != null){
            mInfoWriteCharacteristic.setValue(data);
            boolean status = mBluetoothGatt.writeCharacteristic(mInfoWriteCharacteristic);
            Log.e("BluetoothGatt", "Write via UUID:" + mInfoWriteCharacteristic.getUuid().toString());
            Log.e("BluetoothGatt", status ? "SUCCESS" : "FAILURE");
        }
    }

    /**
     * Write data to remote device via Sync characteristic.
     * @param data data to send to the device
     */
    public void write(byte[] data, String uuid) {
        if (mBluetoothGatt != null) {
            BluetoothGattCharacteristic characteristic = mBluetoothGatt.getService(UUID.fromString(serviceUUID)).getCharacteristic(UUID.fromString(uuid));
            if (characteristic != null) {
                characteristic.setValue(data);
                boolean status = mBluetoothGatt.writeCharacteristic(characteristic);
                Log.e("BluetoothGatt", "Write via UUID:" + characteristic.getUuid().toString());
                Log.e("BluetoothGatt", status ? "SUCCESS" : "FAILURE");
            } else {
                Log.e("BluetoothGatt", "Characteristic not exist");
            }
        }
    }

    public void setServiceUUID(String uuid) {
        serviceUUID = uuid;
    }

    public void setWriteCharacteristic(String characteristicUUID) {
        infoWriteUUID = characteristicUUID;
    }

    public void setReadCharacteristic(String characteristicUUID) {
        infoReadUUID = characteristicUUID;
    }

    public void setSyncWriteCharacteristic(String UUID) {
        syncWriteUUID = UUID;
    }

    public void setSyncReadCharacteristic(String UUID) {
        syncReadUUID = UUID;
    }

    public void setConfigUUID(String uuid) {
        configUUID = uuid;
    }

    private BluetoothGattCallback mBTGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            switch (newState){
                case BluetoothProfile.STATE_CONNECTED:
                    setState(State.STATE_CONNECTED);
                    gatt.discoverServices();
                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    setState(State.STATE_DISCONNECTED);
                    break;

                default:break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> services = gatt.getServices();
                if(mBluetoothListener != null){
                    ((BluetoothLEListener)mBluetoothListener).onDiscoveringServices(services);
                }
                for (BluetoothGattService service : services) {
                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                    for (BluetoothGattCharacteristic characteristic : characteristics) {
                        final int charaProp = characteristic.getProperties();
                        final String charaUUID = characteristic.getUuid().toString();
                        Log.d("Assigning UUID", "charaUUID : " + charaUUID);
                        if ((charaProp | BluetoothGattCharacteristic.PERMISSION_READ) > 0){
                            if(infoReadUUID.isEmpty()){
                                if (mInfoNotifyCharacteristic != null){
                                    mBluetoothGatt.setCharacteristicNotification(mInfoNotifyCharacteristic, false);
                                    mInfoNotifyCharacteristic = null;
                                }
                                gatt.readCharacteristic(characteristic);
                            }
                            Log.d("Assigning UUID", "Assigning read characteristic : " + characteristic.getUuid());
                        }

                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            if(infoReadUUID.isEmpty()){
                                mInfoNotifyCharacteristic = characteristic;
                                mBluetoothGatt.setCharacteristicNotification(characteristic, true);
                            }else if(charaUUID.equalsIgnoreCase(infoReadUUID)){
                                mInfoNotifyCharacteristic = characteristic;
                                if( mBluetoothGatt.setCharacteristicNotification(characteristic, true) ) {
                                    Log.d("Assigning UUID", "Subscribing to characteristic : " + characteristic.getUuid());
                                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(configUUID));
                                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                    mBluetoothGatt.writeDescriptor(descriptor);
                                }
                            }
                            //Enable Sync characteristic
                            if (charaUUID.equalsIgnoreCase(syncReadUUID)) {
                                mSyncNotifyCharacteristic = characteristic;
                                if (mBluetoothGatt.setCharacteristicNotification(characteristic, true)) {
                                    Log.d("Assigning UUID", "Subscribing to characteristic : " + characteristic.getUuid());
                                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(configUUID));
                                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                    mBluetoothGatt.writeDescriptor(descriptor);
                                }
                            }
                        }

                        if(infoWriteUUID.isEmpty()){
                            if (((charaProp & BluetoothGattCharacteristic.PERMISSION_WRITE)
                                    | (charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0){
                                mInfoWriteCharacteristic = characteristic;
                            }
                        } else{
                            if (((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE)
                                    | (charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0
                                    & charaUUID.equalsIgnoreCase(infoWriteUUID)) {
                                Log.d("Assigning UUID", "Assigning write characteristic : " + characteristic.getUuid());
                                mInfoWriteCharacteristic = characteristic;
                            }
                        }

                        if (((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE)
                                | (charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0
                                & charaUUID.equalsIgnoreCase(syncWriteUUID)) {
                            Log.d("Assigning UUID", "Assigning write characteristic : " + characteristic.getUuid());
                            mSyncWriteCharacteristic = characteristic;
                        }
                    }

                    if(mBluetoothListener != null){
                        ((BluetoothLEListener)mBluetoothListener).onDiscoveringCharacteristics(characteristics);
                    }
                }
                setState(State.STATE_GOT_CHARACTERISTICS);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (mBluetoothListener != null){
                ((BluetoothLEListener)mBluetoothListener).onReadData(characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (mBluetoothListener != null){
                ((BluetoothLEListener)mBluetoothListener).onWriteData(characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if (mBluetoothListener != null){
                ((BluetoothLEListener)mBluetoothListener).onDataChanged(characteristic);
            }
        }
    };
}
