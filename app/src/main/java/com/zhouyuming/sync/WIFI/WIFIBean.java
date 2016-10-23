package com.zhouyuming.sync.WIFI;

import java.io.Serializable;

/**
 * Created by ZhouYuming on 2016/7/28.
 */
public class WIFIBean implements Serializable{

	public String ssid;
	public int intensity;

	public WIFIBean(int intensity, String ssid) {
		this.intensity = intensity;
		this.ssid = ssid;
	}

	public int getIntensity() {
		return intensity;
	}

	public void setIntensity(int intensity) {
		this.intensity = intensity;
	}

	public String getSsid() {
		return ssid;
	}

	public void setSsid(String ssid) {
		this.ssid = ssid;
	}

	@Override
	public String toString() {
		return "WIFI{" +
			"intensity=" + intensity +
			", ssid='" + ssid + '\'' +
			'}';
	}
}
