package com.android.launcher3;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class DrawEditIcons {
	/**
	 * Draw unread number for the given icon.
	 * 
	 * @param canvas
	 * @param icon
	 * @return
	 */
	static void drawUnreadEventIfNeed(Canvas canvas, View icon) {
		ItemInfo info = (ItemInfo) icon.getTag();
		if (info != null /* && info.unreadNum > 0 */) {
			Resources res = icon.getContext().getResources();

			// / M: Meature sufficent width for unread text and background image
			Paint unreadTextNumberPaint = new Paint();
			unreadTextNumberPaint.setTextSize(res
					.getDimension(R.dimen.unread_text_number_size));
			unreadTextNumberPaint.setTypeface(Typeface.DEFAULT_BOLD);
			unreadTextNumberPaint.setColor(0xffffffff);
			unreadTextNumberPaint.setTextAlign(Paint.Align.CENTER);

			Paint unreadTextPlusPaint = new Paint(unreadTextNumberPaint);
			unreadTextPlusPaint.setTextSize(res
					.getDimension(R.dimen.unread_text_plus_size));

			String unreadTextNumber = "1";
			String unreadTextPlus = "+";
			Rect unreadTextNumberBounds = new Rect(0, 0, 0, 0);
			Rect unreadTextPlusBounds = new Rect(0, 0, 0, 0);
			/*
			 * if (info.unreadNum > Launcher.MAX_UNREAD_COUNT) {
			 * unreadTextNumber = String.valueOf(Launcher.MAX_UNREAD_COUNT);
			 * unreadTextPlusPaint.getTextBounds(unreadTextPlus, 0,
			 * unreadTextPlus.length(), unreadTextPlusBounds); } else {
			 * unreadTextNumber = String.valueOf(info.unreadNum); }
			 */
			unreadTextNumberPaint.getTextBounds(unreadTextNumber, 0,
					unreadTextNumber.length(), unreadTextNumberBounds);
			int textHeight = unreadTextNumberBounds.height();
			int textWidth = unreadTextNumberBounds.width()
					+ unreadTextPlusBounds.width();

			// / M: Draw unread background image.
			Drawable unreadBgNinePatchDrawable = (Drawable) res
					.getDrawable(R.drawable.ic_newevents_numberindication);
			int unreadBgWidth = unreadBgNinePatchDrawable.getIntrinsicWidth();
			int unreadBgHeight = unreadBgNinePatchDrawable.getIntrinsicHeight();

			int unreadMinWidth = (int) res
					.getDimension(R.dimen.unread_minWidth);
			if (unreadBgWidth < unreadMinWidth) {
				unreadBgWidth = unreadMinWidth;
			}
			int unreadTextMargin = (int) res
					.getDimension(R.dimen.unread_text_margin);
			if (unreadBgWidth < textWidth + unreadTextMargin) {
				unreadBgWidth = textWidth + unreadTextMargin;
			}
			if (unreadBgHeight < textHeight) {
				unreadBgHeight = textHeight;
			}
			Rect unreadBgBounds = new Rect(0, 0, unreadBgWidth, unreadBgHeight);
			unreadBgNinePatchDrawable.setBounds(unreadBgBounds);

			int unreadMarginTop = 0;
			int unreadMarginRight = 0;
			if (info instanceof ShortcutInfo) {
				if (info.container == (long) LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
					unreadMarginTop = (int) res
							.getDimension(R.dimen.hotseat_unread_margin_top);
					unreadMarginRight = (int) res
							.getDimension(R.dimen.hotseat_unread_margin_right);
				} else if (info.container == (long) LauncherSettings.Favorites.CONTAINER_DESKTOP) {
					unreadMarginTop = (int) res
							.getDimension(R.dimen.workspace_unread_margin_top);
					unreadMarginRight = (int) res
							.getDimension(R.dimen.workspace_unread_margin_right);
				} else {
					unreadMarginTop = (int) res
							.getDimension(R.dimen.folder_unread_margin_top);
					unreadMarginRight = (int) res
							.getDimension(R.dimen.folder_unread_margin_right);
				}
			} else if (info instanceof FolderInfo) {
				if (info.container == (long) LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
					unreadMarginTop = (int) res
							.getDimension(R.dimen.hotseat_unread_margin_top);
					unreadMarginRight = (int) res
							.getDimension(R.dimen.hotseat_unread_margin_right);
				} else if (info.container == (long) LauncherSettings.Favorites.CONTAINER_DESKTOP) {
					unreadMarginTop = (int) res
							.getDimension(R.dimen.workspace_unread_margin_top);
					unreadMarginRight = (int) res
							.getDimension(R.dimen.workspace_unread_margin_right); 
				}
			} /*
			 * else if (info instanceof ApplicationInfo) { unreadMarginTop =
			 * (int) res .getDimension(R.dimen.app_list_unread_margin_top);
			 * unreadMarginRight = (int) res
			 * .getDimension(R.dimen.app_list_unread_margin_right); }
			 */

			int unreadBgPosX = icon.getScrollX() + icon.getWidth()
					- unreadBgWidth - unreadMarginRight;
			int unreadBgPosY = icon.getScrollY() + unreadMarginTop;

			canvas.save();
			canvas.translate(unreadBgPosX, unreadBgPosY);

			unreadBgNinePatchDrawable.draw(canvas);

			// / M: Draw unread text.
			Paint.FontMetrics fontMetrics = unreadTextNumberPaint
					.getFontMetrics();
			if (/* info.unreadNum > Launcher.MAX_UNREAD_COUNT */false) {
				canvas.drawText(unreadTextNumber,
						(unreadBgWidth - unreadTextPlusBounds.width()) / 2,
						(unreadBgHeight + textHeight) / 2,
						unreadTextNumberPaint);
				canvas.drawText(unreadTextPlus,
						(unreadBgWidth + unreadTextNumberBounds.width()) / 2,
						(unreadBgHeight + textHeight) / 2 + fontMetrics.ascent
								/ 2, unreadTextPlusPaint);
			} else {
				canvas.drawText(unreadTextNumber, unreadBgWidth / 2,
						(unreadBgHeight + textHeight) / 2,
						unreadTextNumberPaint);
			}

			canvas.restore();
		}
	}
	//add by zhouerlong 0728 begin
	static void drawDataTimeIcon(Canvas canvas, View icon, Drawable d) {
		Rect bgBounds = new Rect(0, 0, d.getIntrinsicWidth(),
				d.getIntrinsicHeight());
		d.setBounds(bgBounds);

		int bgposX = icon.getScrollX();
		int bgposY = icon.getScrollY();

		canvas.save();
		canvas.translate(bgposX, bgposY);
		d.draw(canvas);
		canvas.restore();

	}
	
	
	public static void drawFirstInstall(Canvas canvas, View icon,int id) {
		ItemInfo info = (ItemInfo) icon.getTag();
		if (info != null /* && info.unreadNum > 0 */) {
			Resources res = icon.getContext().getResources();
			Drawable unreadBgNinePatchDrawable = (Drawable) res
					.getDrawable(id);
			int width = unreadBgNinePatchDrawable.getIntrinsicWidth();
			int height = unreadBgNinePatchDrawable.getIntrinsicHeight();
			
			int unreadBgWidth = width/*(int) (unreadBgNinePatchDrawable.getIntrinsicWidth())*/;//图标太大
			int unreadBgHeight = height/*(int) (unreadBgNinePatchDrawable.getIntrinsicHeight())*/;
			/*if (width == 0 || height == 0) {

				 unreadBgWidth = (int) (unreadBgNinePatchDrawable.getIntrinsicWidth());//图标太大
				 unreadBgHeight = (int) (unreadBgNinePatchDrawable.getIntrinsicHeight());
			}*/
			Rect unreadBgBounds = new Rect(0, 0, unreadBgWidth, unreadBgHeight);
			unreadBgNinePatchDrawable.setBounds(unreadBgBounds);

			int unreadMarginTop = 0;
			int unreadMarginRight = 0;
			if (info instanceof ShortcutInfo || info instanceof AppInfo) {//如果是应用类型也添加删除图标的位置操作
				if (info.container == (long) LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
					unreadMarginTop = (int) res
							.getDimension(R.dimen.hotseat_unread_margin_top);
					unreadMarginRight = (int) res
							.getDimension(R.dimen.hotseat_unread_margin_right);
				} else if (info.container == (long) LauncherSettings.Favorites.CONTAINER_DESKTOP) {
					unreadMarginTop = (int) res
							.getDimension(R.dimen.workspace_unread_margin_top);
					unreadMarginRight = (int) res
							.getDimension(R.dimen.workspace_unread_margin_right);
				} else {
					unreadMarginTop = (int) res
							.getDimension(R.dimen.folder_unread_margin_top);
					unreadMarginRight = (int) res
							.getDimension(R.dimen.folder_unread_margin_right);
				}
			} else if (info instanceof FolderInfo) {
				if (info.container == (long) LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
					unreadMarginTop = (int) res
							.getDimension(R.dimen.hotseat_unread_margin_top);
					unreadMarginRight = (int) res
							.getDimension(R.dimen.hotseat_unread_margin_right);
				} else if (info.container == (long) LauncherSettings.Favorites.CONTAINER_DESKTOP) {
					unreadMarginTop = (int) res
							.getDimension(R.dimen.workspace_unread_margin_top);
					unreadMarginRight = (int) res
							.getDimension(R.dimen.workspace_unread_margin_right); 
				}
			} 
            /*Prize--调整首次安装小点的位置--fuqiang--2016-02-27--begin*/
			int unreadBgPosX = icon.getScrollX() ;
					//+ unreadBgWidth ;
			int unreadBgPosY = icon.getScrollY();//(int) (icon.getScrollY() +icon.getHeight()-unreadBgHeight/2*Launcher.scale-unreadMarginTop-unreadMarginTop/2 - icon.getPaddingLeft() + icon.getPaddingLeft()/8);
			/*Prize--调整首次安装小点的位置--fuqiang--2016-02-27--end*/

			canvas.save();
			Paint paint = new Paint();
			canvas.translate(unreadBgPosX, unreadBgPosY);
			drawCenterLeftDrawable(canvas,height,width,(TextView)icon);
			unreadBgNinePatchDrawable.draw(canvas);
			canvas.restore();
		}
	}
	
	public static  void drawCenterLeftDrawable(Canvas canvas,int h,int w,TextView v) {
  	  Drawable[] drawables = v.getCompoundDrawables();
        if (drawables != null) {
            Drawable drawableTop = drawables[1];
            if (drawableTop != null) {
                float textWidth = v.getPaint().measureText(v.getText().toString());
//                int drawablePadding = getCompoundDrawablePadding();
//                int drawableWidth = 0;
//                drawableWidth = drawableTop.getIntrinsicWidth();
                
//                float bodyWidth = textWidth + drawableWidth + drawablePadding;
                int maxWidth=v.getWidth()-v.getPaddingLeft()-v.getPaddingRight()-w/2;
                textWidth = Math.min(maxWidth, textWidth);
                float left =  (v.getWidth() - textWidth) / 2-w;
                int y = v.getPaddingTop()+drawableTop.getIntrinsicHeight();
                int textHeight = v.getHeight()-v.getPaddingTop()-drawableTop.getIntrinsicHeight();
//                int top = y+(textHeight-h)/2;
                int top = (int) (y+(textHeight-(h*Launcher.scale/2))/2);
                canvas.translate(left, top);
            }
        }
  }
	//add by zhouerlong 0728 end
	static void drawStateIcon(Canvas canvas, View icon,int id,int width,int height,float percent) {
		ItemInfo info = (ItemInfo) icon.getTag();
		if (info != null /* && info.unreadNum > 0 */) {
			Resources res = icon.getContext().getResources();
			Drawable unreadBgNinePatchDrawable = (Drawable) res
					.getDrawable(id);
			
			int unreadBgWidth = width/*(int) (unreadBgNinePatchDrawable.getIntrinsicWidth())*/;//图标太大
			int unreadBgHeight = height/*(int) (unreadBgNinePatchDrawable.getIntrinsicHeight())*/;
			/*if (width == 0 || height == 0) {

				 unreadBgWidth = (int) (unreadBgNinePatchDrawable.getIntrinsicWidth());//图标太大
				 unreadBgHeight = (int) (unreadBgNinePatchDrawable.getIntrinsicHeight());
			}*/
			Rect unreadBgBounds = new Rect(0, 0, unreadBgWidth, unreadBgHeight);
			unreadBgNinePatchDrawable.setBounds(unreadBgBounds);

			int unreadMarginTop = 0;
			int unreadMarginRight = 0;
			if (info instanceof ShortcutInfo || info instanceof AppInfo) {//如果是应用类型也添加删除图标的位置操作
				if (info.container == (long) LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
					unreadMarginTop = (int) res
							.getDimension(R.dimen.hotseat_uninstall_margin_top);
					unreadMarginRight = (int) res
							.getDimension(R.dimen.hotseat_uninstall_margin_right);
				} else if (info.container == (long) LauncherSettings.Favorites.CONTAINER_DESKTOP) {
					unreadMarginTop = (int) res
							.getDimension(R.dimen.workspace_uninstall_margin_top);
					unreadMarginRight = (int) res
							.getDimension(R.dimen.workspace_uninstall_margin_right);
				} else {
					unreadMarginTop = (int) res
							.getDimension(R.dimen.folder_uninstall_margin_top);
					unreadMarginRight = (int) res
							.getDimension(R.dimen.folder_uninstall_margin_right);
				}
			} else if (info instanceof FolderInfo) {
				if (info.container == (long) LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
					unreadMarginTop = (int) res
							.getDimension(R.dimen.hotseat_uninstall_margin_top);
					unreadMarginRight = (int) res
							.getDimension(R.dimen.hotseat_uninstall_margin_right);
				} else if (info.container == (long) LauncherSettings.Favorites.CONTAINER_DESKTOP) {
					unreadMarginTop = (int) res
							.getDimension(R.dimen.workspace_uninstall_margin_top);
					unreadMarginRight = (int) res
							.getDimension(R.dimen.workspace_uninstall_margin_right); 
				}
			} 

			int unreadBgPosX = icon.getScrollX() 
					+ unreadBgWidth -unreadMarginRight;
			if(icon instanceof LauncherAppWidgetHostView) {
				unreadBgPosX = icon.getScrollX();
			}
			int unreadBgPosY = icon.getScrollY() + unreadMarginTop;

			canvas.save();
			Paint paint = new Paint();
			canvas.translate(unreadBgPosX, unreadBgPosY);
        	canvas.scale(percent, percent,unreadBgWidth/2,unreadBgHeight/2);
//			Log.i("zhouerlong", "percent::::"+percent+"alple:"+(int)(percent*255));
			unreadBgNinePatchDrawable.setAlpha((int) (percent*255));
			unreadBgNinePatchDrawable.draw(canvas);
			canvas.restore();
		}
	}
	
	
	/**绘画批处理图标勾选状态
	 * @param canvas
	 * @param icon
	 * @param id
	 */
	public static void drawStateIconForBatch(Canvas canvas, View icon,int id,int w,int h,double percent) {
		ItemInfo info = (ItemInfo) icon.getTag();
		if (info != null /* && info.unreadNum > 0 */) {
			Resources res = icon.getContext().getResources();
			Drawable unreadBgNinePatchDrawable = (Drawable) res
					.getDrawable(id);
			int unreadBgWidth =w ;//(int) (unreadBgNinePatchDrawable.getIntrinsicWidth()/1.8f);//图标太大
			int unreadBgHeight =h;// (int) (unreadBgNinePatchDrawable.getIntrinsicHeight()/1.8f);
			Rect unreadBgBounds = new Rect(0, 0, unreadBgWidth, unreadBgHeight);
			unreadBgNinePatchDrawable.setBounds(unreadBgBounds);

			int unreadMarginTop = 0;
			int unreadMarginRight = 0;
			if (info instanceof ShortcutInfo || info instanceof AppInfo) {//如果是应用类型也添加删除图标的位置操作
				if (info.container == (long) LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
					unreadMarginTop = (int) res
							.getDimension(R.dimen.hotseat_unread_margin_top);
					unreadMarginRight = (int) res
							.getDimension(R.dimen.hotseat_unread_margin_right);
				} else if (info.container == (long) LauncherSettings.Favorites.CONTAINER_DESKTOP) {
					unreadMarginTop = (int) res
							.getDimension(R.dimen.workspace_unread_margin_top);
					unreadMarginRight = (int) res
							.getDimension(R.dimen.workspace_unread_margin_right);
				} else {
					unreadMarginTop = (int) res
							.getDimension(R.dimen.folder_unread_margin_top);
					unreadMarginRight = (int) res
							.getDimension(R.dimen.folder_unread_margin_right);
				}
			} else if (info instanceof FolderInfo) {
				if (info.container == (long) LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
					unreadMarginTop = (int) res
							.getDimension(R.dimen.hotseat_unread_margin_top);
					unreadMarginRight = (int) res
							.getDimension(R.dimen.hotseat_unread_margin_right);
				} else if (info.container == (long) LauncherSettings.Favorites.CONTAINER_DESKTOP) {
					unreadMarginTop = (int) res
							.getDimension(R.dimen.workspace_unread_margin_top);
					unreadMarginRight = (int) res
							.getDimension(R.dimen.workspace_unread_margin_right); 
				}
			} 

			int unreadBgPosX = icon.getScrollX() + icon.getWidth()
					- unreadBgWidth - unreadMarginRight;
			int unreadBgPosY = icon.getScrollY() + unreadMarginTop;

			canvas.save();
			canvas.translate(unreadBgPosX, unreadBgPosY);

			unreadBgNinePatchDrawable.setAlpha((int) (percent*255));
			unreadBgNinePatchDrawable.draw(canvas);
			canvas.restore();
		}
	}
	
	
	
	//A by zel
	static void drawSelect(Canvas canvas, View icon,int w,int h) {
		if (icon.isSelected()) {
			Resources res = icon.getContext().getResources();
			Drawable unreadBgNinePatchDrawable = (Drawable) res
					.getDrawable(R.drawable.in_use);
			int unreadBgWidth = w;
			int unreadBgHeight = h;
			Rect unreadBgBounds = new Rect(0, 0, unreadBgWidth, unreadBgHeight);
			unreadBgNinePatchDrawable.setBounds(unreadBgBounds);

			int unreadMarginTop = unreadBgWidth/4;
			int unreadMarginRight = unreadBgWidth/4;
			int unreadBgPosX = icon.getScrollX() + icon.getWidth()
					- unreadBgWidth - unreadMarginRight;
			int unreadBgPosY = icon.getScrollY() + unreadMarginTop;

			canvas.save();
			canvas.translate(unreadBgPosX, unreadBgPosY);

			unreadBgNinePatchDrawable.draw(canvas);
			canvas.restore();
		}
	}
	
	
	

}