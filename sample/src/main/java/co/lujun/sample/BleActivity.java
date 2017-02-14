package co.lujun.sample;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import co.lujun.lmbluetoothsdk.BluetoothLEController;
import co.lujun.lmbluetoothsdk.base.BluetoothLEListener;
import diing.com.core.command.CommandTool;
import diing.com.core.command.info.GetBattertInfoKit;
import diing.com.core.interfaces.OnResponseHandler;
import diing.com.core.response.BaseResponse;
import diing.com.core.response.BatteryInfoResponse;
import diing.com.core.response.DeviceInfoResponse;
import diing.com.core.response.DeviceSupportFunctionsResopnse;
import diing.com.core.response.DeviceTimeResponse;
import diing.com.core.util.DIException;
import diing.com.core.util.Logger;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.BLUETOOTH;
import static android.Manifest.permission.BLUETOOTH_ADMIN;

/**
 * Author: lujun(http://blog.lujun.co)
 * Date: 2016-1-25 17:53
 */
@TargetApi(21)
public class BleActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST = 29;
    private static final int REQUEST_ENABLE_BT = 1;

    private BluetoothLEController mBLEController;

    private List<String> mList;
    private BaseAdapter mFoundAdapter;

    private ListView lvDevices;
    private Button btnScan, btnDisconnect, btnReconnect, btnSend, btnBind, btnUnBind;
    private TextView tvConnState, tvContent;

    private static final String TAG = "LMBluetoothSdk";

    // You can change this options if you want to search by service and specify read/write
    // characteristics to be added to the controller
    public static final String SERVICE_ID = "00000b10-0000-1000-8000-00805f9b34fb";
    public static final String READ_CHARACTERISTIC_ID = "00000b17-0000-1000-8000-00805f9b34fb";
    public static final String WRITE_CHARACTERISTIC_ID = "00000b16-0000-1000-8000-00805f9b34fb";
    public static final String BIND_CONFIG_UUID = "00002902-0000-1000-8000-00805f9b34fb";

    private BluetoothLEListener mBluetoothLEListener = new BluetoothLEListener() {

        @Override
        public void onBond() {
            Log.e(TAG, "OnBond");
        }

        @Override
        public void onUnBond() {
            Log.e(TAG, "OnUnBond");
        }


        @Override
        public void onDiscoveringCharacteristics(final List<BluetoothGattCharacteristic> characteristics) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (BluetoothGattCharacteristic characteristic : characteristics) {
                        Log.d(TAG, "onDiscoveringCharacteristics - characteristic : " + characteristic.getUuid());

                    }
                }
            });
        }

        @Override
        public void onDiscoveringServices(final List<BluetoothGattService> services) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (BluetoothGattService service : services) {
                        if (service.getUuid().toString().equals(SERVICE_ID)) {
                            //Correct id
                        }
                    }

                }
            });
        }

        @Override
        public void onReadData(final BluetoothGattCharacteristic characteristic) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String result = Utils.logCommand("OnReadData", characteristic.getValue());
                    tvContent.append("Read from " + result + "\n");
                }
            });
        }

        @Override
        public void onWriteData(final BluetoothGattCharacteristic characteristic) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String result = Utils.logCommand("OnWriteData", characteristic.getValue());
                    tvContent.append("Me" + ": " + result + "\n");
                }
            });
        }

        @Override
        public void onDataChanged(final BluetoothGattCharacteristic characteristic) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    byte[] response = characteristic.getValue();
                    try {
                        CommandTool.shared().getResult(response, responseHandler);
                    } catch (DIException e) {
                        Toast.makeText(BleActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    String result = Utils.logCommand("OnDataChanged", response);
                    tvContent.append(result);
                }
            });
        }

        @Override
        public void onActionStateChanged(int preState, int state) {
            Logger.d(TAG, "onActionStateChanged: " + state);
        }

        @Override
        public void onActionDiscoveryStateChanged(String discoveryState) {
            if (discoveryState.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                Toast.makeText(BleActivity.this, "scanning!", Toast.LENGTH_SHORT).show();
            } else if (discoveryState.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                Toast.makeText(BleActivity.this, "scan finished!", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onActionScanModeChanged(int preScanMode, int scanMode) {
            Logger.d(TAG, "onActionScanModeChanged:  " + scanMode);
        }

        @Override
        public void onBluetoothServiceStateChanged(final int state) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvConnState.setText("Conn state: " + Utils.transConnStateAsString(state));
                }
            });
        }

        @Override
        public void onActionDeviceFound(final BluetoothDevice device, short rssi) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (device.getName().contains("BODHI")) {
                            String key = device.getName() + "@" + device.getAddress();
                            if (!mList.contains(key)) {
                                mList.add(key);
                            }
                            mFoundAdapter.notifyDataSetChanged();
                        }
                    } catch (NullPointerException e) {

                    }
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);
        getSupportActionBar().setTitle("BLE Sample");
        checkPermissions();
        init();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        if (requestCode == PERMISSION_REQUEST) {
            mList.clear();
            mFoundAdapter.notifyDataSetChanged();

            if (mBLEController.startScan()) {
                Toast.makeText(BleActivity.this, "Scanning!", Toast.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void checkPermissions() {
        int locationPermission = ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        int coarseLocationPermission = ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION);
        int bluetoothPermission = ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.BLUETOOTH);
        int bluetoothAdminPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_ADMIN);
        if (locationPermission != PackageManager.PERMISSION_GRANTED ||
                coarseLocationPermission != PackageManager.PERMISSION_GRANTED ||
                bluetoothPermission != PackageManager.PERMISSION_GRANTED ||
                bluetoothAdminPermission != PackageManager.PERMISSION_GRANTED) {
            // 無權限，向使用者請求
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{ACCESS_FINE_LOCATION,
                            ACCESS_COARSE_LOCATION, BLUETOOTH, BLUETOOTH_ADMIN},
                    PERMISSION_REQUEST
            );
        } else {

        }
//        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        if (mBluetoothAdapter != null) {
//            if (!mBluetoothAdapter.isEnabled()) {
//                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                startActivityForResult(enableBtIntent, PERMISSION_REQUEST);
//            }
//        }
    }

    private void init() {
        mBLEController = BluetoothLEController.getInstance().build(this);
        mBLEController.setBluetoothListener(mBluetoothLEListener);

        mList = new ArrayList<String>();
        mFoundAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mList);

        lvDevices = (ListView) findViewById(R.id.lv_ble_devices);
        btnScan = (Button) findViewById(R.id.btn_ble_scan);
        btnDisconnect = (Button) findViewById(R.id.btn_ble_disconnect);
        btnReconnect = (Button) findViewById(R.id.btn_ble_reconnect);
        btnSend = (Button) findViewById(R.id.btn_ble_send);
        btnBind = (Button) findViewById(R.id.btn_ble_bind);
        btnUnBind = (Button) findViewById(R.id.btn_ble_unbind);
        tvConnState = (TextView) findViewById(R.id.tv_ble_conn_state);
        tvContent = (TextView) findViewById(R.id.tv_ble_chat_content);

        lvDevices.setAdapter(mFoundAdapter);

        mBLEController.setServiceUuid(SERVICE_ID);
        mBLEController.setReadCharacteristic(READ_CHARACTERISTIC_ID);
        mBLEController.setWriteCharacteristic(WRITE_CHARACTERISTIC_ID);
        mBLEController.setConfigUuid(BIND_CONFIG_UUID);

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mList.clear();
                mFoundAdapter.notifyDataSetChanged();

                if (mBLEController.startScan()) {
                    Toast.makeText(BleActivity.this, "Scanning!", Toast.LENGTH_SHORT).show();
                }

//                You can scan by service using the following code:
//                List<UUID> uuids = new ArrayList<UUID>();
//                uuids.add(UUID.fromString(SERVICE_ID));
//
//                if( mBLEController.startScanByService(uuids) ){
//                    Toast.makeText(BleActivity.this, "Scanning!", Toast.LENGTH_SHORT).show();
//                }
            }
        });

        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBLEController.disconnect();
            }
        });
        btnReconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBLEController.reConnect();
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] data = new byte[20];
                data[0] = (byte) 0x32;
                data[1] = (byte) 0xa1;
                for (int i = 2; i < 20; i++) {
                    data[i] = (byte) 0x00;
                }
                Utils.logCommand("onClick", data);
                mBLEController.write(data);
            }
        });

        btnBind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                registerReceiver(mBondingBroadcastReceiver, filter);
                //Bind
//                byte[] data = new byte[20];
//                data[0] = (byte) 0x34;
//                data[1] = (byte) 0x28;
//                data[2] = (byte) 0x01;
//                data[3] = (byte) Build.VERSION.SDK_INT;
//                data[4] = (byte) 0x55;
//                data[5] = (byte) 0xaa;
//                for (int i = 6 ; i < 20; i++) {
//                    data[i] = (byte) 0x00;
//                }
//                Utils.logCommand("onClick", data);
//                mBLEController.write(data);
                try {
                    final int state = mBLEController.getConnectedDevice().getBondState();
                    switch (state) {
                        case BluetoothDevice.BOND_NONE:
                            Logger.e(TAG, "BOND_NONE");
                            mBLEController.bond();
                            break;
                        case BluetoothDevice.BOND_BONDED:
                            Logger.e(TAG, "BOND_BONDED");
                            break;
                        case BluetoothDevice.BOND_BONDING:
                            Logger.e(TAG, "BOND_BONDING");
                            break;
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        });

        btnUnBind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                registerReceiver(mBondingBroadcastReceiver, filter);
//                byte[] data = new byte[20];
//                data[0] = (byte) 0x34;
//                data[1] = (byte) 0x29;
//                data[2] = (byte) 0xaa;
//                data[3] = (byte) 0x55;
//                data[4] = (byte) 0x55;
//                data[5] = (byte) 0xaa;
//                for (int i = 6 ; i < 20; i++) {
//                    data[i] = (byte) 0x00;
//                }
//                byte[] data = UnBindKit.getCommand();
//                byte[] data = GetDeviceInfoKit.getCommand();
//                byte[] data = GetSupportFunctionsKit.getCommand();
//                byte[] data = GetMacAddress.getCommand();
                byte[] data = GetBattertInfoKit.getCommand();
                Utils.logCommand("onClick", data);
                mBLEController.write(data);

//                mBLEController.unBond();
            }
        });

        Set<BluetoothDevice> bondDevices = mBLEController.getBondedDevices();
        for (BluetoothDevice device : bondDevices) {
            String key = device.getName() + "@" + device.getAddress();
            Logger.e("Bonded", key);
            mBLEController.unBond(device);
        }

        lvDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String itemStr = mList.get(position);
                mBLEController.connect(itemStr.substring(itemStr.length() - 17));
            }
        });

        if (!mBLEController.isSupportBLE()) {
            Toast.makeText(BleActivity.this, "Unsupport BLE!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBLEController.release();
    }

    private String parseData(BluetoothGattCharacteristic characteristic) {

        String result = characteristic.getStringValue(0);

        //String result = "";
        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
//        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
//            int flag = characteristic.getProperties();
//            int format = -1;
//            if ((flag & 0x01) != 0) {
//                format = BluetoothGattCharacteristic.FORMAT_UINT16;
//                Log.d(TAG, "Heart rate format UINT16.");
//            } else {
//                format = BluetoothGattCharacteristic.FORMAT_UINT8;
//                Log.d(TAG, "Heart rate format UINT8.");
//            }
//            final int heartRate = characteristic.getIntValue(format, 1);
//            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
//            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
//        } else {
        // For all other profiles, writes the data formatted in HEX.
        //final byte[] data = characteristic.getValue();
        //if (data != null && data.length > 0) {
        //    result =  new String(data);
        //}
//      //  }
        return result;
    }

    /**
     * BLE response handler
    * */
    private OnResponseHandler responseHandler = new OnResponseHandler() {
        @Override
        public void onUpgrade(BaseResponse response) {

        }

        @Override
        public void onBindCompletion(BaseResponse response) {
            if (response.getStatus()) {
                Toast.makeText(BleActivity.this, "綁定成功", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(BleActivity.this, response.getError().getMessage(), Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onUnBindCompletion(BaseResponse response) {
            if (response.getStatus()) {
                Toast.makeText(BleActivity.this, "解除綁定成功", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(BleActivity.this, response.getError().getMessage(), Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onGetDeviceInfoCompletion(DeviceInfoResponse response) {
            Logger.i("DeviceInfoResponse", response.toString());
        }

        @Override
        public void onGetDeviceSupportFunctionsCompletion(DeviceSupportFunctionsResopnse response) {
            Logger.i("DeviceInfoResponse", response.toString());
        }

        @Override
        public void onGetDeviceTimeCompletion(DeviceTimeResponse response) {
            Logger.i(response.toString());
        }

        @Override
        public void onGetMacCompletion(String mac) {
            Logger.i(mac);
        }

        @Override
        public void onGetBatteryInfoCompletion(BatteryInfoResponse response) {
            Logger.i(response.toString());
        }
    };

    private BroadcastReceiver mBondingBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            final int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
            final int previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);

            Logger.e(TAG, "Bond state changed for: " + device.getAddress() + " new state: " + bondState + " previous: " + previousBondState);

            // skip other devices
            if (!device.getAddress().equals(mBLEController.getConnectedDevice().getAddress()))
                return;

            if (bondState == BluetoothDevice.BOND_BONDED) {
                unregisterReceiver(this);
                //mCallbacks.onBonded();
            } else if (bondState == BluetoothDevice.BOND_NONE) {
                unregisterReceiver(this);
            }
        }
    };
}