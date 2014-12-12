package com.chinaiiss.utils;

import android.graphics.PointF;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.Animator.AnimatorListener;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.IntEvaluator;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.TypeEvaluator;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.animation.ValueAnimator.AnimatorUpdateListener;

public class AnimatorUtils {

	protected static final String TAG = "AnimatorUtils";

	/**
	 * 位移动画
	 * 
	 */
	public static void transfor(View view, float from, float to,
			AnimatorListener listener, int durationTime, String type) {
		float[] f = new float[2];
		f[0] = from;
		f[1] = to;
		ObjectAnimator animator = ObjectAnimator.ofFloat(view, type, f);
		animator.setInterpolator(new AccelerateDecelerateInterpolator());
		if (listener != null) {
			animator.addListener(listener);
		}
		animator.setDuration(durationTime);
		animator.start();
	}

	/**
	 * 回弹效果动画
	 * 
	 */
	public static void transforBomb(View view, float from, float to) {
		ObjectAnimator anim1 = ObjectAnimator.ofFloat(view, "translationY",
				from, to - 30);
		anim1.setDuration(500);
		ObjectAnimator anim2 = ObjectAnimator.ofFloat(view, "translationY", to);
		anim2.setDuration(150);
		AnimatorSet animSet = new AnimatorSet();
		animSet.play(anim1);
		animSet.play(anim2).after(anim1);
		animSet.start();
	}

	/**
	 * 翻转动画
	 * 
	 */

	public static void rotateyAnimRun(View view, String type, float degrees,
			int durationTime) {
		if (type.equals("x")) {
			ObjectAnimator.ofFloat(view, "rotationX", 0.0F, degrees)
					.setDuration(durationTime).start();
		} else if (type.equals("y")) {
			ObjectAnimator.ofFloat(view, "rotationY", 0.0F, degrees)
					.setDuration(durationTime).start();
		} else if (type.equals("z")) {
			ObjectAnimator.ofFloat(view, "rotation", 0.0F, degrees)
					.setDuration(durationTime).start();
		}
	}

	/**
	 * 抛物线
	 * 
	 * @param view
	 *            vPX 水平方向速度 x1 一米 == 像素点 没有测试过自己改改即可
	 */
	public static void parabola(final View view, int durationTime,
			float stateX, float stateY, final float vPX, final float x1) {

		ValueAnimator valueAnimator = new ValueAnimator();
		valueAnimator.setDuration(durationTime);
		valueAnimator.setObjectValues(new PointF(stateX, stateY));
		valueAnimator.setInterpolator(new LinearInterpolator());
		valueAnimator.setEvaluator(new TypeEvaluator<PointF>() {
			// fraction = t / duration
			@Override
			public PointF evaluate(float fraction, PointF startValue,
					PointF endValue) {
				// x=1/2 gt2
				// x方向200px/s ，则y方向0.5 * 10 * t
				PointF point = new PointF();
				point.x = vPX * x1 * fraction * 3;
				point.y = 0.5f * 9.8f * x1 * (fraction * 3) * (fraction * 3);
				return point;
			}
		});

		valueAnimator.start();
		valueAnimator.addUpdateListener(new AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				PointF point = (PointF) animation.getAnimatedValue();
				view.setX(point.x);
				view.setY(point.y);
			}
		});
	}

	/**
	 * 垂直下落
	 * 
	 * @param view
	 *            vPX 水平方向速度 x1 一米 == 像素点 没有测试过自己改改即可
	 */
	public static void vertical(final View view, float stateX,
			final float stateY, final float H, final float x1) {
		ValueAnimator valueAnimator = new ValueAnimator();
		// x=1/2 gt2
		double downTime = Math.sqrt(H / x1 / 9.8f * 2);
		// 根据高度设置下落时间
		Log.e(TAG, "downTime:" + downTime);
		// 下落时的时间
		valueAnimator.setDuration((long) downTime);
		valueAnimator.setObjectValues(new PointF(stateX, stateY));
		valueAnimator.setInterpolator(new LinearInterpolator());
		valueAnimator.setEvaluator(new TypeEvaluator<PointF>() {
			// fraction = t / duration
			@Override
			public PointF evaluate(float fraction, PointF startValue,
					PointF endValue) {
				// x=1/2 gt2
				// x方向200px/s ，则y方向0.5 * 10 * t
				PointF point = new PointF();
				point.y = stateY + 0.5f * 9.8f * x1 * (fraction * 3)
						* (fraction * 3);
				return point;
			}
		});

		valueAnimator.start();
		valueAnimator.addUpdateListener(new AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				PointF point = (PointF) animation.getAnimatedValue();
				view.setX(point.x);
				view.setY(point.y);
			}
		});
	}

	/**
	 * 同时加载多个动画
	 * 
	 * @param durationTime
	 * @param anims
	 */
	public static void startAnimator(int durationTime, Animator... anims) {
		AnimatorSet animSet = new AnimatorSet();
		animSet.setDuration(durationTime);
		if (anims != null && anims[0] != null) {
			com.nineoldandroids.animation.AnimatorSet.Builder builder = animSet
					.play(anims[0]);
			for (int i = 1; i < anims.length; i++) {
				builder.with(anims[i]);
			}
		}
		animSet.start();
	}

	// 对高度进行变化
	public static ValueAnimator performAnimateH(final View target,
			final int start, final int end, int time) {
		ValueAnimator valueAnimator = ValueAnimator.ofInt(1, 100);
		valueAnimator.addUpdateListener(new AnimatorUpdateListener() {
			// 持有一个IntEvaluator对象，方便下面估值的时候使用
			private IntEvaluator mEvaluator = new IntEvaluator();

			@Override
			public void onAnimationUpdate(ValueAnimator animator) {
				// 获得当前动画的进度值，整型，1-100之间
				int currentValue = (Integer) animator.getAnimatedValue();
				// 计算当前进度占整个动画过程的比例，浮点型，0-1之间
				float fraction = currentValue / 100f;
				// 这里我偷懒了，不过有现成的干吗不用呢
				// 直接调用整型估值器通过比例计算出宽度，然后再设给Button
				target.getLayoutParams().height = mEvaluator.evaluate(fraction,
						start, end);
				target.requestLayout();
			}
		});
		if (time != 0) {
			valueAnimator.setDuration(time);
		}
		return valueAnimator;
	}

	// 对宽度进行变化
	public static ValueAnimator performAnimateW(final View target,
			final int start, final int end, int time) {
		ValueAnimator valueAnimator = ValueAnimator.ofInt(1, 100);
		valueAnimator.addUpdateListener(new AnimatorUpdateListener() {
			// 持有一个IntEvaluator对象，方便下面估值的时候使用
			private IntEvaluator mEvaluator = new IntEvaluator();

			@Override
			public void onAnimationUpdate(ValueAnimator animator) {
				// 获得当前动画的进度值，整型，1-100之间
				int currentValue = (Integer) animator.getAnimatedValue();
				// 计算当前进度占整个动画过程的比例，浮点型，0-1之间
				float fraction = currentValue / 100f;

				// 直接调用整型估值器通过比例计算出宽度，然后再设给Button
				target.getLayoutParams().width = mEvaluator.evaluate(fraction,
						start, end);
				target.requestLayout();
			}
		});
		if (time != 0) {
			valueAnimator.setDuration(time);
		}
		return valueAnimator;
	}

}
