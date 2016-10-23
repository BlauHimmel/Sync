package com.zhouyuming.sync.Notification;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.v4.util.LruCache;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.zhouyuming.sync.R;

import java.util.List;

/**
 * Created by ZhouYuming on 2016/8/28.
 * 设置界面RecyclerView控件的适配器
 */
public class NoteSelectRecyclerViewAdapter extends RecyclerView.Adapter<NoteSelectRecyclerViewAdapter.MyViewHolder> {

	private List<APPbean> lists;
	private LayoutInflater inflater;
	private Context context;

	public NoteSelectRecyclerViewAdapter(List<APPbean> lists, Context context) {
		this.lists = lists;
		this.context = context;
		this.inflater = LayoutInflater.from(context);
	}

	@Override
	public NoteSelectRecyclerViewAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

		View view = inflater.inflate(R.layout.noteselect_layout, parent, false);
		MyViewHolder viewHolder = new MyViewHolder(view);
		return viewHolder;
	}

	@Override
	public void onBindViewHolder(NoteSelectRecyclerViewAdapter.MyViewHolder holder, int position) {

		APPbean bean = lists.get(position);
		if(position <= 10){
			try {
				PackageManager packageManager = context.getPackageManager();
				Drawable icon = packageManager.getApplicationIcon(bean.packageName);
				lists.set(position, new APPbean(bean.appName, icon, bean.packageName));
				bean = lists.get(position);
			} catch (PackageManager.NameNotFoundException e) {
				e.printStackTrace();
			}
		}
		if(bean.icon != null){
			holder.appIcon.setImageDrawable(bean.icon);
		}else {
			holder.appIcon.setImageResource(R.mipmap.ic_launcher);
		}

		holder.appName.setText(bean.appName);
		NoteDBHelper noteDBHelper = new NoteDBHelper();
		noteDBHelper.init(context);
		noteDBHelper.open();
		holder.switcher.setChecked(noteDBHelper.getAppValid(bean.appName));
		noteDBHelper.close();
	}

	@Override
	public int getItemCount() {
		return lists.size();
	}

	public void loadIcon(int position){
		APPbean bean = lists.get(position);
		if(bean.icon == null) {
			try {
				PackageManager packageManager = context.getPackageManager();
				Drawable icon = packageManager.getApplicationIcon(bean.packageName);
				lists.set(position, new APPbean(bean.appName, icon, bean.packageName));
				notifyItemChanged(position);
			} catch (PackageManager.NameNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	class MyViewHolder extends RecyclerView.ViewHolder{

		public ImageView appIcon;
		public TextView appName;
		public Switch switcher;

		public MyViewHolder(View itemView) {
			super(itemView);
			appIcon = (ImageView) itemView.findViewById(R.id.appicon_iv);
			appName = (TextView) itemView.findViewById(R.id.appname_tv);
			switcher = (Switch) itemView.findViewById(R.id.app_switch);
			switcher.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					NoteDBHelper dbHelper = new NoteDBHelper();
					dbHelper.init(context);
					dbHelper.open();
					dbHelper.setApp(appName.getText().toString(), switcher.isChecked());
					dbHelper.close();
				}
			});
		}
	}
}
