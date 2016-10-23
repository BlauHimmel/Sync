package com.zhouyuming.sync.Notification;

import android.app.Notification;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.zhouyuming.sync.MsgSendService;
import com.zhouyuming.sync.PackageBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by ZhouYuming on 2016/8/21.
 * 推送监听服务，继承自系统NotificationListenerService服务
 */
public class NoticationListeningService extends NotificationListenerService {

	@Override
	public void onNotificationPosted(StatusBarNotification sbn) {
		Log.i("info", "a notification posted!");
		super.onNotificationPosted(sbn);
		//获取推送发送的时间
		long postTime = sbn.getPostTime();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date d = new Date(postTime);
		String date = dateFormat.format(d);
		//应用名称
		String appName = new String();
		//获得发送推送应用的包名
		String packageName = sbn.getPackageName();

		try {
			ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(packageName, 0);
			appName = applicationInfo.loadLabel(getPackageManager()).toString();

		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}

		//得到该推送携带的额外信息的Bundle
          	Bundle extras = sbn.getNotification().extras;
		//推送标题
		String notificationTitle = extras.getString(Notification.EXTRA_TITLE);
		//推送正文
		String notificationText = extras.getString(Notification.EXTRA_TEXT);
		//推送副标题（可能为空）
		String notificationSubText = extras.getString(Notification.EXTRA_SUB_TEXT);

		NotificationBean bean = new NotificationBean(appName, date, notificationSubText, notificationText, notificationTitle);

		//将信息打包准备发送
		String msg = PackageBuilder.createNotificationPackage(bean.appName, bean.date, bean.title, bean.text, bean.subText);

		Bundle bundle = new Bundle();
		bundle.putString("msg", msg);

		Intent intentService = new Intent(NoticationListeningService.this, MsgSendService.class);
		intentService.putExtras(bundle);
		startService(intentService);
	}

}
