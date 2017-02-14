package co.lujun.sample;

import diing.com.core.response.BatteryInfoResponse;
import diing.com.core.response.DeviceInfoResponse;
import diing.com.core.response.SupportFunctionsResponse;

/**
 * Created by leo.chang on 14/02/2017.
 */

public class Device {
    //用來取得BluthtoothDevice 的Key
    private String mac;
    //Device Info
    private long deviceId;
    private int version;
    private String deviceMode;
    private String batteryStatus;
    private int batteryPower;
    private String bondingState;
    private boolean isReboot;
    private boolean isBodhi;
    private boolean isBeta;

    //BatteryInfo
    private String batteryType;
    private int voltage;
    private int powerLevel;
    private long usingTime;
    private long usingLife;

    private int alarmCount;

    private static Device instance;
    public static Device current() {
        synchronized (Device.class) {
            if (instance == null) {
                instance = new Device();
            }
        }
        return instance;
    }

    public Device() {
        super();
        deviceId = -1;
        //TODO: Load device info from database
    }

    public boolean isEmpty() {
        if (deviceId == -1) {
            return true;
        }
        return false;
    }

    public void setDeviceInfo(DeviceInfoResponse response) {
        deviceId = response.getDeviceId();
        version = response.getVersion();
        deviceMode = response.getDeviceMode().toValue();
        batteryStatus = response.getBatteryStatus().toValue();
        batteryPower = response.getBatteryPower();
        bondingState = response.getBondingState().toValue();
        isReboot = response.isReboot();
        isBodhi = response.isBobhi();
        isBeta = response.isBeta();
    }

    public void setBatteryInfo(BatteryInfoResponse response) {
        batteryType = response.getBatteryType().toValue();
        voltage = response.getVoltage();
        powerLevel = response.getPowerLevel();
        usingTime = response.getUsingTime();
        usingLife = response.getUsingLife();
    }

    public void setSupportFunctions(SupportFunctionsResponse response) {

    }
}
