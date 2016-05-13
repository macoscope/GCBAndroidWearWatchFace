package com.macoscope.gcbwtachface;

import com.macoscpoe.gcbmodel.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DummyEvents {

    public List<Event> getDummyEventsList(int count, long interval, TimeUnit timeUnit, String calendarName) {
        List<Event> events = new ArrayList<>(count);
        long intervalMs = timeUnit.toMillis(interval);
        long eventStartDate = System.currentTimeMillis() + intervalMs;
        for (int i = 0; i < count; i++) {
            events.add(new Event(i, eventStartDate, "Event "+i, calendarName));
            eventStartDate += intervalMs;
        }
        return events;
    }
}
