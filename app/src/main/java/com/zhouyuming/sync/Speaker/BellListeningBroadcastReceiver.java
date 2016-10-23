package com.zhouyuming.sync.Speaker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;

import com.zhouyuming.sync.MsgSendService;
import com.zhouyuming.sync.PackageBuilder;

/**
 * Created by ZhouYuming on 2016/8/20.
 * 此广播用来监听声音模式，当声音模式改变，会启动MsgSendService服务，该服务向PC端发送一条UDP数据包
 */
public class BellListeningBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		Log.i("info", "bell mode has changed!");

		AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
		//得到声音模式
		int ringerMode = audioManager.getRingerMode();

		//将信息打包准备发送
		String msg;
		switch (ringerMode) {
			case AudioManager.RINGER_MODE_NORMAL:
				msg = PackageBuilder.createBellPackage(PackageBuilder.SOUND_MODE);
				break;
			case AudioManager.RINGER_MODE_VIBRATE:
				msg = PackageBuilder.createBellPackage(PackageBuilder.VIBRATE_MODE);
				break;
			case AudioManager.RINGER_MODE_SILENT:
				msg = PackageBuilder.createBellPackage(PackageBuilder.MUTE_MODE);
				break;
			default:
				msg = null;
		}

		if(msg == null){
			return;
		}

		Bundle bundle = new Bundle();
		bundle.putString("msg", msg);

		Intent intentService = new Intent(context, MsgSendService.class);
		intentService.putExtras(bundle);
		context.startService(intentService);
	}
}
