/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;

import com.mediatek.launcher3.ext.LauncherLog;

/**
 * The grid based layout used strictly for the widget/wallpaper tab of the AppsCustomize pane
 */
public class PagedViewGridLayout extends GridLayout implements Page {
    static final String TAG = "PagedViewGridLayout";

    private int mCellCountX;
    private int mCellCountY;
    private Runnable mOnLayoutListener;

    public PagedViewGridLayout(Context context, int cellCountX, int cellCountY) {
        super(context, null, 0);
        mCellCountX = cellCountX;
        mCellCountY = cellCountY;
    }

    int getCellCountX() {
        return mCellCountX;
    }

    int getCellCountY() {
        return mCellCountY;
    }

    /**
     * Clears all the key listeners for the individual widgets.
     */
    public void resetChildrenOnKeyListeners() {
        int childCount = getChildCount();
        for (int j = 0; j < childCount; ++j) {
            getChildAt(j).setOnKeyListener(null);
        }
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // PagedView currently has issues with different-sized pages since it calculates the
        // offset of each page to scroll to before it updates the actual size of each page
        // (which can change depending on the content if the contents aren't a fixed size).
        // We work around this by having a minimum size on each widget page).
        int widthSpecSize = Math.min(getSuggestedMinimumWidth(),
                MeasureSpec.getSize(widthMeasureSpec));
        int widthSpecMode = MeasureSpec.EXACTLY;
        super.onMeasure(MeasureSpec.makeMeasureSpec(widthSpecSize, widthSpecMode),
                heightMeasureSpec);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mOnLayoutListener = null;
    }

    public void setOnLayoutListener(Runnable r) {
        mOnLayoutListener = r;
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mOnLayoutListener != null) {
            mOnLayoutListener.run();
        }
        

		int p = 0;//(int) (12*Launcher.scale);
		int childCount = getChildCount();
		int width = this.getMeasuredWidth();
		int childleft = 0;
		for (int i = 0; i < childCount; i++) {
			final View childView = getChildAt(i);
			int height = childView.getLayoutParams().height;
			final int childWidth = (int) (85*Launcher.scale);//childView.getLayoutParams().width;
			int left1 = childWidth * childCount + p * (childCount - 1);
			if (childView.getVisibility() != View.GONE) {
				int pageNum =0;
				// 此处获取到的宽度就是在onMeasure中设置的值
				if(childCount>5) {
					pageNum = i/6;
					left = ((childWidth+p)*i - pageNum*p);
				}else {
					 left = (int) (width / 2f - left1 / 2f) + i
							* (childWidth + p);
				}
				 right = left + childWidth;

				// 为每一个子View布局
				childView.layout(left, 0, right, height);

			}
		}
	
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);
        int count = getPageChildCount();
        if (count > 0) {
            // We only intercept the touch if we are tapping in empty space after the final row
            View child = getChildOnPageAt(count - 1);
            int bottom = child.getBottom();
            result = result || (event.getY() < bottom);
        }
        return result;
    }

    @Override
    public void removeAllViewsOnPage() {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "removeAllViewsOnPage: this = " + this);
        }
        removeAllViews();
        mOnLayoutListener = null;
        setLayerType(LAYER_TYPE_NONE, null);
    }

    @Override
    public void removeViewOnPageAt(int index) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "removeViewOnPageAt: index = " + index);
        }
        removeViewAt(index);
    }

    @Override
    public int getPageChildCount() {
        return getChildCount();
    }

    @Override
    public View getChildOnPageAt(int i) {
        return getChildAt(i);
    }

    @Override
    public int indexOfChildOnPage(View v) {
        return indexOfChild(v);
    }

    public static class LayoutParams extends FrameLayout.LayoutParams {
        public LayoutParams(int width, int height) {
            super(width, height);
        }
    }
}
