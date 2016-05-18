package com.macoscope.gcbwatchface;


public class CalendarModel {
    private long id;
    private String displayName;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public CalendarModel(long id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }
}
