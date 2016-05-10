package com.macoscope.gcbwatchface;

import java.util.concurrent.TimeUnit;

public class EventModel {
    private final long id;
    private String title;
    private long startDate;

    public EventModel(long id, String title, long startDate) {
        this.id = id;
        this.title = title;
        this.startDate = startDate;
    }

    public String getTitle() {
        return title;
    }

    public long getStartDate() {
        return startDate;
    }

    public long getMinutesToEvent(long datePointTime){
        return TimeUnit.MILLISECONDS.toMinutes(startDate - datePointTime);
    }

    public boolean isAfter(long datePointTime){
        return datePointTime < startDate;
    }
}
