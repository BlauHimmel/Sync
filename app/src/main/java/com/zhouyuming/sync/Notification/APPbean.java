package com.zhouyuming.sync.Notification;

import android.graphics.drawable.Drawable;

/**
 * Created by ZhouYuming on 2016/8/27.
 */
public class APPbean {

	public String appName;
	public String packageName;
	public Drawable icon;

	public APPbean(String appName, String packageName) {
		this.appName = appName;
		this.packageName = packageName;
		this.icon = null;
	}

	public APPbean(String appName, Drawable icon, String packageName) {
		this.appName = appName;
		this.icon = icon;
		this.packageName = packageName;
	}
}
