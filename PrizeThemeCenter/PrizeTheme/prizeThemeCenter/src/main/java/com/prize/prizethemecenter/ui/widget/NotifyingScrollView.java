package com.prize.prizethemecenter.ui.widget;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * @author prize with modifications from Manuel Peinado
 */
public class NotifyingScrollView extends ScrollView {
	// Edge-effects don't mix well with the translucent action bar in Android
	// 2.X
	private boolean mDisableEdgeEffects = true;

	/**
	 * @author prize
	 */
	public interface OnScrollChangedListener {
		void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt);
	}

	private OnScrollChangedListener mOnScrollChangedListener;

	public NotifyingScrollView(Context context) {
		super(context);
	}

	public NotifyingScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public NotifyingScrollView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
		super.onScrollChanged(l, t, oldl, oldt);
		if (mOnScrollChangedListener != null) {
			mOnScrollChangedListener.onScrollChanged(this, l, t, oldl, oldt);
		}
	}

	public void setOnScrollChangedListener(OnScrollChangedListener listener) {
		mOnScrollChangedListener = listener;
	}

	@Override
	protected float getTopFadingEdgeStrength() {
		if (mDisableEdgeEffects
				&& Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			return 0.0f;
		}
		return super.getTopFadingEdgeStrength();
	}

	@Override
	protected float getBottomFadingEdgeStrength() {
		if (mDisableEdgeEffects
				&& Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			return 0.0f;
		}
		return super.getBottomFadingEdgeStrength();
	}
}