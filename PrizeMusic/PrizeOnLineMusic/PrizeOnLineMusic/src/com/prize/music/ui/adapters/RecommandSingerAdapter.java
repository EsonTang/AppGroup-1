package com.prize.music.ui.adapters;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.prize.music.helpers.utils.UILimageUtil;
import com.prize.music.R;
import com.prize.onlinemusibean.ArtistsBean;
import com.xiami.sdk.utils.ImageUtil;

/***
 * 推荐歌手的适配器
 * 
 * @author Administrator
 *
 */
public class RecommandSingerAdapter extends BaseAdapter {

	private Context ctx;
	private ArrayList<ArtistsBean> datas;

	public RecommandSingerAdapter(Activity activity) {
		ctx = activity;
	}

	public void setData(ArrayList<ArtistsBean> datas) {
		if (datas == null || datas.size() <= 0)
			return;
		this.datas = datas;
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		if (datas == null) {
			return 0;
		}
		return datas.size();
	}

	@Override
	public ArtistsBean getItem(int position) {
		return datas.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		final ViewHolder viewHolder;
		if (convertView == null) {
			viewHolder = new ViewHolder();
			convertView = LayoutInflater.from(ctx).inflate(
					R.layout.recommand_singer_item, null);
			viewHolder.singeritemImg = (ImageView) convertView
					.findViewById(R.id.singerItem_img_id);
			viewHolder.singerItemName = (TextView) convertView
					.findViewById(R.id.singerItem_name_id);
			convertView.setTag(viewHolder);
			convertView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		final ArtistsBean data = datas.get(position);
		if (data == null) {
			return convertView;
		}
		if (!TextUtils.isEmpty(data.artist_logo)) {
			ImageLoader.getInstance().displayImage(
					ImageUtil.transferImgUrl(data.artist_logo, 120),
					viewHolder.singeritemImg,
					UILimageUtil.getTwoOneZeroDpLoptions(), null);
		}
		viewHolder.singeritemImg.setTag(data);
		viewHolder.singerItemName.setText(data.artist_name);

		return convertView;
	}

	class ViewHolder {
		public ImageView singeritemImg;
		public TextView singerItemName;
	}

}
