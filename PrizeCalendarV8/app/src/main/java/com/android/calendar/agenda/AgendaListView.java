/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2009 The Android Open Source Project
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

import com.android.calendar.AllInOneActivity;
import com.android.calendar.CalendarController;
import com.android.calendar.CalendarController.EventType;
import com.android.calendar.DeleteEventHelper;
import com.android.calendar.R;
import com.android.calendar.Utils;
import com.android.calendar.agenda.AgendaWindowAdapter.AgendaItem;
import com.android.calendar.agenda.AgendaWindowAdapter.DayAdapterInfo;
import com.mediatek.calendar.LogUtil;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.provider.CalendarContract.Attendees;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

public class AgendaListView extends ListView implements OnItemClickListener {

    private static final String TAG = "AgendaListView";
    private static final boolean DEBUG = false;
    private static final int EVENT_UPDATE_TIME = 300000;  // 5 minutes

    private AgendaWindowAdapter mWindowAdapter;
    private DeleteEventHelper mDeleteEventHelper;
    private Context mContext;
    private String mTimeZone;
    private Time mTime;
    private boolean mShowEventDetailsWithAgenda;
    private Handler mHandler = null;

    boolean isMonthAgenda = false;
    private final Runnable mTZUpdater = new Runnable() {
        @Override
        public void run() {
            mTimeZone = Utils.getTimeZone(mContext, this);
            mTime.switchTimezone(mTimeZone);
        }
    };

    // runs every midnight and refreshes the view in order to update the past/present
    // separator
    private final Runnable mMidnightUpdater = new Runnable() {
        @Override
        public void run() {
        	/*PRIZE-update date-wanzhijuan-2015-6-29-start*/
            refresh(true, isMonthAgenda);
            /*PRIZE-update date-2015-6-29-end*/
            Utils.setMidnightUpdater(mHandler, mMidnightUpdater, mTimeZone);
        }
    };

    // Runs every EVENT_UPDATE_TIME to gray out past events
    private final Runnable mPastEventUpdater = new Runnable() {
        @Override
        public void run() {
            if (updatePastEvents() == true) {
            	/*PRIZE-updatePastEvent-wanzhijuan-2015-6-29-start*/
                refresh(true, isMonthAgenda);
                /*PRIZE-updatePastEvent-wanzhijuan-2015-6-29-end*/
            }
            setPastEventsUpdater();
        }
    };

    public AgendaListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.d("hekeyi","[AgendaListView]-onMeasure   clickItem = "+clickItem);
        if(isMonthAgenda && !clickItem){
            this.setSelection(0);
        }
//        }

//        int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);//add by hekeyi for calendar v8.
//        super.onMeasure(widthMeasureSpec, expandSpec);
    }

    private void initView(Context context) {
        mContext = context;
        mTimeZone = Utils.getTimeZone(context, mTZUpdater);
        mTime = new Time(mTimeZone);
        mTime.setToNow();
        mTime.normalize(true);
      
        setOnItemClickListener(this);
        setVerticalScrollBarEnabled(false);

        ///M: Don't show details of an event for AgendaChoiceActivity.@{
        if (context instanceof com.mediatek.calendar.selectevent.AgendaChoiceActivity) {

            mWindowAdapter = new AgendaWindowAdapter(context, this, false, isMonthAgenda);
        } else {
            mWindowAdapter = new AgendaWindowAdapter(context, this, Utils.getConfigBool(context,
                    R.bool.show_event_details_with_agenda), isMonthAgenda);
        }
        ///@}

        mWindowAdapter.setSelectedInstanceId(-1/* TODO:instanceId */);
        setAdapter(mWindowAdapter);
        setCacheColorHint(context.getResources().getColor(R.color.agenda_item_not_selected));
        mDeleteEventHelper =
                new DeleteEventHelper(context, null, false /* don't exit when done */);

        ///M: Don't show details of an event for AgendaChoiceActivity.@{
        if (context instanceof com.mediatek.calendar.selectevent.AgendaChoiceActivity) {
            mShowEventDetailsWithAgenda = false;
        } else {
            mShowEventDetailsWithAgenda = Utils.getConfigBool(mContext, R.bool.show_event_details_with_agenda);
        }
        ///@}

        // Hide ListView dividers, they are done in the item views themselves
        setDivider(null);
        setDividerHeight(0);

        mHandler = new Handler();
    }

    // Sets a thread to run every EVENT_UPDATE_TIME in order to update the list
    // with grayed out past events
    private void setPastEventsUpdater() {

        // Run the thread in the nearest rounded EVENT_UPDATE_TIME
        long now = System.currentTimeMillis();
        long roundedTime = (now / EVENT_UPDATE_TIME) * EVENT_UPDATE_TIME;
        mHandler.removeCallbacks(mPastEventUpdater);
        mHandler.postDelayed(mPastEventUpdater, EVENT_UPDATE_TIME - (now - roundedTime));
    }

    // Stop the past events thread
    private void resetPastEventsUpdater() {
        mHandler.removeCallbacks(mPastEventUpdater);
    }

    // Go over all visible views and checks if all past events are grayed out.
    // Returns true is there is at least one event that ended and it is not
    // grayed out.
    private boolean updatePastEvents() {

        int childCount = getChildCount();
        boolean needUpdate = false;
        long now = System.currentTimeMillis();
        Time time = new Time(mTimeZone);
        time.set(now);
        int todayJulianDay = Time.getJulianDay(now, time.gmtoff);

        // Go over views in list
        for (int i = 0; i < childCount; ++i) {
            View listItem = getChildAt(i);
            Object o = listItem.getTag();
            if (o instanceof AgendaByDayAdapter.ViewHolder) {
                // day view - check if day in the past and not grayed yet
                AgendaByDayAdapter.ViewHolder holder = (AgendaByDayAdapter.ViewHolder) o;
                if (holder.julianDay <= todayJulianDay && !holder.grayed) {
                    needUpdate = true;
                    break;
                }
            } else if (o instanceof AgendaAdapter.ViewHolder) {
                // meeting view - check if event in the past or started already and not grayed yet
                // All day meetings for a day are grayed out
                AgendaAdapter.ViewHolder holder = (AgendaAdapter.ViewHolder) o;
                if (!holder.grayed && ((!holder.allDay && holder.startTimeMilli <= now) ||
                        (holder.allDay && holder.julianDay <= todayJulianDay))) {
                    needUpdate = true;
                    break;
                }
            }
        }
        return needUpdate;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mWindowAdapter.close();
    }

    private boolean clickItem = false;
    // Implementation of the interface OnItemClickListener
    @Override
    public void onItemClick(AdapterView<?> a, View v, int position, long id) {
        if (id != -1) {
            clickItem = true;
            // Switch to the EventInfo view
            AgendaItem item = mWindowAdapter.getAgendaItemByPosition(position);
            long oldInstanceId = mWindowAdapter.getSelectedInstanceId();
            mWindowAdapter.setSelectedView(v);

            // If events are shown to the side of the agenda list , do nothing
            // when the same event is selected , otherwise show the selected event.

            if (item != null && (oldInstanceId != mWindowAdapter.getSelectedInstanceId() ||
                    !mShowEventDetailsWithAgenda)) {
                long startTime = item.begin;
                long endTime = item.end;
                // Holder in view holds the start of the specific part of a multi-day event ,
                // use it for the goto
                long holderStartTime;
                Object holder = v.getTag();
                if (holder instanceof AgendaAdapter.ViewHolder) {
                    holderStartTime = ((AgendaAdapter.ViewHolder) holder).startTimeMilli;
                } else {
                    holderStartTime = startTime;
                }
                if (item.allDay) {
                    startTime = Utils.convertAlldayLocalToUTC(mTime, startTime, mTimeZone);
                    endTime = Utils.convertAlldayLocalToUTC(mTime, endTime, mTimeZone);
                }
                mTime.set(startTime);
                CalendarController controller = CalendarController.getInstance(mContext);
                LogUtil.oi(TAG, "onItemClick, the clicked event id is " + item.id);
                controller.sendEventRelatedEventWithExtra(this, EventType.VIEW_EVENT, item.id,
                        startTime, endTime, 0, 0, CalendarController.EventInfo.buildViewExtraLong(
                                Attendees.ATTENDEE_STATUS_NONE, item.allDay), holderStartTime);
            }
        }
    }

    public void goTo(Time time, long id, String searchQuery, boolean forced,
            boolean refreshEventInfo,boolean isMonth) {
    	if(time != null){
    		Log.d(TAG,"in AgendaListView.goTo() Time = " + time.monthDay + ":" + time.hour + ":" + time.minute + " ,searchQuery = " + searchQuery    			
    			+ ",id = "+ id +",forced = " + forced + ",refreshEventInfo = " + refreshEventInfo + ",isMonth = " + isMonth);/*t f f*/
    	}else{
    		Log.d(TAG,"in AgendaListView.goTo() time = null" + " ,searchQuery = " + searchQuery    			
        			+ ",id = " + id + ",forced = " + forced + ",refreshEventInfo = " + refreshEventInfo + ",isMonth = " + isMonth);
    	}
    	
    	isMonthAgenda = isMonth;
        if (time == null) {
            time = mTime;
            long goToTime = getFirstVisibleTime(null);
            if (goToTime <= 0) {
                goToTime = System.currentTimeMillis();
            }
            time.set(goToTime);
        }
        mTime.set(time);
        mTime.switchTimezone(mTimeZone);
        mTime.normalize(true);
        if (DEBUG) {
            Log.d(TAG, "Goto with time " + mTime.toString());
        }
        mWindowAdapter.refresh(mTime, id, searchQuery, forced, refreshEventInfo, isMonth);
    }

    /*PRIZE-calendar event show-wanzhijuan-2015-6-29-start*/
    //  when get date at onResume after user's onResume isMonthAgenda Interface matching
    public void refresh(boolean forced, boolean isMonth) {
    	isMonthAgenda = isMonth;
        mWindowAdapter.refresh(mTime, -1, null, forced, false, isMonthAgenda);
       
    }
    /**prize-calendar-hekeyi-2015.5.8-begin*/
    public void refresh(boolean forced,Time selectTime, boolean isMonth) {
    	isMonthAgenda = isMonth;
    	Log.d("hky", "AgendaListView.refresh  selectTime = "+selectTime +"  forced  = "+forced+"  isMonthAgenda = "+isMonthAgenda);
        mWindowAdapter.refresh(selectTime, -1, null, forced, false, isMonthAgenda);
      
    }

    
    /**prize-calendar-hekeyi-2015.5.8-end*/
    public void deleteSelectedEvent() {
        int position = getSelectedItemPosition();
        AgendaItem agendaItem = mWindowAdapter.getAgendaItemByPosition(position);
        if (agendaItem != null) {
            mDeleteEventHelper.delete(agendaItem.begin, agendaItem.end, agendaItem.id, -1);
        }
    }

    public View getFirstVisibleView() {
        Rect r = new Rect();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; ++i) {
            View listItem = getChildAt(i);
            listItem.getLocalVisibleRect(r);
            if (r.top >= 0) { // if visible
                return listItem;
            }
        }
        return null;
    }

    public long getSelectedTime() {
        int position = getSelectedItemPosition();
        if (position >= 0) {
            AgendaItem item = mWindowAdapter.getAgendaItemByPosition(position);
            if (item != null) {
                return item.begin;
            }
        }
        return getFirstVisibleTime(null);
    }

    public AgendaAdapter.ViewHolder getSelectedViewHolder() {
        return mWindowAdapter.getSelectedViewHolder();
    }

    public long getFirstVisibleTime(AgendaItem item) {
        AgendaItem agendaItem = item;
        if (item == null) {
            agendaItem = getFirstVisibleAgendaItem();
        }
        if (agendaItem != null) {
            Time t = new Time(mTimeZone);
            t.set(agendaItem.begin);
            // Save and restore the time since setJulianDay sets the time to 00:00:00
            int hour = t.hour;
            int minute = t.minute;
            int second = t.second;
            t.setJulianDay(agendaItem.startDay);
            t.hour = hour;
            t.minute = minute;
            t.second = second;
            if (DEBUG) {
                t.normalize(true);
                Log.d(TAG, "first position had time " + t.toString());
            }
            return t.normalize(false);
        }
        return 0;
    }

    public AgendaItem getFirstVisibleAgendaItem() {
        int position = getFirstVisiblePosition();
        if (DEBUG) {
            Log.v(TAG, "getFirstVisiblePosition = " + position);
        }

        // mShowEventDetailsWithAgenda == true implies we have a sticky header. In that case
        // we may need to take the second visible position, since the first one maybe the one
        // under the sticky header.
        if (mShowEventDetailsWithAgenda) {
            View v = getFirstVisibleView ();
            if (v != null) {
                Rect r = new Rect ();
                v.getLocalVisibleRect(r);
                if (r.bottom - r.top <=  mWindowAdapter.getStickyHeaderHeight()) {
                    position ++;
                }
            }
        }

        return mWindowAdapter.getAgendaItemByPosition(position,
                false /* startDay = date separator date instead of actual event startday */);

    }

    public int getJulianDayFromPosition(int position) {
        DayAdapterInfo info = mWindowAdapter.getAdapterInfoByPosition(position);
        if (info != null) {
            return info.dayAdapter.findJulianDayFromPosition(position - info.offset);
        }
        return 0;
    }

    // Finds is a specific event (defined by start time and id) is visible
    public boolean isAgendaItemVisible(Time startTime, long id) {

        if (id == -1 || startTime == null) {
            return false;
        }

        View child = getChildAt(0);
        // View not set yet, so not child - return
        if (child == null) {
            return false;
        }
        int start = getPositionForView(child);
        long milliTime = startTime.toMillis(true);
        int childCount = getChildCount();
        int eventsInAdapter = mWindowAdapter.getCount();

        for (int i = 0; i < childCount; i++) {
            if (i + start >= eventsInAdapter) {
                break;
            }
            AgendaItem agendaItem = mWindowAdapter.getAgendaItemByPosition(i + start);
            if (agendaItem == null) {
                continue;
            }
            if (agendaItem.id == id && agendaItem.begin == milliTime) {
                View listItem = getChildAt(i);
                if (listItem.getTop() <= getHeight() &&
                        listItem.getTop() >= mWindowAdapter.getStickyHeaderHeight()) {
                    return true;
                }
            }
        }
        return false;
    }

    public long getSelectedInstanceId() {
        return mWindowAdapter.getSelectedInstanceId();
    }

    public void setSelectedInstanceId(long id) {
        mWindowAdapter.setSelectedInstanceId(id);
    }

    // Move the currently selected or visible focus down by offset amount.
    // offset could be negative.
    public void shiftSelection(int offset) {
        shiftPosition(offset);
        int position = getSelectedItemPosition();
        if (position != INVALID_POSITION) {
            setSelectionFromTop(position + offset, 0);
        }
    }

    private void shiftPosition(int offset) {
        if (DEBUG) {
            Log.v(TAG, "Shifting position " + offset);
        }

        View firstVisibleItem = getFirstVisibleView();

        if (firstVisibleItem != null) {
            Rect r = new Rect();
            firstVisibleItem.getLocalVisibleRect(r);
            // if r.top is < 0, getChildAt(0) and getFirstVisiblePosition() is
            // returning an item above the first visible item.
            int position = getPositionForView(firstVisibleItem);
            setSelectionFromTop(position + offset, r.top > 0 ? -r.top : r.top);
            if (DEBUG) {
                if (firstVisibleItem.getTag() instanceof AgendaAdapter.ViewHolder) {
                    AgendaAdapter.ViewHolder viewHolder = (AgendaAdapter.ViewHolder) firstVisibleItem.getTag();
                    Log.v(TAG, "Shifting from " + position + " by " + offset + ". Title "
                            + viewHolder.title.getText());
                } else if (firstVisibleItem.getTag() instanceof AgendaByDayAdapter.ViewHolder) {
                    AgendaByDayAdapter.ViewHolder viewHolder =
                            (AgendaByDayAdapter.ViewHolder) firstVisibleItem.getTag();
                    Log.v(TAG, "Shifting from " + position + " by " + offset + ". Date  "
                            + viewHolder.dateView.getText());
                } else if (firstVisibleItem instanceof TextView) {
                    Log.v(TAG, "Shifting: Looking at header here. " + getSelectedItemPosition());
                }
            }
        } else if (getSelectedItemPosition() >= 0) {
            if (DEBUG) {
                Log.v(TAG, "Shifting selection from " + getSelectedItemPosition() +
                        " by " + offset);
            }
            setSelection(getSelectedItemPosition() + offset);
        }
    }

    public void setHideDeclinedEvents(boolean hideDeclined) {
        mWindowAdapter.setHideDeclinedEvents(hideDeclined);
    }

    public void onResume() {
        mTZUpdater.run();
        Utils.setMidnightUpdater(mHandler, mMidnightUpdater, mTimeZone);
        setPastEventsUpdater();
        mWindowAdapter.onResume();
        clickItem = false;
    }

    public void onPause() {
        Utils.resetMidnightUpdater(mHandler, mMidnightUpdater);
        resetPastEventsUpdater();
    }

    /**
     * M: for sub class to get event info
     * @param position
     * @return
     */
    protected long getEventIdByPosition(int position) {
        if (position > 0 && position <= mWindowAdapter.getCount()) {
            /// M: getAgendaItemByPosition() may return null
            AgendaItem item = mWindowAdapter.getAgendaItemByPosition(position);
            if (item != null) {
                return item.id;
            }
        }
        return -1;
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
//        getParent().requestDisallowInterceptTouchEvent(true);
        /*if(getRootView().findViewById(R.id.main_pane).findViewById(R.id.viewpager)!=null){
            getParent().requestDisallowInterceptTouchEvent(true);
        }*/
        /*int firstVisibleItem = getFirstVisiblePosition();

        View fistView = getFirstVisibleView();
        if(fistView == null) return false;
        boolean isFirstRow = false;
        boolean isLastRow = false;
        if (firstVisibleItem == 0) {
            if (fistView.getTop() >= 0) {
                isFirstRow = true;
            }
        }
        else if(getLastVisiblePosition()==(mWindowAdapter.getCount()-1)){
            isLastRow = true;
        }
        if((mWindowAdapter.getCount()>=6)&& (!isFirstRow)){
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        if(isFirstRow&&(disY>0)){
            getParent().requestDisallowInterceptTouchEvent(false);
        }
        if(isFirstRow&&(disY<0)){
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        if((mWindowAdapter.getCount()<3)||isLastRow&&(disY<0)){
            getParent().requestDisallowInterceptTouchEvent(false);
        }*/
        return super.dispatchTouchEvent(ev);
    }

    private float mDownY;
    private float disY;
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        /*switch (ev.getAction()){
            case MotionEvent.ACTION_DOWN:
                mDownY = ev.getY();
                break;
            case MotionEvent.ACTION_UP:
                disY = ev.getY() - mDownY;
                break;

        }*/
        /*if(getRootView().findViewById(R.id.main_pane).findViewById(R.id.viewpager)!=null){
            return false;
        }else {
            return super.onTouchEvent(ev);
        }*/
        return super.onTouchEvent(ev);
    }

    /*@Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int firstVisibleItem = getFirstVisiblePosition();
        View fistView = getFirstVisibleView();
        if(fistView == null) return false;
        boolean isFirstRow = false;
        boolean isLastRow = false;
        if (firstVisibleItem == 0) {
            if (fistView.getTop() >= 0) {
                isFirstRow = true;
            }
        }
        else if(getLastVisiblePosition()==(mWindowAdapter.getCount()-1)){
            isLastRow = true;
        }
        if((mWindowAdapter.getCount()>=6)&& (!isFirstRow)){
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        if(isFirstRow&&(disY>0)){
            getParent().requestDisallowInterceptTouchEvent(false);
        }
        if(isFirstRow&&(disY<0)){
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        if((mWindowAdapter.getCount()<3)||isLastRow&&(disY<0)){
            getParent().requestDisallowInterceptTouchEvent(false);
        }
        return super.onInterceptTouchEvent(ev);
    }*/

}
