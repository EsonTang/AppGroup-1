/*******************************************
 * 版权所有©2015,深圳市铂睿智恒科技有限公司
 * <p>
 * 内容摘要：
 * 当前版本：
 * 作	者：
 * 完成日期：
 * 修改记录：
 * 修改日期：
 * 版 本 号：
 * 修 改 人：
 * 修改内容：
 * ...
 * 修改记录：
 * 修改日期：
 * 版 本 号：
 * 修 改 人：
 * 修改内容：
 *********************************************/

package com.prize.appcenter.ui.widget;

import android.app.Activity;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.prize.app.BaseApplication;
import com.prize.app.beans.ClientInfo;
import com.prize.app.download.AppManagerCenter;
import com.prize.app.net.datasource.base.AppsItemBean;
import com.prize.app.util.FileUtils;
import com.prize.app.util.PackageUtils;
import com.prize.appcenter.R;
import com.prize.appcenter.ui.dialog.DownDialog;
import com.prize.appcenter.ui.util.AIDLUtils;
import com.prize.appcenter.ui.util.ToastUtils;
import com.prize.appcenter.ui.util.UILimageUtil;
import com.prize.appcenter.ui.util.UIUtils;

import java.io.File;

/**
 * 类描述：游戏页（精彩游戏每个应用布局）
 *
 * @author 聂礼刚
 * @version 1.0
 */
public class ItemWonderfulView extends RelativeLayout {
    private Activity mContext;
    private ImageView game_iv;
    private TextView game_name_tv;
//    private TextView download_count_tv;
    private TextView state_tv;
    private android.widget.ProgressBar ProgressBar;
    private OnClickListener mOnClickListener;
    private DownDialog mDownDialog;
    private RelativeLayout mDetailRl;
    private ShapeDrawable mTopShapeDrawable;
    private ShapeDrawable mBottomShapeDrawable;

    public ItemWonderfulView(Activity context) {
        super(context);
        mContext = context;
        float Radius = 10;
        float[] topRadii = {Radius, Radius, Radius, Radius, 0, 0, 0, 0};
        float[] bottomeRadii = {0, 0, 0, 0, Radius, Radius, Radius, Radius};
        mTopShapeDrawable = new ShapeDrawable(new RoundRectShape(topRadii, null, null));
        mBottomShapeDrawable = new ShapeDrawable(new RoundRectShape(bottomeRadii, null, null));
        setGravity(Gravity.CENTER_HORIZONTAL);
        View view = inflate(context, R.layout.item_wonderful, this);
        findViewById(view);
        mOnClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                AppsItemBean item = (AppsItemBean) state_tv.getTag();
                ItemWonderfulView.this.onClick(item);
            }
        };
    }


    private void findViewById(View view) {
        mDetailRl = (RelativeLayout) view.findViewById(R.id.detail_rl);
        game_iv = (ImageView) view.findViewById(R.id.game_iv);
        game_name_tv = (TextView) view.findViewById(R.id.game_name_tv);
//        download_count_tv = (TextView) view.findViewById(R.id.download_count_tv);
        state_tv = (TextView) view.findViewById(R.id.state_tv);
        ProgressBar = (android.widget.ProgressBar) view.findViewById(R.id.ProgressBar);
    }

    public void setData(AppsItemBean item) {
        if (item == null)
            return;

        game_name_tv.setText(item.name);
//        download_count_tv.setText(item.downloadTimes);
//        if (null != item.downloadTimesFormat) {
//            String user = item.downloadTimesFormat.replace("次", "人");
//            download_count_tv.setText(getResources().getString(
//                    R.string.person_like, user));
//        }
        state_tv.setTag(item);
        String url = TextUtils.isEmpty(item.largeIcon) ? item.iconUrl : item.largeIcon;
        ImageLoader.getInstance().displayImage(url, game_iv, UILimageUtil.getUILoptions());
        setState(item);
        state_tv.setOnClickListener(mOnClickListener);
    }

    public void setBackgroundColor(int topColorId, int bottomColorId) {

        mTopShapeDrawable.getPaint().setColor(getResources().getColor(topColorId));
        mBottomShapeDrawable.getPaint().setColor(getResources().getColor(bottomColorId));

        mDetailRl.setBackground(mTopShapeDrawable);
        state_tv.setBackground(mBottomShapeDrawable);
    }

    public void setState(AppsItemBean item) {

        if (state_tv == null || ProgressBar == null) {
            return;
        }
        ProgressBar.setVisibility(View.GONE);
        int state = AIDLUtils.getGameAppState(item.packageName, item.id, item.versionCode);
        switch (state) {
            case AppManagerCenter.APP_STATE_WAIT:
                // 等待中 下载
                state_tv.setText(R.string.progress_btn_wait);
                ProgressBar.setVisibility(View.VISIBLE);
                setProgress(item);
                break;
            case AppManagerCenter.APP_STATE_DOWNLOADED:
                state_tv.setText(R.string.progress_btn_install);

                break;
            case AppManagerCenter.APP_STATE_INSTALLED:
                // 安装完成(启动应用)
                state_tv.setText(R.string.progress_btn_start);
                break;
            case AppManagerCenter.APP_STATE_DOWNLOAD_PAUSE:
                // 暂停
                state_tv.setText(R.string.progress_continue);
                ProgressBar.setVisibility(View.VISIBLE);
                setProgress(item);
                break;
            case AppManagerCenter.APP_STATE_DOWNLOADING:
                // 下载中
                state_tv.setText(R.string.progress_pause);
                ProgressBar.setVisibility(View.VISIBLE);
                setProgress(item);
                break;
            case AppManagerCenter.APP_STATE_UNEXIST:
                int textId = R.string.progress_btn_download;
                if (BaseApplication.isThird) {
                    textId = R.string.progress_btn_download;
                } else {
                    textId = R.string.progress_btn_install;
                }
                state_tv.setText(textId);
                break;
            case AppManagerCenter.APP_STATE_UPDATE:
                // 更新
                state_tv.setText(R.string.progress_btn_upload);
                break;
            case AppManagerCenter.APP_STATE_INSTALLING:
                state_tv.setText(R.string.progress_btn_installing);
                break;
            case AppManagerCenter.APP_PATCHING:
                state_tv.setText(R.string.progress_btn_patching);
                // 安装中
                break;
        }
    }


    private void setProgress(AppsItemBean item) {
        // 进度条进度指示
        final float progress = AIDLUtils.getDownloadProgress(item.packageName);
        int mProgress = 0;

        if (progress > 0.0 && progress < 1.0f) {
            mProgress = 1;
        } else {
            mProgress = (int) progress;
        }
        ProgressBar.setProgress(mProgress);
    }

    /**
     * 点击Button
     */
    public void onClick(final AppsItemBean item) {
        if (null == item) {
            return;
        }
        final int state = AIDLUtils.getGameAppState(item.packageName, item.id + "",
                item.versionCode);
        switch (state) {
            case AppManagerCenter.APP_STATE_DOWNLOADED:
                if (BaseApplication.isThird) {
                    AppManagerCenter.installGameApk(item);
                } else {
                    String gameAPKFilePath = FileUtils.getGameAPKFilePath(item.id);
                    File gameAPKFile = new File(gameAPKFilePath);
                    if (gameAPKFile.exists()) {
                        PackageUtils.installNormal(BaseApplication.curContext, gameAPKFilePath);
                    }
                }

                break;
            case AppManagerCenter.APP_STATE_INSTALLED:
                UIUtils.startGame(item);
                break;

            case AppManagerCenter.APP_STATE_UNEXIST:
            case AppManagerCenter.APP_STATE_UPDATE:
            case AppManagerCenter.APP_STATE_DOWNLOAD_PAUSE:
                if (ClientInfo.getAPNType(BaseApplication.curContext) == ClientInfo.NONET) {
                    ToastUtils.showToast(R.string.nonet_connect);
                    return;
                }
                if (BaseApplication.isDownloadWIFIOnly()
                        && ClientInfo.getAPNType(BaseApplication.curContext) != ClientInfo.WIFI) {
                    mDownDialog = new DownDialog(mContext,
                            R.style.add_dialog);
                    mDownDialog.show();
                    mDownDialog.setmOnButtonClic(new DownDialog.OnButtonClic() {

                        @Override
                        public void onClick(int which) {
                            dismissDialog();
                            switch (which) {
                                case 0:
                                    break;
                                case 1:
                                    UIUtils.downloadApp(item);
                                    break;
                            }
                        }
                    });
                } else {
                    UIUtils.downloadApp(item);
                }
                break;
            case AppManagerCenter.APP_STATE_DOWNLOADING:
            case AppManagerCenter.APP_STATE_WAIT:
                AIDLUtils.pauseDownload(item, true);
                break;
        }
    }

    private void dismissDialog() {
        if (mDownDialog != null && mDownDialog.isShowing()) {
            mDownDialog.dismiss();
            mDownDialog = null;
        }
    }
}