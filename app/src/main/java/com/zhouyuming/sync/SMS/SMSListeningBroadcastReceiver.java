package com.zhouyuming.sync.SMS;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsMessage;
import android.util.Log;

import com.zhouyuming.sync.MsgSendService;
import com.zhouyuming.sync.PackageBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by ZhouYuming on 2016/7/10.
 * 此广播用来监听是否收到短信，当收到一条短信的时候，会启动MsgSendService服务，该服务向PC端发送一条UDP数据包
 * 需要权限 android.permission.RECEIVE_SMS
 */
public class SMSListeningBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		Log.i("info", "you received a sms!");

		//获取当前收到短信的信息
		Bundle bundle = intent.getExtras();

		Object[] myObjectPDUS=(Object[]) bundle.get("pdus");

		SmsMessage[] msg=new SmsMessage[myObjectPDUS.length];

		String address=new String();
		String body=new String();
		String name;
		long date = 0;

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		//获取短信正文
		for (int i = 0; i < msg.length; i++) {
			msg[i]=SmsMessage.createFromPdu((byte[]) myObjectPDUS[i]);
			body+=msg[i].getDisplayMessageBody();
		}
		//获取电话号码
		address +=msg[0].getDisplayOriginatingAddress();
		//获取当前时间
		date += msg[0].getTimestampMillis();

		Date d= new Date(date);
		//通过电话号码到通讯录中查询是否有对应的姓名
		name = getNameByNumber(address, context);

		SMSBean bean = new SMSBean(name, address, body, dateFormat.format(d));
		bean.type = "2";

		//将信息打包准备发送
		String msgstr = PackageBuilder.createSmsPackage(bean.name, bean.number, bean.date, PackageBuilder.RECEIVED, bean.msg);

		bundle = new Bundle();
		bundle.putString("msg", msgstr);

		Intent intentService = new Intent(context, MsgSendService.class);
		intentService.putExtras(bundle);
		context.startService(intentService);
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
