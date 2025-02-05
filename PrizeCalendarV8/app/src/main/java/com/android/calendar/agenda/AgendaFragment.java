/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.calendar.agenda;


import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.CalendarContract.Attendees;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Adapter;
import android.widget.HeaderViewListAdapter;

import com.android.calendar.CalendarController;
import com.android.calendar.CalendarController.EventInfo;
import com.android.calendar.CalendarController.EventType;
import com.android.calendar.CalendarController.ViewType;
import com.android.calendar.EventInfoFragment;
import com.android.calendar.GeneralPreferences;
import com.android.calendar.R;
import com.android.calendar.StickyHeaderListView;
import com.android.calendar.Utils;

import java.util.Date;

public class AgendaFragment extends Fragment implements CalendarController.EventHandler,
        OnScrollListener {

    private static final String TAG = AgendaFragment.class.getSimpleName();
    private static boolean DEBUG = true;

    protected static final String BUNDLE_KEY_RESTORE_TIME = "key_restore_time";
    protected static final String BUNDLE_KEY_RESTORE_INSTANCE_ID = "key_restore_instance_id";
    protected static final String BUNDLE_KEY_RESTORE_TOP_DEVIATION = "key_restore_top_deviation";

    private AgendaListView mAgendaListView;
    private Activity mActivity;
    private final Time mTime;
    private String mTimeZone;
    private final long mInitialTimeMillis;
    private boolean mShowEventDetailsWithAgenda;
    private CalendarController mController;
    private EventInfoFragment mEventFragment;
    private String mQuery;
    private boolean mUsedForSearch = false;
    private boolean mIsTabletConfig;
    private EventInfo mOnAttachedInfo = null;
    private boolean mOnAttachAllDay = false;
    private AgendaWindowAdapter mAdapter = null;
    private boolean mForceReplace = true;
    private long mLastShownEventId = -1;
    /// M: View of event info
    private View mEventView = null;

    boolean isMonthAgenda = false;

    // Tracks the time of the top visible view in order to send UPDATE_TITLE messages to the action
    // bar.
    int  mJulianDayOnTop = -1;

    private final Runnable mTZUpdater = new Runnable() {
        @Override
        public void run() {
            mTimeZone = Utils.getTimeZone(getActivity(), this);
            mTime.switchTimezone(mTimeZone);
        }
    };

    public AgendaFragment() {
        this(0, false, false);
    }


    // timeMillis - time of first event to show
    // usedForSearch - indicates if this fragment is used in the search fragment
    public AgendaFragment(long timeMillis, boolean usedForSearch , boolean isMonth) {
    	isMonthAgenda = isMonth;
        mInitialTimeMillis = timeMillis;
        ///M:keep the time that enter the AgendaFragment{
        mOriginalTime.set(timeMillis);
        mOriginalTime.normalize(false);
        ///@}
        mTime = new Time();
        mLastHandledEventTime = new Time();

        if (mInitialTimeMillis == 0) {
            mTime.setToNow();
        } else {
            mTime.set(mInitialTimeMillis);
        }
        mLastHandledEventTime.set(mTime);
        mUsedForSearch = usedForSearch;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mTimeZone = Utils.getTimeZone(activity, mTZUpdater);
        mTime.switchTimezone(mTimeZone);
        mActivity = activity;
        if (mOnAttachedInfo != null) {
            showEventInfo(mOnAttachedInfo, mOnAttachAllDay, true);
            mOnAttachedInfo = null;
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mController = CalendarController.getInstance(mActivity);

        ///M: Don't show details of an event for AgendaChoiceActivity.@{
        if (getActivity() instanceof com.mediatek.calendar.selectevent.AgendaChoiceActivity) {
            mShowEventDetailsWithAgenda = false;
        }
        else {
            mShowEventDetailsWithAgenda =
                Utils.getConfigBool(mActivity, R.bool.show_event_details_with_agenda);
        }
        ///@}

        mIsTabletConfig =
            Utils.getConfigBool(mActivity, R.bool.tablet_config);
        if (icicle != null) {
            long prevTime = icicle.getLong(BUNDLE_KEY_RESTORE_TIME, -1);
            if (prevTime != -1) {
                mTime.set(prevTime);
                if (DEBUG) {
                    Log.d(TAG, "Restoring time to " + mTime.toString());
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {


        int screenWidth = mActivity.getResources().getDisplayMetrics().widthPixels;
        ///M: #multi-event share# make the sub class be able to inflate the view @{
        View v = extInflateFragmentView(inflater);
        ///@}

        ///M: #multi-event share# make the sub class be able to get the list view @{
        mAgendaListView = extFindListView(v);
        ///@}
        mAgendaListView.setClickable(true);

        if (savedInstanceState != null) {
            long instanceId = savedInstanceState.getLong(BUNDLE_KEY_RESTORE_INSTANCE_ID, -1);
            if (instanceId != -1) {
                mAgendaListView.setSelectedInstanceId(instanceId);
            }

            /*
             * M: Get the data to set the deviation of the listView top, will be
             * used in AgendaWindowAdapter after listView refresh finished.
             */
            if (!mShowEventDetailsWithAgenda) {
                int[] listTopDeviationInfo = savedInstanceState
                        .getIntArray(BUNDLE_KEY_RESTORE_TOP_DEVIATION);
                AgendaWindowAdapter.setTopDeviation(listTopDeviationInfo);
            }
        }

        mEventView =  v.findViewById(R.id.agenda_event_info);
        if (!mShowEventDetailsWithAgenda) {
            mEventView.setVisibility(View.GONE);
        }

        View topListView;
        // Set adapter & HeaderIndexer for StickyHeaderListView
        StickyHeaderListView lv =
            (StickyHeaderListView)v.findViewById(R.id.agenda_sticky_header_list);
        if (lv != null) {
            Adapter a = mAgendaListView.getAdapter();
            lv.setAdapter(a);
            if (a instanceof HeaderViewListAdapter) {
                mAdapter = (AgendaWindowAdapter) ((HeaderViewListAdapter)a).getWrappedAdapter();
                lv.setIndexer(mAdapter);
                lv.setHeaderHeightListener(mAdapter);
            } else if (a instanceof AgendaWindowAdapter) {
                mAdapter = (AgendaWindowAdapter)a;
                lv.setIndexer(mAdapter);
                lv.setHeaderHeightListener(mAdapter);
            } else {
                Log.wtf(TAG, "Cannot find HeaderIndexer for StickyHeaderListView");
            }

            // Set scroll listener so that the date on the ActionBar can be set while
            // the user scrolls the view
            lv.setOnScrollListener(this);
            /** prize-add by-wanzhijuan-2015-7-8-start */
//            lv.setHeaderSeparator(getResources().getColor(R.color.agenda_list_separator_color), 1);
            /** prize-add by-wanzhijuan-2015-7-8-end */
            topListView = lv;
        } else {
            topListView = mAgendaListView;
        }

        // Since using weight for sizing the two panes of the agenda fragment causes the whole
        // fragment to re-measure when the sticky header is replaced, calculate the weighted
        // size of each pane here and set it

        if (!mShowEventDetailsWithAgenda) {
            ViewGroup.LayoutParams params = topListView.getLayoutParams();
            params.width = screenWidth;
            topListView.setLayoutParams(params);
        } else {
            ViewGroup.LayoutParams listParams = topListView.getLayoutParams();
            listParams.width = screenWidth * 4 / 10;
            topListView.setLayoutParams(listParams);
            ViewGroup.LayoutParams detailsParams = mEventView.getLayoutParams();
            detailsParams.width = screenWidth - listParams.width;
            mEventView.setLayoutParams(detailsParams);
        }
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        if(!isMonthAgenda){
            SharedPreferences prefs = GeneralPreferences.getSharedPreferences(
                    getActivity());
            boolean hideDeclined = prefs.getBoolean(
                    GeneralPreferences.KEY_HIDE_DECLINED, false);

            mAgendaListView.setHideDeclinedEvents(hideDeclined);
            if (mLastHandledEventId != -1) {
                mAgendaListView.goTo(mLastHandledEventTime, mLastHandledEventId, mQuery, true, false, isMonthAgenda);
                mLastHandledEventTime = null;
                mLastHandledEventId = -1;
            } else {
                mAgendaListView.goTo(mTime, -1, mQuery, true, false, isMonthAgenda);
            }

        }
        
        
          /*prize-ע�͵�����ж�������жϻ�����һ��Bug,��ʱע�͵�--lixing-2015-6-24-end*/          
            
        mAgendaListView.onResume();

//        // Register for Intent broadcasts
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(Intent.ACTION_TIME_CHANGED);
//        filter.addAction(Intent.ACTION_DATE_CHANGED);
//        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
//        registerReceiver(mIntentReceiver, filter);
//
//        mContentResolver.registerContentObserver(Events.CONTENT_URI, true, mObserver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (DEBUG) {
            Log.e(TAG, "OnSaveInstanceState start time: " + System.currentTimeMillis());
        }
        if (mAgendaListView == null) {
            return;
        }
        if (mShowEventDetailsWithAgenda) {
            long timeToSave;
            if (mLastHandledEventTime != null) {
                timeToSave = mLastHandledEventTime.toMillis(true);
                mTime.set(mLastHandledEventTime);
            } else {
                timeToSave =  System.currentTimeMillis();
                mTime.set(timeToSave);
            }
            outState.putLong(BUNDLE_KEY_RESTORE_TIME, timeToSave);
            mController.setTime(timeToSave);
        } else {
            AgendaWindowAdapter.AgendaItem item = mAgendaListView.getFirstVisibleAgendaItem();
            /*
             * M: if the getFirstVisibleEvent return null, may be the first
             * position is head or day view, we should skip this, and use next
             * position to get the correct EventInfo object.
             */
            int firstVisblePos = mAgendaListView.getFirstVisiblePosition();
            int itemCount = mAdapter.getCount();
            if (item == null) {
                if (firstVisblePos == 0 && itemCount > 0) {
                    item = mAdapter.getAgendaItemByPosition(1, false);
                }
            } else if (item.id == 0) {
                if (itemCount > 0) {
                    item = mAdapter.getAgendaItemByPosition(firstVisblePos + 1, false);
                }
            }
            if (item != null) {
                long firstVisibleTime = mAgendaListView.getFirstVisibleTime(item);
                if (firstVisibleTime > 0) {
                    mTime.set(firstVisibleTime);
                    mController.setTime(firstVisibleTime);
                    outState.putLong(BUNDLE_KEY_RESTORE_TIME, firstVisibleTime);
                } else {
                    Log.i(TAG, "firstVisibleTime=" + firstVisibleTime);
                }
                // Tell AllInOne the event id of the first visible event in the list. The id will be
                // used in the GOTO when AllInOne is restored so that Agenda Fragment can select a
                // specific event and not just the time.
                mLastShownEventId = item.id;
                /*
                 * M: Save the first visible event id and time, because the
                 * fragment would use the two value to restore the position when
                 * the agenda fragment resumed.
                 */
                mLastHandledEventId = mLastShownEventId;
                mLastHandledEventTime = mTime;
            }
            /*
             * M: Store the top deviation data to make the rotary smoothly, get
             * it in onCreatView.
             */
            int[] listTopDeviationInfo = mAdapter.saveTopDeviation(mTime,
                    mLastShownEventId);
            outState.putIntArray(BUNDLE_KEY_RESTORE_TOP_DEVIATION,
                    listTopDeviationInfo);
        }
        if (DEBUG) {
            Log.v(TAG, "onSaveInstanceState " + mTime.toString());
        }

        long selectedInstance = mAgendaListView.getSelectedInstanceId();
        if (selectedInstance >= 0) {
            outState.putLong(BUNDLE_KEY_RESTORE_INSTANCE_ID, selectedInstance);
        }
        super.onSaveInstanceState(outState);
    }

    /**
     * This cleans up the event info fragment since the FragmentManager doesn't
     * handle nested fragments. Without this, the action bar buttons added by
     * the info fragment can come back on a rotation.
     *
     * @param fragmentManager
     */
    public void removeFragments(FragmentManager fragmentManager) {
        if (getActivity().isFinishing()) {
            return;
        }
        FragmentTransaction ft = fragmentManager.beginTransaction();
        Fragment f = fragmentManager.findFragmentById(R.id.agenda_event_info);
        if (f != null) {
            ft.remove(f);
        }
        ft.commit();
    }

    @Override
    public void onPause() {
        mAgendaListView.onPause();
        super.onPause();

//        mContentResolver.unregisterContentObserver(mObserver);
//        unregisterReceiver(mIntentReceiver);

        // Record Agenda View as the (new) default detailed view.
//        Utils.setDefaultView(this, CalendarApplication.AGENDA_VIEW_ID);
    }

    private void goTo(EventInfo event, boolean animate) {
        /// M: for special case of event id is -1,just clear all events   @{
        if (mIsTabletConfig && mShowEventDetailsWithAgenda && event.id == -1 && event.endTime != null) {
            showEventInfo(event, false, false);
            return;
        }
        /// @}
        if (event.selectedTime != null) {
            mTime.set(event.selectedTime);
        } else if (event.startTime != null) {
            mTime.set(event.startTime);
        }
        if (mAgendaListView == null) {
            // The view hasn't been set yet. Just save the time and use it
            // later.
            return;
        }
        mAgendaListView.goTo(mTime, event.id, mQuery, false,
                ((event.extraLong & CalendarController.EXTRA_GOTO_TODAY) != 0  &&
                        mShowEventDetailsWithAgenda) ? true : false, isMonthAgenda);
        AgendaAdapter.ViewHolder vh = mAgendaListView.getSelectedViewHolder();
        // Make sure that on the first time the event info is shown to recreate it
        Log.d(TAG, "selected viewholder is null: " + (vh == null));
        showEventInfo(event, vh != null ? vh.allDay : false, mForceReplace);
        mForceReplace = false;
    }

    private void search(String query, Time time) {
        mQuery = query;
        if (time != null) {
            mTime.set(time);
        }
        if (mAgendaListView == null) {
            // The view hasn't been set yet. Just return.
            return;
        }
        mAgendaListView.goTo(time, -1, mQuery, true, false, isMonthAgenda);
    }

    @Override
    public void eventsChanged() {
        if (mAgendaListView != null  ) {
        	/*PRIZE-add by-wanzhijuan-2015-6-29-start*/
            mAgendaListView.refresh(true, isMonthAgenda);
            /*PRIZE-add by-wanzhijuan-2015-6-29-end*/
            
            Log.d(TAG,"this is in eventsChanged()");
//            mAgendaListView.goTo(mTime, -1, mQuery, true, false, isMonthAgenda);
//            mAgendaListView.onResume();    
        }
    }
    @Override
	public void eventsChanged(Time selectedTime) {
		
		// TODO Auto-generated method stub
    	if (mAgendaListView != null) {
    		/*PRIZE-add by-wanzhijuan-2015-6-29-start*/
            mAgendaListView.refresh(true,selectedTime, isMonthAgenda);
            /*PRIZE-add by-wanzhijuan-2015-6-29-end*/
            Log.d(TAG,"this is in eventsChanged(Time selectedTime)"); 
//            mAgendaListView.goTo(mTime, -1, mQuery, true, false, isMonthAgenda);
//            mAgendaListView.onResume(); 
        }
    }

    @Override
    public long getSupportedEventTypes() {
        return EventType.GO_TO | EventType.EVENTS_CHANGED | ((mUsedForSearch)?EventType.SEARCH:0);
    }

    private long mLastHandledEventId = -1;
    private Time mLastHandledEventTime = null;
    @Override
    public void handleEvent(EventInfo event) {
        if (event.eventType == EventType.GO_TO) {
            /// M: for special case of event id is -1,just clear all events    @{
            if (mIsTabletConfig && mShowEventDetailsWithAgenda && event.id == -1 && event.endTime != null) {
                goTo(event, false);
                return;
            }
            /// @}
            // TODO support a range of time
            // TODO support event_id
            // TODO figure out the animate bit
            mLastHandledEventId = event.id;
            mLastHandledEventTime =
                    (event.selectedTime != null) ? event.selectedTime : event.startTime;
            goTo(event, true);
        } else if (event.eventType == EventType.SEARCH) {
            search(event.query, event.startTime);
        } else if (event.eventType == EventType.EVENTS_CHANGED) {
            eventsChanged();
        }
    }

    public long getLastShowEventId() {
        return mLastShownEventId;
    }

    // Shows the selected event in the Agenda view
    private void showEventInfo(EventInfo event, boolean allDay, boolean replaceFragment) {
        /// M: for special case of event id is -1 ,just clear all events   @{
        if (mIsTabletConfig && mShowEventDetailsWithAgenda && event.id == -1 && event.endTime != null) {
            mEventView.setVisibility(View.INVISIBLE);
            Log.d(TAG, "showEventInfo, event ID is -1, set eventView INVISIBLE ");
            return;
        }
        if (mIsTabletConfig && mShowEventDetailsWithAgenda &&
                mEventView.getVisibility() == View.INVISIBLE) {
            mEventView.setVisibility(View.VISIBLE);
        }
        /// @}

        // Ignore unknown events
        if (event.id == -1) {
            Log.e(TAG, "showEventInfo, event ID = " + event.id);
            return;
        }

        mLastShownEventId = event.id;

        // Create a fragment to show the event to the side of the agenda list
        if (mShowEventDetailsWithAgenda) {
            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager == null) {
                // Got a goto event before the fragment finished attaching,
                // stash the event and handle it later.
                mOnAttachedInfo = event;
                mOnAttachAllDay = allDay;
                return;
            }
            FragmentTransaction ft = fragmentManager.beginTransaction();

            if (allDay) {
                event.startTime.timezone = Time.TIMEZONE_UTC;
                event.endTime.timezone = Time.TIMEZONE_UTC;
            }

            if (DEBUG) {
                Log.d(TAG, "***");
                Log.d(TAG, "showEventInfo: start: " + new Date(event.startTime.toMillis(true)));
                Log.d(TAG, "showEventInfo: end: " + new Date(event.endTime.toMillis(true)));
                Log.d(TAG, "showEventInfo: all day: " + allDay);
                Log.d(TAG, "***");
            }

            long startMillis = event.startTime.toMillis(true);
            long endMillis = event.endTime.toMillis(true);
            EventInfoFragment fOld =
                    (EventInfoFragment)fragmentManager.findFragmentById(R.id.agenda_event_info);
            if (fOld == null || replaceFragment || fOld.getStartMillis() != startMillis ||
                    fOld.getEndMillis() != endMillis || fOld.getEventId() != event.id) {
                mEventFragment = new EventInfoFragment(mActivity, event.id,
                        startMillis, endMillis,
                        Attendees.ATTENDEE_STATUS_NONE, false,
                        EventInfoFragment.DIALOG_WINDOW_STYLE, null);
                ft.replace(R.id.agenda_event_info, mEventFragment);
                ft.commit();
            } else {
                fOld.reloadEvents();
            }
        }
    }

    // OnScrollListener implementation to update the date on the pull-down menu of the app

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // Save scroll state so that the adapter can stop the scroll when the
        // agenda list is fling state and it needs to set the agenda list to a new position
        if (mAdapter != null) {
            mAdapter.setScrollState(scrollState);
        }
    }

    // Gets the time of the first visible view. If it is a new time, send a message to update
    // the time on the ActionBar
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        int julianDay = mAgendaListView.getJulianDayFromPosition(firstVisibleItem
                - mAgendaListView.getHeaderViewsCount());
        // On error - leave the old view
        if (julianDay == 0) {
            return;
        }
        // If the day changed, update the ActionBar
        if (mJulianDayOnTop != julianDay) {
            mJulianDayOnTop = julianDay;
            mController.setTime(getTimeOnTopEvent().toMillis(true));
            // Cannot sent a message that eventually may change the layout of the views
            // so instead post a runnable that will run when the layout is done
            if (!mIsTabletConfig) {
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        Time t = getTimeOnTopEvent();
                        mController.sendEvent(this, EventType.UPDATE_TITLE, t, t, null, -1,
                                ViewType.CURRENT, 0, null, null);
                    }
                });
            }
        }
    }

    /**
     * M: for sub class override, to inflate the view
     * @param inflater
     * @return agenda view or Events pick view
     */
    protected View extInflateFragmentView(LayoutInflater inflater) {
        return inflater.inflate(R.layout.agenda_fragment, null);
    }

    /**
     * M: for sub class override, to get the AgendaListView
     * @param v
     * @return AgendaListView or EventsListView
     */
    protected AgendaListView extFindListView(View v) {
        return (AgendaListView) v.findViewById(R.id.agenda_events_list);
    }

    /**
     * M:Get the time On Top Event
     * @return
     */
    private Time getTimeOnTopEvent() {
        Time t = new Time(mTimeZone);
        t.setJulianDay(mJulianDayOnTop);
        t.hour = mOriginalTime.hour;
        t.minute = mOriginalTime.minute;
        t.second = mOriginalTime.second;
        return t;
    }
    ///M:The time that the event to show when enter Agenda Fragement
    private Time mOriginalTime = new Time();
}
