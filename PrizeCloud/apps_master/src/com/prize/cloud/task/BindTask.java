/*******************************************
 *版权所有©2015,深圳市铂睿智恒科技有限公司
 *
 *内容摘要：
 *当前版本：1.0.0
 *作	者：yiyi
 *完成日期：2015年7月23日
 *修改记录：
 *修改日期：
 *版 本 号：
 *修 改 人：
 *修改内容：
 ...
 *修改记录：
 *修改日期：
 *版 本 号：
 *修 改 人：
 *修改内容：
*********************************************/
package com.prize.cloud.task;

import android.content.Context;

import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.http.RequestParams;
import com.lidroid.xutils.http.client.HttpRequest.HttpMethod;
import com.prize.cloud.R;
import com.prize.cloud.bean.Respond;
import com.prize.cloud.util.Utils;

/**
 * 绑定邮箱
 * @author yiyi
 * @version 1.0.0
 */
public class BindTask extends NetTask<Void> {
	private String passport, key, email, userId;

	public BindTask(Context ctx, TaskCallback<Void> taskCallback,
			String passport, String key, String email, String userId) {
		super(ctx, taskCallback);
		this.passport = passport;
		this.key = key;
		this.email = email;
		this.userId = userId;
	}

	@Override
	public void doExecute() {

		HttpUtils http = new HttpUtils(NETWORK_TIMEOUT);
		RequestParams params = new RequestParams();
		params.addHeader("KOOBEE", "dido");
		params.addBodyParameter("key", key);
		params.addBodyParameter("email", email);
		params.addBodyParameter("passport", passport);
		params.addBodyParameter("userId", userId);
		String url = HOST + "/cloud/account/binding";
		http.send(HttpMethod.POST, url, params, new RespondCallback() {

			@Override
			public void onSuccess(Respond respond) {
				if (!respond.getMsg().equals(NETWORK_OK)) {
					if (mContext != null) {
						onTaskError(0, mContext.getResources().getString(R.string.network_connection_fail));
					}
					return;
				}
				Utils.updateEmail(mContext, email);
				onTaskSuccess(null);// 验证码验证成功，进行下一步操作
			}

			@Override
			public void onError(int errorCode, String msg) {
				if (errorCode == NetTask.ERROR_TIMEOUT) {
					onTaskError(errorCode, mContext.getString(R.string.timeout));
				} else if (errorCode == NetTask.ERROR_NETWORK) {
					onTaskError(0, mContext.getResources().getString(R.string.network_connection_fail));
				} else if (errorCode == NetTask.ERROR_FAILURE) {
					onTaskError(errorCode, mContext.getString(R.string.failure));
				} else {
					onTaskError(errorCode, msg);
				}
			}

		});

	}

}
