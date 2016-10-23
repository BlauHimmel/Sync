package com.zhouyuming.sync.Battery;

import java.io.Serializable;

/**
 * Created by ZhouYuming on 2016/7/28.
 */
public class BatteryBean implements Serializable{

	public float percent;
	public String status;

	public BatteryBean(float percent, String status) {
		this.percent = percent;
		this.status = status;
	}

	public float getPercent() {
		return percent;
	}

	public void setPercent(float percent) {
		this.percent = percent;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return "Battery{" +
			"percent=" + percent +
			"%, status='" + status + '\'' +
			'}';
	}
}
