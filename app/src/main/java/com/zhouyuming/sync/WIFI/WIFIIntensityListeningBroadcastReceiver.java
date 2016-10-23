package com.zhouyuming.sync.WIFI;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

import com.zhouyuming.sync.MsgSendService;
import com.zhouyuming.sync.PackageBuilder;

/**
 * Created by ZhouYuming on 2016/7/11.
 * 此广播用来监听WIFI变化，当WIFI名称或者信号强度，会启动MsgSendService服务，该服务向PC端发送一条UDP数据包
 * 需要权限 android.permission.ACCESS_WIFI_STATE
 */
public class WIFIIntensityListeningBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		Log.i("info", "wifi intensity changed!");

		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wifiManager.getConnectionInfo();
		//如果有WIFI连接（BSSID不为null）
		if (info.getBSSID() != null) {
			//获得信号强度（0-5）
			int intensity = WifiManager.calculateSignalLevel(info.getRssi(), 5);
			//获得WIFI名称
			String ssid = info.getSSID();

			WIFIBean bean = new WIFIBean(intensity, ssid);

			//将信息打包准备发送
			String msg = PackageBuilder.createWifiPackage(bean.ssid, bean.intensity);

			Bundle bundle = new Bundle();
			bundle.putString("msg", msg);

			Intent intentService = new Intent(context, MsgSendService.class);
			intentService.putExtras(bundle);
			context.startService(intentService);
		}
	}
}
