package com.prize.music.ui.fragments.list;

import java.util.Calendar;

import android.os.Bundle;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.MediaColumns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.RelativeLayout;

import com.prize.app.constants.Constants;
import com.prize.music.helpers.utils.MusicUtils;
import com.prize.music.service.ApolloService;
import com.prize.music.ui.adapters.list.AlbumListAdapter;
import com.prize.music.R;

public class AlbumListFragment extends ScrollerFragment {

	public AlbumListFragment(Bundle args) {
		setArguments(args);
	}
	
	public AlbumListFragment() {
	}

	@Override
	public void setupFragmentData() {
		mAdapter = new AlbumListAdapter(getActivity(),
				R.layout.item_songs_layout, null, new String[] {},
				new int[] {}, 0);
		mProjection = new String[] { BaseColumns._ID, MediaColumns.TITLE,
				AudioColumns.ALBUM, AudioColumns.ARTIST };
		StringBuilder where = new StringBuilder();
		where.append(AudioColumns.IS_MUSIC + "=1").append(
				" AND " + MediaColumns.TITLE + " != ''");
		long albumId = getArguments().getLong(BaseColumns._ID);
		where.append(" AND " + AudioColumns.ALBUM_ID + "=" + albumId);
		mWhere = where.toString();
		mSortOrder = MediaColumns.TITLE;
		// mSortOrder = Audio.Media.TRACK + ", " +
		// Audio.Media.DEFAULT_SORT_ORDER;
		mUri = Audio.Media.EXTERNAL_CONTENT_URI;
		mFragmentGroupId = 89;
		mType = Constants.TYPE_ALBUM;
		mTitleColumn = MediaColumns.TITLE;
		View shuffle_temp = View.inflate(getActivity(),
				R.layout.artist_shuffle_all, null);
		mListView.addHeaderView(shuffle_temp);
		RelativeLayout shuffle = (RelativeLayout) shuffle_temp
				.findViewById(R.id.shuffle_wrapper);
		shuffle.setVisibility(View.VISIBLE);
		shuffle.setOnClickListener(new RelativeLayout.OnClickListener() {
			public void onClick(View v) {
				// MusicUtils.shuffleAll(getActivity(), mCursor);
				//prize-add-bug:29635-tangzeming-20170309-start
				currentTime = Calendar.getInstance().getTimeInMillis(); 
				android.util.Log.d("tzm", "albumshuffleClick: "+"num="+(currentTime - lastClickTime));
				if (currentTime - lastClickTime > MIN_CLICK_DELAY_TIME) {
				lastClickTime = currentTime;	
				//prize-add-bug:29635-tangzeming-20170309-end
				MusicUtils.removeAllTracks();
				MusicUtils.shuffleAll2(getActivity(), mCursor);
				try {
					MusicUtils.mService
							.setRepeatMode(ApolloService.REPEAT_NONE);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		  }
		});
	}

	//prize-add-bug:29635-tangzeming-20170309-start
	public static final int MIN_CLICK_DELAY_TIME = 1000;
	private long lastClickTime = 0;
	private long currentTime = 0;
	//prize-add-bug:29635-tangzeming-20170309-end
	
	@Override
	public void onItemClick(AdapterView<?> parent, View v, final int position,
			long id) {
		//prize-add-bug:29635-tangzeming-20170309-start
		currentTime = Calendar.getInstance().getTimeInMillis();
		android.util.Log.d("tzm", "albumitemclick"+"num="+(currentTime - lastClickTime));
		if (currentTime - lastClickTime > MIN_CLICK_DELAY_TIME) {
			lastClickTime = currentTime;	
		//prize-add-bug:29635-tangzeming-20170309-end
		MusicUtils.removeAllTracks();
		mAdapter.play(v);
		parent.post(new Runnable() {

			@Override
			public void run() {
				MusicUtils.playAll(getActivity(), mCursor,
						position - mListView.getHeaderViewsCount());

			}
		});
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		// TODO Auto-generated method stub
		return true;
	}
}
