package com.zhouyuming.sync;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.zhouyuming.sync.Notification.NoteDBHelper;


/**
 * Created by ZhouYuming on 2016/7/28.
 * 信息发送服务，仅在需要向PC发送网络信息时启动，在启动时会bundle中获取键为“msg”的信息
 * 然后在数据库中检查该类型的信息的用户设置
 * 检查通过后向PC发送信息，否则不做任何操作
 */
public class MsgSendService extends Service {

	private PowerManager.WakeLock wakeLock;

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private boolean settingCheck(String type, String msg){

		DBHelper dbHelper = new DBHelper();
		dbHelper.init(this);
		dbHelper.open();

		if(type.equals("SMS")){
			boolean isSMS = dbHelper.isSMS();
			if(!isSMS){
				Log.i("info", "you don't want to send it!");
				dbHelper.close();
				return false;
			}
		}else if(type.equals("TEL")){
			boolean isTel = dbHelper.isTel();
			if(!isTel){
				Log.i("info", "you don't want to send it!");
				dbHelper.close();
				return false;
			}
		}else if(type.equals("WIFI")){
			boolean isWifi = dbHelper.isWIFI();
			if(!isWifi){
				Log.i("info", "you don't want to send it!");
				dbHelper.close();
				return false;
			}
		}else if(type.equals("BELL")){
			boolean isSpeaker = dbHelper.isSpeaker();
			if(!isSpeaker){
				Log.i("info", "you don't want to send it!");
				dbHelper.close();
				return false;
			}
		}else if(type.equals("NOTE")){
			boolean isNotification = dbHelper.isNotification();
			if(!isNotification){
				Log.i("info", "you don't want to send it!");
				dbHelper.close();
				return false;
			}else {
				String appName = PackageBuilder.getNoteApp(msg);
				NoteDBHelper noteDBHelper = new NoteDBHelper();
				noteDBHelper.init(MsgSendService.this);
				noteDBHelper.open();
				boolean valid = noteDBHelper.getAppValid(appName);
				noteDBHelper.close();
				if(!valid){
					Log.i("info", "you ban this app's notification!");
					return false;
				}
			}
		}

		Log.i("info", "sending...");
		dbHelper.close();
		return true;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("info", "MsgSendService start!");
		getWakeLock();

		if(intent !=null) {
			Bundle bundle = intent.getExtras();
			String msg = bundle.getString("msg");

			String type = PackageBuilder.getMsgType(msg);

			if(settingCheck(type, msg)) {
				UDPSender.send(msg, MsgSendService.this);
			}
		}else {
			stopSelf();
		}

		releaseWakeLock();
		return super.onStartCommand(intent, Service.START_REDELIVER_INTENT, startId);
	}

	@Override
	public void onDestroy() {
		Log.i("info", "MsgSendService destroy!");
		super.onDestroy();
	}

	/**
	 * 获得唤醒锁
	 */
	private void getWakeLock(){
		PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"com.zhouyuming.sync");
		wakeLock.acquire();
	}

	/**
	 * 释放唤醒锁
	 */
	private void releaseWakeLock(){
		if(wakeLock != null) {
			wakeLock.release();
			wakeLock = null;
		}
	}
}
