package com.example.demo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.boe.mhealth.bean.Alarm;
import com.boe.mhealth.bean.BindResult;
import com.boe.mhealth.bean.DeviceBean;
import com.boe.mhealth.bean.DisplayItem;
import com.boe.mhealth.bean.Longsit;
import com.boe.mhealth.bean.MsgSwith;
import com.boe.mhealth.bean.NotRemind;
import com.boe.mhealth.bean.Person;
import com.boe.mhealth.bean.Sleep;
import com.boe.mhealth.bean.SleepBean;
import com.boe.mhealth.bean.SyncStatusBean;
import com.boe.mhealth.bean.TimeStyleItem;
import com.boe.mhealth.cmd.Command;
import com.boe.mhealth.imp.CmdHandler;
import com.boe.mhealth.listener.BleDataListener;
import com.boe.mhealth.listener.IAutoConnectListener;
import com.boe.mhealth.listener.IConnectListener;
import com.boe.mhealth.listener.InitializeListener;
import com.boe.mhealth.listener.OnCheckOtaListener;
import com.boe.mhealth.listener.OnOtaListener;
import com.boe.mhealth.listener.OnProgerssListener;
import com.boe.mhealth.listener.OnReplyCallback;
import com.boe.mhealth.manager.BluetoothWristManager;
import com.boe.mhealth.manager.Callback;
import com.boe.mhealth.manager.DataManager;
import com.boe.mhealth.manager.IBleDevice;
import com.boe.mhealth.manager.IUpdateManager;
import com.boe.mhealth.mode.CallbackMode;
import com.boe.mhealth.mode.DisplayAbleMode;
import com.boe.mhealth.mode.ErrorCode;
import com.boe.mhealth.mode.MessageMode;
import com.boe.mhealth.mode.OtaState;
import com.boe.mhealth.mode.OtaType;
import com.boe.mhealth.mode.ResetMode;
import com.boe.mhealth.mode.ResultMode;
import com.boe.mhealth.mode.SexMode;
import com.boe.mhealth.mode.SwithMode;
import com.boe.mhealth.mode.SyncDataMode;
import com.boe.mhealth.mode.SyncDataType;
import com.boe.mhealth.mode.SyncStatusMode;
import com.boe.mhealth.mode.TakePhotoMode;
import com.boe.mhealth.utils.DebugLogger;
import com.boe.mhealth.utils.ToastUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity implements IConnectListener, BleDataListener, IAutoConnectListener,View.OnClickListener {

    private String TAG="MainActivity";

    private TextView connect_statues;

    private List<DisplayItem> displayItems;

    private List<TimeStyleItem> timeStyleItems;

    private List<MsgSwith> msgSwiths;

    private EditText input_cmd,input_data,appVersion;

    private TextView result_tv,tvFilePaht;
    //是否开发环境
    private CheckBox cbxIsDev,cbxIsDFU;

    private final int SELECT_FILE_REQ=1;

    private Uri mFileStreamUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //开启日志
        DebugLogger.init(getApplication(),true,true);
        //注册权限
        requstPermissions();

        connect_statues=(TextView)findViewById(R.id.connect_statues);
        result_tv=(TextView)findViewById(R.id.result_tv);
        cbxIsDev=(CheckBox)findViewById(R.id.isdev_cbx);
        cbxIsDFU = findViewById(R.id.is_dfu_cbx);
        tvFilePaht = findViewById(R.id.file_path_tv);

        input_cmd=(EditText)findViewById(R.id.input_cmd);
        input_data=(EditText)findViewById(R.id.input_data);
        appVersion=(EditText)findViewById(R.id.appVersion);

        BluetoothWristManager.getInstance().initialize(getApplicationContext(), new InitializeListener() {
            @Override
            public void onInitializeSuccess() {
                BluetoothWristManager.getInstance().getBleDevice().registerConnectListener(MainActivity.this);
                BluetoothWristManager.getInstance().getBleDevice().registerDataListenr(MainActivity.this);
                BluetoothWristManager.getInstance().getBleDevice().setAutoConnect(true, 5,MainActivity.this);
            }

            @Override
            public void onInitializeFailure(String errorMsg) {

            }
        });

        //判断通知权限是否有开启
        if (!NotificationService.isEnabled(this)) {
            //未开启跳转到通知栏
            NotificationService.openNotificationAccess(this);
        }else{
            //申请授权
            NotificationService.ensureCollectorRunning(this);
        }
    }

    private String[] authComArr=new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_PRIVILEGED,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.INTERNET,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
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
    private void requstPermissions(){
        // 权限申请
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            if (!hasCompletePhoneAuth()) {
                if (!hasRequestComAuth) {
                    hasRequestComAuth = true;
                    DebugLogger.d(TAG,"requstPermissions "+authComArr);
                    this.requestPermissions(authComArr, 1);
                    return;
                }
            }

        }
    }

    Command command=null;
    @Override
    public void onClick(View v) {
        if(sb.length() > 0) {
            sb.delete(0, sb.length());
        }
        switch (v.getId()){
//            case R.id.getbattery_btn:
//                int battery = BluetoothWristManager.getInstance().getBleDevice().getBattery();
//                result_tv.setText("剩余电量：" + battery);
//                return;
            case R.id.getdeviceinfo_btn:
                DeviceBean bean = BluetoothWristManager.getInstance().getBleDevice().getDeviceInfo();
                if(bean != null) {
                    StringBuffer sb = new StringBuffer();
                    sb.append("设备地址：").append(bean.getAddress()).append("\n");
                    sb.append("设备名称：").append(bean.getName()).append("\n");
                    sb.append("设备版本：").append(bean.getVersion()).append("\n");
                    sb.append("设备项目号：").append(bean.getProjectId()).append("\n");
                    sb.append("设备剩余电量：").append(bean.getBattery());

                    result_tv.setText(sb.toString());
                }else{
                    result_tv.setText("请先连接蓝牙");
                }
                return;
            case R.id.connect_btn:
                startActivity(new Intent(this,BondActivity.class));
                return;
            case R.id.disconnect_btn:
                if(BluetoothWristManager.getInstance().getBleDevice()!=null) {
                    BluetoothWristManager.getInstance().getBleDevice().disconnect();
                }
                return;
            case R.id.get_battery_btn:
                initCommand(DataManager.getBatteryBytes(true),"getBattery");
                break;
            case R.id.get_combinationdata_btn:
                //组合获取运动+心率
                initCommand(DataManager.getCombinationDataBytes(SyncDataType.SPORT|SyncDataType.HEARTRATE,true));
                break;
            case R.id.get_heartrate_btn:
                if(!canNext()){
                    return;
                }
                isSynFinish = false;
                initCommand(DataManager.getHeartrateBytes(true));
                break;
            case R.id.get_sleep_btn:
                if(!canNext()){
                    return;
                }
                isSynFinish = false;
                initCommand(DataManager.getSleepBytes(true));
                break;
            case R.id.get_sport_btn:
                if(!canNext()){
                    return;
                }
                isSynFinish = false;
                initCommand(DataManager.getSportBytes(true));
                break;
            case R.id.get_alldata_btn:
                if(!canNext()){
                    return;
                }
                isSynFinish = false;
                initCommand(DataManager.getAllDataBytes(true));
                break;
            case R.id.get_version_btn:
                initCommand(DataManager.getVersionBytes(true));
                break;
            case R.id.read_person_btn:
                initCommand(DataManager.getReadPersonInfoBytes(true),"getPersonInfo");
                break;
//            case R.id.clock_btn:
//                initCommand(DataManager.getReadAlarmInfoBytes(true));
//                break;
            case R.id.longsit_btn:
                initCommand(DataManager.getReadLongsitBytes(true),"getLongsit");
                break;
            case R.id.noticeways_btn:
                initCommand(DataManager.getReadVibratBytes(true));
                break;
            case R.id.gesture_btn:
                initCommand(DataManager.getReadWristBytes(true));
                break;
            case R.id.display_btn:
                initCommand(DataManager.getReadDisplayBytes(true));
                break;
            case R.id.timestyle_btn:
                initCommand(DataManager.getReadTimePageBytes(true));
                break;
            case R.id.sports_btn:
                if(!canNext()){
                    return;
                }
                isSynFinish = false;
                initCommand(DataManager.getSyncRealdateBytes(SyncDataMode.SPORT,true));
                break;
            case R.id.sleep_btn:
                if(!canNext()){
                    return;
                }
                isSynFinish = false;
                initCommand(DataManager.getSyncRealdateBytes(SyncDataMode.SLEEP,true));
                break;
            case R.id.heartrate_btn:
                if(!canNext()){
                    return;
                }
                isSynFinish = false;
                initCommand(DataManager.getSyncRealdateBytes(SyncDataMode.HEARTRATE,true));
                break;
            case R.id.bp_btn:
                if(!canNext()){
                    return;
                }
                isSynFinish = false;
                initCommand(DataManager.getSyncRealdateBytes(SyncDataMode.BLOODPRESSURE,true));
                break;
            case R.id.bo_btn:
                if(!canNext()){
                    return;
                }
                isSynFinish = false;
                initCommand(DataManager.getSyncRealdateBytes(SyncDataMode.BLOODOXYGEN,true));
                break;
            case R.id.sport_detail_btn:
                if(!canNext()){
                    return;
                }
                isSynFinish = false;
                initCommand(DataManager.getSyncRealdateBytes(SyncDataMode.SPORT_DETAIL,true));
                break;
            case R.id.sync_all_btn:
                if(!canNext()){
                    return;
                }
                isSynFinish = false;
                initCommand(DataManager.getSyncRealdateBytes(SyncDataMode.ALL,true));
                break;
            case R.id.close_sync_btn:
                initCommand(DataManager.getSyncRealdateBytes(SyncDataMode.CLOSE,true));
                break;
            case R.id.combination_sync_btn:
                initCommand(DataManager.getSyncRealdateBytes(SyncDataType.SPORT_DETAIL,true));
                break;
            case R.id.in_camera_btn:
                initCommand(DataManager.getWriteTakePhotoStateBytes(TakePhotoMode.IN,true));
                break;
            case R.id.out_camera_btn:
                initCommand(DataManager.getWriteTakePhotoStateBytes(TakePhotoMode.OUT,true));
                break;
            case R.id.target_btn:
                initCommand(DataManager.getReadSportTargetBytes(true));
                break;
            case R.id.notremind_btn:
                initCommand(DataManager.getReadNotremindBytes(true));
                break;
            case R.id.hearalerm_btn:
                initCommand(DataManager.getReadHeartRateAlarmBytes(true));
                break;
            case R.id.allday_btn:
                initCommand(DataManager.getReadHeartRateAllDayBytes(true));
                break;
            case R.id.read_allday_bp_btn:
                initCommand(DataManager.getReadBloodPressureAllDayBytes(true));
                break;
            case R.id.get_clock_vibrate_btn:
                initCommand(DataManager.getReadAlarmInfoSetVibrationBytes(true));
                break;
            case R.id.get_bright_screen_msg_btn:
                initCommand(DataManager.getReadBrightScreenSwithBytes(true));
                break;
            case R.id.msgswitch_btn:
                initCommand(DataManager.getMsgSwitchStateBytes(true));
                break;
            case R.id.read_device_state_btn:
                if(!isEmptyDevice()) return;
                sb.append("返回：" + BluetoothWristManager.getInstance().getBleDevice().getStatus()).append("\n");
                result_tv.setText(sb.toString());
                return;
            case R.id.update_ui_btn:
                //update(UpdateType.UI);
                if(updateManager!=null){
                    updateManager.cancelDownloadOta();
                }
                return;
            case R.id.ota_btn:
                //update(UpdateType.OTA);
                //通过服务器检测升级
                update();
                return;
            case R.id.update_file_btn:
                //直接传升级固件升级　
                updateOtaByFile();
                break;
            case R.id.open_file_choice_btn:
                openFileChooser();
                break;
        }
        if(command != null && command.data != null && BluetoothWristManager.getInstance().getBleDevice() != null){
            BluetoothWristManager.getInstance().getBleDevice().writeData(new OnReplyCallback() {
                @Override
                public void success(CmdHandler peripheral, Command command, Object obj) {
                    Log.e("callback","callback onResult;"+command.tag);
                }

                @Override
                public void error(CmdHandler peripheral, Command command, String errorMsg) {
                    Log.e("callback","commandTag:"+command.tag+";errorMsg:"+errorMsg);
                }

                @Override
                public boolean timeout(CmdHandler peripheral, Command command) {
                    Log.e("callback","callback timeout;"+command.tag);
                    return false;
                }
            },command);
        }
    }

    IUpdateManager updateManager;
    /**
     * 在线检测升级
     */
    private void update(){
        if(!isEmptyDevice()) return;
        final IBleDevice device=BluetoothWristManager.getInstance().getBleDevice();
        if(device!=null){
            //versionCode app版本号
            //isDev  true 开发环境　　false  商用环境
            //
            int version= 0;
            try {
                version = Integer.parseInt(appVersion.getText().toString());
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            //是否是开发环境　false为商用环境
            boolean isDev=cbxIsDev.isChecked();
            //检测升级
            device.checkOtaVersion(version,isDev,new OnCheckOtaListener() {

                @Override
                public void onError(ErrorCode code, final String errorMsg) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            result_tv.setText(errorMsg);
                        }
                    });
                }

                @Override
                public void onResult(OtaState state, int isForced, String version, String version_name, String version_desc, long fileSize) {
                    if(state==OtaState.CAN_OTA){
                        DebugLogger.d(TAG,"can ota");
                        //取消升级　updateManager.cancelDownloadOta();
                        updateManager=device.startOta(new OnOtaListener() {
                            @Override
                            public void start(OtaType type) {
                                runUIThread(type+" start");
                            }

                            @Override
                            public void onProgress(OtaType type, float percent) {
                                Log.e(TAG,type+" progress="+percent);
                                runUIThread(type+" progress="+percent);
                            }

                            @Override
                            public void finish(OtaType type) {
                                runUIThread(type+" finish");
                            }

                            @Override
                            public void fail(OtaType type, ErrorCode errorCode, String errorMsg) {
                                Log.d("otafail",errorCode.name()+"   "+errorMsg);
                                runUIThread(type+" fail:"+errorMsg);
                            }

                            @Override
                            public void cancelDownload(OtaType type, ResultMode mode) {
                                runUIThread("取消升级");
                            }


                        });
                    }else if(state==OtaState.LASTER_VERSION){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                result_tv.setText("暂无更新");
                            }
                        });
                    }else if(state==OtaState.UPDATE_APP){
                        runUIThread("app版本过低");
                    }
                }
            });
        }
    }

    /**
     * 本地文件升级
     */
    private void updateOtaByFile(){
        if(mFileStreamUri==null){
            ToastUtils.showShort(getApplicationContext(),"请选择固件");
            return;
        }
        final IBleDevice device=BluetoothWristManager.getInstance().getBleDevice();
        if(device!=null){
            if(!cbxIsDFU.isChecked()){
                device.otaUpdateByFile(readBinary(), new OnProgerssListener() {
                    @Override
                    public void start() {
                        DebugLogger.d(TAG,"otaUpdate start");
                        runUIThread("otaUpdate start");
                    }

                    @Override
                    public void onProgress(float percent) {
                        DebugLogger.d(TAG,"otaUpdate progress="+percent);
                        runUIThread("otaUpdate progress="+percent);
                    }

                    @Override
                    public void finish() {
                        DebugLogger.d(TAG,"otaUpdate finish");
                        runUIThread("otaUpdate finish");
                    }

                    @Override
                    public void fail(ErrorCode code,String errorMsg) {
                        DebugLogger.d(TAG,"otaUpdate fail:"+errorMsg);
                        runUIThread(code+" fail:"+errorMsg);
                    }

                    @Override
                    public void cancel() {
                        runUIThread(" cancel ota");
                    }
                });
            }else{//1.3寸DFU升级模式
                updateManager=device.startDfuUpdate(mFileStreamUri,new OnOtaListener() {
                    @Override
                    public void start(OtaType type) {
                        runUIThread(type + " start");
                    }

                    @Override
                    public void onProgress(OtaType type, float percent) {
                        Log.e("startDfuUpdate", type + " progress=" + percent);
                        runUIThread(type + " progress=" + percent);
                    }

                    @Override
                    public void finish(OtaType type) {
                        runUIThread(type + " finish");
                    }

                    @Override
                    public void fail(OtaType type, ErrorCode errorCode, String errorMsg) {
                        Log.d("startDfuUpdate", errorCode.name() + "   " + errorMsg);
                        runUIThread(type + " fail:" + errorMsg);
                    }

                    @Override
                    public void cancelDownload(OtaType type, ResultMode mode) {
                        runUIThread("取消升级");
                    }

                });
            }
        }
    }

    private byte [] readBinary() {
        byte []BinaryData=null;
        InputStream ins=null;
        try {
            ins = getContentResolver().openInputStream(mFileStreamUri);
            ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
            int size = 0;
            byte[] buffer = new byte[1024];
            while((size=ins.read(buffer,0,1024))>=0){
                outputStream.write(buffer,0,size);
            }
            ins.close();

            BinaryData=outputStream.toByteArray();
            DebugLogger.i(TAG, "Binary data size : "+BinaryData.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return BinaryData;
    }

    private void runUIThread(final String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                result_tv.setText(message);
            }
        });
    }

    private void initCommand(byte[] data){
        if(!isEmptyDevice()) return;
        command = Command.newInstance();
        command.data = data;
    }

    private void initCommand(byte[] data, String tag){
        if(!isEmptyDevice()) return;
        command = Command.newInstance();
        command.tag = tag;
        command.data = data;
    }

    public void onWriteClick(View v){
        if(sb.length() > 0) {
            sb.delete(0, sb.length());
        }
        Command command=null;
        switch (v.getId()){
            case R.id.write_person_btn:
                if(!isEmptyDevice()) return;
                Person person=new Person();
                person.setWeight(70);
                person.setHeight(170);
                person.setWalkUnit(66);
                person.setAge(28);
                person.setSex(SexMode.MALE);
                command=Command.newInstance();
                command.data=DataManager.getWritePersonInfoBytes(person,true);
                if(command.data != null && BluetoothWristManager.getInstance().getBleDevice() != null){
                    BluetoothWristManager.getInstance().getBleDevice().writeData(command);
                    ToastUtils.showShort(getApplicationContext(),"设置成功");
                }
                break;
//            case R.id.write_clock_btn: {
//                if (!isEmptyDevice()) return;
//                //只传打开的闹钟
//                List<Alarm> list = new ArrayList<>();
//                Alarm alarm = new Alarm();
//                alarm.setType(1);
//                alarm.setHour(18);
//                alarm.setMunite(30);
//                alarm.setOpened(SwithMode.ON);
//                alarm.setRepeat(new byte[]{1, 1, 1, 1, 1, 1, 1});
//                list.add(alarm);
//                command = Command.newInstance();
//                command.data = DataManager.getWriteAlarmInfoBytes(list, true);
//                if (BluetoothWristManager.getInstance().getBleDevice() != null) {
//                    BluetoothWristManager.getInstance().getBleDevice().writeData(command);
//                    ToastUtils.showShort(getApplicationContext(), "设置成功");
//                }
//                break;
//            }
            case R.id.set_clock_vibrate_btn: {
                if (!isEmptyDevice()) return;
                Calendar calendar=Calendar.getInstance();
                calendar.add(Calendar.MINUTE,1);
                //只传打开的闹钟
                List<Alarm> list = new ArrayList<>();
                Alarm alarm = new Alarm();
                alarm.setType(0);
                alarm.setHour(calendar.get(Calendar.HOUR_OF_DAY));
                alarm.setMunite(calendar.get(Calendar.MINUTE)+2);
                alarm.setOpened(SwithMode.ON);
                alarm.setRepeat(new byte[]{1, 1, 1, 1, 1, 1, 1});
                alarm.setVibrationTime((byte)3);//震动时间3秒
                alarm.setContent("我是默认的闹钟");//提醒内容
                list.add(alarm);

                calendar.add(Calendar.HOUR_OF_DAY,1);
                alarm = new Alarm();
                alarm.setType(0);
                alarm.setHour(calendar.get(Calendar.HOUR_OF_DAY));
                alarm.setMunite(calendar.get(Calendar.MINUTE));
                alarm.setOpened(SwithMode.ON);
                alarm.setRepeat(new byte[]{1, 1, 1, 1, 1, 1, 1});
                alarm.setVibrationTime((byte)3);//震动时间3秒
                alarm.setContent("我是闹钟2");//提醒内容
                list.add(alarm);

                ArrayList<byte[]> bytes=DataManager.getWriteAlarmInfoSetVibrationBytes(list, true);
                if(bytes != null) {
                    for (byte[] b : bytes) {
                        command = Command.newInstance();
                        command.data = b;
                        if (command.data != null && BluetoothWristManager.getInstance().getBleDevice() != null) {
                            BluetoothWristManager.getInstance().getBleDevice().writeData(command);
                        }
                    }
                    ToastUtils.showShort(getApplicationContext(), "设置成功");
                }
                break;
            }
            case R.id.write_longsit_btn:
                if(!isEmptyDevice()) return;
                Longsit longsit=new Longsit();
                longsit.setDuration(5);
                longsit.setStartHour((byte) 9);
                longsit.setStartMin((byte) 0);
                longsit.setEndHour((byte) 19);
                longsit.setEndMin((byte) 0);
                longsit.setMode(SwithMode.ON);
                longsit.setRepeat(new byte[]{1,1,1,1,1,1,1});//每天
                command=Command.newInstance();
                command.data=DataManager.getSettingLongsitBytes(longsit,true);
                if(command.data != null && BluetoothWristManager.getInstance().getBleDevice()!=null){
                    BluetoothWristManager.getInstance().getBleDevice().writeData(command);
                    ToastUtils.showShort(getApplicationContext(),"设置成功");
                }
                break;
            case R.id.write_noticeways_btn:
                if(!isEmptyDevice()) return;
                command=Command.newInstance();
                command.data=DataManager.getVibratBytes(SwithMode.OFF,SwithMode.OFF,true);//震动+亮屏
                if(command.data != null && BluetoothWristManager.getInstance().getBleDevice()!=null){
                    BluetoothWristManager.getInstance().getBleDevice().writeData(command);
                    ToastUtils.showShort(getApplicationContext(),"设置成功");
                }
                break;
            case R.id.write_gesture_btn:
                if(!isEmptyDevice()) return;
                command=Command.newInstance();
                command.data=DataManager.getWristBytes(SwithMode.ON,SwithMode.ON,true);//抬腕+翻腕
                if(command.data != null && BluetoothWristManager.getInstance().getBleDevice()!=null){
                    BluetoothWristManager.getInstance().getBleDevice().writeData(command);
                    ToastUtils.showShort(getApplicationContext(),"设置成功");
                }
                break;
            case R.id.write_display_btn:
                if(!isEmptyDevice()) return;
                if(displayItems==null){
                    ToastUtils.showShort(getApplicationContext(),"请先同步手环数据");
                    return;
                }
                DisplayItem item=displayItems.get(0);
                if(item.getEnabled()== DisplayAbleMode.ENABLE){//当前手环是否支持该界面显示
                    displayItems.get(0).setDisplay(false);//隐藏
                }
                //注意这里需要把获取到的所有项都传不能删减
                command=Command.newInstance();
                command.data=DataManager.getDisplayBytes(displayItems,true);
                if(command.data != null && BluetoothWristManager.getInstance().getBleDevice()!=null){
                    BluetoothWristManager.getInstance().getBleDevice().writeData(command);
                    ToastUtils.showShort(getApplicationContext(),"设置成功");
                }
                break;
            case R.id.write_timestyle_btn:
                if(!isEmptyDevice()) return;
                if(timeStyleItems==null){
                    ToastUtils.showShort(getApplicationContext(),"请先同步手环数据");
                    return;
                }
                TimeStyleItem styleItem=timeStyleItems.get(timeStyleItems.size()-1);
                styleItem.setMode(SwithMode.ON);//显示心率
                //注意这里需要把获取到的所有项都传不能删减
                command=Command.newInstance();
                command.data=DataManager.getTimePageBytes(timeStyleItems,true);
                if(command.data != null && BluetoothWristManager.getInstance().getBleDevice()!=null){
                    BluetoothWristManager.getInstance().getBleDevice().writeData(command);
                    ToastUtils.showShort(getApplicationContext(),"设置成功");
                }
                break;
            case R.id.write_target_btn:
                if(!isEmptyDevice()) return;
                command=Command.newInstance();
                command.data=DataManager.getWriteSportTargetBytes(5000,true);
                if(command.data != null && BluetoothWristManager.getInstance().getBleDevice()!=null){
                    BluetoothWristManager.getInstance().getBleDevice().writeData(command);
                    ToastUtils.showShort(getApplicationContext(),"设置成功");
                }
                break;
            case R.id.write_notremind_btn:
                if(!isEmptyDevice()) return;
                NotRemind notRemind=new NotRemind();
                notRemind.setStartHour((byte) 12);
                notRemind.setStartMin((byte)30);
                notRemind.setEndHour((byte) 14);
                notRemind.setEndMin((byte)30);
                notRemind.setOpened(SwithMode.ON);
                command=Command.newInstance();
                command.data=DataManager.getNotremindBytes(notRemind,true);
                if(command.data != null && BluetoothWristManager.getInstance().getBleDevice()!=null){
                    BluetoothWristManager.getInstance().getBleDevice().writeData(command);
                    ToastUtils.showShort(getApplicationContext(),"设置成功");
                }
                break;
            case R.id.write_heartalarm_btn:
                if(!isEmptyDevice()) return;
                //心率超过180手环报警
                command=Command.newInstance();
                command.data=DataManager.getHeartRateAlarmBytes(SwithMode.ON,180,true);
                if(command.data != null && BluetoothWristManager.getInstance().getBleDevice()!=null){
                    BluetoothWristManager.getInstance().getBleDevice().writeData(command);
                    ToastUtils.showShort(getApplicationContext(),"设置成功");
                }
                break;
            case R.id.write_allday_btn:
                if(!isEmptyDevice()) return;
                //全天心率测试20分钟检测一次
                command=Command.newInstance();
                command.data=DataManager.getHeartRateAllDayBytes(SwithMode.ON,30,true);
                if(command.data != null && BluetoothWristManager.getInstance().getBleDevice()!=null){
                    BluetoothWristManager.getInstance().getBleDevice().writeData(command);
                    ToastUtils.showShort(getApplicationContext(),"设置成功");
                }
                break;
            case R.id.write_allday_bp_btn:
                if(!isEmptyDevice()) return;
                command=Command.newInstance();
                command.data=DataManager.getBloodPressureAllDayBytes(SwithMode.ON,5,true);
                if(command.data != null && BluetoothWristManager.getInstance().getBleDevice()!=null){
                    BluetoothWristManager.getInstance().getBleDevice().writeData(command);
                    ToastUtils.showShort(getApplicationContext(),"设置成功");
                }
                break;
            case R.id.write_msgsend_btn:
                if(!isEmptyDevice()) return;
                ArrayList<byte[]> bytes= DataManager.getNotifycationBytes(MessageMode.CALL,"三哥三哥1234",true);
                if(bytes != null) {
                    for (byte[] data : bytes) {
                        command = Command.newInstance();
                        command.data = data;
                        if (command.data != null && BluetoothWristManager.getInstance().getBleDevice() != null) {
                            BluetoothWristManager.getInstance().getBleDevice().writeData(command);
                        }
                    }
                    ToastUtils.showShort(getApplicationContext(), "设置成功");
                }
                break;
            case R.id.write_msgswitch_btn:
                if(!isEmptyDevice()) return;
                if(msgSwiths==null){
                    ToastUtils.showShort(getApplicationContext(),"请先同步手环数据");
                    return;
                }
                for (MsgSwith msgSwith:msgSwiths){
                    msgSwith.setMode(SwithMode.ON);
                }
                command=Command.newInstance();
                command.data=DataManager.getMessagePushSwitchBytes(msgSwiths,true);
                if(command.data != null && BluetoothWristManager.getInstance().getBleDevice()!=null){
                    BluetoothWristManager.getInstance().getBleDevice().writeData(command);
                    ToastUtils.showShort(getApplicationContext(),"设置成功");
                }
                break;
            case R.id.write_finddevice_btn:
                if(!isEmptyDevice()) return;
                command=Command.newInstance();
                command.data=DataManager.getFindDeviceBytes(true);
                if(command.data != null && BluetoothWristManager.getInstance().getBleDevice()!=null){
                    BluetoothWristManager.getInstance().getBleDevice().writeData(command);
                    ToastUtils.showShort(getApplicationContext(),"设置成功");
                }
                break;
            case R.id.write_unbind_btn:
                //传false不检查绑定关系直接解绑
                command=Command.newInstance();
                command.data=DataManager.getUnBindDeviceBytes(false,true);
                if(command.data != null && BluetoothWristManager.getInstance().getBleDevice()!=null){
                    BluetoothWristManager.getInstance().getBleDevice().writeData(command);
                    ToastUtils.showShort(getApplicationContext(),"解绑成功");
                }
                break;
            case R.id.send_btn:
                if(!isEmptyDevice()) return;

                String cmd=input_cmd.getText().toString();
                if(TextUtils.isEmpty(cmd)){
                    ToastUtils.showShort(getApplicationContext(),"输入命令");
                    return;
                }
                String data=input_data.getText().toString();
                if(TextUtils.isEmpty(data)||data.length()%2>0){
                    ToastUtils.showShort(getApplicationContext(),"输入正确的内容");
                    return;
                }
                byte[] value=new byte[data.length()/2];
                for(int i=0;i<value.length;i++){
                    value[i]=(byte) Integer.parseInt(data.substring(i*2,i*2+2),16);
                }

                ArrayList<byte[]> bytesList=DataManager.getWriteListBytes((byte) Integer.parseInt(cmd,16),value);
                for(byte[] bs:bytesList){
                    command=Command.newInstance();
                    command.data=bs;
                    if(command.data != null && BluetoothWristManager.getInstance().getBleDevice()!=null){
                        BluetoothWristManager.getInstance().getBleDevice().writeData(command);
                        ToastUtils.showShort(getApplicationContext(),"发送成功");
                    }
                }
                break;
            case R.id.reset_btn:
                if(!isEmptyDevice()) return;
                command=Command.newInstance();
                command.data=DataManager.getResetFactoryBytes(ResetMode.FACTORY,true);
                if(command.data != null && BluetoothWristManager.getInstance().getBleDevice()!=null){
                    BluetoothWristManager.getInstance().getBleDevice().writeData(command);
                    ToastUtils.showShort(getApplicationContext(),"设置成功");
                }
                break;
            case R.id.set_bright_screen_msg_btn:
                if(!isEmptyDevice()) return;
                command=Command.newInstance();
                command.data=DataManager.getBrightScreenSwithBytes(SwithMode.OFF,true);
                if(command.data != null && BluetoothWristManager.getInstance().getBleDevice()!=null){
                    BluetoothWristManager.getInstance().getBleDevice().writeData(command);
                    ToastUtils.showShort(getApplicationContext(),"设置成功");
                }
                break;
        }
    }

    private boolean isEmptyDevice(){
        if(BluetoothWristManager.getInstance().getBleDevice()==null){
            ToastUtils.showShort(getApplicationContext(),"请先绑定设备");
        }
        return BluetoothWristManager.getInstance().getBleDevice()!=null;
    }

    StringBuilder sb=new StringBuilder();

    @Override
    public void onDataCallback(final Callback callback) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(callback.data!=null) {
                    if(callback.mode == CallbackMode.LIGHT_SCREEN_SWITCH){
                        sb.append("亮屏消息提醒开关：").append(callback.data.toString()).append("\n");
                    }else {
                        sb.append("返回：" + callback.data.toString()).append("\n");
                    }
                    result_tv.setText(sb.toString());
                }
            }
        });

        //绑定返回
        if(callback.mode== CallbackMode.BIND_RESULT) {
            if (((BindResult)callback.data).result == ResultMode.SUCCESS) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connect_statues.setText("绑定成功,userId"+userId);
                    }
                });
            } else if(((BindResult)callback.data).result == ResultMode.WAIT){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connect_statues.setText("请在手环上点击确定");
                    }
                });
            }else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connect_statues.setText("绑定失败");
                    }
                });
            }
        }

        //消息开关
        if(callback.mode==CallbackMode.MSG_SWITCH){
            msgSwiths=(List<MsgSwith>)callback.data;
        }

        //时间样式
        if(callback.mode==CallbackMode.TIME_STYLE){
            timeStyleItems=(List<TimeStyleItem>)callback.data;
        }

        //手环显示界面
        if(callback.mode==CallbackMode.DISPLAY_ITEMS){
            displayItems=(List<DisplayItem>)callback.data;
        }

        if(callback.mode==CallbackMode.SLEEP_DATA){
            SimpleDateFormat dateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            SleepBean sleepBean=(SleepBean)callback.data;
            DebugLogger.e(TAG,"------------------------------------------------------------");
            for(Sleep sleep:sleepBean.getSleepList()){
                DebugLogger.e(TAG,"sleepStartTime="+dateFormat.format(new Date(sleep.getStartTime()))+"  sleepEndTime="+dateFormat.format(new Date(sleep.getEndTime())) + " mode:"+sleep.getMode());
            }
            DebugLogger.e(TAG,"------------------------------------------------------------");
        }

        if(callback.mode == CallbackMode.SYSNC_STATUS){
            SyncStatusBean statusBean = (SyncStatusBean)callback.data;
            if(statusBean.getStatusMode() == SyncStatusMode.START){
                isSynFinish = false;
                mHandler.removeMessages(0);
                mHandler.sendEmptyMessageDelayed(0, 5000);
            }else if(statusBean.getStatusMode() == SyncStatusMode.FINISH){
                isSynFinish = true;
                mHandler.removeMessages(0);
            }
            if((statusBean.getDataType()&SyncDataType.SPORT)>0){
                DebugLogger.d(TAG,"同步运动数据");
            }
            if((statusBean.getDataType()&SyncDataType.SLEEP)>0){
                DebugLogger.d(TAG,"同步睡眠数据");
            }
            if((statusBean.getDataType()&SyncDataType.HEARTRATE)>0){
                DebugLogger.d(TAG,"同步心率数据");
            }
            if((statusBean.getDataType()&SyncDataType.BATTERY)>0){
                DebugLogger.d(TAG,"同步电池数据");
            }
            if((statusBean.getDataType()&SyncDataType.VERSION)>0){
                DebugLogger.d(TAG,"同步版本数据");
            }
        }
    }

    @Override
    public void onCallbackFailure(String errorMsg) {

    }

    @Override
    public void onConnectFailure(String errorMsg) {

    }

    boolean isSynFinish = true;
    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0:
                    isSynFinish = true;
                    break;
            }
        }
    };

    public boolean canNext(){
        /*if(!isSynFinish){
            ToastUtils.showLong(this, "上一次还未同步完成，请等待");
        }
        return isSynFinish;*/
        return true;
    }

    int userId = 0;
    @Override
    public void onConectListener(IBleDevice.DeviceStatus status) {
        DebugLogger.e(TAG,status.name());
        if(status == IBleDevice.DeviceStatus.CONNECTED){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connect_statues.setText("连接成功");
                }
            });
        }else if(status == IBleDevice.DeviceStatus.CONNECTING){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connect_statues.setText("连接中...");
                }
            });
        }else if(status == IBleDevice.DeviceStatus.DISCOVERSERVICESING){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connect_statues.setText("正在获取服务...");
                }
            });
        }else if(status == IBleDevice.DeviceStatus.DISCONNECT || status == IBleDevice.DeviceStatus.NONE){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connect_statues.setText("未连接");
                    if(sb != null && sb.length() > 0){
                        sb.delete(0, sb.length());
                    }
                    result_tv.setText("");
                }
            });
        }else if(status==IBleDevice.DeviceStatus.DISCOVERSERVICES_COMPLETED){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    userId = 2000+new Random().nextInt(1000);
                    connect_statues.setText("获取服务成功");
                    Command command=Command.newInstance();
                    command.data=DataManager.getBindDeviceBytes(String.valueOf(20061),"A1:45:88:33:DD:22",false);
                    BluetoothWristManager.getInstance().getBleDevice().writeData(command);
                }
            });
        }
    }

    @Override
    public void onAutoConnectStateChange(boolean isAuto, int connectCount) {
        DebugLogger.d(TAG,"isAuto="+isAuto+" connectCount="+connectCount);
    }

    private void openFileChooser() {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        if(cbxIsDFU.isChecked()){
            intent.setType("application/zip");
        }else {
            intent.setType("application/octet-stream");
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, SELECT_FILE_REQ);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==SELECT_FILE_REQ&&data!=null){
            final Uri uri = data.getData();
            String type=uri.getScheme();
            if (type.equals("content")) {
                // an Uri has been returned
                mFileStreamUri = uri;
                // if application returned Uri for streaming, let's us it. Does it works?
                // FIXME both Uris works with Google Drive app. Why both? What's the difference? How about other apps like DropBox?
                final Bundle extras = data.getExtras();
                if (extras != null && extras.containsKey(Intent.EXTRA_STREAM))
                    mFileStreamUri = extras.getParcelable(Intent.EXTRA_STREAM);
                // file name and size must be obtained from Content Provider
                /*final Bundle bundle = new Bundle();
                bundle.putParcelable("uri", uri);
                getLoaderManager().restartLoader(SELECT_FILE_REQ, bundle, this);*/
                String mFilePath=uri.getPath();
                int indext=mFilePath.indexOf(":")+1;
                mFilePath= Environment.getExternalStorageDirectory().getPath()+ File.separator+mFilePath.substring(indext);
                tvFilePaht.setText(mFilePath);
            }

        }
    }

}
