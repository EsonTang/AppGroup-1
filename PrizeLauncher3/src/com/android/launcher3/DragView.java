/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.os.Trace;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.android.gallery3d.util.LogUtils;
import com.android.launcher3.R;
import com.android.launcher3.Workspace.ZoomInInterpolator;

import com.mediatek.launcher3.ext.LauncherLog;

public class DragView extends View {
    private static final String TAG = "DragView";
    private static float sDragAlpha = 1f;

    private Bitmap mBitmap;
    private Bitmap mCrossFadeBitmap;
    private Paint mPaint;
    private int mRegistrationX;
    public int getRegistrationX() {
		return mRegistrationX;
	}
    
    

	@Override
	public void bringToFront() {
		Log.i("zhouerlong", "bringToFront");
		super.bringToFront();
	}



	public void setRegistrationX(int mRegistrationX) {
		this.mRegistrationX = mRegistrationX;
	}

	public int getRegistrationY() {
		return mRegistrationY;
	}

	public void setRegistrationY(int mRegistrationY) {
		this.mRegistrationY = mRegistrationY;
	}


	private int mRegistrationY;

    private Point mDragVisualizeOffset = null;
    private Rect mDragRegion = null;
    private DragLayer mDragLayer = null;
    private boolean mHasDrawn = false;
    private float mCrossFadeProgress = 0f;

    ValueAnimator mAnim;
    private float mOffsetX = 0.0f;
    private float mOffsetY = 0.0f;
    private float mInitialScale = 1f;

    /**
     * Construct the drag view.
     * <p>
     * The registration point is the point inside our view that the touch events should
     * be centered upon.
     *
     * @param launcher The Launcher instance
     * @param bitmap The view that we're dragging around.  We scale it up when we draw it.
     * @param registrationX The x coordinate of the registration point.
     * @param registrationY The y coordinate of the registration point.
     */
    public DragView(Launcher launcher, Bitmap bitmap, int registrationX, int registrationY,
            int left, int top, int width, int height, final float initialScale) {
        super(launcher);
        mDragLayer = launcher.getDragLayer();
        mInitialScale = initialScale;

        final Resources res = getResources();
        final float offsetX = res.getDimensionPixelSize(R.dimen.dragViewOffsetX);
        final float offsetY = res.getDimensionPixelSize(R.dimen.dragViewOffsetY);
        final float scaleDps = res.getDimensionPixelSize(R.dimen.dragViewScale);
        final float scale = (width + scaleDps) / width;

        // Set the initial scale to avoid any jumps
        setScaleX(initialScale);
        setScaleY(initialScale);

        // Animate the view into the correct position
        mAnim = LauncherAnimUtils.ofFloat(this, 0f, 1f);
        mAnim.setDuration(this.getResources().getInteger(R.integer.config_dropAnimAndFadeDuration));
        mAnim.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final float value = (Float) animation.getAnimatedValue();

                final int deltaX = (int) ((value * offsetX) - mOffsetX);
                final int deltaY = (int) ((value * offsetY) - mOffsetY);

                mOffsetX += deltaX;
                mOffsetY += deltaY;
                setScaleX(initialScale + (value * (scale - initialScale)));
                setScaleY(initialScale + (value * (scale - initialScale)));
                if (sDragAlpha != 1f) {
                    setAlpha(sDragAlpha * value + (1f - value));
                }

                if (getParent() == null) {
                    animation.cancel();
                } else {
                    setTranslationX(getTranslationX() + deltaX);
                    setTranslationY(getTranslationY() + deltaY);
                }
            }
        });
		/*//释放 空间
        if (!mBitmap.isRecycled()) {
        	mBitmap.recycle();
        }*/
        mBitmap = Bitmap.createBitmap(bitmap, left, top, width, height);
        setDragRegion(new Rect(0, 0, width, height));

        // The point in our scaled bitmap that the touch events are located
        mRegistrationX = registrationX;
        mRegistrationY = registrationY;

        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "DragView constructor: mRegistrationX = " + mRegistrationX
                    + ", mRegistrationY = " + mRegistrationY + ", this = " + this);
        }

        // Force a measure, because Workspace uses getMeasuredHeight() before the layout pass
        int ms = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        measure(ms, ms);
        mPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    }

    public float getOffsetY() {
        return mOffsetY;
    }

    public int getDragRegionLeft() {
        return mDragRegion.left;
    }

    public int getDragRegionTop() {
        return mDragRegion.top;
    }

    public int getDragRegionWidth() {
        return mDragRegion.width();
    }

    public int getDragRegionHeight() {
        return mDragRegion.height();
    }

    public void setDragVisualizeOffset(Point p) {
        mDragVisualizeOffset = p;
    }

    public Point getDragVisualizeOffset() {
        return mDragVisualizeOffset;
    }

    public void setDragRegion(Rect r) {
        mDragRegion = r;
    }

    public Rect getDragRegion() {
        return mDragRegion;
    }

    public float getInitialScale() {
        return mInitialScale;
    }

    public void updateInitialScaleToCurrentScale() {
        mInitialScale = getScaleX();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mBitmap.getWidth(), mBitmap.getHeight());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        @SuppressWarnings("all") // suppress dead code warning
        final boolean debug = false;
        if (debug) {
            Paint p = new Paint();
            p.setStyle(Paint.Style.FILL);
            p.setColor(0x66ffffff);
            canvas.drawRect(0, 0, getWidth(), getHeight(), p);
        }

        mHasDrawn = true;
        boolean crossFade = mCrossFadeProgress > 0 && mCrossFadeBitmap != null;
        if (crossFade) {
            int alpha = crossFade ? (int) (255 * (1 - mCrossFadeProgress)) : 255;
            mPaint.setAlpha(alpha);
        }
        canvas.drawBitmap(mBitmap, 0.0f, 0.0f, mPaint);
        if (crossFade) {
            mPaint.setAlpha((int) (255 * mCrossFadeProgress));
            canvas.save();
            float sX = (mBitmap.getWidth() * 1.0f) / mCrossFadeBitmap.getWidth();
            float sY = (mBitmap.getHeight() * 1.0f) / mCrossFadeBitmap.getHeight();
            canvas.scale(sX, sY);
            canvas.drawBitmap(mCrossFadeBitmap, 0.0f, 0.0f, mPaint);
            canvas.restore();
        }
    }

    public void setCrossFadeBitmap(Bitmap crossFadeBitmap) {
        mCrossFadeBitmap = crossFadeBitmap;
    }

    public void crossFade(int duration) {
        ValueAnimator va = LauncherAnimUtils.ofFloat(this, 0f, 1f);
        va.setDuration(duration);
        va.setInterpolator(new DecelerateInterpolator(1.5f));
        va.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mCrossFadeProgress = animation.getAnimatedFraction();
            }
        });
        va.start();
    }

    public void setColor(int color) {
        if (mPaint == null) {
            mPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        }
        if (color != 0) {
            mPaint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
        } else {
            mPaint.setColorFilter(null);
        }
        invalidate();
    }

    public boolean hasDrawn() {
        return mHasDrawn;
    }

    @Override
    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
        mPaint.setAlpha((int) (255 * alpha));
        invalidate();
    }

    /**
     * Create a window containing this view and show it.
     *
     * @param windowToken obtained from v.getWindowToken() from one of your views
     * @param touchX the x coordinate the user touched in DragLayer coordinates
     * @param touchY the y coordinate the user touched in DragLayer coordinates
     */
    public void show(int touchX, int touchY) {
    	if (mDragLayer.indexOfChild(this)==-1) {
            mDragLayer.addView(this);
    	}

        // Start the pick-up animation
        DragLayer.LayoutParams lp = new DragLayer.LayoutParams(0, 0);
        lp.width = mBitmap.getWidth();
        lp.height = mBitmap.getHeight();
        lp.customPosition = true;
        setLayoutParams(lp);
        setTranslationX(touchX - mRegistrationX);
        setTranslationY(touchY - mRegistrationY);
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "show DragView: x = " + lp.x + ", y = " + lp.y + ", width = " + lp.width
                    + ", height = " + lp.height + ", this = " + this);
        }

        // Post the animation to skip other expensive work happening on the first frame
        post(new Runnable() {
                public void run() {
                    mAnim.start();
                }
            });
    }
    
    
    /**批量整理图标功能 显示功能
     * @param touchX
     * @param touchY
     */
    public void shows(int touchX, int touchY) {
    	if (mDragLayer.indexOfChild(this)==-1) {
    		Log.i("zhouerlong", "addddddddddddddd");
            mDragLayer.addView(this);
    	}

        // Start the pick-up animation
        DragLayer.LayoutParams lp = new DragLayer.LayoutParams(0, 0);
        lp.width = mBitmap.getWidth();
        lp.height = mBitmap.getHeight();
        lp.customPosition = true;
        setLayoutParams(lp);
        setTranslationX(touchX - mRegistrationX);
        setTranslationY(touchY - mRegistrationY);
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "show DragView: x = " + lp.x + ", y = " + lp.y + ", width = " + lp.width
                    + ", height = " + lp.height + ", this = " + this);
        }
    }
    
    public void showNoAnimation(int touchX, int touchY) { //将其add 到DragLayer中
    	if (mDragLayer.indexOfChild(this) ==-1) {
    		Log.i("zhouerlong", "addddddddddddddd111111111111");
            mDragLayer.addView(this);
            mDragLayer.bringChildToFront(this);
    	}
        
        
        // Start the pick-up animation
        DragLayer.LayoutParams lp = new DragLayer.LayoutParams(0, 0);
        lp.width = mBitmap.getWidth();//将其隐藏
        lp.height = mBitmap.getHeight();
        lp.customPosition = true;
        setLayoutParams(lp);
        setTranslationX(/*touchX - */mRegistrationX);//移动时候不需要 touchX 因为不是拖动 是点击
        setTranslationY(/*touchY - */mRegistrationY);
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "show DragView: x = " + lp.x + ", y = " + lp.y + ", width = " + lp.width
                    + ", height = " + lp.height + ", this = " + this);
        }
    }

    public void cancelAnimation() {
        if (mAnim != null && mAnim.isRunning()) {
            mAnim.cancel();
        }
    }

    public void resetLayoutParams() {
        mOffsetX = mOffsetY = 0;
        requestLayout();
    }

    /**
     * Move the window containing this view.
     *
     * @param touchX the x coordinate the user touched in DragLayer coordinates
     * @param touchY the y coordinate the user touched in DragLayer coordinates
     */
    void move(int touchX, int touchY) {
        /// M: Add systrace to track drag icon latency{@
        Trace.traceCounter(Trace.TRACE_TAG_INPUT, "posX", touchX);
        Trace.traceCounter(Trace.TRACE_TAG_INPUT, "posY", touchY);
        Trace.traceBegin(Trace.TRACE_TAG_INPUT, "move");
        /// M: @}
        setTranslationX(touchX - mRegistrationX + (int) mOffsetX);
        setTranslationY(touchY - mRegistrationY + (int) mOffsetY);
        /// M: Add systrace to track drag icon latency{@
        Trace.traceEnd(Trace.TRACE_TAG_INPUT);
        /// M: @}
    }
    

    void remove() {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "remove DragView: this = " + this);
        }

        if (getParent() != null) {
            mDragLayer.removeView(DragView.this);
        }
    }
}
