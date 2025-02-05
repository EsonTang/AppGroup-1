package com.prize.uploadappinfo.utils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.TextView;

import com.google.gson.Gson;
import com.prize.uploadappinfo.BaseApplication;
import com.prize.uploadappinfo.bean.AppInfo;
import com.prize.uploadappinfo.bean.AppRecordInfo;
import com.prize.uploadappinfo.bean.ClientInfo;
import com.prize.uploadappinfo.database.InstalledAppTable;
import com.prize.uploadappinfo.database.PrizeDatabaseHelper;
import com.prize.uploadappinfo.database.dao.AppInstalledDAO;
import com.prize.uploadappinfo.database.dao.AppStateDAO;

/**
 * 
 * @author longbaoxiu
 * @version V1.0
 */
public class CommonUtils {
	public static String TAG = "CommonUtils";
	public static int DOWNLOADING = 1;
	public static int DOWNLOADED = 2;
	public static int DOWNUPDATE = 3;

	/**
	 * 转换long类型为string类型，用于apk大小换算
	 * 
	 * @param fileS
	 * @return
	 */
	public static String formatSize(long fileS) {
		DecimalFormat df = new DecimalFormat("#.00");
		String fileSizeString = "";
		if (fileS < 1024) {
			fileSizeString = df.format((double) fileS) + "B";
		} else if (fileS < 1048576) {
			fileSizeString = df.format((double) fileS / 1024) + "KB";
		} else if (fileS < 1073741824) {
			fileSizeString = df.format((double) fileS / 1048576) + "MB";
		} else {
			fileSizeString = df.format((double) fileS / 1073741824) + "GB";
		}
		return fileSizeString;
	}

	/**
	 * 
	 * 转byte为M
	 * 
	 * @param size
	 *            long
	 * @return String
	 */
	public static String paresAppSize(long size) {
		return String.format("%1$.2f", size / (1024 * 1024f));
	}

	/**
	 * 程序是否在前台运行
	 * 
	 * @return
	 */
	public static boolean isAppOnForeground(Context contex) {
		ActivityManager activityManager = (ActivityManager) contex
				.getApplicationContext().getSystemService(
						Context.ACTIVITY_SERVICE);
		String packageName = contex.getApplicationContext().getPackageName();

		List<RunningAppProcessInfo> appProcesses = activityManager
				.getRunningAppProcesses();
		if (appProcesses == null)
			return false;

		for (RunningAppProcessInfo appProcess : appProcesses) {
			// The name of the process that this object is associated with.
			if (appProcess.processName.equals(packageName)
					&& appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
				return true;
			}
		}

		return false;
	}

	/**
	 * 方法描述：获得所有第三方应用的包名,以,号拼接的字符串
	 */
	public static String getPackgeNames() {
		// 获得系统所有应用的安装包信息
		ArrayList<PackageInfo> appPackage = getPackageInfoList();
		if (appPackage == null || appPackage.size() <= 0) {
			return null;
		}
		StringBuilder stringBuilder = new StringBuilder();
		for (PackageInfo packageInfo : appPackage) {
			stringBuilder.append(packageInfo.packageName).append(",");
		}
		String packgeNames = stringBuilder.toString().trim();
		if (packgeNames.length() >= 0 && packgeNames.endsWith(",")) {
			packgeNames = packgeNames.substring(0, packgeNames.length() - 1);
		}
		return packgeNames;
	}

	// /**
	// * 方法描述：获得所有第三方应用的包名versionCode,以,号拼接的字符串(eg:
	// * com.geili.koudai#1321,com.geili.koudai#132)
	// *
	// */
	// public static String getPackgeInfo(Context contex) {
	// String packgeNames = null;
	//
	// try {
	//
	// List<PackageInfo> appPackage = contex.getApplicationContext()
	// .getPackageManager()
	// .getInstalledPackages(PackageManager.GET_ACTIVITIES);
	// StringBuilder stringBuilder = new StringBuilder();
	// for (int i = 0; i < appPackage.size(); i++) {
	// PackageInfo packageInfo = appPackage.get(i);
	// //
	// //
	// //
	// 获取第三方应用packageInfo.applicationInfo.flags=0，系统应用packageInfo.applicationInfo.flags=1
	// // if ((packageInfo.applicationInfo.flags &
	// // android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
	// stringBuilder.append(packageInfo.packageName).append("#")
	// .append(packageInfo.versionCode).append(",");
	// }
	// packgeNames = stringBuilder.toString().trim();
	// if (packgeNames.length() > 0 && packgeNames.endsWith(",")) {
	// packgeNames = packgeNames
	// .substring(0, packgeNames.length() - 1);
	// }
	// } catch (Exception e) {
	// return packgeNames;
	// }
	// return packgeNames;
	//
	// }

	/**
	 * 获取手机安装的应用插入到本地数据库
	 * 
	 * @param contex
	 * @return
	 */
	public static boolean inert2DB(Context contex) {
		try {
			List<AppInfo> appPackage = AppUtil.getAllApp(contex);
			List<ContentValues> datas = new ArrayList<ContentValues>();
			int size = appPackage.size();
			for (int i = 0; i < size; i++) {
				ContentValues value = new ContentValues();
				AppInfo Info = appPackage.get(i);
				value.put(InstalledAppTable.PKG_NAME, Info.packageName);
				value.put(InstalledAppTable.VERSION_CODE, Info.versionCode);
				value.put(InstalledAppTable.APPNAME, Info.appName);
				value.put(InstalledAppTable.VERSIONNAME, Info.versionName);
				datas.add(value);
			}
			int result = PrizeDatabaseHelper.batchInsert(datas);
			if (result == 1) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/***
	 * @prize fanjunchen added {
	 * @return app updated request string from DataBase
	 */
	public static String getPackgeInfoStrFormDB() {
		String packgeNames = null;
		try {
			Cursor cur = PrizeDatabaseHelper.query(
					InstalledAppTable.TABLE_NAME, new String[] {
							InstalledAppTable.PKG_NAME,
							InstalledAppTable.VERSION_CODE }, null, null, null,
					null, null);

			if (null == cur)
				return packgeNames;

			int count = cur.getCount();
			if (count < 1) {
				cur.close();
				return packgeNames;
			}

			StringBuilder stringBuilder = new StringBuilder(2048);
			// int indexPkg = cur.getColumnIndex(InstalledAppTable.PKG_NAME);
			// int indexVer =
			// cur.getColumnIndex(InstalledAppTable.VERSION_CODE);
			int i = 0;
			while (cur.moveToNext()) {
				i++;
				stringBuilder.append(cur.getString(0)).append("#")
						.append(cur.getString(1));
				if (i != count) {
					stringBuilder.append(",");
				}
			}
			cur.close();
			packgeNames = stringBuilder.toString();
			stringBuilder = null;
		} catch (Exception e) {
			return packgeNames;
		}
		return packgeNames;

	}

	/***
	 * @prize fanjunchen added {
	 * @return true haveData; false no data.
	 */
	public static boolean isInitIntalledAppOk() {
		try {
			Cursor cur = PrizeDatabaseHelper.query(
					InstalledAppTable.TABLE_NAME,
					new String[] { InstalledAppTable.PKG_NAME }, null, null,
					null, null, null);
			if (null == cur)
				return false;

			boolean rs = cur.getCount() > 0;
			cur.close();
			return rs;
		} catch (Exception e) {
			return false;
		}
	}

	// @prize }
	// /**
	// * 方法描述：获得所有第三方应用的包名versionCode,以,号拼接的字符串(eg:
	// * com.geili.koudai#1321,com.geili.koudai#132)
	// *
	// */
	// public static String getPackgeInfoByQueryIntent() {
	// String packgeNames = null;
	// List<ResolveInfo> lists = getResolveInfoList();
	// if (lists == null || lists.size() <= 0) {
	// return packgeNames;
	// }
	//
	// int size = lists.size();
	// PackageManager pm = BaseApplication.curContext.getPackageManager();
	// StringBuilder stringBuilder = new StringBuilder();
	// try {
	// ResolveInfo resolveInfo;
	// for (int i = 0; i < size; i++) {
	// resolveInfo = lists.get(i);
	// String pkgName = resolveInfo.activityInfo.packageName;
	// int versionCode = pm.getPackageInfo(pkgName, 0).versionCode;
	// String param = new StringBuilder(pkgName).append("#")
	// .append(versionCode).append(",").toString();
	// if (stringBuilder != null
	// && !stringBuilder.toString().contains(param)) {
	// stringBuilder.append(param);
	// // stringBuilder.append(pkgName).append("#")
	// // .append(versionCode).append(",");
	//
	// }
	// }
	// packgeNames = stringBuilder.toString().trim();
	// if (packgeNames.length() > 0 && packgeNames.endsWith(",")) {
	// packgeNames = packgeNames
	// .substring(0, packgeNames.length() - 1);
	// }
	//
	// return packgeNames;
	// } catch (NameNotFoundException e) {
	// return packgeNames;
	// }
	//
	// }

	public static List<ResolveInfo> getResolveInfoList() {
		List<ResolveInfo> lists = null;
		try {
			PackageManager pm = BaseApplication.curContext.getPackageManager();
			Intent intent = new Intent(Intent.ACTION_MAIN, null);
			// intent.addCategory(Intent.CATEGORY_DEFAULT);
			lists = pm.queryIntentActivities(intent,
					PackageManager.GET_ACTIVITIES);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		if (lists == null) {
			return null;
		}
		// Iterator<ResolveInfo> iters = lists.iterator();
		// while (iters.hasNext()) {
		// ResolveInfo res = iters.next();
		// //
		//
		// if ((res.activityInfo.applicationInfo.flags &
		// android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
		// iters.remove();
		// }
		// }
		return lists;
	}

	public static ArrayList<PackageInfo> getPackageInfoList() {
		ArrayList<PackageInfo> appPackage = null;
		try {
			appPackage = (ArrayList<PackageInfo>) BaseApplication.curContext
					.getPackageManager().getInstalledPackages(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (appPackage == null) {
			return null;
		}
		ArrayList<PackageInfo> mPackages = new ArrayList<PackageInfo>();
		for (int i = 0; i < appPackage.size(); i++) {
			PackageInfo packageInfo = appPackage.get(i);
			// 获取第三方应用packageInfo.applicationInfo.flags=0，系统应用packageInfo.applicationInfo.flags=1
			// if ((packageInfo.applicationInfo.flags &
			// android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
			mPackages.add(packageInfo);
			// }
		}
		return mPackages;
	}

	public static ArrayList<PackageInfo> getThirdPackageInfoList() {
		ArrayList<PackageInfo> appPackage = null;
		try {
			appPackage = (ArrayList<PackageInfo>) BaseApplication.curContext
					.getPackageManager().getInstalledPackages(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (appPackage == null) {
			return null;
		}
		ArrayList<PackageInfo> mPackages = new ArrayList<PackageInfo>();
		for (int i = 0; i < appPackage.size(); i++) {
			PackageInfo packageInfo = appPackage.get(i);
			// 获取第三方应用packageInfo.applicationInfo.flags=0，系统应用packageInfo.applicationInfo.flags=1
			if ((packageInfo.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
				mPackages.add(packageInfo);
			}
		}
		return mPackages;
	}

	/**
	 * 
	 * @param activity
	 * @return > 0 success; <= 0 fail
	 */
	public static int getStatusHeight(Activity activity) {
		int statusHeight = 0;
		Rect localRect = new Rect();
		activity.getWindow().getDecorView()
				.getWindowVisibleDisplayFrame(localRect);
		statusHeight = localRect.top;
		if (0 == statusHeight) {
			Class<?> localClass;
			try {
				localClass = Class.forName("com.android.internal.R$dimen");
				Object localObject = localClass.newInstance();
				int i5 = Integer.parseInt(localClass
						.getField("status_bar_height").get(localObject)
						.toString());
				statusHeight = activity.getResources()
						.getDimensionPixelSize(i5);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			}
		}
		return statusHeight;
	}

	public static void copyText(TextView tv, Context context) {
		ClipboardManager cmb = (ClipboardManager) context
				.getSystemService(Context.CLIPBOARD_SERVICE);
		cmb.setText(tv.getText());

	}

	/**
	 * 方法描述：查询是否登录云账号 返回userId
	 * 
	 * @return void 返回userId 或者unkouwn
	 */
	public static String queryUserId() {
		ContentResolver resolver = BaseApplication.curContext
				.getContentResolver();
		Uri uri = null;
		uri = Uri
				.parse("content://com.prize.appcenter.provider.appstore/table_person");
		String userId = null;
		try {
			Cursor cs = resolver.query(uri, null, null, null, null);
			if (cs != null && cs.moveToFirst()) {
				userId = cs.getString(cs.getColumnIndex("userId"));
			}
			if (cs != null) {
				cs.close();
			}
			if (TextUtils.isEmpty(userId))
				return ClientInfo.UNKNOWN;
		} catch (Exception e) {
			return ClientInfo.UNKNOWN;
		}
		return userId;
	}

	private static long lastClickTime;

	/**
	 * @Description:[如果是连续点击则返回true]
	 * @return
	 */
	public static boolean isFastDoubleClick() {
		long time = System.currentTimeMillis();
		long timeD = time - lastClickTime;
		if (0 < timeD && timeD < 800) {
			return true;
		}
		lastClickTime = time;
		return false;
	}

	/**
	 * 拼接安装卸载信息格式为： [ {type: "common", data: "保留"}, { type: "all", data: [ {
	 * packageName: "com.prize.appstore", appName: "i酷市场", versionName:
	 * "1.7.release", versionCode: 11, apkSize: 300112 }, ... ] }, { type:
	 * "record", data: [ { packageName: "com.prize.appstore", appName: "i酷市场",
	 * type: "install", opTime: "2016-1-1", address: "中国 广东 深圳 南山区" }, {
	 * packageName: "com.prize.appstore", appName: "i酷市场", type: "uninstall",
	 * opTime: "2016-1-1", address: "中国 广东 深圳 南山区" }, ... ] } ]
	 * 
	 * @return
	 */
	public static String getRequestParam() {
		List<Map<String, Object>> datas = new ArrayList<Map<String, Object>>();
		ArrayList<AppInfo> allApps = AppInstalledDAO.getInstance()
				.getAllAppsInPhone();

		ArrayList<AppRecordInfo> apps = AppStateDAO.getInstance().getApps();
		if (apps != null && apps.size() > 0) {
			Map<String, Object> recordmap = new HashMap<String, Object>();
			recordmap.put("type", "record");
			recordmap.put("data", apps);
			datas.add(recordmap);
		} else {
			if (allApps == null || allApps.size() <= 0) {
				return null;
			}
		}
		Map<String, Object> allAppsdmap = new HashMap<String, Object>();
		allAppsdmap.put("type", "all");
		allAppsdmap.put("data", allApps);
		datas.add(allAppsdmap);
		return new Gson().toJson(datas);
	}

}
