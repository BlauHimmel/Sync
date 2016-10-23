package com.zhouyuming.sync.Notification;

/**
 * Created by ZhouYuming on 2016/8/22.
 */
public class NotificationBean {

	String appName;
	String date;
	String title;
	String text;
	String subText;

	public NotificationBean(String appName, String date, String subText, String text, String title) {
		this.appName = appName;
		this.date = date;
		this.subText = subText;
		this.text = text;
		this.title = title;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public void setSubText(String subText) {
		this.subText = subText;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAppName() {
		return appName;
	}

	public String getDate() {
		return date;
	}

	public String getSubText() {
		return subText;
	}

	public String getText() {
		return text;
	}

	public String getTitle() {
		return title;
	}

	@Override
	public String toString() {
		return "NotificationBean{" +
			"appName='" + appName + '\'' +
			", date='" + date + '\'' +
			", title='" + title + '\'' +
			", text='" + text + '\'' +
			", subText='" + subText + '\'' +
			'}';
	}
}
