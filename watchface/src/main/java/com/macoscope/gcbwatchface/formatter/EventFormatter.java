package com.macoscope.gcbwatchface.formatter;

import android.content.res.Resources;

import com.macoscope.gcbmodel.Event;
import com.macoscope.gcbwatchface.R;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class EventFormatter {
    private Calendar calendar;
    private Event event;

    public EventFormatter() {
        this.calendar = new GregorianCalendar();
    }

    public void setTimeZone(TimeZone timeZone){
        calendar.setTimeZone(timeZone);
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public String getName() {
        return event.getTitle();
    }

    public String getCalendarName() {
        return event.getCalendarDisplayName();
    }

    public String getMinutesToEventString(Resources resources, long datePointTime) {
        long minutes = event.getMinutesToEvent(datePointTime);
        if (minutes > 0) {
            return resources.getQuantityString(R.plurals.minutes, (int) minutes, minutes);
        } else {
            return resources.getString(R.string.less_than_minute);
        }
    }

    public int getHourMinutes() {
        calendar.setTimeInMillis(event.getStartDate());
        return calendar.get(Calendar.MINUTE);
    }

    public boolean hasEvent() {
        return event != null;
    }

    public void clearEvent() {
        event = null;
    }
}
