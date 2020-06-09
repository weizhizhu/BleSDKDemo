package com.example.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.boe.mhealth.listener.WristScannerListener;
import com.boe.mhealth.manager.BluetoothWristManager;
import com.boe.mhealth.scanner.ExtendedBluetoothDevice;
import com.boe.mhealth.utils.DebugLogger;
import com.boe.mhealth.utils.ToastUtils;
import com.example.demo.adapter.CommonAdapter;
import com.example.demo.adapter.ViewHolder;

import java.util.ArrayList;
import java.util.List;

public class BondActivity extends Activity implements WristScannerListener {

    private final String TAG = "BondActivity";

    protected static final int REQUEST_ENABLE_BT = 2;

    String deviceName = "0";
    String deviceAddr = "1";

    private Boolean mSearchFlag = false;

    private BluetoothAdapter mBluetoothAdapter;

    private boolean mIsScanning = false;

    private static final boolean DEVICE_IS_BONDED = true;
    private static final boolean DEVICE_NOT_BONDED = false;
    private static final int NO_RSSI = -1000;

    static final int CONNECT_RSSI = 23;

    ListView listview;

    private CommonAdapter<ExtendedBluetoothDevice> mAdapter;

    private List<ExtendedBluetoothDevice> mList=new ArrayList<>();


    @Override
    protected void onDestroy() {
        super.onDestroy();

        BluetoothWristManager.getInstance().stopWristScanner();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bound);

        listview = (ListView) findViewById(R.id.listview);

        final BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();

        init();

        findViewById(R.id.stop_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BluetoothWristManager.getInstance().stopWristScanner();
            }
        });

    }

    void init() {
        listview.setAdapter(mAdapter = new CommonAdapter<ExtendedBluetoothDevice>(this,mList,R.layout.device_list_row) {
            @Override
            public void convert(ViewHolder holder, final ExtendedBluetoothDevice device) {
                holder.setTextView(R.id.name_tv,device.device.getName());
                holder.setTextView(R.id.mac_tv,device.device.getAddress());
                /*holder.getConvertView().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onDeviceSelected(device);
                    }
                });*/
            }
        });
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ExtendedBluetoothDevice device=(ExtendedBluetoothDevice)parent.getItemAtPosition(position);
                onDeviceSelected(device);
            }
        });

        ensureBLESupported();
        if (!isBLEEnabled()) {
            showBLEDialog();
        }

        requstPermissions();

        startScanner();
    }

    private void startScanner(){
        BluetoothWristManager.getInstance().startWristScanner(null,this);
    }


    private void onDeviceSelected(final ExtendedBluetoothDevice exdevice) {
        BluetoothWristManager.getInstance().stopWristScanner();
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("连接蓝牙");
        builder.setMessage("是否连接该蓝牙？");
        builder.setNegativeButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                exdevice.isBonded = true;
                exdevice.rssi = CONNECT_RSSI;
                exdevice.connect();
                dialog.dismiss();
                ToastUtils.showShort(getApplicationContext(),"正在连接");
                finish();
            }
        });
        builder.setPositiveButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    /**
     * if scanned device already in the list then update it otherwise add as a
     * new device
     */
    private void addScannedDevice(ExtendedBluetoothDevice device) {
        if (device != null) {
            final int indexInBonded = mList.indexOf(device);
            if (indexInBonded >= 0) {
                ExtendedBluetoothDevice previousDevice = mList.get(indexInBonded);
                previousDevice.rssi = device.rssi;
            }else{
                mList.add(device);
            }
            mAdapter.notifyDataSetChanged();
            DebugLogger.d("addBondedDevice", "name=" + device.device.getName() + " address=" + device.device.getAddress());
        }
    }


    private void ensureBLESupported() {
        if (!getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            finish();
        }
    }

    protected boolean isBLEEnabled() {
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter adapter = bluetoothManager.getAdapter();
        return adapter != null && adapter.isEnabled();
    }

    protected void showBLEDialog() {
        final Intent enableIntent = new Intent(
                BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }

    private String[] authComArr = new String[]{"android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.BLUETOOTH", "android.permission.BLUETOOTH_ADMIN", "android.permission.BLUETOOTH_PRIVILEGED"};
    private boolean hasRequestComAuth;

    private boolean hasCompletePhoneAuth() {
        // TODO Auto-generated method stub

        PackageManager pm = this.getPackageManager();
        for (String auth : authComArr) {
            if (pm.checkPermission(auth, this.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requstPermissions() {
        // 权限申请
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            if (!hasCompletePhoneAuth()) {
                if (!hasRequestComAuth) {
                    hasRequestComAuth = true;
                    DebugLogger.d(TAG, "requstPermissions " + authComArr);
                    this.requestPermissions(authComArr, 1);
                    return;
                }
            } else {
                if (!isOpenGps()) {
                    openGPS();
                } else {
                    startScanner();
                }
            }
        } else {
            startScanner();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // TODO Auto-generated method stub
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            for (int i = 0; i < grantResults.length; i++) {
                int ret = grantResults[i];
                DebugLogger.d(TAG, "onRequestPermissionsResult permission=" + permissions[i] + " result=" + ret);
                if (ret == 0) {
                    continue;
                }
            }
            if (!isOpenGps()) {
                openGPS();
            } else {
                startScanner();
            }
        }

    }

    private void openGPS() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivityForResult(intent, 0);
        startScanner();
    }

    private boolean isOpenGps() {
        LocationManager locationManager
                = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // 通过GPS卫星定位，定位级别可以精确到街（通过24颗卫星定位，在室外和空旷的地方定位准确、速度快）
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // 通过WLAN或移动网络(3G/2G)确定的位置（也称作AGPS，辅助GPS定位。主要用于在室内或遮盖物（建筑群或茂密的深林等）密集的地方定位）
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (gps || network) {
            return true;
        }
        return false;
    }

    public void onDeviceConnected() {

    }

    @Override
    public void onScanSuccess(ExtendedBluetoothDevice device) {
        addScannedDevice(device);
    }

    @Override
    public void onScanFailure(String errorMsg) {

    }

    @Override
    public void onScannerStatus(ScannerStatus status) {

    }
}