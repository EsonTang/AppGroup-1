/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.calendar.selectcalendars;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.android.calendar.AsyncQueryService;
import com.android.calendar.CalendarController;
import com.android.calendar.R;
import com.android.calendar.Utils;
import com.android.calendar.selectcalendars.CalendarColorCache.OnCalendarColorsLoadedListener;

public class SelectVisibleCalendarsFragment extends Fragment
        implements AdapterView.OnItemClickListener, CalendarController.EventHandler,
        OnCalendarColorsLoadedListener {

    private static final String TAG = "Calendar";
    private static final String IS_PRIMARY = "\"primary\"";
    private static final String SELECTION = Calendars.SYNC_EVENTS + "=?";
    private static final String[] SELECTION_ARGS = new String[] {"1"};

    private static final String[] PROJECTION = new String[] {
        Calendars._ID,
        Calendars.ACCOUNT_NAME,
        Calendars.ACCOUNT_TYPE,
        Calendars.OWNER_ACCOUNT,
        Calendars.CALENDAR_DISPLAY_NAME,
        Calendars.CALENDAR_COLOR,
        Calendars.VISIBLE,
        Calendars.SYNC_EVENTS,
        "(" + Calendars.ACCOUNT_NAME + "=" + Calendars.OWNER_ACCOUNT + ") AS " + IS_PRIMARY,
      };
    private static int mUpdateToken;
    private static int mQueryToken;
    private static int mCalendarItemLayout = R.layout.mini_calendar_item;

    private View mView = null;
    private CalendarController mController;
    private ListView mList;
    private SelectCalendarsSimpleAdapter mAdapter;
    private Activity mContext;
    private AsyncQueryService mService;
    private Cursor mCursor;

    public SelectVisibleCalendarsFragment() {
    }

    public SelectVisibleCalendarsFragment(int itemLayout) {
        mCalendarItemLayout = itemLayout;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        mController = CalendarController.getInstance(activity);
        mController.registerEventHandler(R.layout.select_calendars_fragment, this);
        mService = new AsyncQueryService(activity) {
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                mAdapter.changeCursor(cursor);
                mCursor = cursor;
                /**
                 * M: When account is only PC sync, set button of calendar to
                 * sync as disable.@{
                 */
                if (cursor == null) {
                    return;
                }
                int index = cursor.getColumnIndex(Calendars.ACCOUNT_TYPE);
                View v = mView.findViewById(R.id.manage_sync_set);
                cursor.moveToFirst();
                do {
                    if (index != -1
                            && CalendarContract.ACCOUNT_TYPE_LOCAL.equals(cursor.getString(index))) {
                        v.setEnabled(false);
                    } else {
                        Log.e(TAG, "the colume do not exsit or it is not local account");
                        v.setEnabled(true);
                        break;
                    }
                } while(cursor.moveToNext());
                /** @} */
            }
        };
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mController.deregisterEventHandler(R.layout.select_calendars_fragment);
        if (mCursor != null) {
            mAdapter.changeCursor(null);
            mCursor.close();
            mCursor = null;
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mView = inflater.inflate(R.layout.select_calendars_fragment, null);
        mList = (ListView)mView.findViewById(R.id.list);

        // Hide the Calendars to Sync button on tablets for now.
        // Long terms stick it in the list of calendars
        if (Utils.getConfigBool(getActivity(), R.bool.multiple_pane_config)) {
            // Don't show dividers on tablets
            mList.setDivider(null);
            View v = mView.findViewById(R.id.manage_sync_set);
            if (v != null) {
                v.setVisibility(View.GONE);
            }
        }
        return mView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Drawable divider = getActivity().getDrawable(R.drawable.listview_divider);
        mList.setDivider(divider);
        mList.setDividerHeight(1);
        mAdapter = new SelectCalendarsSimpleAdapter(mContext, mCalendarItemLayout, null,
                getFragmentManager());
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(this);
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id)  {
        if (mAdapter == null || mAdapter.getCount() <= position) {
            return;
        }
        toggleVisibility(position);
    }

    @Override
    public void onResume() {
        super.onResume();
        mQueryToken = mService.getNextToken();
        mService.startQuery(mQueryToken, null, Calendars.CONTENT_URI, PROJECTION, SELECTION,
                SELECTION_ARGS, Calendars.ACCOUNT_NAME);
    }

    /*
     * Write back the changes that have been made.
     */
    public void toggleVisibility(int position) {
        mUpdateToken = mService.getNextToken();
        Uri uri = ContentUris.withAppendedId(Calendars.CONTENT_URI, mAdapter.getItemId(position));
        ContentValues values = new ContentValues();
        // Toggle the current setting
        int visibility = mAdapter.getVisible(position)^1;
        values.put(Calendars.VISIBLE, visibility);
        mService.startUpdate(mUpdateToken, null, uri, values, null, null, 0);
        mAdapter.setVisible(position, visibility);
    }

    @Override
    public void eventsChanged() {
        if (mService != null) {
            mService.cancelOperation(mQueryToken);
            mQueryToken = mService.getNextToken();
            mService.startQuery(mQueryToken, null, Calendars.CONTENT_URI, PROJECTION, SELECTION,
                    SELECTION_ARGS, Calendars.ACCOUNT_NAME);
        }
    }

    @Override
    public long getSupportedEventTypes() {
        return CalendarController.EventType.EVENTS_CHANGED;
    }

    @Override
    public void handleEvent(CalendarController.EventInfo event) {
        if (event.eventType == CalendarController.EventType.EVENTS_CHANGED) {
            eventsChanged();
        }
    }

    @Override
    public void onCalendarColorsLoaded() {
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

	@Override
	public void eventsChanged(Time selectedTime) {
		// TODO Auto-generated method stub
		
	}
}
