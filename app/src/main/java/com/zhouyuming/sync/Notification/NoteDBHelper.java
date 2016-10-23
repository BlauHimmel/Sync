package com.zhouyuming.sync.Notification;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.List;

/**
 * Created by ZhouYuming on 2016/8/27.
 * 数据库类，用来管理记录和读取本机应用的推送监听功能是否开启
 * 如果要使用这个类，首先需要依次调用init和open方法
 * 操作完成后调用close方法
 */
public class NoteDBHelper {

	/*
	* 数据库列名 _id		  app	        valid
	* 数据类型  integer	  text		text
	* 意义	    主键		应用名称		是否开启
	* 					推送监听
	* */

	private static final String DATABASE_CREATE = "create table note( _id integer primary key autoincrement, app text not null, valid text not null);";

	private static final String DATABASE_NAME = "SyncDBNote";
	private static final String DATABASE_TABLE = "note";
	private static final int DATABASE_VERSION = 1;

	private Context context;

	private DatabaseHelper dbHelper;
	private SQLiteDatabase dbNote;

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
		dbNote = dbHelper.getWritableDatabase();
	}

	/**
	 * 更改APP设置
	 * @param appName app名称
	 * @param valid	是否启用推送通知
	 * @return 失败将返回0，否则返回1
	 */
	public long setApp(String appName, boolean valid){
		ContentValues contentValues = new ContentValues();
		contentValues.put("valid", Boolean.toString(valid));
		return dbNote.update(DATABASE_TABLE, contentValues, new String("app=?"),new String[]{appName});
	}

	/**
	 * 返回一个APP的设置信息
	 * @param appName app名
	 * @return 是否有效
	 */
	public boolean getAppValid(String appName){
		boolean valid = false;
		Cursor cursor = dbNote.query(DATABASE_TABLE, new String[]{"valid"}, "app=?", new String[]{appName}, null, null, null);
		if(cursor.moveToNext()){
			valid = Boolean.parseBoolean(cursor.getString(cursor.getColumnIndex("valid")));
		}
		cursor.close();
		return valid;
	}


	/**
	 * 关闭数据库
	 */
	public void close() {
		dbHelper.close();
	}

	private class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		//写入初始化数据
		@Override
		public void onCreate(SQLiteDatabase db) {
			try {
				Log.i("info", "dbNote create!");
				db.execSQL(DATABASE_CREATE);

				PackageManager packageManager = context.getPackageManager();
				List<ApplicationInfo> applicationInfos = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
				for(ApplicationInfo appInfo : applicationInfos){
					String appName = appInfo.loadLabel(packageManager).toString();
					ContentValues contentValues = new ContentValues();
					contentValues.put("app", appName);
					contentValues.put("valid", Boolean.toString(false));
					db.insert(DATABASE_TABLE, null, contentValues);
				}

			} catch(SQLException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.i("info", "dbNote upgrade!");
			db.execSQL("DROP TABLE IF EXISTS status");
			onCreate(db);
		}
	}
}
