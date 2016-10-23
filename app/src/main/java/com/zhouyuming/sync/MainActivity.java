package com.zhouyuming.sync;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.zhouyuming.sync.Notification.NotificationSelectActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

	private EditText verifyCodeEditText;	//输入连接码的编辑框
	private Button linkBtn;			//连接按钮
	private Button disconnectBtn;		//断开按钮
	private LinearLayout verifyLinearLayout;	//链接码编辑框和连接按钮的布局
	private LinearLayout linkstateLineaLayout;	//连接状态的布局
	private Switch linkSwitch;			//toolbar上显示连接状态的switch控件
	private MenuItem linkMenuItem;			//toolbar上显示连接状态的switch控件的MenuItem对象
	private MenuItem shareMenuItem;			//分享按钮
	private Button fileTransBtn;			//传输文件按钮
	private FloatingActionButton optionFABtn;	//右下角的设置按钮
	private CoordinatorLayout coordinatorLayout;	//全屏幕的一个CoordinatorLayout
	private TextView cloudTextView;			//点击设置按钮后用来起遮罩作用的TextView

	private FloatingActionButton optionFABsms;	//设置菜单中的短信子按钮
	private FloatingActionButton optionFABtel;	//设置菜单中的通话子按钮
	private FloatingActionButton optionFABwifi;	//设置菜单中的网络子按钮
	private FloatingActionButton optionFABspeaker;	//设置菜单中的响铃子按钮
	private FloatingActionButton optionFABnotification;//设置菜单中的推送子按钮

	//按钮旁边的文本框
	private TextView settingTextView;
	private TextView smsTextView;
	private TextView telTextView;
	private TextView wifiTextView;
	private TextView speakerTextView;
	private TextView notificationTextView;
	private TextView pcNameTextView;

	//设置按钮是否处于开启状态
	private boolean fabOpened = false;

	//申请权限时使用的权限码
	private final int MY_TEL_PERMISSION = 111;
	private final int MY_SMS_PERMISSION = 112;
	private final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 113;

	//分本
	private final String SHARE_INFORMATION = "我觉得这个应用很不错，向你推荐!";
	private final String GET_NOTIFICATION_SET = "为了获得推送信息，请让本程序能够获取通知！";
	private final String SELF_BOOT = "如果你安装有安全卫士或者你的系统UI中有自启动管理功能，请设置本程序为自动启动，这样程序在后台才能正常工作！";
	private final String VARIFY_CODE_LENGTH_ERROR = "链接码的长度不正确！";
	private final String LINK_SUCCESS = "连接成功！";
	private final String LINK_FAILED = "连接失败！";
	private final String LINK_DISCONNECT = "连接中断！";
	private final String CANCEL = "取消";
	private final String OK = "确定";
	private final String GO_TO_SET = "前往设置";
	private final String ON = "ON";
	private final String OFF = "OFF";
	private final String CLICK_MORE_SET = "点击详细设置";
	private final String SHARE_TO = "分享到";
	private final String NO_PERMISSION_SMS = "无法获得相应权限，将关闭短信监听功能！";
	private final String NO_PERMISSION_TEL = "无法获得相应权限，将关闭来电监听功能！";
	private final String NO_PERMISSION_ALL = "无法获得相应权限，将关闭程序！";

	//手机名
	private String phoneInfo;

	//数据库类
	private DBHelper dbHelper = new DBHelper();

	//断开连接监听
	private Handler disconnectHandler;
	private DisconnectBroadcastReceiver disconnectBroadcastReceiver;

	class DisconnectHandler extends Handler{

		@Override
		public void handleMessage(Message msg) {
			Log.i("info", "MainActivity receive broadcast from ListeningService!");
			super.handleMessage(msg);
			//隐藏连接图标，显示输入框和按钮
			disconnected();
			//更改ActionBar上的连接开关
			linkSwitch.setChecked(false);

			//弹出错误信息，并且为了防止动画效果错乱，在SnackBar弹出时禁用设置按钮，SnackBar收回后恢复
			Snackbar.make(coordinatorLayout, LINK_DISCONNECT, Snackbar.LENGTH_SHORT).setCallback(new Snackbar.Callback() {
				@Override
				public void onDismissed(Snackbar snackbar, int event) {
					optionFABtn.setEnabled(true);
					super.onDismissed(snackbar, event);
				}

				@Override
				public void onShown(Snackbar snackbar) {
					optionFABtn.setEnabled(false);
					super.onShown(snackbar);
				}
			}).setAction(CANCEL, new View.OnClickListener() {
				@Override
				public void onClick(View v) {
				}
			}).show();
		}
	}

	class DisconnectBroadcastReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			disconnectHandler.sendEmptyMessage(0);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
		getWindow().setEnterTransition(new Slide(Gravity.LEFT).setDuration(500));
		getWindow().setExitTransition(new Slide(Gravity.RIGHT).setDuration(500));
		getWindow().setReenterTransition(new Slide(Gravity.LEFT).setDuration(500));
		getWindow().setReturnTransition(new Slide(Gravity.RIGHT).setDuration(500));

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//开启监听断开连接广播
		disconnectHandler = new DisconnectHandler();
		disconnectBroadcastReceiver = new DisconnectBroadcastReceiver();
		registerReceiver(disconnectBroadcastReceiver, new IntentFilter("com.zhouyuming.sync.DISCONNECT"));

		//开启数据库
		dbHelper.init(MainActivity.this);
		dbHelper.open();

		//获取控件变量
		verifyCodeEditText = (EditText) findViewById(R.id.id_verify_edittext);
		linkBtn = (Button) findViewById(R.id.id_link_btn);
		disconnectBtn = (Button) findViewById(R.id.id_disconnect_btn);
		verifyLinearLayout = (LinearLayout) findViewById(R.id.id_verify_linearlayout);
		linkstateLineaLayout = (LinearLayout) findViewById(R.id.linkstate_linearLayout);
		fileTransBtn = (Button) findViewById(R.id.id_filetrans_btn);
		optionFABtn = (FloatingActionButton) findViewById(R.id.id_option_fabtn);
		coordinatorLayout = (CoordinatorLayout) findViewById(R.id.id_coordinatorlayout);

		optionFABsms = (FloatingActionButton) findViewById(R.id.id_option1);
		optionFABtel = (FloatingActionButton) findViewById(R.id.id_option2);
		optionFABwifi = (FloatingActionButton) findViewById(R.id.id_option3);
		optionFABspeaker = (FloatingActionButton) findViewById(R.id.id_option4);
		optionFABnotification = (FloatingActionButton) findViewById(R.id.id_option5);

		settingTextView = (TextView) findViewById(R.id.id_settv);
		smsTextView = (TextView) findViewById(R.id.id_smstv);
		telTextView = (TextView) findViewById(R.id.id_teltv);
		wifiTextView = (TextView) findViewById(R.id.id_wifitv);
		speakerTextView = (TextView) findViewById(R.id.id_speakertv);
		notificationTextView = (TextView) findViewById(R.id.id_notificationtv);
		pcNameTextView = (TextView) findViewById(R.id.id_pcname_textview);

		cloudTextView = (TextView) findViewById(R.id.id_cloud);

		//设定点击输入连接码输入框后弹出的键盘类型
		verifyCodeEditText.setInputType(EditorInfo.TYPE_CLASS_NUMBER);

		//绑定事件监听器
		linkBtn.setOnClickListener(this);
		disconnectBtn.setOnClickListener(this);
		fileTransBtn.setOnClickListener(this);
		optionFABtn.setOnClickListener(this);
		cloudTextView.setOnClickListener(this);

		optionFABsms.setOnClickListener(this);
		optionFABspeaker.setOnClickListener(this);
		optionFABwifi.setOnClickListener(this);
		optionFABtel.setOnClickListener(this);
		optionFABnotification.setOnClickListener(this);

		notificationTextView.setOnClickListener(this);

		//查询设置数据，并显示
		boolean isSMS = dbHelper.isSMS();
		if(!isSMS){
			smsTextView.setText(OFF);
		}else{
			smsTextView.setText(ON);
		}
		boolean isTel = dbHelper.isTel();
		if(!isTel){
			telTextView.setText(OFF);
		}else{
			telTextView.setText(ON);
		}
		boolean isWifi = dbHelper.isWIFI();
		if(!isWifi){
			wifiTextView.setText(OFF);
		}else{
			wifiTextView.setText(ON);
		}
		boolean isSpeaker = dbHelper.isSpeaker();
		if(!isSpeaker){
			speakerTextView.setText(OFF);
		}else{
			speakerTextView.setText(ON);
		}
		boolean isNotification = dbHelper.isNotification();
		if(!isNotification){
			notificationTextView.setText(OFF);
		}else{
			notificationTextView.setText(CLICK_MORE_SET);
		}

		//检查权限，仅在API23以上（android 6.0以上版本）时有效果
		checkPermissions();

		//设置ToolBar的图标，标题
		phoneInfo = Build.MODEL;
		getSupportActionBar().setTitle(Build.MODEL);
		getSupportActionBar().setDisplayUseLogoEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);

		//判断是否能够获取通知，如果不能提示用户前往设置(API 22 以上支持自动跳转)
		if(Build.VERSION.SDK_INT >= 22) {
			if (!isNotificationAccess()) {
				new AlertDialog.Builder(MainActivity.this).setMessage(GET_NOTIFICATION_SET)
					.setPositiveButton(GO_TO_SET, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
						}
					})
					.setNegativeButton(CANCEL, null)
					.create().show();
			}
		}else {
			if (!isNotificationAccess()) {
				new AlertDialog.Builder(MainActivity.this).setMessage(GET_NOTIFICATION_SET)
					.setPositiveButton(OK, null)
					.create().show();
			}
		}

		//设置端口号
		UDPSender.setPort(21212, MainActivity.this);

		//判断是否已经处于连接状态，如果处于连接状态则隐藏掉输入框和按钮。由于此时ActionBar上的元素还未加载，故对显示连接状态的Switch控件的调整在onCreateOptionsMenu回调函数中执行
		if(dbHelper.isConnected()){
			verifyLinearLayout.setVisibility(View.INVISIBLE);
			linkstateLineaLayout.setVisibility(View.VISIBLE);
			pcNameTextView.setText(dbHelper.getPCName());
		}
	}

	@Override
	protected void onDestroy() {
		Log.i("info", "MainActivity destroy!");
		unregisterReceiver(disconnectBroadcastReceiver);
		dbHelper.close();
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i("info", "option menu create!");
		//ActionBar被创建时，获取控件变量
		getMenuInflater().inflate(R.menu.menu, menu);
		linkMenuItem = menu.findItem(R.id.id_islinked);
		shareMenuItem = menu.findItem(R.id.id_share);
		linkMenuItem.setActionView(R.layout.switch_layout);
		linkSwitch = (Switch) linkMenuItem.getActionView().findViewById(R.id.id_switch);
		//查询当前连接状态并修改Switch的状态
		if(dbHelper.isConnected()){
			linkSwitch.setChecked(true);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()){
			//分享
			case R.id.id_share:
				Intent intent=new Intent(Intent.ACTION_SEND);
				intent.setType("text/plain");
				intent.putExtra(Intent.EXTRA_TEXT, SHARE_INFORMATION);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(Intent.createChooser(intent, SHARE_TO));

				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onClick(View v) {

		//连接按钮
		if(v == linkBtn){

			InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
			if(inputMethodManager != null){
				inputMethodManager.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
			}
			String code = verifyCodeEditText.getText().toString();
			//判断长度
			if(code.length() < 6){

				//弹出错误信息，并且为了防止动画效果错乱，在SnackBar弹出时禁用设置按钮，SnackBar收回后恢复
				Snackbar.make(coordinatorLayout, VARIFY_CODE_LENGTH_ERROR, Snackbar.LENGTH_SHORT).setCallback(new Snackbar.Callback() {
					@Override
					public void onDismissed(Snackbar snackbar, int event) {
						optionFABtn.setEnabled(true);
						super.onDismissed(snackbar, event);
					}

					@Override
					public void onShown(Snackbar snackbar) {
						optionFABtn.setEnabled(false);
						super.onShown(snackbar);
					}
				}).setAction(CANCEL, new View.OnClickListener() {
					@Override
					public void onClick(View v) {
					}
				}).show();
				return;
			}

			final int codeInt = Integer.parseInt(code);
			final String req = PackageBuilder.createLinkPackage(codeInt, 0, phoneInfo);
			final Handler onLinkHandler = new Handler(){
				@Override
				public void handleMessage(Message msg) {
					super.handleMessage(msg);
					boolean tag;
					if(msg.arg1 == 1){
						tag = true;
					}else {
						tag = false;
					}
					onLinkResult(tag, codeInt, req);
				}
			};

			//发送连接请求，并等待相应码，会短暂阻塞线程
			Thread reqThread = new Thread(new Runnable() {
				@Override
				public void run() {
					boolean tag = false;
					try {
						tag = UDPSender.sendWithCheckBroadcast(req, Integer.toString(codeInt + 1), MainActivity.this);
					} catch (IOException e) {
						e.printStackTrace();
					}
					Message msg = Message.obtain();
					if(tag){
						msg.arg1 = 1;
					}else {
						msg.arg1 = 0;
					}
					onLinkHandler.sendMessage(msg);
				}
			});
			linkBtn.setEnabled(false);
			reqThread.start();
			return;
		}

		//中断连接
		if(v == disconnectBtn){
			UDPSender.send(PackageBuilder.createLinkPackage(0, 0, ""), MainActivity.this);
			UDPSender.setStatus(false, MainActivity.this);
			Intent listeningServiceIntent = new Intent(MainActivity.this, ListeningService.class);
			stopService(listeningServiceIntent);
			sendBroadcast(new Intent("com.zhouyuming.sync.DISCONNECT"));
		}

		//传输文件按钮
		if(v == fileTransBtn){

			/*
				文件传输模块
			 */

			return;
		}

		//设置按钮
		if(v == optionFABtn){

			//根据设置按钮的状态来展开或者关闭设置按钮，在展开时禁用掉Activity中的两个按钮
			if(!fabOpened){
				shareMenuItem.setEnabled(false);
				linkBtn.setEnabled(false);
				disconnectBtn.setEnabled(false);
				fileTransBtn.setEnabled(false);
				openMenu(v);
			}else{
				closeMenu(v);
				fileTransBtn.setEnabled(true);
				linkBtn.setEnabled(true);
				disconnectBtn.setEnabled(true);
				shareMenuItem.setEnabled(true);
			}
			return;
		}

		//背景遮罩，点击后关闭展开的设置按钮
		if(v == cloudTextView){
			if(fabOpened){
				fileTransBtn.setEnabled(true);
				linkBtn.setEnabled(true);
				disconnectBtn.setEnabled(true);
				shareMenuItem.setEnabled(true);
				closeMenu(optionFABtn);
			}
			return;
		}

		//设置菜单中的短信按钮，在开启时会做权限检查
		if(v == optionFABsms){
			if(smsTextView.getText().toString().equals(ON)) {
				dbHelper.setSMS(false);
				smsTextView.setText(OFF);
				return;
			}else {
				dbHelper.setSMS(true);
				smsTextView.setText(ON);
				checkPermissionSMS();
				return;
			}
		}

		//设置菜单中的来电按钮，在开启时会做权限检查
		if(v == optionFABtel){
			if(telTextView.getText().toString().equals(ON)) {
				dbHelper.setTel(false);
				telTextView.setText(OFF);
				return;
			}else {
				dbHelper.setTel(true);
				telTextView.setText(ON);
				checkPermissionTel();
				return;
			}
		}

		//设置菜单中的声音模式按钮
		if(v == optionFABspeaker){
			if(speakerTextView.getText().toString().equals(ON)) {
				dbHelper.setSpeaker(false);
				speakerTextView.setText(OFF);
				return;
			}else {
				dbHelper.setSpeaker(true);
				speakerTextView.setText(ON);
				return;
			}
		}

		//设置菜单中的wifi按钮
		if(v == optionFABwifi){
			if(wifiTextView.getText().toString().equals(ON)) {
				dbHelper.setWIFI(false);
				wifiTextView.setText(OFF);
				return;
			}else {
				dbHelper.setWIFI(true);
				wifiTextView.setText(ON);
				return;
			}
		}

		//设置菜单中的推送按钮，开启时会检测并提示用户设置获取通知
		if(v == optionFABnotification){
			if(notificationTextView.getText().toString().equals(CLICK_MORE_SET)){
				dbHelper.setNotification(false);
				notificationTextView.setText(OFF);
			}else {
				if(!isNotificationAccess()){
					new AlertDialog.Builder(MainActivity.this).setMessage(GET_NOTIFICATION_SET)
						.setPositiveButton(GO_TO_SET, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
							}
						})
						.setNegativeButton(CANCEL, null)
						.create().show();
				}
				dbHelper.setNotification(true);
				notificationTextView.setText(CLICK_MORE_SET);
				return;
			}
		}

		if(v == notificationTextView){

			if(notificationTextView.getText().equals(CLICK_MORE_SET)) {
				Intent intent = new Intent(MainActivity.this, NotificationSelectActivity.class);
				startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle());
			}
		}
	}

	private void onLinkResult(boolean tag, int codeInt, String req){

		linkBtn.setEnabled(true);
		if (tag) {
			//更新ToolBar上Switch的状态
			req = PackageBuilder.createLinkPackage(0, codeInt + 2, phoneInfo);
			UDPSender.setStatus(true, this);
			UDPSender.send(req, this);

			linkSwitch.setChecked(true);
			connected();

			//启动监听服务
			Intent listeningServiceIntent = new Intent(MainActivity.this, ListeningService.class);
			startService(listeningServiceIntent);

			Snackbar.make(coordinatorLayout, LINK_SUCCESS, Snackbar.LENGTH_SHORT).setCallback(new Snackbar.Callback() {
				@Override
				public void onDismissed(Snackbar snackbar, int event) {
					optionFABtn.setEnabled(true);
					super.onDismissed(snackbar, event);
				}

				@Override
				public void onShown(Snackbar snackbar) {
					optionFABtn.setEnabled(false);
					super.onShown(snackbar);
				}
			}).setAction(CANCEL, new View.OnClickListener() {
				@Override
				public void onClick(View v) {
				}
			}).show();
		}else {
			Snackbar.make(coordinatorLayout, LINK_FAILED, Snackbar.LENGTH_SHORT).setCallback(new Snackbar.Callback() {
				@Override
				public void onDismissed(Snackbar snackbar, int event) {
					optionFABtn.setEnabled(true);
					super.onDismissed(snackbar, event);
				}

				@Override
				public void onShown(Snackbar snackbar) {
					optionFABtn.setEnabled(false);
					super.onShown(snackbar);
				}
			}).setAction(CANCEL, new View.OnClickListener() {
				@Override
				public void onClick(View v) {
				}
			}).show();
		}
	}

	/**
	 * 展开设置菜单
	 * @param view Floating Action Button
	 */
	private void openMenu(View view){

		if(fabOpened){
			return;
		}

		fabOpened = true;

		cloudTextView.setEnabled(false);
		optionFABtn.setEnabled(false);
		ObjectAnimator animator = ObjectAnimator.ofFloat(view, "rotation", 0, -180, -180);
		animator.setDuration(500);
		animator.start();
		cloudTextView.setVisibility(View.VISIBLE);
		AlphaAnimation alphaAnimation = new AlphaAnimation(0,0.6f);
		alphaAnimation.setDuration(500);
		alphaAnimation.setFillAfter(true);
		cloudTextView.startAnimation(alphaAnimation);

		optionFABsms.setElevation(0.0f);
		optionFABtel.setElevation(0.0f);
		optionFABwifi.setElevation(0.0f);
		optionFABspeaker.setElevation(0.0f);
		optionFABnotification.setElevation(0.0f);

		optionFABsms.setVisibility(View.VISIBLE);
		optionFABtel.setVisibility(View.VISIBLE);
		optionFABwifi.setVisibility(View.VISIBLE);
		optionFABspeaker.setVisibility(View.VISIBLE);
		optionFABnotification.setVisibility(View.VISIBLE);
		settingTextView.setVisibility(View.VISIBLE);

		ObjectAnimator menuAnimator1 = ObjectAnimator.ofFloat(optionFABsms, "translationY", dpTopx(0), -dpTopx(80), -dpTopx(60)).setDuration(500);
		ObjectAnimator menuAnimator2 = ObjectAnimator.ofFloat(optionFABtel, "translationY", dpTopx(0), -dpTopx(135), -dpTopx(110)).setDuration(500);
		ObjectAnimator menuAnimator3 = ObjectAnimator.ofFloat(optionFABwifi, "translationY", dpTopx(0), -dpTopx(190), -dpTopx(160)).setDuration(500);
		ObjectAnimator menuAnimator4 = ObjectAnimator.ofFloat(optionFABspeaker, "translationY", dpTopx(0), -dpTopx(245), -dpTopx(210)).setDuration(500);
		ObjectAnimator menuAnimator5 = ObjectAnimator.ofFloat(optionFABnotification, "translationY", dpTopx(0), -dpTopx(300), -dpTopx(260)).setDuration(500);

		ObjectAnimator settingAnimator = ObjectAnimator.ofFloat(settingTextView, "translationX", 0, -dpTopx(85), -dpTopx(65)).setDuration(500);

		AnimatorSet animatorSet = new AnimatorSet();
		animatorSet.play(menuAnimator5).with(menuAnimator4).with(menuAnimator3).with(menuAnimator2).with(menuAnimator1).with(settingAnimator);
		animatorSet.setInterpolator(new AccelerateInterpolator(2));
		animatorSet.start();

		animatorSet.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {

				super.onAnimationEnd(animation);

				optionFABsms.setElevation(dpTopx(6));
				optionFABtel.setElevation(dpTopx(6));
				optionFABwifi.setElevation(dpTopx(6));
				optionFABspeaker.setElevation(dpTopx(6));
				optionFABnotification.setElevation(dpTopx(6));

				speakerTextView.setVisibility(View.VISIBLE);
				wifiTextView.setVisibility(View.VISIBLE);
				telTextView.setVisibility(View.VISIBLE);
				smsTextView.setVisibility(View.VISIBLE);
				notificationTextView.setVisibility(View.VISIBLE);

				ObjectAnimator menutv2Animator1 = ObjectAnimator.ofFloat(smsTextView, "translationX", 0, -dpTopx(80), -dpTopx(60)).setDuration(500);
				ObjectAnimator menutv2Animator2 = ObjectAnimator.ofFloat(telTextView, "translationX", 0, -dpTopx(80), -dpTopx(60)).setDuration(500);
				ObjectAnimator menutv2Animator3 = ObjectAnimator.ofFloat(wifiTextView, "translationX", 0, -dpTopx(80), -dpTopx(60)).setDuration(500);
				ObjectAnimator menutv2Animator4 = ObjectAnimator.ofFloat(speakerTextView, "translationX", 0, -dpTopx(80), -dpTopx(60)).setDuration(500);
				ObjectAnimator menutv2Animator5 = ObjectAnimator.ofFloat(notificationTextView, "translationX", 0, -dpTopx(80), -dpTopx(60)).setDuration(500);
				ObjectAnimator menutv2Animator6 = ObjectAnimator.ofFloat(notificationTextView, "scaleX", 0.0f, 1.0f);

				AnimatorSet animatorSet2 = new AnimatorSet();
				animatorSet2.play(menutv2Animator1).with(menutv2Animator2).with(menutv2Animator3).with(menutv2Animator4).with(menutv2Animator5).with(menutv2Animator6);
				animatorSet2.setInterpolator(new AccelerateInterpolator(2));
				animatorSet2.start();

				optionFABtn.setEnabled(true);
				cloudTextView.setEnabled(true);
			}
		});
	}

	/**
	 * 关闭展开的设置菜单
	 * @param view Floating Action Button
	 */
	private void closeMenu(final View view){

		if(!fabOpened){
			return;
		}

		fabOpened = false;
		optionFABtn.setEnabled(false);

		ObjectAnimator menutv2Animator1 = ObjectAnimator.ofFloat(smsTextView, "translationX", -dpTopx(60), -dpTopx(0)).setDuration(500);
		ObjectAnimator menutv2Animator2 = ObjectAnimator.ofFloat(telTextView, "translationX", -dpTopx(60), -dpTopx(0)).setDuration(500);
		ObjectAnimator menutv2Animator3 = ObjectAnimator.ofFloat(wifiTextView, "translationX", -dpTopx(60), -dpTopx(0)).setDuration(500);
		ObjectAnimator menutv2Animator4 = ObjectAnimator.ofFloat(speakerTextView, "translationX", -dpTopx(60), -dpTopx(0)).setDuration(500);
		ObjectAnimator menutv2Animator5 = ObjectAnimator.ofFloat(notificationTextView, "translationX", -dpTopx(60), -dpTopx(0)).setDuration(500);
		ObjectAnimator menutv2Animator6 = ObjectAnimator.ofFloat(notificationTextView, "scaleX", 1.0f, 0.0f);

		AnimatorSet animatorSet2 = new AnimatorSet();
		animatorSet2.play(menutv2Animator1).with(menutv2Animator2).with(menutv2Animator3).with(menutv2Animator4).with(menutv2Animator5);
		animatorSet2.setInterpolator(new DecelerateInterpolator(2));
		animatorSet2.start();
		menutv2Animator6.start();

		animatorSet2.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {

				speakerTextView.setVisibility(View.GONE);
				smsTextView.setVisibility(View.GONE);
				telTextView.setVisibility(View.GONE);
				wifiTextView.setVisibility(View.GONE);
				notificationTextView.setVisibility(View.GONE);

				optionFABsms.setElevation(0.0f);
				optionFABtel.setElevation(0.0f);
				optionFABwifi.setElevation(0.0f);
				optionFABspeaker.setElevation(0.0f);
				optionFABnotification.setElevation(0.0f);

				ObjectAnimator menuAnimator1 = ObjectAnimator.ofFloat(optionFABsms, "translationY", -dpTopx(60), dpTopx(0)).setDuration(500);
				ObjectAnimator menuAnimator2 = ObjectAnimator.ofFloat(optionFABtel, "translationY", -dpTopx(110), dpTopx(0)).setDuration(500);
				ObjectAnimator menuAnimator3 = ObjectAnimator.ofFloat(optionFABwifi, "translationY", -dpTopx(160), dpTopx(0)).setDuration(500);
				ObjectAnimator menuAnimator4 = ObjectAnimator.ofFloat(optionFABspeaker, "translationY", -dpTopx(210), dpTopx(0)).setDuration(500);
				ObjectAnimator menuAnimator5 = ObjectAnimator.ofFloat(optionFABnotification, "translationY", -dpTopx(260), dpTopx(0)).setDuration(500);

				ObjectAnimator settingAnimator = ObjectAnimator.ofFloat(settingTextView, "translationX", -dpTopx(65), -dpTopx(0)).setDuration(500);

				AnimatorSet animatorSet = new AnimatorSet();
				animatorSet.play(menuAnimator5).with(menuAnimator4).with(menuAnimator3).with(menuAnimator2).with(menuAnimator1).with(settingAnimator);
				animatorSet.setInterpolator(new DecelerateInterpolator(2));
				animatorSet.start();

				ObjectAnimator animator = ObjectAnimator.ofFloat(view, "rotation", -180, 30, 0);
				animator.setDuration(500);
				animator.start();
				AlphaAnimation alphaAnimation = new AlphaAnimation(0.6f, 0);
				alphaAnimation.setDuration(500);
				cloudTextView.startAnimation(alphaAnimation);

				animatorSet.addListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationEnd(Animator animation) {
						super.onAnimationEnd(animation);
						optionFABsms.setVisibility(View.GONE);
						optionFABtel.setVisibility(View.GONE);
						optionFABwifi.setVisibility(View.GONE);
						optionFABspeaker.setVisibility(View.GONE);
						optionFABnotification.setVisibility(View.GONE);
						cloudTextView.setVisibility(View.GONE);
						settingTextView.setVisibility(View.GONE);
						optionFABtn.setEnabled(true);
					}
				});
				super.onAnimationEnd(animation);
			}
		});
	}

	//连接时界面变化
	private void connected() {

		if(verifyLinearLayout.getVisibility() == View.VISIBLE && linkstateLineaLayout.getVisibility() == View.INVISIBLE) {

			Animator animatorRemoveClear = ViewAnimationUtils.createCircularReveal(verifyLinearLayout, verifyLinearLayout.getWidth() / 2,
				verifyLinearLayout.getHeight() / 2, (float) Math.hypot(verifyLinearLayout.getWidth(), verifyLinearLayout.getHeight()), 0);
			Animator animatorRemoveCreate = ViewAnimationUtils.createCircularReveal(linkstateLineaLayout, linkstateLineaLayout.getWidth() / 2,
				linkstateLineaLayout.getHeight() / 2, 0, (float) Math.hypot(linkstateLineaLayout.getWidth(), linkstateLineaLayout.getHeight()));
			animatorRemoveClear.setDuration(500);
			animatorRemoveCreate.setDuration(500);

			animatorRemoveClear.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					super.onAnimationEnd(animation);
					verifyLinearLayout.setVisibility(View.INVISIBLE);
					linkstateLineaLayout.setVisibility(View.VISIBLE);
					pcNameTextView.setText(dbHelper.getPCName());
				}
			});

			AnimatorSet animatorSet = new AnimatorSet();
			animatorSet.play(animatorRemoveClear).before(animatorRemoveCreate);
			animatorSet.start();
		}
	}

	//断开连接时界面变化
	private void disconnected(){

		if(verifyLinearLayout.getVisibility() == View.INVISIBLE && linkstateLineaLayout.getVisibility() == View.VISIBLE) {

			Animator animatorRemoveClear = ViewAnimationUtils.createCircularReveal(linkstateLineaLayout, linkstateLineaLayout.getWidth() / 2,
				linkstateLineaLayout.getHeight() / 2, (float) Math.hypot(linkstateLineaLayout.getWidth(), linkstateLineaLayout.getHeight()), 0);
			Animator animatorRemoveCreate = ViewAnimationUtils.createCircularReveal(verifyLinearLayout, verifyLinearLayout.getWidth() / 2,
				verifyLinearLayout.getHeight() / 2, 0, (float) Math.hypot(verifyLinearLayout.getWidth(), verifyLinearLayout.getHeight()));
			animatorRemoveClear.setDuration(500);
			animatorRemoveCreate.setDuration(500);

			animatorRemoveClear.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					super.onAnimationEnd(animation);
					pcNameTextView.setText("");
					linkstateLineaLayout.setVisibility(View.INVISIBLE);
					verifyLinearLayout.setVisibility(View.VISIBLE);
				}
			});

			AnimatorSet animatorSet = new AnimatorSet();
			animatorSet.play(animatorRemoveClear).before(animatorRemoveCreate);
			animatorSet.start();
		}
	}

	/**
	 * 检查来电监听功能所需要的权限，仅在API23以上（android6.0以上版本）中生效
	 */
	private void checkPermissionTel(){
		if(Build.VERSION.SDK_INT >= 23){
			List<String> permissionsList = new ArrayList<String>();
			addPermission(permissionsList, Manifest.permission.PROCESS_OUTGOING_CALLS);
			addPermission(permissionsList, Manifest.permission.READ_PHONE_STATE);
			addPermission(permissionsList, Manifest.permission.READ_CONTACTS);
			addPermission(permissionsList, Manifest.permission.CALL_PHONE);
			if(permissionsList.size() > 0){
				ActivityCompat.requestPermissions(this, permissionsList.toArray(new String[permissionsList.size()]),
					MY_TEL_PERMISSION);
			}
		}
	}

	/**
	 * 检查短信监听功能所需要的权限，仅在API23以上（android6.0以上版本）中生效
	 */
	private void checkPermissionSMS(){
		if(Build.VERSION.SDK_INT >= 23){
			List<String> permissionsList = new ArrayList<String>();
			addPermission(permissionsList, Manifest.permission.READ_CONTACTS);
			addPermission(permissionsList, Manifest.permission.RECEIVE_SMS);
			addPermission(permissionsList, Manifest.permission.SEND_SMS);
			if(permissionsList.size() > 0) {
				ActivityCompat.requestPermissions(this, permissionsList.toArray(new String[permissionsList.size()]),
					MY_SMS_PERMISSION);
			}
		}
	}

	/**
	 * 检查程序所需要的所有权限，仅在API23以上（android6.0以上版本）中生效
	 */
	private void checkPermissions(){
		if(Build.VERSION.SDK_INT >= 23) {
			List<String> permissionsList = new ArrayList<String>();
			addPermission(permissionsList, Manifest.permission.READ_CONTACTS);
			addPermission(permissionsList, Manifest.permission.READ_PHONE_STATE);
			if (permissionsList.size() > 0) {
				ActivityCompat.requestPermissions(this, permissionsList.toArray(new String[permissionsList.size()]),
					REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
			}
		}
	}

	/**
	 * 检查权限是否被用户赋予，如果没有则将它加入列表中，否则不做任何操作
	 * @param permissionsList 待加入列表
	 * @param permission 待检查权限
	 * @return 暂时无意义，留作备用
	 */
	private boolean addPermission(List<String> permissionsList, String permission) {
		if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
			permissionsList.add(permission);
			// Check for Rationale Option
			if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission))
				return false;
		}
		return true;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

		if(grantResults.length <= 0 || permissions.length <= 0){
			return;
		}

		//开启短信监听功能时申请权限的响应，如果任何一个在checkPermissionSMS方法中被请求的权限没有被给予则关闭SMS监听
		if(requestCode == MY_SMS_PERMISSION){
			for(int result : grantResults) {
				if (result == PackageManager.PERMISSION_DENIED) {
					Toast.makeText(this, NO_PERMISSION_SMS, Toast.LENGTH_SHORT).show();
					dbHelper.setSMS(false);
					smsTextView.setText(OFF);
					return;
				}
			}
		}

		//开启来电监听功能时申请权限的响应，如果任何一个在checkPermissionTel方法中被请求的权限没有被给予则关闭Tel监听
		if(requestCode == MY_TEL_PERMISSION){
			for(int result : grantResults){
				if(result == PackageManager.PERMISSION_DENIED){
					Toast.makeText(this, NO_PERMISSION_TEL, Toast.LENGTH_SHORT).show();
					dbHelper.setTel(false);
					telTextView.setText(OFF);
					return;
				}
			}
		}

		//启动程序时申请权限的响应
		if(requestCode == REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS){
			//checkPermissionS中申请的权限和结果存入Map中，Key为权限名，Value为结果
			Map<String, Integer> perms = new HashMap<String, Integer>();
			for(int i = 0; i < permissions.length; i++){
				perms.put(permissions[i], grantResults[i]);
			}

			int grantResultReadContacts = 0;
			if(perms.get(Manifest.permission.READ_CONTACTS) != null)
				grantResultReadContacts = perms.get(Manifest.permission.READ_CONTACTS);

			int grantResultReceiveSMS = 0;
			if(perms.get(Manifest.permission.RECEIVE_SMS) != null)
				grantResultReceiveSMS = perms.get(Manifest.permission.RECEIVE_SMS);

			int grantResultReadPhoneState = 0;
			if(perms.get(Manifest.permission.READ_PHONE_STATE) != null)
				grantResultReadPhoneState = perms.get(Manifest.permission.READ_PHONE_STATE);

			int grantResultOutgoingCall = 0;
			if(perms.get(Manifest.permission.PROCESS_OUTGOING_CALLS) != null)
				grantResultOutgoingCall = perms.get(Manifest.permission.PROCESS_OUTGOING_CALLS);

			int grantResultSendSMS = 0;
			if(perms.get(Manifest.permission.SEND_SMS) != null)
				grantResultSendSMS = perms.get(Manifest.permission.SEND_SMS);

			int grantResultCallPhone = 0;
			if(perms.get(Manifest.permission.CALL_PHONE) != null)
				grantResultCallPhone = perms.get(Manifest.permission.CALL_PHONE);

			//监听SMS需要读取联系人（查询号码对应姓名）和接受短信权限
			if(grantResultReadContacts == PackageManager.PERMISSION_DENIED || grantResultReceiveSMS == PackageManager.PERMISSION_DENIED){
				Toast.makeText(this, NO_PERMISSION_SMS, Toast.LENGTH_SHORT).show();
				dbHelper.setSMS(false);
				smsTextView.setText(OFF);
			}

			//来电去电监听需要读取手机状态和处理去电的权限
			if(grantResultOutgoingCall == PackageManager.PERMISSION_DENIED || grantResultReadPhoneState == PackageManager.PERMISSION_DENIED ||
				grantResultReadContacts == PackageManager.PERMISSION_DENIED){
				Toast.makeText(this, NO_PERMISSION_TEL, Toast.LENGTH_SHORT).show();
				dbHelper.setTel(false);
				telTextView.setText(OFF);
			}

			//启动程序需要读取手机状态，读取联系人，发送短信和呼叫权限，如果有任意一个没有将会关闭程序
			if(grantResultReadPhoneState == PackageManager.PERMISSION_DENIED || grantResultReadContacts == PackageManager.PERMISSION_DENIED ||
				grantResultSendSMS == PackageManager.PERMISSION_DENIED || grantResultCallPhone == PackageManager.PERMISSION_DENIED){
				Toast.makeText(this, NO_PERMISSION_ALL, Toast.LENGTH_SHORT).show();
				if(dbHelper.isConnected()) {
					Intent listeningServiceIntent = new Intent(MainActivity.this, ListeningService.class);
					stopService(listeningServiceIntent);
					UDPSender.setStatus(false, MainActivity.this);
				}
				finish();
			}
		}

		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	/**
	 * 是否在通知中心中开启获取通知
	 * @return true 已开启，false 未开启
	 */
	private boolean isNotificationAccess() {
		String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
		String pkgName = getPackageName();
		final String flat = Settings.Secure.getString(getContentResolver(),
			ENABLED_NOTIFICATION_LISTENERS);
		if (!TextUtils.isEmpty(flat)) {
			final String[] names = flat.split(":");
			for(String name : names) {
				final ComponentName cn = ComponentName.unflattenFromString(name);
				if (cn != null) {
					if (TextUtils.equals(pkgName, cn.getPackageName())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
	 */
	private int dpTopx(float dpValue) {
		final float scale = this.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f);
	}
}
