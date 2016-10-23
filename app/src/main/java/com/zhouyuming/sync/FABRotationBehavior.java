package com.zhouyuming.sync;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

/**
 * Created by ZhouYuming on 2016/7/14.
 * Animation of floating action bar when the snackbar pop
 */
public class FABRotationBehavior extends CoordinatorLayout.Behavior<FloatingActionButton> {

	/**
	 * Default constructor for instantiating Behaviors.
	 */
	public FABRotationBehavior() {
	}

	/**
	 * Default constructor for inflating Behaviors from layout. The Behavior will have
	 * the opportunity to parse specially defined layout parameters. These parameters will
	 * appear on the child view tag.
	 *
	 * @param context
	 * @param attrs
	 */
	public FABRotationBehavior(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/**
	 * 判断底下弹出的是不是snackbar，如果是，floatingactionbutton才会联动
	 *
	 * @param parent
	 * @param child
	 * @param dependency
	 * @return
	 */
	@Override
	public boolean layoutDependsOn(CoordinatorLayout parent,
				       FloatingActionButton child, View dependency) {

		return dependency instanceof Snackbar.SnackbarLayout;
	}


	/**
	 * 当需要联动的snackbar向上移动的过程中，会不断调用这个方法
	 *
	 * @param parent
	 * @param child
	 * @param dependency
	 * @return
	 */
	@Override
	public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionButton child,
					      View dependency) {

		float fTransY = getFabTranslationYForSnackbar(parent, child);

		float iRate = -fTransY / dependency.getHeight();

		child.setRotation(iRate * 180);

		child.setTranslationY(fTransY);

		return false;
	}


	/**
	 * 用来获取SnackBar逐渐弹出来的时候变化的高度
	 *
	 * @param parent
	 * @param fab
	 * @return
	 */
	private float getFabTranslationYForSnackbar(CoordinatorLayout parent,
						    FloatingActionButton fab) {
		float minOffset = 0;
		final List<View> dependencies = parent.getDependencies(fab);
		for (int i = 0, z = dependencies.size(); i < z; i++) {
			final View view = dependencies.get(i);
			if (view instanceof Snackbar.SnackbarLayout && parent.doViewsOverlap(fab, view)) {
				minOffset = Math.min(minOffset,
					ViewCompat.getTranslationY(view) - view.getHeight());
			}
		}
		return minOffset;
	}
}
