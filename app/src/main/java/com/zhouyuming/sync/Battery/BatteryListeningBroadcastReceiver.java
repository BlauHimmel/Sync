package com.zhouyuming.sync.Battery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;

import com.zhouyuming.sync.MsgSendService;
import com.zhouyuming.sync.PackageBuilder;

/**
 * Created by ZhouYuming on 2016/7/11.
 * 此广播用来监听电量变化，当电量发生变化时候，会启动MsgSendService服务，该服务向PC端发送一条UDP数据包
 */
public class BatteryListeningBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		Log.i("info", "battery status changed!");
		//获取当前电量
		int level = intent.getIntExtra("level", 0);
		//电量的总刻度
		int scale = intent.getIntExtra("scale", 100);
		//把它转成百分比
		float percent = (level * 100) / scale;
		//获得当前电池状态（充电或者未充电）
		int status = intent.getIntExtra("status", BatteryManager.BATTERY_STATUS_DISCHARGING);

		String strstatus;
		if(status == BatteryManager.BATTERY_STATUS_CHARGING){
			strstatus = "charging";
		}else{
			strstatus = "discharging";
		}

		BatteryBean bean = new BatteryBean(percent, strstatus);

		//将信息打包准备发送
		String msg;
		if(bean.status.equals("charging")){
			msg = PackageBuilder.createBatteryPackage((int)bean.percent, PackageBuilder.CHARGING);
		}else {
			msg = PackageBuilder.createBatteryPackage((int)bean.percent, PackageBuilder.UNCHARGING);
		}

		Bundle bundle = new Bundle();
		bundle.putString("msg", msg);

		Intent intentService = new Intent(context, MsgSendService.class);
		intentService.putExtras(bundle);
		context.startService(intentService);
	}
}
