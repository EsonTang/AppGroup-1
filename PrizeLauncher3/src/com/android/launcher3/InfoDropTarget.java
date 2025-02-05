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

import android.content.ComponentName;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.android.launcher3.DropTarget.DragObject;
import com.mediatek.launcher3.ext.LauncherLog;

public class InfoDropTarget extends ButtonDropTarget {

    private ColorStateList mOriginalTextColor;
    private TransitionDrawable mDrawable;
    private static int DELETE_ANIMATION_DURATION = 285;
    private String mSAppInfo=null;
    private String mSFolderDeCompression=null;

    public InfoDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public InfoDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mOriginalTextColor = getTextColors();

        // Get the hover color
        Resources r = getResources();
        mSAppInfo = r.getString(R.string.info_target_label);
        mSFolderDeCompression = r.getString(R.string.disband);
        mHoverColor = r.getColor(R.color.info_target_hover_tint);
        mDrawable = (TransitionDrawable) getCurrentDrawable();
        if (null != mDrawable) {
            mDrawable.setCrossFadeEnabled(true);
        }

        // Remove the text in the Phone UI in landscape
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (!LauncherAppState.getInstance().isScreenLarge()) {
                setText("");
            }
        }
    }

    private boolean isFromAllApps(DragSource source) {
        return(source instanceof AppsCustomizePagedView);
    }

    @Override
    public boolean acceptDrop(DragObject d) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "acceptDrop: d = " + d + ", d.dragInfo = " + d.dragInfo);
        }

        // acceptDrop is called just before onDrop. We do the work here, rather than
        // in onDrop, because it allows us to reject the drop (by returning false)
        // so that the object being dragged isn't removed from the drag source.
        ComponentName componentName = null;
        if (d.dragInfo instanceof AppInfo) {
            componentName = ((AppInfo) d.dragInfo).componentName;
        } else if (d.dragInfo instanceof ShortcutInfo) {
            componentName = ((ShortcutInfo) d.dragInfo).intent.getComponent();
        } else if (d.dragInfo instanceof PendingAddItemInfo) {
            componentName = ((PendingAddItemInfo) d.dragInfo).componentName;
        }
        if (componentName != null) {
            mLauncher.startApplicationDetailsActivity(componentName);
        }

        if (d.dragInfo instanceof FolderInfo) {

            return willAcceptDrop(d.dragInfo); //检测是否为访问模式
        }
        // There is no post-drop animation, so clean up the DragView now
        d.deferDragViewCleanupPostAnimation = false;
        return false;
    }
    
    
    public static boolean willAcceptDrop(Object info) {
        if (info instanceof ItemInfo) {
            ItemInfo item = (ItemInfo) info;
            if (item.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET ||
                    item.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) {
                return true;
            }

            if (!AppsCustomizePagedView.DISABLE_ALL_APPS &&
                    item.itemType == LauncherSettings.Favorites.ITEM_TYPE_FOLDER) {
                return true;
            }

            if (!AppsCustomizePagedView.DISABLE_ALL_APPS &&
                    item.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION &&
                    item instanceof AppInfo) {
                AppInfo appInfo = (AppInfo) info;
                return (appInfo.flags & AppInfo.DOWNLOADED_FLAG) != 0;
            }

            if (item.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION &&
                item instanceof ShortcutInfo) {
                if (AppsCustomizePagedView.DISABLE_ALL_APPS) {
                    ShortcutInfo shortcutInfo = (ShortcutInfo) info;
                    return (shortcutInfo.flags & AppInfo.DOWNLOADED_FLAG) != 0;
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onDragStart(DragSource source, Object info, int dragAction) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "onDratStart: source = " + source + ", info = " + info
                    + ", dragAction = " + dragAction);
        }

        boolean isVisible = true;

        // Hide this button unless we are dragging something from AllApps
        if (!isFromAllApps(source) || Launcher.isInEditMode()) { /// M: Add for op09 Edit and Hide app icons.
            isVisible = false;
        }

    	this.setText(mSAppInfo);
    	boolean folderDecompression=this.getResources().getBoolean(R.bool.is_folder_decompression);
        if (info instanceof FolderInfo && folderDecompression) {
        	this.setText(mSFolderDeCompression);
        	isVisible=false;
        }

        mActive = isVisible;
        mDrawable.resetTransition();
        setTextColor(mOriginalTextColor);
        ((ViewGroup) getParent()).setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDragEnd() {
        super.onDragEnd();
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "onDragEnd.");
        }
        mActive = false;
    }

    public void onDragEnter(DragObject d) {
        super.onDragEnter(d);
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "onDragEnter: d = " + d);
        }
//        mLauncher.setFolderDecompression(true);//启动的时候将其设置为文件夹分解模式
        mDrawable.startTransition(mTransitionDuration);
        setTextColor(mHoverColor);
    }

	private void animateToTrashAndCompleteDrop(final DragObject d) {
		final DragLayer dragLayer = mLauncher.getDragLayer();
		final Rect from = new Rect();
		dragLayer.getViewRectRelativeToSelf(d.dragView, from);
		final Rect to = getIconRect(d.dragView.getMeasuredWidth(),
				d.dragView.getMeasuredHeight(), mDrawable.getIntrinsicWidth(),
				mDrawable.getIntrinsicHeight());
		final float scale = (float) to.width() / from.width();

		mSearchDropTargetBar.deferOnDragEnd();

		Runnable onAnimationEndRunnable = new Runnable() {
			@Override
			public void run() {
				mSearchDropTargetBar.onDragEnd();
				mLauncher.exitSpringLoadedDragMode(false);
			}
		};
		dragLayer.animateView(d.dragView, from, to, scale, 1f, 1f, 0.1f, 0.1f,
				DELETE_ANIMATION_DURATION, new DecelerateInterpolator(2),
				new LinearInterpolator(), onAnimationEndRunnable,
				DragLayer.ANIMATION_END_DISAPPEAR, null, 0, false,null);
	}

	@Override
	public void onDrop(DragObject d) {
		super.onDrop(d);
		if (d.dragInfo instanceof FolderInfo) {
			animateToTrashAndCompleteDrop(d);
			if (d.dragInfo instanceof FolderInfo) {
				FolderInfo folderInfo = (FolderInfo) d.dragInfo;
				mLauncher.addExternalShortItemsToScreen(folderInfo,d.dragView);//add by zel
				mLauncher.removeFolder(folderInfo);

				LauncherModel.deleteFolderContentsFromDatabase(mLauncher,
						folderInfo);

			}
		}
	}

	public void onDragExit(DragObject d) {
        super.onDragExit(d);
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "onDragExit: d = " + d + ", d.dragComplete = " + d.dragComplete);
        }
        
        if (!d.dragComplete) {
            mDrawable.resetTransition();
            setTextColor(mOriginalTextColor);
        }
    }
}