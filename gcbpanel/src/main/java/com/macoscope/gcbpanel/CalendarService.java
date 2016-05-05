package com.macoscope.gcbpanel;

import com.eccyan.optional.Optional;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import java.io.IOException;
import java.util.List;

public class CalendarService {
    private Calendar calendarService;

    public CalendarService(GoogleAccountCredential credential) {
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        calendarService = new Calendar.Builder(
                transport, jsonFactory, credential)
                .setApplicationName("GCB watch face")
                .build();
    }

    /**
     * Fetch a list of the next events from the specific calendar.
     */
    private Optional<List<CalendarListEntry>> getCalendars() {
        CalendarList events;
        try {
            events = calendarService.calendarList().list().execute();
        } catch (IOException exception) {
            exception.printStackTrace();
            return Optional.empty();
        }
        return Optional.of(events.getItems());
    }

    /**
     * Fetch a list of the next events from the specific calendar.
     */
    private Optional<List<Event>> getEvents(String calendarId, int maxResults) {
        DateTime now = new DateTime(System.currentTimeMillis());
        Events events;
        try {
            events = calendarService.events().list(calendarId)
                    .setMaxResults(maxResults)
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
        } catch (IOException exception) {
            exception.printStackTrace();
            return Optional.empty();
        }
        return Optional.of(events.getItems());
    }
}
