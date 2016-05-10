package com.macoscope.gcbwatchface;

import android.content.res.Resources;

import java.util.Calendar;

public class EventFormatter {
    private String calendarName;
    private Calendar calendar;
    private EventModel event;

    public EventFormatter(Calendar calendar) {
        this.calendar = calendar;
    }

    public void setEvent(EventModel event) {
        this.event = event;
    }

    public String getName() {
        return event.getTitle();
    }

    public String getCalendarName() {
        return calendarName;
    }

    public void setCalendarName(String calendarName) {
        this.calendarName = calendarName;
    }

    public String getMinutesToEventString(Resources resources, long datePointTime) {
        long minutes = event.getMinutesToEvent(datePointTime);
        return resources.getQuantityString(R.plurals.minutes, (int) minutes, minutes);
    }

    public int getHourMinutes() {
        calendar.setTimeInMillis(event.getStartDate());
        return calendar.get(Calendar.MINUTE);
    }

    public boolean isReadyToDraw(){
        return event != null;
    }


}
