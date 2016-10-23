package com.zhouyuming.sync.Tel;

/**
 * Created by ZhouYuming on 2016/8/20.
 */
public class TelBean {

	public String name;
	public String addr;
	public String date;

	public TelBean(String addr, String date, String name) {
		this.addr = addr;
		this.date = date;
		this.name = name;
	}

	@Override
	public String toString() {
		return "TelBean{" +
			"addr='" + addr + '\'' +
			", name='" + name + '\'' +
			", date='" + date + '\'' +
			'}';
	}

	public void setAddr(String addr) {
		this.addr = addr;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAddr() {
		return addr;
	}

	public String getDate() {
		return date;
	}

	public String getName() {
		return name;
	}
}
