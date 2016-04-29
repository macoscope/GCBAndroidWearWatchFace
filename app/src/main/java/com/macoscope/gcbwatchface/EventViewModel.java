package com.macoscope.gcbwatchface;

import android.content.res.Resources;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

public class EventViewModel {
    private String name;
    private Date date;
    private String place;
    private Calendar calendar;

    public EventViewModel(String name, Date date, String place) {
        calendar = new GregorianCalendar();
        setName(name);
        setDate(date);
        setCalendarDate(date);
        setPlace(place);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
        setCalendarDate(date);
    }

    private void setCalendarDate(Date date) {
        calendar.setTime(date);
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getMinutesToEventString(Resources resources, long datePointTime) {
        long minutes = getMinutesToEvent(datePointTime);
        return resources.getQuantityString(R.plurals.minutes, (int) minutes, minutes);
    }
    //TODO move to DataModel when ready
    public long getMinutesToEvent(long datePointTime){
        return TimeUnit.MILLISECONDS.toMinutes(date.getTime() - datePointTime);
    }
    //TODO move to DataModel when ready
    public int getHourMinutes() {
        return calendar.get(Calendar.MINUTE);
    }
}
