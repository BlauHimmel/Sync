package com.zhouyuming.sync;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.android.internal.telephony.ITelephony;
import com.zhouyuming.sync.Battery.BatteryListeningBroadcastReceiver;
import com.zhouyuming.sync.Notification.APPbean;
import com.zhouyuming.sync.SMS.SMSListeningBroadcastReceiver;
import com.zhouyuming.sync.Speaker.BellListeningBroadcastReceiver;
import com.zhouyuming.sync.Tel.TelListeningBroadcastReceiver;
import com.zhouyuming.sync.WIFI.WIFIIntensityListeningBroadcastReceiver;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by ZhouYuming on 2016/7/28.
 * 监听服务，用于管理开启和关闭广播接收器，检测连接状态，接受信息
 * 需要权限 android.permission.INTERNET android.permission.CALL_PHONE android.permission.SEND_SMS android.permission.READ_CONTACTS android.permission.READ_PHONE_STATE
 */
public class ListeningService extends Service {

	private SMSListeningBroadcastReceiver smsListeningBroadcastReceiver = new SMSListeningBroadcastReceiver();
	private WIFIIntensityListeningBroadcastReceiver wifiIntensityListeningBroadcastReceiver = new WIFIIntensityListeningBroadcastReceiver();
	private BatteryListeningBroadcastReceiver batteryListeningBroadcastReceiver = new BatteryListeningBroadcastReceiver();
	private TelListeningBroadcastReceiver telListeningBroadcastReceiver = new TelListeningBroadcastReceiver();
	private BellListeningBroadcastReceiver bellListeningBroadcastReceiver = new BellListeningBroadcastReceiver();

	final private int TICK = 5000;		//心跳时间间隔
	final private int TIME = 30000;         //如果TIME ms没有收到心跳包则视为断开连接
	private volatile int counter = 0;	//计数器，记录到上一次收到心跳包的时间
	private volatile boolean interrupted = true;
	private DatagramSocket datagramSocket = null;

	private PowerManager.WakeLock wakeLock;		//唤醒锁

	//每隔TICK ms 发送一个心跳包
	private Runnable send = new Runnable() {
		@Override
		public void run() {
			Log.i("info", "thread-send start!");
			while (interrupted){
				Log.i("info", "heart beats!");
				String msg = PackageBuilder.getHeartBeat();
				UDPSender.send(msg, ListeningService.this);
				try {
					Thread.sleep(TICK);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			Log.i("info", "thread-send end!");
		}
	};

	//接受包，并做处理
	private Runnable recv = new Runnable() {

		@Override
		public void run() {
			Log.i("info", "thread-recv start!");
			//查询发送端口
			DBHelper dbHelper = new DBHelper();
			dbHelper.init(ListeningService.this);
			dbHelper.open();
			final int port = dbHelper.getPort();
			dbHelper.close();
			//接受端口为发送端口+1
			int checkPort = port + 1;

			try {
				datagramSocket = new DatagramSocket(checkPort);
			} catch (SocketException e) {
				e.printStackTrace();
			}

			while(interrupted) {
				Log.i("info", "thread-recv waiting...");
				try {
					//接受包
					DatagramPacket datagramPacket = new DatagramPacket(new byte[500], 500);
					datagramSocket.receive(datagramPacket);
					String msg = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
					//判断包类型
					String type = PackageBuilder.getMsgType(msg);
					Log.i("info", "receive : " + msg);

					if(type.equals("SMS")) {
						Log.i("info", "receive type SMS");
						//SMS类型的则回复短信
						DBHelper db = new DBHelper();
						db.init(ListeningService.this);
						db.open();
						boolean isSMS = db.isSMS();
						dbHelper.close();
						if(isSMS) {
							Log.i("info", "prepare to send message...");
							sendMessage(PackageBuilder.getSMSAddr(msg), PackageBuilder.getSMSBody(msg));
							UDPSender.send(PackageBuilder.createSmsPackage(null, PackageBuilder.getSMSAddr(msg)
								, null, PackageBuilder.SEND_SUCCESS, PackageBuilder.getSMSBody(msg)), ListeningService.this);
						}else {
							UDPSender.send(PackageBuilder.createSmsPackage(null, PackageBuilder.getSMSAddr(msg)
								, null, PackageBuilder.SEND_FAIL, PackageBuilder.getSMSBody(msg)), ListeningService.this);
						}
					}else if(type.equals("BEAT")){
						//心跳包则将计数器至0
						Log.i("info", "receive type BEAT");
						Log.i("info", "counter = " + counter);
						counter = 0;
					}else if(type.equals("TEL")){
						Log.i("info", "receive type TEL");
						DBHelper db = new DBHelper();
						db.init(ListeningService.this);
						db.open();
						boolean isTel = db.isTel();
						dbHelper.close();
						if(isTel) {
							if (PackageBuilder.getTelCode(msg).equals("03")) {
								endCall();
							} else if (PackageBuilder.getTelCode(msg).equals("04")) {
								String phoneNum = PackageBuilder.getTelAddr(msg);
								calling(phoneNum);
							}
						}else {
							UDPSender.send(PackageBuilder.createTelPackage(PackageBuilder.OPERATION_FAIL, null, PackageBuilder.getTelAddr(msg), null)
								, ListeningService.this);
						}
					}else if(type.equals("LINK")){
						if(PackageBuilder.getLinkSEQ(msg).equals("30")){
							alert(ListeningService.this);
						} else if(PackageBuilder.getLinkACK(msg).equals("0") && PackageBuilder.getLinkSEQ(msg).equals("0")){
							Log.i("info", "disconnected by pc!");
							clear();
						}
					}
				} catch (SocketException e) {

				} catch (IOException e) {
					e.printStackTrace();
				}
			};
			Log.i("info", "thread-recv end!");
		}
	};

	//计数器，每隔1s便将计数变量counter+1，当计数变量超过TIME ms时表示很长时间没有收到心跳包，断开连接
	private Runnable count = new Runnable() {
		@Override
		public void run() {
			Log.i("info", "thread-count start!");
			while (interrupted){
				try {
					counter++;
					if(counter * 1000 > TIME){
						Log.i("info", "not receive heart beats for a long time! disconnect! counter = " + counter);
						clear();
					}else {
						Thread.sleep(1000);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			Log.i("info", "thread-count end!");
		}
	};

	/**
	 * 结束服务
	 */
	private void clear(){
		interrupted = false;
		counter = 0;
		UDPSender.setStatus(false, ListeningService.this);
		datagramSocket.close();
		Log.i("info", "send broadcast to MainActivity!");
		//发送广播
		sendBroadcast(new Intent("com.zhouyuming.sync.DISCONNECT"));
		stopSelf();
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		Log.i("info", "ListeningService destroy!");
		//服务结束时注销广播接收器并且停止正在运行的线程
		unregisterReceiver(smsListeningBroadcastReceiver);
		unregisterReceiver(wifiIntensityListeningBroadcastReceiver);
		unregisterReceiver(batteryListeningBroadcastReceiver);
		unregisterReceiver(telListeningBroadcastReceiver);
		unregisterReceiver(bellListeningBroadcastReceiver);
		interrupted = false;
		counter = 0;
		UDPSender.setStatus(false, ListeningService.this);
		releaseWakeLock();
		stopForeground(true);
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification.Builder(ListeningService.this)
			.setSmallIcon(R.drawable.icon)
			.setContentTitle("InfoSync")
			.setContentText("连接已经中断")
			.build();
		notificationManager.notify(2, notification);

		try{
			datagramSocket.close();
		} catch (Exception e){
		}
		super.onDestroy();
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("info", "ListeningService start!");
		//开启服务时注册广播接收器，并开启相应的线程
		getWakeLock();
		registerReceiver(smsListeningBroadcastReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
		registerReceiver(wifiIntensityListeningBroadcastReceiver, new IntentFilter("android.net.wifi.RSSI_CHANGED"));
		registerReceiver(batteryListeningBroadcastReceiver, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("android.intent.action.PHONE_STATE");
		intentFilter.addAction("android.intent.action.NEW_OUTGOING_CALL");
		registerReceiver(telListeningBroadcastReceiver, intentFilter);
		registerReceiver(bellListeningBroadcastReceiver, new IntentFilter("android.media.RINGER_MODE_CHANGED"));

		interrupted = true;
		counter = 0;
		Thread threadSend = new Thread(send);
		Thread threadRecv = new Thread(recv);
		Thread threadCount = new Thread(count);
		threadSend.start();
		threadRecv.start();
		threadCount.start();

		Set<String> contacts = getContacts();
		for(String msg : contacts){
			UDPSender.send(msg, ListeningService.this);
		}
		Log.i("info", "completed! size = " + contacts.size());

		Intent notificationIntent = new Intent(ListeningService.this, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(ListeningService.this, 0, notificationIntent, 0);
		Notification notification = new Notification.Builder(ListeningService.this)
			.setSmallIcon(R.drawable.icon)
			.setOngoing(true)
			.setContentTitle("InfoSync")
			.setContentText("正在运行中")
			.setWhen(System.currentTimeMillis())
			.setContentIntent(pendingIntent)
			.build();
		startForeground(1, notification);


		return super.onStartCommand(intent, flags, startId);
	}

	/**
	 * 向指定号码发送指定内容的短信，短信过长时可能会拆分成若干条短信发送
	 * @param phoneNum 号码
	 * @param messageText 内容
	 */
	private void sendMessage(String phoneNum,String messageText) {
		Log.i("info", "send message to " + phoneNum + " message is " + messageText);
		//获取短信管理器
		android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
		//拆分短信内容（手机短信长度限制）
		List<String> divideContents = smsManager.divideMessage(messageText);
		for (String text : divideContents) {
			smsManager.sendTextMessage(phoneNum, null, text, null, null);
		}
	}

	private void calling(String phoneNum){
		Log.i("info", "call " + phoneNum);
		Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" +
			phoneNum));
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		try {
			startActivity(intent);
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * 挂断当前电话（如果有的话），否则什么都不做
	 */
	private void endCall() {
		Log.i("info", "end call!");
		try {
			TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
			Class<TelephonyManager> c = TelephonyManager.class;
			Method getITelephonyMethod = c.getDeclaredMethod("getITelephony",(Class[]) null);
			getITelephonyMethod.setAccessible(true);
			ITelephony iTelephony = (ITelephony) getITelephonyMethod.invoke(telephonyManager, (Object[]) null);
			iTelephony.endCall();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获得手机联系人
	 * @return 联系人CONTACT类型消息的集合
	 */
	private Set<String> getContacts() {

		Set<String> set = new HashSet<String>();

		ContentResolver resolver = this.getContentResolver();
		// 获取联系人信息
		Cursor cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
		// 循环遍历，获取每个联系人的姓名和电话号码
		while (cursor.moveToNext()) {
			// 联系人姓名
			String name = "";
			// 联系人电话
			String number = "";
			// 联系人id号码
			String ID;

			ID = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
			// 联系人姓名
			name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

			// id的整型数据
			int id = Integer.parseInt(ID);

			if (id > 0) {
				// 获取指定id号码的电话号码
				Cursor c = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
					ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + ID, null, null);
				// 遍历游标
				while (c.moveToNext()) {
					number = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
				}

				String msg = PackageBuilder.createContactPackage(name, number);
				set.add(msg);
			}
		}
		return set;
	}

	/**
	 * 用最大音量播放当前系统铃声
	 * @param context
	 */
	private void alert(Context context)
	{
		//改变媒体音量并调用系统铃声响铃
		AudioManager audioManager= (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), AudioManager.FLAG_PLAY_SOUND);
		final MediaPlayer mMediaPlayer = MediaPlayer.create(this, RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE));
		mMediaPlayer.setLooping(true);

		final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Intent cancelAlertIntent = new Intent("com.zhouyuming.sync.CANCEL_ALERT");
		PendingIntent cancelAlertPendingIntent = PendingIntent.getBroadcast(ListeningService.this, 0, cancelAlertIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.alert_layout);
		remoteViews.setOnClickPendingIntent(R.id.alertcancel_tv, cancelAlertPendingIntent);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
		builder.setContent(remoteViews).setOngoing(true).setTicker("手机寻回").setSmallIcon(R.drawable.icon);
		Notification notification = builder.build();
		notificationManager.notify(0, notification);

		mMediaPlayer.start();
		class AlertCancelBroadcastReceiver extends BroadcastReceiver{

			@Override
			public void onReceive(Context context, Intent intent) {
				mMediaPlayer.stop();
				notificationManager.cancel(0);
				unregisterReceiver(this);
			}
		}
		registerReceiver(new AlertCancelBroadcastReceiver(), new IntentFilter("com.zhouyuming.sync.CANCEL_ALERT"));
	}

	/**
	 * 获得唤醒锁
	 */
	private void getWakeLock(){
		Log.i("info" , "get wake lock!");
		PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"com.zhouyuming.sync");
		wakeLock.acquire();
	}

	/**
	 * 释放唤醒锁
	 */
	private void releaseWakeLock(){
		Log.i("info", "release wake lock!");
		if(wakeLock != null) {
			wakeLock.release();
			wakeLock = null;
		}
	}
}
