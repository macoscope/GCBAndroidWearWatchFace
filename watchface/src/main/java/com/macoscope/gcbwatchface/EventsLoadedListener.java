package com.macoscope.gcbwatchface;


import java.util.List;

public interface EventsLoadedListener {
    void onEventsLoaded(List<EventModel> events);
    void onNoEventsLoaded();
}
