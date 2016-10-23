package com.zhouyuming.sync.SMS;

import java.io.Serializable;

/**
 * Created by ZhouYuming on 2016/7/28.
 */
public class SMSBean implements Serializable {

	public String name;
	public String number;
	public String msg;
	public String date;
	public String type;	//2:receive 1:send

	public SMSBean(String name, String number, String msg, String date) {
		this.name = name;
		this.number = number;
		this.msg = msg;
		this.date = date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDate() {
		return date;
	}

	public String getMsg() {
		return msg;
	}

	public String getName() {
		return name;
	}

	public String getNumber() {
		return number;
	}

	public String getType() {
		return type;
	}

	@Override
	public String toString() {
		return "SMS{" +
			"name='" + name + '\'' +
			", number='" + number + '\'' +
			", msg='" + msg + '\'' +
			", date='" + date + '\'' +
			", type='" + type + '\'' +
			'}';
	}

}
