package com.zhouyuming.sync;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ZhouYuming on 2016/7/18.
 * 用于构建发送的网络数据包
 */
public class PackageBuilder {

	public static final int VERIFICATION =100;			//验证码验证连接
	public static final int PHONE_REQUEST_FOR_CONNECTION = 101;	//移动端请求连接
	public static final int CONNECTION_TEST = 102;			//连接性测试

	public static final int SOUND_MODE = 103;		//铃声模式
	public static final int VIBRATE_MODE = 104;		//震动模式
	public static final int MUTE_MODE = 105;		//静音模式

	public static final int CHARGING = 106;			//充电中
	public static final int UNCHARGING = 107;		//未充电

	public static final int SEND_FAIL = 115;		//发送失败，因为功能被禁用
	public static final int SEND_SUCCESS = 116;		//发送成功
	public static final int RECEIVED = 108;			//接收
	public static final int SEND = 109;			//发送
	public static final int NULL_TYPE = 110;		//无类型

	public static final int ANSWER_THE_CALL = 111;			//接听
	public static final int HANG_UP_THE_CALL = 112;			//挂断
	public static final int CALLING = 113;				//来电
	public static final int CALL = 114;			//对方挂断电话时
	public static final int OPERATION_FAIL = 117;		//操作失败

	public static final int REQUEST_FOR_FILE_TRANSFER = 1;			//请求传输
	public static final int ACCEPT_TRANSFER = 2;    			//接受传输
	public static final int REJECT_TRANSFER = 3;				//拒绝传输
	public static final int TRANSFER_SUCCEED = 4;				//传输成功
	public static final int TRANSFER_FAIL = 5;				//传输失败

	/**
	 * 为指定类型的LINK消息生成随机数
	 * @param MODE LINK消息类型:VERIFICATION,PHONE_REQUEST_FOR_CONNECTION,CONNECTION_TEST
	 * @return 返回相应的随机数，如果传入其它的数值则返回-1
	 */
	public static int random(int MODE){
		switch (MODE){
			case VERIFICATION:
				return (int)(Math.random() * 89997 + 10000);

			case PHONE_REQUEST_FOR_CONNECTION:
				return (int)(Math.random() * 897 + 100);

			case CONNECTION_TEST:
				return (int)(Math.random() * 8997 + 100);

			default:
				return -1;
		}
	}

	/**
	 * 创建一个LINK消息，格式如下
	 * LINK{SEQ=?,ACK=?,MSG=?}
	 * @param SEQ SEQ码
	 * @param ACK ACK码
	 * @param MSG 包附带的信息
	 * @return 返回一个该消息的字符串
	 */
	public static String createLinkPackage(int SEQ, int ACK, String MSG){
		String str = "LINK{SEQ=" + SEQ + ",ACK=" + ACK + ",MSG=" + MSG + "}";
		return str;
	}

	/**
	 * 得到一个LINK包中的ACK码
	 * @param msg LINK数据包
	 * @return ACK码
	 */
	public static String getLinkACK(String msg){
		Pattern pattern = Pattern.compile("(\\d*)(?=,MSG)");
		Matcher matcher = pattern.matcher(msg);
		if(matcher.find()) {
			return matcher.group();
		}else{
			return "NULL";
		}
	}

	/**
	 * 得到一个LINK包中的SEQ码
	 * @param msg LINK数据包
	 * @return SEQ码
	 */
	public static String getLinkSEQ(String msg){
		Pattern pattern = Pattern.compile("(\\d*)(?=,ACK)");
		Matcher matcher = pattern.matcher(msg);
		if(matcher.find()) {
			return matcher.group();
		}else{
			return "NULL";
		}
	}

	/**
	 * 得到一个LINK包中的MSG码
	 * @param msg LINK数据包
	 * @return MSG码
	 */
	public static String getLinkMSG(String msg){
		Pattern pattern = Pattern.compile("(?<=MSG=)(.*)(?=\\})");
		Matcher matcher = pattern.matcher(msg);
		if(matcher.find()) {
			return matcher.group();
		}else{
			return "NULL";
		}
	}

	/**
	 * 返回一个BELL类型的消息，格式如下
	 * BELL{?}
	 * @param BELL_MODE 响铃模式:SOUND_MODE,VIBRATE_MODE,MUTE_MODE
	 * @return 返回一个该消息的字符串，如果传入其它的数值将返回null
	 */
	public static String createBellPackage(int BELL_MODE){
		String str;
		switch (BELL_MODE){
			case SOUND_MODE:
				str = "BELL{02}";
				return str;

			case VIBRATE_MODE:
				str = "BELL{01}";
				return str;

			case MUTE_MODE:
				str = "BELL{00}";
				return str;

			default:
				return null;
		}
	}

	/**
	 * 返回一个BATTERY类型的消息，格式如下
	 * BATTERY{NUM=?,STATE=?}
	 * @param num 剩余电量百分比
	 * @param STATE 充电状态:CHARGING,UNCHARGING
	 * @return 返回一个该消息的字符串，如果传入其它的数值将返回null
	 */
	public static String createBatteryPackage(int num, int STATE){
		String str;
		switch (STATE){
			case CHARGING:
				str = "BATTERY{NUM=" + num + ",STATE=1}";
				return str;

			case UNCHARGING:
				str = "BATTERY{NUM=" + num + ",STATE=0}";
				return str;

			default:
				return null;
		}
	}

	/**
	 * 返回一个FILE类型的消息,使用指定代码 1 请求传输， 2 接受传输，3 拒绝传输，4 传输成功，5 传输失败,格式如下
	 * FILE{SEQ=?,ACK=?,MSG=?}
	 * ACK和SEQ码可以引用：
	 * REQUEST_FOR_FILE_TRANSFER,ACCEPT_TRANSFER,REJECT_TRANSFER,TRANSFER_SUCCEED,TRANSFER_FAIL
	 * @param SEQ SEQ码
	 * @param ACK ACK码或者文件大小
	 * @param MSG 文件名
	 * @return 返回一个该消息的字符串
	 */
	public static String createFilePackage(int SEQ, int ACK, String MSG){
		String str = "FILE{SEQ=" + SEQ + ",ACK=" + ACK + ",MSG=" + MSG + "}";
		return str;
	}

	/**
	 * 返回一个WIFI类型的消息，格式如下
	 * WIFI{SSID=?,NUM=?}
	 * @param SSID WIFI名称
	 * @param num 信号强度 1 - 5
	 * @return 返回一个该消息的字符串
	 */
	public static String createWifiPackage(String SSID, int num){
		String str = "WIFI{SSID=" + SSID + ",NUM=" + num +"}";
		return str;
	}

	/**
	 * 返回一个SMS类型的消息，格式如下
	 * SMS{NAME=?,ADDR=?,DATE=?,TYPE=?,BODY=?}
	 * @param name 联系人姓名
	 * @param addr 联系人号码
	 * @param date 短信收发的日期，YYYY-MM-DD HH:MM:SS
	 * @param type 短信类型:RECEIVED,SEND,NULL_TYPE,SEND_FAIL,SEND_SUCCESS
	 * @param body 短信内容
	 * @return 返回一个该消息的字符串，如果传入其它的数值将返回null
	 */
	public static String createSmsPackage(String name, String addr, String date, int type, String body){
		String str;
		if(name == null){
			name="null";
		}
		switch (type){

			case SEND_FAIL:
				str = "SMS{NAME=" + name + ",ADDR=" + addr + ",DATE=" + date + ",TYPE=4" + ",BODY=" + body + "}";
				return str;

			case SEND_SUCCESS:
				str = "SMS{NAME=" + name + ",ADDR=" + addr + ",DATE=" + date + ",TYPE=3" + ",BODY=" + body + "}";
				return str;

			case RECEIVED:
				str = "SMS{NAME=" + name + ",ADDR=" + addr + ",DATE=" + date + ",TYPE=2" + ",BODY=" + body + "}";
				return str;

			case SEND:
				str = "SMS{NAME=" + name + ",ADDR=" + addr + ",DATE=" + date + ",TYPE=1" + ",BODY=" + body + "}";
				return str;

			case NULL_TYPE:
				str = "SMS{NAME=" + name + ",ADDR=" + addr + ",DATE=" + date + ",TYPE=NULL" + ",BODY=" + body + "}";
				return str;

			default:
				return null;
		}
	}

	/**
	 * 返回SMS信息中的电话号码
	 * @param msg SMS信息字符串
	 * @return 电话号码字符串
	 */
	public static String getSMSAddr(String msg){
		Pattern pattern = Pattern.compile("(?<=ADDR=)(.*)(?=,DATE)");
		Matcher matcher = pattern.matcher(msg);
		if(matcher.find()) {
			return matcher.group();
		}else{
			return "NULL";
		}
	}

	/**
	 * 返回SMS信息中的短信内容
	 * @param msg SMS信息字符串
	 * @return 短信内容字符串
	 */
	public static String getSMSBody(String msg){
		Pattern pattern = Pattern.compile("(?<=BODY=)(.*)(?=\\})");
		Matcher matcher = pattern.matcher(msg);
		if(matcher.find()) {
			return matcher.group();
		}else{
			return "NULL";
		}
	}

	/**
	 * 返回一个TEL类型的消息，格式如下
	 * TEL{CODE=?,NAME=?,ADDR=?,DATE=?}
	 * @param code TEL消息类型:CALLING,ANSWER_THE_CALL,HANG_UP_THE_CALL,OPERATION_FAIL
	 * @param name 联系人姓名
	 * @param addr 联系人号码
	 * @param date 通话时间
	 * @return 返回一个该消息的字符串，如果传入其它的数值将返回null
	 */
	public static String createTelPackage(int code, String name, String addr, String date){
		String str;
		if(name == null){
			name="null";
		}
		switch (code){

			case CALLING:
				str = "TEL{CODE=01" + ",NAME=" + name + ",ADDR=" + addr + ",DATE=" + date + "}";
				return str;

			case ANSWER_THE_CALL:
				str = "TEL{CODE=02" + ",NAME=" + name + ",ADDR=" + addr + ",DATE=" + date + "}";
				return str;

			case HANG_UP_THE_CALL:
				str = "TEL{CODE=03" + ",NAME=" + name + ",ADDR=" + addr + ",DATE=" + date + "}";
				return str;

			case CALL:
				str = "TEL{CODE=04" + ",NAME=" + name + ",ADDR=" + addr + ",DATE=" + date + "}";
				return str;

			case OPERATION_FAIL:
				str = "TEL{CODE=05" + ",NAME=" + name + ",ADDR=" + addr + ",DATE=" + date + "}";
				return str;

			default:
				return null;
		}
	}

	/**
	 * 得到Tel消息中的Code
	 * @param msg Tel消息
	 * @return Code码
	 */
	public static String getTelCode(String msg){
		Pattern pattern = Pattern.compile("(?<=CODE=)(.*)(?=\\,NAME)");
		Matcher matcher = pattern.matcher(msg);
		if(matcher.find()) {
			return matcher.group();
		}else{
			return "NULL";
		}
	}

	/**
	 * 得到Tel消息中的号码
	 * @param msg Tel消息
	 * @return 电话号码
	 */
	public static String getTelAddr(String msg){
		Pattern pattern = Pattern.compile("(?<=ADDR=)(.*)(?=\\,DATE)");
		Matcher matcher = pattern.matcher(msg);
		if(matcher.find()) {
			return matcher.group();
		}else{
			return "NULL";
		}
	}

	/**
	 * 返回一个CONTACT类型的消息，格式如下
	 * CONTACT{NAME=?,TEL=?}
	 * @param name 联系人姓名
	 * @param tel 联系号码
	 * @return 返回一个该消息的字符串
	 */
	public static String createContactPackage(String name, String tel){
		String str;

		str = "CONTACT{NAME=" + name + ",TEL=" + tel + "}";

		return str;
	}

	/**
	 * 返回一个NOTIFICATION类型的消息，格式如下：
	 * NOTE{APP=?,TITLE=?,TEXT=?,SUBTEXT=?}
	 * @param appName 发送推送的app名称
	 * @param date 发送推送的时间
	 * @param title 标题
	 * @param text 正文
	 * @param subText 第三行正文（低端机上一般不显示，请查阅官方文档）
	 * @return 返回一个该消息的字符串
	 */
	public static String createNotificationPackage(String appName, String date, String title, String text, String subText){
		String str;

		str = "NOTE{APP=" + appName + ",TITLE=" + title + ",TEXT=" + text + ",SUBTEXT=" + subText + "}";

		return str;
	}

	/**
	 * 返回一个Note信息中的app名
	 * @param msg Note信息
	 * @return app名
	 */
	public static String getNoteApp(String msg){
		Pattern pattern = Pattern.compile("(?<=APP=)(.*)(?=\\,TITLE)");
		Matcher matcher = pattern.matcher(msg);
		if(matcher.find()) {
			return matcher.group();
		}else{
			return "NULL";
		}
	}

	/**
	 * 返回一个心跳包，格式如下：
	 * BEAT{}
	 * @return 返回一个该消息的字符串
	 */
	public static String getHeartBeat(){
		return new String("BEAT{}");
	}

	/**
	 * 获得当前消息的类型
	 * @param msg 消息字符串
	 * @return 返回当前消息的类型
	 */
	public static String getMsgType(String msg){
		Pattern pattern = Pattern.compile("(.*)(?=\\{)");
		Matcher matcher = pattern.matcher(msg);
		if(matcher.find()) {
			return matcher.group();
		}else{
			return "NULL";
		}
	}
}
