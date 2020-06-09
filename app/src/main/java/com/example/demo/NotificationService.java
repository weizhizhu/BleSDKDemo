
package com.example.demo;


import android.app.ActivityManager;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;

import com.boe.mhealth.cmd.Command;
import com.boe.mhealth.manager.BluetoothWristManager;
import com.boe.mhealth.manager.DataManager;
import com.boe.mhealth.mode.MessageMode;
import com.boe.mhealth.utils.DebugLogger;

import java.util.ArrayList;
import java.util.List;

public class NotificationService extends NotificationListenerService {

	private static final String TAG = "NotificationService";
	private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
	private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";

	public static final String ACTION_PUSH="action.mltcode.ACTION_PUSH";

	public static boolean canRemove;

	//当系统收到新的通知后出发回调
	@Override
	public void onNotificationPosted(StatusBarNotification sbn) {
		DebugLogger.i(TAG, "Notifi onNotificationPosted ");

		canRemove=true;
		try {
			if(sbn == null){
                DebugLogger.e(TAG, "StatusBarNotification is null");
                return;
            }
			Notification notification = sbn.getNotification();

			if(notification == null){
                DebugLogger.e(TAG, "notification is null");
                return;
            }

			Bundle localBundle = NotificationCompat.getExtras(notification);

			if(localBundle == null) {
                DebugLogger.i(TAG, "localBundle is null");
                return;
            }

			if(localBundle.getInt(Notification.EXTRA_PROGRESS, 0) > 0){
                DebugLogger.i(TAG, " filter progress noti ");
                return;
            }

			String pckName = sbn.getPackageName();
			CharSequence from = localBundle.getCharSequence(Notification.EXTRA_TITLE);
			CharSequence msgStr = localBundle.getCharSequence(Notification.EXTRA_TEXT);

			DebugLogger.i(TAG, "---packageName:" + pckName);
			DebugLogger.i(TAG, "---tickerTitle:" + from);
			DebugLogger.i(TAG, "---tickerText:" + msgStr);

			if(TextUtils.isEmpty(pckName)){
                DebugLogger.i(TAG, "packageName is empty");
                return;
            }


			if(TextUtils.isEmpty(msgStr)) {
                DebugLogger.i(TAG, "msgStr is empty");
                return;
            }

			String msg = from + ":" + msgStr;

			if (pckName.equals("com.tencent.mobileqq")) {
                sendMessage(MessageMode.QQ, msg);
            } else if (pckName.equals("com.tencent.mm")) {
                sendMessage(MessageMode.WETCHAT,msg);
            } else if(pckName.equals("com.facebook.katana")){
                sendMessage(MessageMode.FACEBOOK,msg);
            } else if(pckName.equals("com.skype.rover")){
                sendMessage(MessageMode.SKYPE,msg);
            } else if(pckName.equals("com.twitter.android")){
                sendMessage(MessageMode.TWITTER,msg);
            } else if(pckName.equals("com.whatsapp")){
                sendMessage(MessageMode.WHATSAPP,msg);
            } else if(pckName.equals("jp.naver.line.android")){
                sendMessage(MessageMode.LINE,msg);
            } else if(pckName.equals("com.eg.android.AlipayGphone")){
                sendMessage(MessageMode.ALIPAY,msg);
            } else if(pckName.equals("com.taobao.taobao")){
                sendMessage(MessageMode.TAOBAO,msg);
            }
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void sendMessage(MessageMode mode, String msg){
		ArrayList<byte[]> bytes= DataManager.getNotifycationBytes(mode,msg,true);
		for(byte[] data:bytes) {
			Command command = Command.newInstance();
			command.data = data;
			if(BluetoothWristManager.getInstance().getBleDevice()!=null){
				BluetoothWristManager.getInstance().getBleDevice().writeData(command);
			}
		}
	}

	@Override
	public void onNotificationRemoved(StatusBarNotification sbn) {
		super.onNotificationRemoved(sbn);
		DebugLogger.i(TAG, "Notifi Removed " + "\n");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		DebugLogger.i(TAG, "Notifi Destrory " + "\n");
		if(canRemove) {
			toggleNotificationListenerService(getApplicationContext());
			canRemove =false;
		}
	}

	public static boolean isEnabled(Context context) {
		String pkgName = context.getPackageName();
		final String flat = Settings.Secure.getString(context.getContentResolver(), ENABLED_NOTIFICATION_LISTENERS);
		if (!TextUtils.isEmpty(flat)) {
			final String[] names = flat.split(":");
			for (int i = 0; i < names.length; i++) {
				final ComponentName cn = ComponentName.unflattenFromString(names[i]);
				if (cn != null) {
					if (TextUtils.equals(pkgName, cn.getPackageName())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public static void openNotificationAccess(Context context) {
		try {
			context.startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
		} catch (Exception e) {
		}

	}

	public static void toggleNotificationListenerService(Context context) {
		PackageManager pm = context.getPackageManager();
		pm.setComponentEnabledSetting(new ComponentName(context, NotificationService.class),
				PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

		pm.setComponentEnabledSetting(new ComponentName(context, NotificationService.class),
				PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
		DebugLogger.e(TAG,"toggleNotificationListenerService");
	}

	/**
	 * 确认NotificationMonitor是否开启
	 * @param context
	 */
	public static void ensureCollectorRunning(Context context) {
		ComponentName collectorComponent = new ComponentName(context, NotificationService.class);
		ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		boolean collectorRunning = false;
		List<ActivityManager.RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);
		if (runningServices == null ) {
			return;
		}
		for (ActivityManager.RunningServiceInfo service : runningServices) {
			if (service.service.equals(collectorComponent)) {
				if (service.pid == android.os.Process.myPid() ) {
					collectorRunning = true;
					break;
				}
			}
		}
		if (collectorRunning) {
			return;
		}
		toggleNotificationListenerService(context);
	}

}
