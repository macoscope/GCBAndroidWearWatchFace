package com.macoscpoe.gcbwatchface;

import android.content.res.Resources;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

public class EventViewModel {
    private String name;
    private Date date;
    private String place;
    private GregorianCalendar calendar;

    public EventViewModel(String name, Date date, String place) {
        setName(name);
        setDate(date);
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
        calendar = new GregorianCalendar();
        calendar.setTime(date);
        this.date = date;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getMinutesToEvent(Resources resources, long datePointTime) {
        int minutes = (int) TimeUnit.MILLISECONDS.toMinutes(date.getTime() - datePointTime);
        return resources.getQuantityString(R.plurals.minutes, minutes, minutes);
    }

    public int getHourMinutes() {
        return calendar.get(Calendar.MINUTE);
    }
}
