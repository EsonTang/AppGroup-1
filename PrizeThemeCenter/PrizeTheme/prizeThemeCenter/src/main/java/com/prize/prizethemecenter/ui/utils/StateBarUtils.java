package com.prize.prizethemecenter.ui.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.prize.prizethemecenter.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.StatusBarManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

/**
 * 处理状态栏工具类
 * @author Administrator
 */
public class StateBarUtils {
	/**
	 * 沉浸式状态栏方法
	 * @param a
	 */
	@SuppressLint({ "NewApi", "ResourceAsColor" })
	public static void initStateBar(Activity a) {
		Window window = ((Activity) a).getWindow();
		window.requestFeature(Window.FEATURE_NO_TITLE);
		if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
			window = a.getWindow();
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
					| WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

			window.getDecorView().setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
							| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
							| View.SYSTEM_UI_FLAG_IMMERSIVE);

			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

			window.setStatusBarColor(Color.TRANSPARENT);
		}
	}

	public static void initStateBar(Activity a, int color) {
		Window window = ((Activity) a).getWindow();
		window.requestFeature(Window.FEATURE_NO_TITLE);
		if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
			window = a.getWindow();
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
					| WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

			window.getDecorView().setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
							| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
							| View.SYSTEM_UI_FLAG_IMMERSIVE);

			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

			window.setStatusBarColor(color);
		}
	}

	/**
	 * 方法描述：沉浸式状态栏
	 */
	public static void initStateBar(Window window, Context mCtx) {
		window.requestFeature(Window.FEATURE_NO_TITLE);
		if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
					| WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
			window.getDecorView().setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
							| View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.setStatusBarColor(mCtx.getResources().getColor(
					R.color.transparent));
		}
	}

	/**
	 * 是否含有虚拟键
	 * 
	 * @param context
	 * @return
	 */
	public static boolean checkDeviceHasNavigationBar(Context context) {
		if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
			boolean hasNavigationBar = false;
			Resources rs = context.getResources();
			int id = rs.getIdentifier("config_showNavigationBar", "bool",
					"android");
			if (id > 0) {
				hasNavigationBar = rs.getBoolean(id);
			}
			try {
				Class systemPropertiesclass = Class
						.forName("android.os.SystemProperties");
				Method m = systemPropertiesclass.getMethod("get", String.class);
				String navBarOverride = (String) m.invoke(
						systemPropertiesclass, "qemu.hw.mainkeys");
				if ("1".equals(navBarOverride)) {
					hasNavigationBar = false;
				} else if ("0".equals(navBarOverride)) {
					hasNavigationBar = true;
				}
			} catch (Exception e) {
				return hasNavigationBar;
			}

			return hasNavigationBar;
		}
		return false;
	}

	/**
	 * 获取虚拟按键的高度
	 * 
	 * @param context
	 * @return
	 */
	public static int getNavigationBarHeight(Context context) {

		int navigationBarHeight = 0;
		if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
			Resources rs = context.getResources();
			int id = rs.getIdentifier("navigation_bar_height", "dimen",
					"android");
			if (id > 0 && checkDeviceHasNavigationBar(context)) {
				navigationBarHeight = rs.getDimensionPixelSize(id);
			}
			return navigationBarHeight;
		}
		return 0;
	}

	/**
	 * @see Inverse status ico,invoked after setContentView
	 * @param mWindow
	 */
	public static void changeStatus(Window mWindow) {
		WindowManager.LayoutParams lp = mWindow.getAttributes();
		Field[] fields = LayoutParams.class.getDeclaredFields();
		boolean b = false;
		for (int i = 0; i < fields.length; i++) {// 暂时如此处理，防止在不同的手机crash
			if (fields[i].getName().equals("statusBarInverse")) {
				b = true;
				break;
			}
		}
		if (b) {
			lp.statusBarInverse = StatusBarManager.STATUS_BAR_INVERSE_GRAY;
			mWindow.setAttributes(lp);
		}
	}

	public static void changeOriginalStatus(Window mWindow) {
		WindowManager.LayoutParams lp = mWindow.getAttributes();
		Field[] fields = LayoutParams.class.getDeclaredFields();
		boolean b = false;
		for (int i = 0; i < fields.length; i++) {// 暂时如此处理，防止在不同的手机crash
			if (fields[i].getName().equals("statusBarInverse")) {
				b = true;
				break;
			}
		}
		if (b) {
			lp.statusBarInverse = StatusBarManager.STATUS_BAR_COLOR_WHITE;
			mWindow.setAttributes(lp);
		}
	}
}
