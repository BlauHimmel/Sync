package com.zhouyuming.sync;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by ZhouYuming on 2016/7/29.
 * 数据库类，用来管理记录和读取本机各功能是否开启，连接状态，主机IP和端口号
 * 如果要使用这个类，首先需要依次调用init和open方法
 * 操作完成后调用close方法
 */
public class DBHelper {

	/*
	* 数据库列名 _id		connectet	sms		wifi		tel		speaker		notification		ip	port         pcname       calling
	* 数据类型  integer	  text		text		text		text		text		text			text	integer      text	  text
	* 意义	    主键		是否处于连接	是否开启 	是否开启 	是否开启		是否开启响铃	是否开启推送通知		主机地址	网络端口      PC名称        是否处于通话状态
	* 			状态		SMS监听		WIFI监听 	来电监听		模式监听
	* */

	private static final String DATABASE_CREATE = "create table status( _id integer primary key autoincrement, " +
			"connected text not null, sms text not null, wifi text not null, tel text not null, speaker text not null, notification text not null, ip text, port integer, pcname text, calling text);";

	private static final String DATABASE_NAME = "SyncDBSet";
	private static final String DATABASE_TABLE = "status";
	private static final int DATABASE_VERSION = 1;

	private Context context;

	private DatabaseHelper dbHelper;
	private SQLiteDatabase dbSet;

	/**
	 * 对数据库进行初始化操作
	 * @param context 上下文
	 */
	public void init(Context context) {
		this.context = context;
		dbHelper = new DatabaseHelper(context);
	}

	/**
	 * 开启数据库
	 */
	public void open() {
		dbSet = dbHelper.getWritableDatabase();
	}

	/**
	 * 关闭数据库
	 */
	public void close() {
		dbHelper.close();
	}

	/**
	 * 设置主机的ip地址，将其写入数据库钟
	 * @param ipAddress ip地址，如202.168.0.2
	 * @return 失败将返回0，否则返回1
	 */
	public long setAddr(String ipAddress){
		ContentValues contentValues = new ContentValues();
		contentValues.put("ip", ipAddress);
		return dbSet.update(DATABASE_TABLE, contentValues, new String("_id=1"),null);
	}

	/**
	 * 设置主机的端口号，将其写入数据库钟
	 * @param port 端口号，如8888
	 * @return 失败将返回0，否则返回1
	 */
	public long setPort(int port){
		ContentValues contentValues = new ContentValues();
		contentValues.put("port", port);
		return dbSet.update(DATABASE_TABLE, contentValues, new String("_id=1"),null);
	}

	/**
	 * 获得当前设置ip的相应的InetAddress对象
	 * @return 返回当前ip对应的InetAddress对象
	 */
	public InetAddress getIPAddr(){
		String ipAddress = new String();
		Cursor cursor = dbSet.query(DATABASE_TABLE, new String[]{"ip"}, "_id=1", null, null, null, null);
		if(cursor.moveToNext()){
			ipAddress = cursor.getString(cursor.getColumnIndex("ip"));
		}
		try {
			return InetAddress.getByName(ipAddress);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 获得当前设置的端口号
	 * @return 返回当前设置的端口号
	 */
	public int getPort(){
		int port = -1;
		Cursor cursor = dbSet.query(DATABASE_TABLE, new String[]{"port"}, "_id=1", null, null, null, null);
		if(cursor.moveToNext()){
			port = cursor.getInt(cursor.getColumnIndex("port"));
		}
		return port;
	}

	/**
	 * 设置是否开启短信监听功能，将其写入数据库
	 * @param isSMS	是否开启
	 * @return 失败将返回0，否则返回1
	 */
	public long setSMS(boolean isSMS) {
		ContentValues contentValues = new ContentValues();
		contentValues.put("sms", Boolean.toString(isSMS));
		return dbSet.update(DATABASE_TABLE, contentValues, new String("_id=1"),null);
	}

	/**
	 * 获得当前是否开启短信监听功能
	 * @return 是否开启
	 */
	public boolean isSMS(){
		boolean isSMS = false;
		Cursor cursor = dbSet.query(DATABASE_TABLE, new String[]{"sms"}, "_id=1", null, null, null, null);
		if(cursor.moveToNext()){
			isSMS = Boolean.parseBoolean(cursor.getString(cursor.getColumnIndex("sms")));
		}
		cursor.close();
		return isSMS;
	}

	/**
	 * 设置是否开启来电监听功能，将其写入数据库
	 * @param isTel	是否开启
	 * @return 失败将返回0，否则返回1
	 */
	public long setTel(boolean isTel) {
		ContentValues contentValues = new ContentValues();
		contentValues.put("tel", Boolean.toString(isTel));
		return dbSet.update(DATABASE_TABLE, contentValues, new String("_id=1"),null);
	}

	/**
	 * 获得当前是否开启来电监听功能
	 * @return 是否开启
	 */
	public boolean isTel(){
		boolean isTel = false;
		Cursor cursor = dbSet.query(DATABASE_TABLE, new String[]{"tel"}, "_id=1", null, null, null, null);
		if(cursor.moveToNext()){
			isTel = Boolean.parseBoolean(cursor.getString(cursor.getColumnIndex("tel")));
		}
		cursor.close();
		return isTel;
	}

	/**
	 * 设置是否开启响铃监听功能，将其写入数据库
	 * @param isSpeaker 是否开启
	 * @return 失败将返回0，否则返回1
	 */
	public long setSpeaker(boolean isSpeaker) {
		ContentValues contentValues = new ContentValues();
		contentValues.put("speaker", Boolean.toString(isSpeaker));
		return dbSet.update(DATABASE_TABLE, contentValues, new String("_id=1"),null);
	}

	/**
	 * 获得当前是否开启响铃监听功能
	 * @return 是否开启
	 */
	public boolean isSpeaker(){
		boolean isSpeaker = false;
		Cursor cursor = dbSet.query(DATABASE_TABLE, new String[]{"speaker"}, "_id=1", null, null, null, null);
		if(cursor.moveToNext()){
			isSpeaker = Boolean.parseBoolean(cursor.getString(cursor.getColumnIndex("speaker")));
		}
		cursor.close();
		return isSpeaker;
	}

	/**
	 * 设置是否开启wifi监听功能，将其写入数据库
	 * @param isWIFI 是否开启
	 * @return 失败将返回0，否则返回1
	 */
	public long setWIFI(boolean isWIFI) {
		ContentValues contentValues = new ContentValues();
		contentValues.put("wifi", Boolean.toString(isWIFI));
		return dbSet.update(DATABASE_TABLE, contentValues, new String("_id=1"),null);
	}

	/**
	 * 获得当前是否开启wifi监听功能
	 * @return 是否开启
	 */
	public boolean isWIFI(){
		boolean isWIFI = false;
		Cursor cursor = dbSet.query(DATABASE_TABLE, new String[]{"wifi"}, "_id=1", null, null, null, null);
		if(cursor.moveToNext()){
			isWIFI = Boolean.parseBoolean(cursor.getString(cursor.getColumnIndex("wifi")));
		}
		cursor.close();
		return isWIFI;
	}

	/**
	 * 设置是否开启推送监听功能，将其写入数据库
	 * @param isNotification 是否开启
	 * @return 失败将返回0，否则返回1
	 */
	public long setNotification(boolean isNotification) {
		ContentValues contentValues = new ContentValues();
		contentValues.put("notification", Boolean.toString(isNotification));
		return dbSet.update(DATABASE_TABLE, contentValues, new String("_id=1"),null);
	}

	/**
	 * 获得当前是否开启推送监听功能
	 * @return 是否开启
	 */
	public boolean isNotification(){
		boolean isNotification = false;
		Cursor cursor = dbSet.query(DATABASE_TABLE, new String[]{"notification"}, "_id=1", null, null, null, null);
		if(cursor.moveToNext()){
			isNotification = Boolean.parseBoolean(cursor.getString(cursor.getColumnIndex("notification")));
		}
		cursor.close();
		return isNotification;
	}

	/**
	 * 设置是当前是否属于连接状态，将其写入数据库
	 * @param isConnected 是否连接
	 * @return 失败将返回0，否则返回1
	 */
	public long setConnected(boolean isConnected) {
		Log.i("info", "set connect to " + Boolean.toString(isConnected));
		ContentValues contentValues = new ContentValues();
		contentValues.put("connected", Boolean.toString(isConnected));
		return dbSet.update(DATABASE_TABLE, contentValues, new String("_id=1"),null);
	}

	/**
	 * 获得当前是否处于连接状态
	 * @return 是否开启
	 */
	public boolean isConnected(){
		boolean isConnected = false;
		Cursor cursor = dbSet.query(DATABASE_TABLE, new String[]{"connected"}, "_id=1", null, null, null, null);
		if(cursor.moveToNext()){
			isConnected = Boolean.parseBoolean(cursor.getString(cursor.getColumnIndex("connected")));
		}
		cursor.close();
		return isConnected;
	}

	/**
	 * 设置当前PC名称
	 * @param PCName PC名称
	 * @return 失败将返回0，否则返回1
	 */
	public long setPCName(String PCName){
		ContentValues contentValues = new ContentValues();
		contentValues.put("pcname", PCName);
		return dbSet.update(DATABASE_TABLE, contentValues, new String("_id=1"), null);
	}

	/**
	 * 获得PC名称
	 * @return PC名称
	 */
	public String getPCName(){
		String PCName = new String();
		Cursor cursor = dbSet.query(DATABASE_TABLE, new String[]{"pcname"}, "_id=1", null, null, null, null);
		if(cursor.moveToNext()){
			PCName = cursor.getString(cursor.getColumnIndex("pcname"));
		}
		cursor.close();
		return PCName;
	}

	/**
	 * 设置当前通话状态
	 * @param isCalling 是否处于呼出状态
	 * @return 失败将返回0，否则返回1
	 */
	public long setCalling(boolean isCalling){
		ContentValues contentValues = new ContentValues();
		contentValues.put("calling", Boolean.toString(isCalling));
		return dbSet.update(DATABASE_TABLE, contentValues, new String("_id=1"), null);
	}

	/**
	 * 获得通话状态
	 * @return 是否处于呼出状态
	 */
	public boolean isCalling(){
		boolean isCalling = false;
		Cursor cursor = dbSet.query(DATABASE_TABLE, new String[]{"calling"}, "_id=1", null, null, null, null);
		if(cursor.moveToNext()){
			isCalling = Boolean.parseBoolean(cursor.getString(cursor.getColumnIndex("calling")));
		}
		cursor.close();
		return isCalling;
	}


	private class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		//写入初始化数据
		@Override
		public void onCreate(SQLiteDatabase db) {
			try {
				Log.i("info", "dbSet create!");
				db.execSQL(DATABASE_CREATE);
				ContentValues initialValues = new ContentValues();
				initialValues.put("connected", "false");
				initialValues.put("sms", "true");
				initialValues.put("tel", "true");
				initialValues.put("wifi", "true");
				initialValues.put("speaker", "true");
				initialValues.put("notification", "true");
				initialValues.put("calling", "false");
				db.insert(DATABASE_TABLE, null, initialValues);
			} catch(SQLException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.i("info", "dbSet upgrade!");
			db.execSQL("DROP TABLE IF EXISTS status");
			onCreate(db);
		}
	}
}
