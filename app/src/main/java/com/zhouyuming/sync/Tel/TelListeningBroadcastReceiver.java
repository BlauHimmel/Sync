package com.zhouyuming.sync.Tel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.zhouyuming.sync.DBHelper;
import com.zhouyuming.sync.MsgSendService;
import com.zhouyuming.sync.PackageBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by ZhouYuming on 2016/8/20.
 * 此广播用来监听呼叫，当呼入呼出接听或者挂断时，会启动MsgSendService服务，该服务向PC端发送一条UDP数据包
 * 需要权限 android.permission.PROCESS_OUTGOING_CALLS android.permission.READ_PHONE_STATE
 */
public class TelListeningBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		//去电
		Log.i("info", "ACTION: " + intent.getAction());
		if(intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)){

			TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			String number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
			String name;
			String date;

			//获取当前时间
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date d = new Date(System.currentTimeMillis());
			date = dateFormat.format(d);
			//通过电话号码到通讯录中查询姓名
			name = getNameByNumber(number, context);

			TelBean bean = new TelBean(number, date, name);

			//设置为呼出状态
			DBHelper dbHelper = new DBHelper();
			dbHelper.init(context);
			dbHelper.open();
			dbHelper.setCalling(true);
			dbHelper.close();

			Log.i("info", "call someone...");

			//将信息打包准备发送
			//CODE = 04
			String msg = PackageBuilder.createTelPackage(PackageBuilder.CALL, bean.name, bean.addr, bean.date);

			Bundle bundle = new Bundle();
			bundle.putString("msg", msg);

			Intent intentService = new Intent(context, MsgSendService.class);
			intentService.putExtras(bundle);
			context.startService(intentService);
		}else {
			//来电
			TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

			String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
			String name;
			String date;
			//获得当前时间
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date d = new Date(System.currentTimeMillis());//获取当前时间
			date = dateFormat.format(d);
			//通过电话号码到通讯录中查询姓名
			name = getNameByNumber(number, context);

			//获得通话状态，是否为呼出
			DBHelper dbHelper = new DBHelper();
			dbHelper.init(context);
			dbHelper.open();
			boolean isCalling = dbHelper.isCalling();
			dbHelper.close();

			TelBean bean = new TelBean(number, date, name);
			//得到当前的呼叫状态
			int state = telephony.getCallState();
			switch (state) {
				//来电
				case TelephonyManager.CALL_STATE_RINGING:
					Log.i("info", "a calling is waiting...");
					//CODE = 01
					String msg1 = PackageBuilder.createTelPackage(PackageBuilder.CALLING, bean.name, bean.addr, bean.date);

					Bundle bundle = new Bundle();
					bundle.putString("msg", msg1);

					Intent intentService = new Intent(context, MsgSendService.class);
					intentService.putExtras(bundle);
					context.startService(intentService);

					break;

				//挂断
				case TelephonyManager.CALL_STATE_IDLE:
					Log.i("info", "hand off the call...");
					//CODE = 03
					String msg2 = PackageBuilder.createTelPackage(PackageBuilder.HANG_UP_THE_CALL, bean.name, bean.addr, bean.date);

					Bundle bundle2 = new Bundle();
					bundle2.putString("msg", msg2);

					if(isCalling){
						dbHelper = new DBHelper();
						dbHelper.init(context);
						dbHelper.open();
						dbHelper.setCalling(false);
						dbHelper.close();
						return;
					}

					Intent intentService2 = new Intent(context, MsgSendService.class);
					intentService2.putExtras(bundle2);
					context.startService(intentService2);

					break;

				//接听
				case TelephonyManager.CALL_STATE_OFFHOOK:
					Log.i("info", "pick up the call...");
					//CODE = 02
					String msg3 = PackageBuilder.createTelPackage(PackageBuilder.ANSWER_THE_CALL, bean.name, bean.addr, bean.date);

					if (isCalling){
						return;
					}

					Bundle bundle3 = new Bundle();
					bundle3.putString("msg", msg3);

					Intent intentService3 = new Intent(context, MsgSendService.class);
					intentService3.putExtras(bundle3);
					context.startService(intentService3);

					break;
			}
		}
	}

	/**
	 * 通过号码到通讯录中查询姓名，需要系统权限 android.permission.READ_CONTACTS
	 * @param number 电话号码
	 * @param context 上下文
	 * @return 姓名，如果查询不到则返回“”
	 */
	private String getNameByNumber(String number, Context context){
		String name = new String();
		//通过号码去数据库查找该号码是否在联系人列表中有对应的名称
		Uri conUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
		Cursor cursorCon = context.getContentResolver().query(conUri, new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME}, ContactsContract.CommonDataKinds.Phone.NUMBER + "=? "
			, new String[]{number}, null);

		//要做三次查询，因为号码可能是156XXXXXXXX 或者 +86156XXXXXXXX
		if (cursorCon.moveToNext()) {
			name = cursorCon.getString(cursorCon.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
		}
		else{
			cursorCon.close();
			cursorCon = context.getContentResolver().query(conUri, new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME}, ContactsContract.CommonDataKinds.Phone.NUMBER + "=? "
				, new String[]{number.substring(3)}, null);
			if (cursorCon.moveToNext()) {
				name = cursorCon.getString(cursorCon.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
			}else {
				cursorCon.close();
				cursorCon = context.getContentResolver().query(conUri, new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME}, ContactsContract.CommonDataKinds.Phone.NUMBER + "=? "
					, new String[]{("+86") + number}, null);
				if(cursorCon.moveToNext()) {
					name = cursorCon.getString(cursorCon.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
				}
			}
		}
		cursorCon.close();
		return name;
	}
}
