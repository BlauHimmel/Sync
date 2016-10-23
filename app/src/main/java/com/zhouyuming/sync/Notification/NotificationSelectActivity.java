package com.zhouyuming.sync.Notification;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.Window;

import com.zhouyuming.sync.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ZhouYuming on 2016/8/27.
 * 设置界面
 */
public class NotificationSelectActivity extends AppCompatActivity {

	private RecyclerView recyclerView;
	private final String MORE_SET = "详细设置";
	private LinearLayoutManager linearLayoutManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
		getWindow().setEnterTransition(new Slide(Gravity.LEFT).setDuration(500));
		getWindow().setExitTransition(new Slide(Gravity.RIGHT).setDuration(500));
		getWindow().setReenterTransition(new Slide(Gravity.LEFT).setDuration(500));
		getWindow().setReturnTransition(new Slide(Gravity.RIGHT).setDuration(500));

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_noteselect);

		recyclerView = (RecyclerView) findViewById(R.id.noteselect_rv);

		getSupportActionBar().setTitle(MORE_SET);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		NoteSelectRecyclerViewAdapter noteSelectRecyclerViewAdapter = new NoteSelectRecyclerViewAdapter(getAppInfos(), this);
		linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);

		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
			}

			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				int lastPos = linearLayoutManager.findLastVisibleItemPosition();
				int firstPos = linearLayoutManager.findFirstVisibleItemPosition();
				for(int i = firstPos; i <= lastPos; i++){
					((NoteSelectRecyclerViewAdapter)recyclerView.getAdapter()).loadIcon(i);
				}
			}
		});

		recyclerView.setLayoutManager(linearLayoutManager);
		recyclerView.setItemAnimator(new DefaultItemAnimator());
		recyclerView.setAdapter(noteSelectRecyclerViewAdapter);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finishAfterTransition();
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	//手指上下滑动时的最小速度
	private static final int YSPEED_MIN = 1000;

	//手指向右滑动时的最小距离
	private static final int XDISTANCE_MIN = 200;

	//手指向上滑或下滑时的最小距离
	private static final int YDISTANCE_MIN = 100;

	//记录手指按下时的横坐标。
	private float xDown;

	//记录手指按下时的纵坐标。
	private float yDown;

	//记录手指移动时的横坐标。
	private float xMove;

	//记录手指移动时的纵坐标。
	private float yMove;

	//用于计算手指滑动的速度。
	private VelocityTracker mVelocityTracker;

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		createVelocityTracker(event);
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				xDown = event.getRawX();
				yDown = event.getRawY();
				break;
			case MotionEvent.ACTION_MOVE:
				xMove = event.getRawX();
				yMove= event.getRawY();
				//滑动的距离
				int distanceX = (int) (xMove - xDown);
				int distanceY= (int) (yMove - yDown);
				//获取顺时速度
				int ySpeed = getScrollVelocity();
				//关闭Activity需满足以下条件：
				//1.x轴滑动的距离>XDISTANCE_MIN
				//2.y轴滑动的距离在YDISTANCE_MIN范围内
				//3.y轴上（即上下滑动的速度）<XSPEED_MIN，如果大于，则认为用户意图是在上下滑动而非左滑结束Activity
				if(distanceX > XDISTANCE_MIN &&(distanceY<YDISTANCE_MIN && distanceY>-YDISTANCE_MIN) && ySpeed < YSPEED_MIN) {
					finishAfterTransition();
				}
				break;
			case MotionEvent.ACTION_UP:
				recycleVelocityTracker();
				break;
			default:
				break;
		}
		return super.dispatchTouchEvent(event);
	}

	/**
	 * 创建VelocityTracker对象，并将触摸界面的滑动事件加入到VelocityTracker当中。
	 * @param event
	 */
	private void createVelocityTracker(MotionEvent event) {
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(event);
	}

	/**
	 * 回收VelocityTracker对象。
	 */
	private void recycleVelocityTracker() {
		mVelocityTracker.recycle();
		mVelocityTracker = null;
	}

	/**
	 * @return 滑动速度，以每秒钟移动了多少像素值为单位。
	 */
	private int getScrollVelocity() {
		mVelocityTracker.computeCurrentVelocity(1000);
		int velocity = (int) mVelocityTracker.getYVelocity();
		return Math.abs(velocity);
	}

	/**
	 * 得到用户安装的应用列表
	 * @return 用户安装App的名称和包名组成的List
	 */
	private List<APPbean> getAppInfos(){

		List<APPbean> lists = new ArrayList<APPbean>();
		PackageManager packageManager = getPackageManager();
		List<ApplicationInfo> applicationInfos = packageManager.getInstalledApplications(PackageManager.GET_SHARED_LIBRARY_FILES);
		for(ApplicationInfo appInfo : applicationInfos){
			if(isUserApp(appInfo)) {
				String appName = appInfo.loadLabel(packageManager).toString();
				String packageName = appInfo.packageName;
				lists.add(new APPbean(appName, packageName));
			}
		}
		return lists;
	}

	/**
	 * 判断程序是否是系统内程序的附属程序
	 * @param info 待判断程序的ApplicationInfo对象
	 * @return true or false
	 */
	private boolean isSystemUpdateApp(ApplicationInfo info){
		return ((info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0);
	}

	/**
	 * 判断程序是否是系统内建应用程序
	 * @param info 待判断程序的ApplicationInfo对象
	 * @return true or false
	 */
	private boolean isSystemApp(ApplicationInfo info){
		return ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
	}

	/**
	 * 判断程序是否是用户自行安装的应用程序
	 * @param info 待判断程序的ApplicationInfo对象
	 * @return true or false
	 */
	private boolean isUserApp(ApplicationInfo info){
		return (!isSystemApp(info) && !isSystemUpdateApp(info));
	}
}
