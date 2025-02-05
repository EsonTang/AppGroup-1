package com.prize.music.helpers.utils;

import static com.prize.music.Constants.APOLLO_PREFERENCES;
import static com.prize.music.Constants.EXTERNAL;
import static com.prize.music.Constants.GENRES_DB;
import static com.prize.music.Constants.PLAYLIST_NAME_FAVORITES;
import static com.prize.music.Constants.PLAYLIST_NEW;
import static com.prize.music.Constants.PLAYLIST_QUEUE;
import static com.prize.music.Constants.TYPE_ALBUM;
import static com.prize.music.Constants.TYPE_ARTIST;
import static com.prize.music.Constants.TYPE_GENRE;
import static com.prize.music.Constants.TYPE_PLAYLIST;
import static com.prize.music.Constants.TYPE_SONG;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.Environment;
import android.os.RemoteException;
import android.os.storage.StorageManager;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Audio.AlbumColumns;
import android.provider.MediaStore.Audio.ArtistColumns;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.Audio.Genres;
import android.provider.MediaStore.Audio.GenresColumns;
import android.provider.MediaStore.Audio.Media;
import android.provider.MediaStore.Audio.Playlists;
import android.provider.MediaStore.Audio.PlaylistsColumns;
import android.provider.MediaStore.MediaColumns;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.prize.music.IApolloService;
import com.prize.music.R;
import com.prize.music.service.ApolloService;
import com.prize.music.service.ServiceBinder;
import com.prize.music.service.ServiceToken;

/**
 * Various methods used to help with specific music statements
 */
public class MusicUtils {

	// Used to make number of albums/songs/time strings
	private final static StringBuilder sFormatBuilder = new StringBuilder();

	private final static Formatter sFormatter = new Formatter(sFormatBuilder,
			Locale.getDefault());

	public static IApolloService mService = null;

	private static HashMap<Context, ServiceBinder> sConnectionMap = new HashMap<Context, ServiceBinder>();

	private final static long[] sEmptyList = new long[0];

	private static final Object[] sTimeArgs = new Object[5];

	private static ContentValues[] sContentValuesCache = null;

	private static Equalizer mEqualizer = null;

	private static BassBoost mBoost = null;

	private static String TAG = "MusicUtils";

	/**
	 * @param context
	 * @return
	 */
	public static ServiceToken bindToService(Activity context) {
		return bindToService(context, null);
	}

	/**
	 * @param context
	 * @param callback
	 * @return
	 */
	public static ServiceToken bindToService(Context context,
			ServiceConnection callback) {
		/*Activity realActivity = ((Activity) context).getParent();
		if (realActivity == null) {
			realActivity = (Activity) context;
		}*/
		//ContextWrapper cw = new ContextWrapper(realActivity);
		if (context == null) return null;
		ContextWrapper cw = new ContextWrapper(context);
		cw.startService(new Intent(cw, ApolloService.class));
		ServiceBinder sb = new ServiceBinder(callback);
		if (cw.bindService( new Intent().setClass(cw, ApolloService.class),sb, 0) ) {
			Log.d("LIXING","prepare put ServiceBinder into sConnectionMap");
			sConnectionMap.put(cw, sb);
			return new ServiceToken(cw);
		}
		return null;
	}

	/**
	 * @param token
	 */
	public static void unbindFromService(ServiceToken token) {
		if (token == null) {
			Log.d("LIXING","unbindFromService() token == null");
			return;
		}
		ContextWrapper cw = token.mWrappedContext;
		ServiceBinder sb = sConnectionMap.remove(cw);
		if (sb == null) {
			Log.d("LIXING","unbindFromService() sb == null");
			return;
		}
		cw.unbindService(sb);
		if (sConnectionMap.isEmpty()) {
			Log.d("LIXING","unbindFromService() sConnectionMap.isEmpty() = true");
			mService = null;
		}
		Log.d("LIXING","unbindFromService() has unbindService");
	}

	public static void releaseEqualizer() {
		if (mEqualizer != null) {
			mEqualizer.release();
		}
		if (mBoost != null) {
			mBoost.release();
		}
	}

	/**
	 * @param media
	 *            player from apollo service.
	 */
	public static void initEqualizer(MediaPlayer player, Context context) {
		releaseEqualizer();
		int id = player.getAudioSessionId();
		mEqualizer = new Equalizer(1, id);
		mBoost = new BassBoost(1, id);
		updateEqualizerSettings(context);
	}

	public static int[] getEqualizerFrequencies() {
		short numBands = mEqualizer.getNumberOfBands() <= 6 ? mEqualizer
				.getNumberOfBands() : 6;
		int[] freqs = new int[numBands];
		if (mEqualizer != null) {
			for (int i = 0; i <= numBands - 1; i++) {
				int[] temp = mEqualizer.getBandFreqRange((short) i);
				freqs[i] = ((temp[1] - temp[0]) / 2) + temp[0];
			}
			return freqs;
		}
		return null;
	}

	public static void updateEqualizerSettings(Context context) {

		SharedPreferences mPreferences = context.getSharedPreferences(
				APOLLO_PREFERENCES, Context.MODE_WORLD_READABLE
						| Context.MODE_WORLD_WRITEABLE);

		if (mBoost != null) {
			mBoost.setEnabled(mPreferences.getBoolean("simple_eq_boost_enable",
					false));
			mBoost.setStrength((short) (mPreferences.getInt("simple_eq_bboost",
					0) * 10));
		}

		if (mEqualizer != null) {
			mEqualizer.setEnabled(mPreferences.getBoolean(
					"simple_eq_equalizer_enable", false));
			short numBands = mEqualizer.getNumberOfBands() <= 6 ? mEqualizer
					.getNumberOfBands() : 6;
			short r[] = mEqualizer.getBandLevelRange();
			short min_level = r[0];
			short max_level = r[1];
			for (int i = 0; i <= (numBands - 1); i++) {
				int new_level = min_level
						+ (max_level - min_level)
						* mPreferences.getInt(
								"simple_eq_seekbars" + String.valueOf(i), 100)
						/ 100;
				mEqualizer.setBandLevel((short) i, (short) new_level);
			}
		}
	}

	/**
	 * @param from
	 *            The index the item is currently at.
	 * @param to
	 *            The index the item is moving to.
	 */
	public static void moveQueueItem(final int from, final int to) {
		try {
			if (mService != null) {
				mService.moveQueueItem(from, to);
			} else {
			}
		} catch (final RemoteException ignored) {
		}
	}

	/**
	 * @param context
	 * @param numalbums
	 * @param numsongs
	 * @param isUnknown
	 * @return a string based on the number of albums for an artist or songs for
	 *         an album
	 */
	public static String makeAlbumsLabel(Context mContext, int numalbums,
			int numsongs, boolean isUnknown) {

		StringBuilder songs_albums = new StringBuilder();

		Resources r = mContext.getResources();
		// if (isUnknown) {
		String f = r.getQuantityText(R.plurals.Nsongs, numalbums).toString();
		sFormatBuilder.setLength(0);
		sFormatter.format(f, Integer.valueOf(numalbums));
		songs_albums.append(sFormatBuilder);
		songs_albums.append("\n");
		// } else {
		// String f = r.getQuantityText(R.plurals.Nalbums, numalbums)
		// .toString();
		// sFormatBuilder.setLength(0);
		// sFormatter.format(f, Integer.valueOf(numalbums));
		// songs_albums.append(sFormatBuilder);
		// songs_albums.append("\n");
		// }
		return songs_albums.toString();
	}

	/**
	 * @param mContext
	 * @return
	 */
	public static int getCardId(Context mContext) {

		ContentResolver res = mContext.getContentResolver();
		Cursor c = res.query(Uri.parse("content://media/external/fs_id"), null,
				null, null, null);
		int id = -1;
		if (c != null) {
			c.moveToFirst();
			id = c.getInt(0);
			c.close();
		}
		return id;
	}

	/**
	 * @param context
	 * @param uri
	 * @param projection
	 * @param selection
	 * @param selectionArgs
	 * @param sortOrder
	 * @param limit
	 * @return
	 */
	public static Cursor query(Context context, Uri uri, String[] projection,
			String selection, String[] selectionArgs, String sortOrder,
			int limit) {
		try {
			ContentResolver resolver = context.getContentResolver();
			if (resolver == null) {
				return null;
			}
			if (limit > 0) {
				uri = uri.buildUpon().appendQueryParameter("limit", "" + limit)
						.build();
			}
			return resolver.query(uri, projection, selection, selectionArgs,
					sortOrder);
		} catch (UnsupportedOperationException ex) {
			return null;
		} catch (NullPointerException e) {
			return null;
		}
	}

	/**
	 * @param context
	 * @param uri
	 * @param projection
	 * @param selection
	 * @param selectionArgs
	 * @param sortOrder
	 * @return
	 */
	public static Cursor query(Context context, Uri uri, String[] projection,
			String selection, String[] selectionArgs, String sortOrder) {
		return query(context, uri, projection, selection, selectionArgs,
				sortOrder, 0);
	}

	/**
	 * @param context
	 * @param cursor
	 */
	public static void shuffleAll(Context context, Cursor cursor) {

		long[] list = getRandomSongListForCursor(cursor);
		playAll(context, list, -1, true);
	}

	/**
	 * @param context
	 * @param cursor
	 */
	public static void playAll(Context context, Cursor cursor) {
		playAll(context, cursor, 0, false);
	}

	/**
	 * @param context
	 * @param cursor
	 * @param position
	 */
	public static void playAll(Context context, Cursor cursor, int position) {
		playAll(context, cursor, position, false);
	}

	/**
	 * @param context
	 * @param list
	 * @param position
	 */
	public static void playAll(Context context, long[] list, int position) {
		playAll(context, list, position, false);
	}

	/**
	 * @param context
	 * @param cursor
	 * @param position
	 * @param force_shuffle
	 */
	private static void playAll(Context context, Cursor cursor, int position,
			boolean force_shuffle) {

		long[] list = getSongListForCursor(cursor);
		playAll(context, list, position, force_shuffle);
	}

	public static void shuffleAll2(Context context, Cursor cursor) {
		if (cursor == null || cursor.getCount() <= 0) {
			return;
		}
		long[] list = getSongListForCursor(cursor);
		int position = new Random().nextInt(list.length);
		playAll(context, list, position, true);
	}

	/**
	 * @param cursor
	 * @return
	 */
	public static long[] getSongListForCursor(Cursor cursor) {
		if (cursor == null) {
			return sEmptyList;
		}
		int len = cursor.getCount();
		if(len <= 0){
			return sEmptyList;
		}
		long[] list = new long[len];
		cursor.moveToFirst();
		int colidx = -1;
		try {
			// String path = cursor.getString(cursor
			// .getColumnIndexOrThrow(AudioColumns.DATA));
			// LogUtils.i(TAG, "getSongListForCursor path=" + path);
			colidx = cursor
					.getColumnIndexOrThrow(Audio.Playlists.Members.AUDIO_ID);
		} catch (IllegalArgumentException ex) {
			colidx = cursor.getColumnIndexOrThrow(BaseColumns._ID);
		}
		for (int i = 0; i < len; i++) {
			list[i] = cursor.getLong(colidx);
			cursor.moveToNext();
		}
		
		return list;
	}

	/**
	 * @param cursor
	 * @return
	 */
	public static long[] getRandomSongListForCursor(Cursor cursor) {
		if (cursor == null) {
			return sEmptyList;
		}
		int len = cursor.getCount();
		if(len <= 0){
			return sEmptyList;
		}
		long[] list = new long[len];
		cursor.moveToFirst();
		int colidx = -1;
		try {
			colidx = cursor
					.getColumnIndexOrThrow(Audio.Playlists.Members.AUDIO_ID);
		} catch (IllegalArgumentException ex) {
			colidx = cursor.getColumnIndexOrThrow(BaseColumns._ID);
		}
		for (int i = 0; i < len; i++) {
			list[i] = cursor.getLong(colidx);
			cursor.moveToNext();
		}
		int index;
		Random random = new Random();
		for (int i = list.length - 1; i > 0; i--) {
			index = random.nextInt(i + 1);
			if (index != i) {
				list[index] ^= list[i];
				list[i] ^= list[index];
				list[index] ^= list[i];
			}
		}
		return list;
	}

	/**
	 * @param context
	 * @param list
	 * @param position
	 * @param force_shuffle
	 */
	private static void playAll(Context context, long[] list, int position,
			boolean force_shuffle) {
		if (list.length == 0 || mService == null) {
			return;
		}
		try {
			if (force_shuffle) {
				mService.setShuffleMode(ApolloService.SHUFFLE_NORMAL);
			}
			long curid = mService.getAudioId();
			int curpos = mService.getQueuePosition();
			if (position < list.length) {
				if (position != -1 && curpos == position && curid == list[position]) {
					// The selected file is the file that's currently playing;
					// figure out if we need to restart with a new playlist,a
					// or just launch the playback activity.
					long[] playlist = mService.getQueue();
					if (Arrays.equals(list, playlist)) {
						// we don't need to set a new list, but we should resume
						// playback if needed
						mService.play();
						return;
					}
				}
				if (position < 0) {
					position = 0;
				}
				// mService.open(list, force_shuffle ? -1 : position);
				mService.open(list, position);
				mService.play();
			}
		} catch (RemoteException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * @return
	 */
	public static long[] getQueue() {

		if (mService == null)
			return sEmptyList;

		try {
			return mService.getQueue();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return sEmptyList;
	}

	/**
	 * @param context
	 * @param name
	 * @param def
	 * @return number of weeks used to create the Recent tab
	 */
	public static int getIntPref(Context context, String name, int def) {
		SharedPreferences prefs = context.getSharedPreferences(
				context.getPackageName(), Context.MODE_PRIVATE);
		return prefs.getInt(name, def);
	}

	/**
	 * @param context
	 * @param id
	 * @return
	 */
	public static long[] getSongListForArtist(Context context, long id) {
		final String[] projection = new String[] { BaseColumns._ID };
		String selection = AudioColumns.ARTIST_ID + "=" + id + " AND "
				+ AudioColumns.IS_MUSIC + "=1";
		String sortOrder = AudioColumns.ALBUM_KEY + "," + AudioColumns.TRACK;
		Uri uri = Audio.Media.EXTERNAL_CONTENT_URI;
		Cursor cursor = query(context, uri, projection, selection, null,
				sortOrder);
		if (cursor != null) {
			long[] list = getSongListForCursor(cursor);
			cursor.close();
			return list;
		}
		return sEmptyList;
	}

	/**
	 * @param context
	 * @param id
	 * @return
	 */
	public static long[] getSongListForAlbum(Context context, long id) {
		final String[] projection = new String[] { BaseColumns._ID };
		String selection = AudioColumns.ALBUM_ID + "=" + id + " AND "
				+ AudioColumns.IS_MUSIC + "=1";
		String sortOrder = AudioColumns.TRACK;
		Uri uri = Audio.Media.EXTERNAL_CONTENT_URI;
		Cursor cursor = query(context, uri, projection, selection, null,
				sortOrder);
		if (cursor != null) {
			long[] list = getSongListForCursor(cursor);
			cursor.close();
			return list;
		}
		return sEmptyList;
	}

	/**
	 * @param context
	 * @param id
	 * @return
	 */
	public static long[] getSongListForGenre(Context context, long id) {
		String[] projection = new String[] { BaseColumns._ID };
		StringBuilder selection = new StringBuilder();
		selection.append(AudioColumns.IS_MUSIC + "=1");
		selection.append(" AND " + MediaColumns.TITLE + "!=''");
		Uri uri = Genres.Members.getContentUri(EXTERNAL, id);
		Cursor cursor = context.getContentResolver().query(uri, projection,
				selection.toString(), null, null);
		if (cursor != null) {
			long[] list = getSongListForCursor(cursor);
			cursor.close();
			return list;
		}
		return sEmptyList;
	}

	/**
	 * @param context
	 * @param id
	 * @return
	 */
	public static long[] getSongListForPlaylist(Context context, long id) {
		final String[] projection = new String[] { Audio.Playlists.Members.AUDIO_ID };
		String sortOrder = Playlists.Members.DEFAULT_SORT_ORDER;
		Uri uri = Playlists.Members.getContentUri(EXTERNAL, id);
		Cursor cursor = query(context, uri, projection, null, null, sortOrder);
		if (cursor != null) {
			long[] list = getSongListForCursor(cursor);
			cursor.close();
			return list;
		}
		return sEmptyList;
	}

	public static long[] getSongList(String Type, Context context, long id) {
		if (Type == TYPE_ALBUM) {
			return MusicUtils.getSongListForAlbum(context, id);
		} else if (Type == TYPE_ARTIST) {
			return MusicUtils.getSongListForArtist(context, id);
		} else if (Type == TYPE_GENRE) {
			return MusicUtils.getSongListForGenre(context, id);
		} else if (Type == TYPE_PLAYLIST) {
			return MusicUtils.getSongListForPlaylist(context, id);
		}
		return sEmptyList;
	}

	/**
	 * @param context
	 * @param name
	 * @return
	 */
	public static long createPlaylist(Context context, String name) {
		if (context == null) return -1;
		if (name != null && name.length() > 0) {
			ContentResolver resolver = context.getContentResolver();
			if (resolver == null) return -1;
			String[] cols = new String[] { PlaylistsColumns.NAME };
			String whereclause = PlaylistsColumns.NAME + " = '" + name + "'";
			Cursor cur = null;
			try {
				cur = resolver.query(Audio.Playlists.EXTERNAL_CONTENT_URI,
						cols, whereclause, null, null);
				if (cur != null && cur.getCount() <= 0) {
					ContentValues values = new ContentValues(1);
					values.put(PlaylistsColumns.NAME, name);
					Uri uri = resolver.insert(Audio.Playlists.EXTERNAL_CONTENT_URI,
							values);
					return Long.parseLong(uri.getLastPathSegment());
				}
				return -1;
			} catch (Exception e) {
				e.printStackTrace();
			}		
		}
		return -1;
	}

	/**
	 * @param context
	 * @return
	 */
	public static long getFavoritesId(Context context) {
		FragmentActivity activity = (FragmentActivity) context;
		SharedPreferences sharedPreferences = activity.getSharedPreferences(
				"ShareXML", Context.MODE_PRIVATE);
		String name = sharedPreferences.getString("Favorites", "我喜欢的");
		long favorites_id = -1;
		String favorites_where = PlaylistsColumns.NAME + "='" + name + "'";
		String[] favorites_cols = new String[] { BaseColumns._ID };
		Uri favorites_uri = Audio.Playlists.EXTERNAL_CONTENT_URI;
		Cursor cursor = query(context, favorites_uri, favorites_cols,
				favorites_where, null, null);
		/*Prize-lixing-2016.2.16- resolve NullPoint Exception --start*/
		if (cursor == null){
			return favorites_id;
		}
		/*Prize-lixing-2016.2.16- resolve NullPoint Exception --end*/
		
		if (cursor.getCount() <= 0) {
			favorites_id = createPlaylist(context, name);
		} else {
			cursor.moveToFirst();
			favorites_id = cursor.getLong(0);
			cursor.close();
		}
		return favorites_id;
	}

	/**
	 * @param context
	 * @param id
	 */
	public static void setRingtone(Context context, long id) {
		ContentResolver resolver = context.getContentResolver();
		// Set the flag in the database to mark this as a ringtone
		Uri ringUri = ContentUris.withAppendedId(
				Audio.Media.EXTERNAL_CONTENT_URI, id);
		try {
			ContentValues values = new ContentValues(2);
			values.put(AudioColumns.IS_RINGTONE, "1");
			values.put(AudioColumns.IS_ALARM, "1");
			resolver.update(ringUri, values, null, null);
		} catch (UnsupportedOperationException ex) {
			// most likely the card just got unmounted
			return;
		}

		String[] cols = new String[] { BaseColumns._ID, MediaColumns.DATA,
				MediaColumns.TITLE };

		String where = BaseColumns._ID + "=" + id;
		Cursor cursor = query(context, Audio.Media.EXTERNAL_CONTENT_URI, cols,
				where, null, null);
		try {
			if (cursor != null && cursor.getCount() == 1) {
				// Set the system setting to make this the current ringtone
				cursor.moveToFirst();
				Settings.System.putString(resolver, Settings.System.RINGTONE,
						ringUri.toString());
				String message = context.getString(R.string.set_as_ringtone,
						cursor.getString(2));
				Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * @param context
	 * @param plid
	 */
	public static void clearPlaylist(Context context, int plid) {
		Uri uri = Audio.Playlists.Members.getContentUri(EXTERNAL, plid);
		context.getContentResolver().delete(uri, null, null);
		return;
	}

	/**
	 * @param context
	 * @param ids
	 * @param playlistid
	 */
	public static void addToPlaylist(Context context, long[] ids,
			long playlistid) {

		if (ids == null) {
		} else {
			int size = ids.length;
			ContentResolver resolver = context.getContentResolver();
			// need to determine the number of items currently in the playlist,
			// so the play_order field can be maintained.
			String[] cols = new String[] { "count(*)" };
			Uri uri = Audio.Playlists.Members.getContentUri(EXTERNAL,
					playlistid);
			Cursor cur = resolver.query(uri, cols, null, null, null);
			cur.moveToFirst();
			int base = cur.getInt(0);
			cur.close();
			int numinserted = 0;
			for (int i = 0; i < size; i += 1000) {
				makeInsertItems(ids, i, 1000, base);
				numinserted += resolver.bulkInsert(uri, sContentValuesCache);
			}
			String message = context.getResources().getQuantityString(
					R.plurals.NNNtrackstoplaylist, numinserted, numinserted);
			// Toast.makeText(context, message, Toast.LENGTH_SHORT).show();

			ToastUtils.showOnceToast(context,
					context.getString(R.string.addSuccessful));
		}
	}

	/**
	 * @param ids
	 * @param offset
	 * @param len
	 * @param base
	 */
	private static void makeInsertItems(long[] ids, int offset, int len,
			int base) {

		// adjust 'len' if would extend beyond the end of the source array
		if (offset + len > ids.length) {
			len = ids.length - offset;
		}
		// allocate the ContentValues array, or reallocate if it is the wrong
		// size
		if (sContentValuesCache == null || sContentValuesCache.length != len) {
			sContentValuesCache = new ContentValues[len];
		}
		// fill in the ContentValues array with the right values for this pass
		for (int i = 0; i < len; i++) {
			if (sContentValuesCache[i] == null) {
				sContentValuesCache[i] = new ContentValues();
			}

			sContentValuesCache[i].put(Playlists.Members.PLAY_ORDER, base
					+ offset + i);
			sContentValuesCache[i].put(Playlists.Members.AUDIO_ID, ids[offset
					+ i]);
		}
	}

	/**
	 * Toggle favorites
	 */
	public static void toggleFavorite() {

		if (mService == null)
			return;
		try {
			mService.toggleFavorite();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public static void addToaddToFavorites(Context context, long[] ids) {
		int len = ids.length;
		for (int i = 0; i < len; i++) {
			if (isFavorite(context, ids[i]))
				continue;
			addToFavorites(context, ids[i]);

		}
	}

	/**
	 * @param context
	 * @param id
	 */
	public static void addToFavorites(Context context, long id) {
		if(context == null){
			return;
		}
		SharedPreferences sharedPreferences = context.getSharedPreferences(
				"ShareXML", Context.MODE_PRIVATE);
		String tableName = sharedPreferences.getString("Favorites",
				context.getString(R.string.my_love));

		long favorites_id;

		if (id < 0) {

		} else {
			ContentResolver resolver = context.getContentResolver();

			/*
			 * String favorites_where = PlaylistsColumns.NAME + "='" +
			 * PLAYLIST_NAME_FAVORITES + "'";
			 */
			String favorites_where = PlaylistsColumns.NAME + "='" + tableName
					+ "'";
			String[] favorites_cols = new String[] { BaseColumns._ID };
			Uri favorites_uri = Audio.Playlists.EXTERNAL_CONTENT_URI;
			Cursor cursor = resolver.query(favorites_uri, favorites_cols,
					favorites_where, null, null);
			if(cursor == null){
				return;
			}
			
			if (cursor.getCount() <= 0) {
				// favorites_id = createPlaylist(context,
				// PLAYLIST_NAME_FAVORITES);
				favorites_id = createPlaylist(context, tableName);
			} else {
				cursor.moveToFirst();
				favorites_id = cursor.getLong(0);
				cursor.close();
			}

			String[] cols = new String[] { Playlists.Members.AUDIO_ID };
			Uri uri = Playlists.Members.getContentUri(EXTERNAL, favorites_id);
			Cursor cur = resolver.query(uri, cols, null, null, null);

			int base = cur.getCount();
			cur.moveToFirst();
			while (!cur.isAfterLast()) {
				if (cur.getLong(0) == id)
					return;
				cur.moveToNext();
			}
			cur.close();

			ContentValues values = new ContentValues();
			values.put(Playlists.Members.AUDIO_ID, id);
			values.put(Playlists.Members.PLAY_ORDER, base + 1);
			resolver.insert(uri, values);
		}
	}

	/**
	 * @param context
	 * @param id
	 * @return
	 */
	public static boolean isFavorite(Context context, long id) {
		if(context == null){
			return false;
		}
		
		SharedPreferences sharedPreferences = context.getSharedPreferences(
				"ShareXML", Context.MODE_PRIVATE);
		String tableName = sharedPreferences.getString("Favorites",
				context.getString(R.string.my_love));
		long favorites_id = 0;

		if (id < 0) {

		} else {
			ContentResolver resolver = context.getContentResolver();

			/*
			 * String favorites_where = PlaylistsColumns.NAME + "='" +
			 * PLAYLIST_NAME_FAVORITES + "'";
			 */
			String favorites_where = PlaylistsColumns.NAME + "='" + tableName
					+ "'";
			String[] favorites_cols = new String[] { BaseColumns._ID };
			Uri favorites_uri = Audio.Playlists.EXTERNAL_CONTENT_URI;
			Cursor cursor = null;
			try {
				cursor = resolver.query(favorites_uri, favorites_cols,
						favorites_where, null, null);
				if (cursor == null) return false;
				if (cursor.getCount() <= 0) {
					// favorites_id = createPlaylist(context,
					// PLAYLIST_NAME_FAVORITES);
					favorites_id = createPlaylist(context, tableName);
				} else {
					cursor.moveToFirst();
					favorites_id = cursor.getLong(0);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (cursor != null) {
					cursor.close();
					cursor = null;
				}
			}
			
			if (favorites_id == 0) return false;
			String[] cols = new String[] { Playlists.Members.AUDIO_ID };
			Uri uri = Playlists.Members.getContentUri(EXTERNAL, favorites_id);
			Cursor cur = null;
			try {
				cur = resolver.query(uri, cols, null, null, null);
				if (cur == null) return false;
				cur.moveToFirst();
				while (!cur.isAfterLast()) {
					if (cur.getLong(0) == id) {
						cur.close();
						return true;
					}
					cur.moveToNext();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (cur != null) {
					cur.close();
					cur = null;
				}
			}
			return false;
		}
		return false;
	}

	// huanglingjun
	public static boolean isRepeat(Context context, long id, String tableName) {
		if(context == null){
			return false;
		}
		
		long favorites_id;

		if (id < 0) {

		} else {
			ContentResolver resolver = context.getContentResolver();

			String favorites_where = PlaylistsColumns.NAME + "='" + tableName
					+ "'";
			String[] favorites_cols = new String[] { BaseColumns._ID };
			Uri favorites_uri = Audio.Playlists.EXTERNAL_CONTENT_URI;
			Cursor cursor = resolver.query(favorites_uri, favorites_cols,
					favorites_where, null, null);
			if(cursor == null){
				return false;
			}
			if (cursor.getCount() <= 0) {
				favorites_id = createPlaylist(context, tableName);
			} else {
				cursor.moveToFirst();
				favorites_id = cursor.getLong(0);
				cursor.close();
			}

			String[] cols = new String[] { Playlists.Members.AUDIO_ID };
			Uri uri = Playlists.Members.getContentUri(EXTERNAL, favorites_id);
			Cursor cur = resolver.query(uri, cols, null, null, null);

			cur.moveToFirst();
			while (!cur.isAfterLast()) {
				if (cur.getLong(0) == id) {
					cur.close();
					return true;
				}
				cur.moveToNext();
			}
			cur.close();
			return false;
		}
		return false;
	}

	/**
	 * @param context
	 * @param id
	 */
	public static void removeFromFavorites(Context context, long id) {
		if(context == null){
			return;
		}
		SharedPreferences sharedPreferences = context.getSharedPreferences(
				"ShareXML", Context.MODE_PRIVATE);
		String tableName = sharedPreferences.getString("Favorites",
				context.getString(R.string.my_love));
		long favorites_id;
		if (id < 0) {
		} else {
			ContentResolver resolver = context.getContentResolver();
			/*
			 * String favorites_where = PlaylistsColumns.NAME + "='" +
			 * PLAYLIST_NAME_FAVORITES + "'";
			 */
			String favorites_where = PlaylistsColumns.NAME + "='" + tableName
					+ "'";
			String[] favorites_cols = new String[] { BaseColumns._ID };
			Uri favorites_uri = Audio.Playlists.EXTERNAL_CONTENT_URI;
			Cursor cursor = resolver.query(favorites_uri, favorites_cols,
					favorites_where, null, null);
			if(cursor == null){
				return ;
			}
			if (cursor.getCount() <= 0) {
				// favorites_id = createPlaylist(context,
				// PLAYLIST_NAME_FAVORITES);
				favorites_id = createPlaylist(context, tableName);
			} else {
				cursor.moveToFirst();
				favorites_id = cursor.getLong(0);
				cursor.close();
			}
			Uri uri = Playlists.Members.getContentUri(EXTERNAL, favorites_id);
			resolver.delete(uri, Playlists.Members.AUDIO_ID + "=" + id, null);
		}
	}

	/**
	 * @param mService
	 * @param mImageButton
	 * @param id
	 */
	// public static void setFavoriteImage(ImageButton mImageButton) {
	// try {
	// if (MusicUtils.mService
	// .isFavorite(MusicUtils.mService.getAudioId())) {
	// mImageButton
	// .setImageResource(R.drawable.apollo_holo_light_favorite_selected);
	// } else {
	// mImageButton
	// .setImageResource(R.drawable.apollo_holo_light_favorite_normal);
	// }
	// } catch (RemoteException e) {
	// e.printStackTrace();
	// }
	// }

	/**
	 * @param mContext
	 * @param id
	 * @param name
	 */

	// 新建一个播放列表
	public static void renamePlaylist(Context mContext, long id, String name) {

		if (name != null && name.length() > 0) {
			ContentResolver resolver = mContext.getContentResolver();
			ContentValues values = new ContentValues(1);
			values.put(PlaylistsColumns.NAME, name);
			values.put(PlaylistsColumns.DATA,
					"content://media/external/audio/playlists/" + name);
			resolver.update(Audio.Playlists.EXTERNAL_CONTENT_URI, values,
					BaseColumns._ID + "=?", new String[] { String.valueOf(id) });
			// Toast.makeText(mContext, "Playlist renamed", Toast.LENGTH_SHORT)
			// .show();
		}
	}

	/**
	 * @param mContext
	 * @param list
	 */
	public static void addToCurrentPlaylist(Context mContext, long[] list) {

		if (mService == null)
			return;
		try {
			mService.enqueue(list, ApolloService.LAST);
			String message = mContext.getResources().getQuantityString(
					R.plurals.NNNtrackstoplaylist, list.length,
					Integer.valueOf(list.length));
			Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
		} catch (RemoteException ex) {
		} catch (IllegalStateException e) {
		}
	}

	/**
	 * @param context
	 * @param secs
	 * @return time String
	 */
	public static String makeTimeString(Context context, long secs) {

		String durationformat = context
				.getString(secs < 3600 ? R.string.durationformatshort
						: R.string.durationformatlong);

		/*
		 * Provide multiple arguments so the format can be changed easily by
		 * modifying the xml.
		 */
		sFormatBuilder.setLength(0);

		final Object[] timeArgs = sTimeArgs;
		timeArgs[0] = secs / 3600;
		timeArgs[1] = secs / 60;
		timeArgs[2] = secs / 60 % 60;
		timeArgs[3] = secs;
		timeArgs[4] = secs % 60;

		return sFormatter.format(durationformat, timeArgs).toString();
	}

	/**
	 * @return current album ID
	 */
	public static long getCurrentAlbumId() {

		if (mService != null) {
			try {
				return mService.getAlbumId();
			} catch (RemoteException ex) {
			} catch (IllegalStateException e) {
			}
		}
		return -1;
	}

	/**
	 * @return current artist ID
	 */
	public static long getCurrentArtistId() {

		if (MusicUtils.mService != null) {
			try {
				return mService.getArtistId();
			} catch (RemoteException ex) {
			} catch (IllegalStateException e) {
			}
		}
		return -1;
	}

	/**
	 * @return current track ID
	 */
	public static long getCurrentAudioId() {

		if (MusicUtils.mService != null) {
			try {
				return mService.getAudioId();
			} catch (RemoteException ex) {
			} catch (IllegalStateException e) {
			}
		}
		return -1;
	}

	/**
	 * @return current artist name
	 */
	public static String getArtistName() {

		if (mService != null) {
			try {
				return mService.getArtistName();
			} catch (RemoteException ex) {
			} catch (IllegalStateException e) {
			}
		}
		return null;
	}

	/**
	 * @return current album name
	 */
	public static String getAlbumName() {

		if (mService != null) {
			try {
				return mService.getAlbumName();
			} catch (RemoteException ex) {
			} catch (IllegalStateException e) {
			}
		}
		return null;
	}

	/**
	 * @return current track name
	 */
	public static String getTrackName() {

		if (mService != null) {
			try {
				return mService.getTrackName();
			} catch (RemoteException ex) {
			} catch (IllegalStateException e) {
			}
		}
		return null;
	}

	/**
	 * @return duration of a track
	 */
	public static long getDuration() {
		if (mService != null) {
			try {
				return mService.duration();
			} catch (RemoteException e) {
			} catch (IllegalStateException e) {
			}
		}
		return 0;
	}

	/**
	 * Create a Search Chooser
	 */
	public static void doSearch(Context mContext, Cursor mCursor, int index) {
		CharSequence title = null;
		Intent i = new Intent();
		i.setAction(MediaStore.INTENT_ACTION_MEDIA_SEARCH);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		String query = mCursor.getString(index);
		title = "";
		i.putExtra("", query);
		title = title + " " + query;
		title = "Search " + title;
		i.putExtra(SearchManager.QUERY, query);
		mContext.startActivity(Intent.createChooser(i, title));
	}

	/**
	 * Create a Search Chooser
	 */
	public static void doSearch(Context mContext, Cursor mCursor, String Type) {
		CharSequence title = null;
		Intent i = new Intent();
		i.setAction(MediaStore.INTENT_ACTION_MEDIA_SEARCH);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		String query = "";

		if (Type == TYPE_ALBUM) {
			query = mCursor.getString(mCursor
					.getColumnIndexOrThrow(AlbumColumns.ALBUM));
		} else if (Type == TYPE_ARTIST) {
			query = mCursor.getString(mCursor
					.getColumnIndexOrThrow(ArtistColumns.ARTIST));
		} else if (Type == TYPE_GENRE || Type == TYPE_PLAYLIST
				|| Type == TYPE_SONG) {
			query = mCursor.getString(mCursor
					.getColumnIndexOrThrow(MediaColumns.TITLE));
		}
		title = "";
		i.putExtra("", query);
		title = title + " " + query;
		title = "Search " + title;
		i.putExtra(SearchManager.QUERY, query);
		mContext.startActivity(Intent.createChooser(i, title));
	}

	/**
	 * Method that removes all tracks from the current queue
	 */
	public static void removeAllTracks() {
		try {
			if (mService == null) {
				long[] current = MusicUtils.getQueue();
				if (current != null) {
					mService.removeTracks(0, current.length - 1);
				}
			}
		} catch (RemoteException e) {
		}
	}

	/**
	 * @param id
	 * @return removes track from a playlist
	 */
	public static int removeTrack(long id) {
		if (mService == null)
			return 0;

		try {
			return mService.removeTrack(id);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * @param index
	 */
	public static void setQueuePosition(int index) {
		if (mService == null)
			return;
		try {
			mService.setQueuePosition(index);
		} catch (RemoteException e) {
		}
	}

	public static String getArtistName(Context mContext, long artist_id,
			boolean default_name) {
		String where = BaseColumns._ID + "=" + artist_id;
		String[] cols = new String[] { ArtistColumns.ARTIST };
		Uri uri = Audio.Artists.EXTERNAL_CONTENT_URI;
		Cursor cursor = mContext.getContentResolver().query(uri, cols, where,
				null, null);
		if (cursor == null) {
			return MediaStore.UNKNOWN_STRING;
		}
		if (cursor.getCount() <= 0) {
			if (default_name)
				return mContext.getString(R.string.unknown);
			else
				return MediaStore.UNKNOWN_STRING;
		} else {
			cursor.moveToFirst();
			String name = cursor.getString(0);
			cursor.close();
			if (name == null || MediaStore.UNKNOWN_STRING.equals(name)) {
				if (default_name)
					return mContext.getString(R.string.unknown);
				else
					return MediaStore.UNKNOWN_STRING;
			}
			return name;
		}
	}

	/**
	 * @param mContext
	 * @param album_id
	 * @param default_name
	 * @return album name
	 */
	public static String getAlbumName(Context mContext, long album_id,
			boolean default_name) {
		String where = BaseColumns._ID + "=" + album_id;
		String[] cols = new String[] { AlbumColumns.ALBUM };
		Uri uri = Audio.Albums.EXTERNAL_CONTENT_URI;
		Cursor cursor = mContext.getContentResolver().query(uri, cols, where,
				null, null);
		if (cursor == null) {
			return MediaStore.UNKNOWN_STRING;
		}
		if (cursor.getCount() <= 0) {
			if (default_name)
				return mContext.getString(R.string.unknown);
			else
				return MediaStore.UNKNOWN_STRING;
		} else {
			cursor.moveToFirst();
			String name = cursor.getString(0);
			cursor.close();
			if (name == null || MediaStore.UNKNOWN_STRING.equals(name)) {
				if (default_name)
					return mContext.getString(R.string.unknown);
				else
					return MediaStore.UNKNOWN_STRING;
			}
			return name;
		}
	}

	/**
	 * @param playlist_id
	 * @return playlist name
	 */
	public static String getPlaylistName(Context mContext, long playlist_id) {
		String where = BaseColumns._ID + "=" + playlist_id;
		String[] cols = new String[] { PlaylistsColumns.NAME };
		Uri uri = Audio.Playlists.EXTERNAL_CONTENT_URI;
		Cursor cursor = mContext.getContentResolver().query(uri, cols, where,
				null, null);
		if (cursor == null) {
			return "";
		}
		if (cursor.getCount() <= 0)
			return "";
		cursor.moveToFirst();
		String name = cursor.getString(0);
		cursor.close();
		return name;
	}

	/**
	 * @param mContext
	 * @param genre_id
	 * @param default_name
	 * @return genre name
	 */
	public static String getGenreName(Context mContext, long genre_id,
			boolean default_name) {
		String where = BaseColumns._ID + "=" + genre_id;
		String[] cols = new String[] { GenresColumns.NAME };
		Uri uri = Audio.Genres.EXTERNAL_CONTENT_URI;
		Cursor cursor = mContext.getContentResolver().query(uri, cols, where,
				null, null);
		if (cursor == null) {
			return MediaStore.UNKNOWN_STRING;
		}
		if (cursor.getCount() <= 0) {
			if (default_name)
				return mContext.getString(R.string.unknown);
			else
				return MediaStore.UNKNOWN_STRING;
		} else {
			cursor.moveToFirst();
			String name = cursor.getString(0);
			cursor.close();
			if (name == null || MediaStore.UNKNOWN_STRING.equals(name)) {
				if (default_name)
					return mContext.getString(R.string.unknown);
				else
					return MediaStore.UNKNOWN_STRING;
			}
			return name;
		}
	}

	/**
	 * @param genre
	 * @return parsed genre name
	 */
	public static String parseGenreName(Context mContext, String genre) {
		int genre_id = -1;

		if (genre == null || genre.trim().length() <= 0)
			return mContext.getResources().getString(R.string.unknown);

		try {
			genre_id = Integer.parseInt(genre);
		} catch (NumberFormatException e) {
			return genre;
		}
		if (genre_id >= 0 && genre_id < GENRES_DB.length)
			return GENRES_DB[genre_id];
		else
			return mContext.getResources().getString(R.string.unknown);
	}

	/**
	 * @return if music is playing
	 */
	public static boolean isPlaying() {
		if (mService == null)
			return false;

		try {
			return mService.isPlaying();
		} catch (RemoteException e) {
		}
		return false;
	}

	/**
	 * @return current track's queue position
	 */
	public static int getQueuePosition() {
		if (mService == null)
			return 0;
		try {
			return mService.getQueuePosition();
		} catch (RemoteException e) {
		}
		return 0;
	}

	/**
	 * @param mContext
	 * @param create_shortcut
	 * @param list
	 *
	 */
	// huanglingjun 新建
	public static void makePlaylistList(Context mContext,
			boolean create_shortcut, List<Map<String, String>> list) {

		Map<String, String> map;

		String[] cols = new String[] { Audio.Playlists._ID,
				Audio.Playlists.NAME };
		StringBuilder where = new StringBuilder();

		ContentResolver resolver = mContext.getContentResolver();
		if (resolver == null) {
		} else {
			where.append(Audio.Playlists.NAME + " != ''");
			where.append(" AND " + Audio.Playlists.NAME + " != '"
					+ PLAYLIST_NAME_FAVORITES + "'");
			Cursor cur = resolver.query(Audio.Playlists.EXTERNAL_CONTENT_URI,
					cols, where.toString(), null, Audio.Playlists.DATE_ADDED);
			list.clear();

			map = new HashMap<String, String>();
			map.put("id", String.valueOf(PLAYLIST_QUEUE));
			map.put("name", mContext.getString(R.string.queue));
			list.add(map);

			map = new HashMap<String, String>();
			map.put("id", String.valueOf(PLAYLIST_NEW));
			map.put("name", mContext.getString(R.string.new_playlist));
			list.add(map);

			if (cur != null && cur.getCount() > 0) {
				cur.moveToFirst();
				while (!cur.isAfterLast()) {
					map = new HashMap<String, String>();
					map.put("id", String.valueOf(cur.getLong(0)));
					map.put("name", cur.getString(1));
					list.add(map);
					cur.moveToNext();
				}
			}
			if (cur != null) {
				cur.close();
			}
		}
	}

	/**
	 * @param mContext
	 * @param create_shortcut
	 * @param list
	 *
	 */
	// huanglingjun 新建
	public static void makeNeedPlaylistList(Context mContext,
			boolean create_shortcut, List<Map<String, String>> list) {

		Map<String, String> map;

		String[] cols = new String[] { Audio.Playlists._ID,
				Audio.Playlists.NAME };
		StringBuilder where = new StringBuilder();

		ContentResolver resolver = mContext.getContentResolver();
		if (resolver == null) {
			System.out.println("resolver = null");
		} else {
			// where.append(Audio.Playlists.NAME + " != ''");
			// where.append(" AND " + Audio.Playlists.NAME + " != '" + "新建列表"
			// + "'");
			Cursor cur = resolver.query(Audio.Playlists.EXTERNAL_CONTENT_URI,
					cols, where.toString(), null, Audio.Playlists.DATE_ADDED);
			list.clear();

			// map = new HashMap<String, String>();
			// map.put("id", String.valueOf(PLAYLIST_FAVORITES));
			// map.put("name", mContext.getString(R.string.favorite));
			// list.add(map);
			if (create_shortcut) {

				map = new HashMap<String, String>();
				map.put("id", String.valueOf(PLAYLIST_QUEUE));
				map.put("name", mContext.getString(R.string.queue));
				list.add(map);

				map = new HashMap<String, String>();
				map.put("id", String.valueOf(PLAYLIST_NEW));
				map.put("name", mContext.getString(R.string.new_playlist));
				list.add(map);
			}

			if (cur != null && cur.getCount() > 0) {
				cur.moveToFirst();
				while (!cur.isAfterLast()) {
					map = new HashMap<String, String>();
					if (cur.getLong(0) >= 0) {
						continue;
					}
					map.put("id", String.valueOf(cur.getLong(0)));
					map.put("name", cur.getString(1));
					list.add(map);
					cur.moveToNext();
				}
			}
			if (cur != null) {
				cur.close();
			}
		}
	}

	public static void notifyWidgets(String what) {
		try {
			mService.notifyChange(what);
		} catch (Exception e) {
		}
	}

	/**
	 * 删除指定路径的文件
	 * 
	 * @return 删除成功返回true, 删除失败false.
	 */
	public static boolean deleteFile(String path) {
		if (TextUtils.isEmpty(path)) {
			return false;
		}
		boolean isDeleted = false;
		File file = new File(path);
		if (file.exists()) {
			isDeleted = file.delete();
		}
		return isDeleted;
	}

	/**
	 * 批量删除指定文件
	 * 
	 * @param 要删除的文件路径数组
	 * @return 路径数组不为空即返回true
	 */
	public static boolean deleteFiles(String[] paths) {
		if (paths == null) {
			return false;
		}
		for (int i = 0; i < paths.length; i++) {
			deleteFile(paths[i]);
		}
		return true;
	}

	/**
	 * 从数据库中移除指定的音频，不会实际删除本地文件
	 * 
	 * @param resolver
	 *            ContentResolver
	 * @param audioIds
	 *            音乐Id数组
	 * @return boolean 移除成功返回true, 删除失败false.
	 * @see 类名/完整类名/完整类名#方法名
	 */
	public static boolean removeTrackFromDatabase(ContentResolver resolver,
			long[] audioIds) {
		if (audioIds == null || audioIds.length <= 0) {
			return false;
		}

		boolean isRemoved = false;

		// 将整型数组变成(1,2,3,4,5)的格式，作为稍后的数据库删除的where子句
		StringBuffer toRemoveIds = new StringBuffer("(");
		for (int i = 0; i < audioIds.length; i++) {
			toRemoveIds.append(audioIds[i] + ",");
		}
		toRemoveIds.setCharAt(toRemoveIds.length() - 1, ')');

		// 从数据库中移除音频记录
		int deleteRowCount = resolver.delete(Media.EXTERNAL_CONTENT_URI,
				Media._ID + " in " + toRemoveIds, null);
		if (deleteRowCount > 0) {
			isRemoved = true;
		}
		return isRemoved;
	}

	/**
	 * 从播放列表移除指定歌曲
	 * 
	 * @param resolver
	 *            Context的ContentResolver实例
	 * @param playlistId
	 *            播放列表的ID
	 * @param audioIds
	 *            要移除的音频ID
	 * @return 删除成功返回true,否则返回false
	 */

	public static boolean removeTrackFromPlaylist(ContentResolver resolver,
			long playlistId, long[] audioIds) {
		if (audioIds == null) {
			return false;
		}
		if (playlistId > 0) {
			return false;
		}
		boolean isRemoved = false;
		int deleteRowCount = 0;

		// 将整型数组变成(1,2,3,4,5)的格式，作为稍后的数据库删除的where子句
		StringBuffer toDeletIds = new StringBuffer("(");
		for (int i = 0; i < audioIds.length; i++) {
			toDeletIds.append(audioIds[i] + ",");
		}
		toDeletIds.setCharAt(toDeletIds.length() - 1, ')');

		// 从Members表中移除记录
		Uri uri = Playlists.Members.getContentUri("external", playlistId);
		deleteRowCount = resolver.delete(uri, Playlists.Members.AUDIO_ID
				+ " in " + toDeletIds, null);
		if (deleteRowCount > 0) {
			isRemoved = true;
		}
		return isRemoved;
	}

	/**
	 * 为播放列表添加成员
	 * 
	 * @param resolver
	 *            Context的ContentResolver实例
	 * @param playlistId
	 *            播放列表的ID
	 * @param audioIds
	 *            添加的音频ID们
	 * @return true表示该歌曲已经存在指定列表中，false表示添加成功
	 */
	public static boolean addTrackToPlaylist(Context context, long playlistId, long[] audioIds) {
		if(context == null){
			return false;
		}
		
		if (audioIds == null || audioIds.length == 0) {
			return true;
		}
		ContentResolver resolver = context.getContentResolver();
		boolean hasExistedItems = false;
		long[] existedIds = null;

		// 将audioIds变为(2,3,4,5)的形式，作数据库查询条件用
		StringBuffer audioIdsstring = new StringBuffer("(");
		for (int i = 0; i < audioIds.length; i++) {
			audioIdsstring.append(audioIds[i] + ",");
		}
		audioIdsstring.setCharAt(audioIdsstring.length() - 1, ')');

		// 先查询该播放列表中有无该歌曲，有则不做插入
		Cursor cursor = resolver.query(
				Playlists.Members.getContentUri("external", playlistId),
				new String[] { Playlists.Members.AUDIO_ID },
				Playlists.Members.AUDIO_ID + " in " + audioIdsstring, null,
				null);
		if (cursor != null) {
			if (cursor.getCount() == audioIds.length) {
				// 如果Members表中已经拥有所有要添加的歌曲，直接返回已经存在
				cursor.close();
				return true;
			}
			hasExistedItems = !(cursor.getCount() <= 0);
			if (hasExistedItems) {
				existedIds = new long[cursor.getCount()];
				int index_id = cursor
						.getColumnIndex(Playlists.Members.AUDIO_ID);
				int i = 0;
				while (cursor.moveToNext()) {
					existedIds[i] = cursor.getLong(index_id);
					i++;
				}
				cursor.close();
			}
		}

		// 列表中无指定的歌曲，则向Members表中插入记录
		Uri uri = Playlists.Members.getContentUri("external", playlistId);
		ContentValues values = null;
		if (hasExistedItems) {
			for (int i = 0; i < audioIds.length; i++) {
				if (!isIdInTheIntArray(audioIds[i], existedIds)) {
					values = new ContentValues();
					values.put(Playlists.Members.PLAY_ORDER, audioIds[i]);
					values.put(Playlists.Members.AUDIO_ID, audioIds[i]);
					Uri newInsertUri = resolver.insert(uri, values);
					// LogUtils.i(TAG, "The new uri added to Members:"
					// + newInsertUri);
				}
			}
		} else {
			for (int i = 0; i < audioIds.length; i++) {
				values = new ContentValues();
				values.put(Playlists.Members.PLAY_ORDER, audioIds[i]);
				values.put(Playlists.Members.AUDIO_ID, audioIds[i]);
				Uri newInsertUri = resolver.insert(uri, values);
				LogUtils.i(TAG, "The new uri added to Members:" + newInsertUri);
			}
		}
		return false;
	}

	private static boolean isIdInTheIntArray(long id, long a[]) {
		for (int i = 0; i < a.length; i++) {
			if (a[i] == id) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 
	 * 获取音乐文件的实际路径
	 * 
	 * @param context
	 *            上下文
	 * @param audioIds
	 *            音乐文件的AudioID
	 * @return String[] 音乐文件的实际路径数组
	 */
	public static String[] getAudioPaths(Context context, long[] audioIds) {

		String[] selectedAudioPaths = new String[audioIds.length];
		ContentResolver resolver = context.getContentResolver();
		int length = audioIds.length;
		if (length <= 0) {
			return selectedAudioPaths;
		}
		// 将audioIds变为(2,3,4,5)的形式，作数据库查询条件用
		StringBuffer audioIdsstring = new StringBuffer("(");
		for (int i = 0; i < length; i++) {
			audioIdsstring.append(audioIds[i] + ",");
		}
		if (audioIdsstring.length() == 1) {
			return selectedAudioPaths;
		}
		audioIdsstring.setCharAt(audioIdsstring.length() - 1, ')');

		LogUtils.i(TAG, "audioIdsstring=" + audioIdsstring);
		// 先查询该播放列表中有无该歌曲，有则不做插入
		Cursor cursor = resolver.query(Audio.Media.EXTERNAL_CONTENT_URI,
				new String[] { MediaColumns.DATA }, BaseColumns._ID + " in "
						+ audioIdsstring, null, null);
		int i = 0;
		while (cursor != null && cursor.getCount() > 0 && cursor.moveToNext()) {
			int index_id = cursor.getColumnIndex(MediaColumns.DATA);
			if (index_id < 0) {
				return selectedAudioPaths;
			}
			selectedAudioPaths[i] = cursor.getString(index_id);
			i++;

		}
		
		if(cursor != null){
			cursor.close();
		}
		return selectedAudioPaths;
	}

	/**
	 * 获取音乐文件的
	 * 
	 * @param context
	 * @param audioIds
	 *            音乐文件的AudioID
	 * @return Cursor
	 */
	public static Cursor getAudioIDs(Context context, long[] audioIds) {
		if (audioIds == null) {
			return null;
		}
		ContentResolver resolver = context.getContentResolver();
		int length = audioIds.length;
		if (length <= 0) {
			return null;
		}
		// 将audioIds变为(2,3,4,5)的形式，作数据库查询条件用
		StringBuffer audioIdsstring = new StringBuffer("(");
		for (int i = 0; i < length; i++) {
			if (audioIds[i] <= 0) {
				continue;
			}
			audioIdsstring.append(audioIds[i] + ",");
		}
		if (audioIdsstring.length() == 1) {
			return null;
		}
		audioIdsstring.setCharAt(audioIdsstring.length() - 1, ')');
		LogUtils.i(TAG, "audioIdsstring=" + audioIdsstring.toString());

		Cursor cursor = resolver.query(Audio.Media.EXTERNAL_CONTENT_URI,
				new String[] { MediaColumns.TITLE, AudioColumns.ARTIST,
						BaseColumns._ID }, BaseColumns._ID + " in "
						+ audioIdsstring, null, MediaColumns.DATE_ADDED
						+ " DESC");

		return cursor;
	}

	/**
	 * 获取音乐文件的DisPlayName
	 * 
	 * @param context
	 * @param audioIds
	 *            音乐文件的AudioID
	 * @return String 音乐文件的DisPlayName
	 */
	public static String getAudioDisPlayName(Context context, long audioId) {
		if (audioId < 0 || context == null) {
			return "";
		}
		ContentResolver resolver = context.getContentResolver();

		Cursor cursor = resolver.query(Audio.Media.EXTERNAL_CONTENT_URI,
				new String[] { MediaColumns.DISPLAY_NAME }, BaseColumns._ID
						+ " = " + audioId, null, null);
		if (cursor != null && cursor.getCount() > 0) {
			int index = cursor.getColumnIndex(MediaColumns.DISPLAY_NAME);
			if (index == -1) {
				cursor.close();
				return "";
			} else {
				cursor.moveToNext();
				String displayName = cursor.getString(index);
				cursor.close();
				return displayName;
				
			}		
		}
		return "";
	}

	/**
	 * 得到全部列表id和表名
	 * 
	 * @param context
	 *            上下文
	 * @return List<Map<String,Object>>
	 */
	public static List<Map<String, Object>> getTableList(Context context) {
		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
		String[] tablist_cols = new String[] { BaseColumns._ID,
				PlaylistsColumns.NAME };
		Uri tablist_uri = Audio.Playlists.EXTERNAL_CONTENT_URI;
		Cursor cursor = query(context, tablist_uri, tablist_cols, null, null,
				null);
		try {
			if (cursor != null && cursor.getCount() > 0) {
				while (cursor.moveToNext()) {
					long ids = cursor.getLong(cursor
							.getColumnIndexOrThrow(BaseColumns._ID));
					String title = cursor.getString(cursor
							.getColumnIndexOrThrow(PlaylistsColumns.NAME));
					Map<String, Object> map = new HashMap<String, Object>();
					if (!title.equals(context.getString(R.string.create_list))) {
						map.put("id", ids);
						map.put("name", title);
						maps.add(map);
					}
						
				}	
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return maps;
	}

	/**
	 * Whether the device has a mounded sdcard or not.
	 *
	 * @param context
	 *            a context.
	 * @return If one of the sdcard is mounted, return true, otherwise return
	 *         false.
	 */
	public boolean hasMountedSDcard(Context context) {
		StorageManager storageManager = (StorageManager) context
				.getSystemService(Context.STORAGE_SERVICE);
		boolean hasMountedSDcard = false;
		try {
			if (storageManager != null) {
				String[] volumePath = (String[]) storageManager.getClass()
						.getMethod("getVolumePaths", null)
						.invoke(storageManager, null);
				// String[] volumePath = storageManager.getVolumePaths();
				if (volumePath != null) {
					String status = null;
					int length = volumePath.length;
					for (int i = 0; i < length; i++) {
						status = (String) storageManager
								.getClass()
								.getMethod("getVolumeState",
										new Class[] { String.class })
								.invoke(storageManager, volumePath[i]);
						// status =
						// storageManager.getVolumeState(volumePath[i]);
						LogUtils.d(TAG, "hasMountedSDcard: path = "
								+ volumePath[i] + ",status = " + status);
						if (Environment.MEDIA_MOUNTED.equals(status)) {
							hasMountedSDcard = true;
						}
					}
				}
			}
		} catch (Exception e) {

			return hasMountedSDcard;

		}
		LogUtils.d(TAG, "hasMountedSDcard = " + hasMountedSDcard);
		return hasMountedSDcard;
	}
	
	/**
	 * 方法描述：是否安装虾米
	 */
	public static boolean hasInstalledXiaMi(Context context) {
		PackageManager pm = context.getPackageManager();
        PackageInfo packageInfo = null;
        String versionName = null;
        try {
            //packageInfo = pm.getPackageInfo("fm.xiami.main", PackageManager.GET_ACTIVITIES);
            packageInfo = pm.getPackageInfo("com.duomi.android", PackageManager.GET_ACTIVITIES);
            versionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("FotaUpdate", "isApkExist not found");
            return false;
        }
        if (versionName != null) {
            String[] names = versionName.split("\\.");
            
            if (names.length >= 4 && "9".equals(names[3])) {
                return false;
            }
        }
        Log.i("FotaUpdate", "isApkExist = true");
        return true;
	}
	
	public static String getRealPathFromURI(Context context, Uri contentUri) {
		  Cursor cursor = null;
		  try { 
		    String[] proj = { MediaStore.Audio.Media.DATA };
		    cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
		    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
		    cursor.moveToFirst();
		    return cursor.getString(column_index);
		  } catch(Exception e){
			  Log.i("pengcc", "file does not exit");
			  return null;
		  }finally {
		    if (cursor != null) {
		      cursor.close();
		    }
		  }
		}
	
	public interface Defs {
        public final static int OPEN_URL = 0;
        public final static int ADD_TO_PLAYLIST = 1;
        public final static int USE_AS_RINGTONE = 2;
        public final static int PLAYLIST_SELECTED = 3;
        public final static int NEW_PLAYLIST = 4;
        public final static int PLAY_SELECTION = 5;
        public final static int GOTO_START = 6;
        public final static int GOTO_PLAYBACK = 7;
        public final static int PARTY_SHUFFLE = 8;
        public final static int SHUFFLE_ALL = 9;
        public final static int DELETE_ITEM = 10;
        public final static int SCAN_DONE = 11;
        public final static int QUEUE = 12;
        public final static int EFFECTS_PANEL = 13;
        /// M: add for send fm transmitter
        public final static int FM_TRANSMITTER = 14;
        /// M: add for drm
        public final static int DRM_INFO = 15;
        public final static int CHILD_MENU_BASE = 16; // this should be the last item
        /**M: Add Hotknot menu.@{**/
        public final static int HOTKNOT = CHILD_MENU_BASE + 10;
        /**@}**/
    }
	
	/**
	 * 
	 * 获取音乐文件的实际路径
	 * 
	 * @param context
	 *            上下文
	 * @param audioIds
	 *            音乐文件的AudioID
	 * @return String[] 音乐文件的实际路径数组
	 */
	public static String getCurrentAudioPath(Context context, long audioId) {

		String currentAudioPath=null;
		ContentResolver resolver = context.getContentResolver();
		// 将audioIds变为(2,3,4,5)的形式，作数据库查询条件用
		StringBuffer audioIdsstring = new StringBuffer("(");
		audioIdsstring.append(audioId + ",");
		if (audioIdsstring.length() == 1) {
			return currentAudioPath;
		}
		audioIdsstring.setCharAt(audioIdsstring.length() - 1, ')');

		LogUtils.i(TAG, "audioIdsstring=" + audioIdsstring);
		// 先查询该播放列表中有无该歌曲，有则不做插入
		Cursor cursor = resolver.query(Audio.Media.EXTERNAL_CONTENT_URI,
				new String[] { MediaColumns.DATA }, BaseColumns._ID + " in "
						+ audioIdsstring, null, null);
		int i = 0;
		while (cursor != null && cursor.getCount() > 0 && cursor.moveToNext()) {
			int index_id = cursor.getColumnIndex(MediaColumns.DATA);
			if (index_id < 0) {
				return currentAudioPath;
			}
			currentAudioPath = cursor.getString(index_id);
			i++;

		}
		return currentAudioPath;
	}
}
