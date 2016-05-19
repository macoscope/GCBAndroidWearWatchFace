package com.macoscpoe.gcbmodel;

import java.util.concurrent.TimeUnit;

public class Event {
    private long id;
    private long startDate;
    private String title;
    private String calendarDisplayName;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCalendarDisplayName() {
        return calendarDisplayName;
    }

    public void setCalendarDisplayName(String calendarDisplayName) {
        this.calendarDisplayName = calendarDisplayName;
    }

    public Event(long id, long startDate, String title, String calendarDisplayName) {
        this.id = id;
        this.startDate = startDate;
        this.title = title;
        this.calendarDisplayName = calendarDisplayName;
    }

    public long getMinutesToEvent(long datePointTime){
        return TimeUnit.MILLISECONDS.toMinutes(startDate - datePointTime);
    }

    public boolean isValidRelativeTo(long time){
        return startDate > time;
    }
}
