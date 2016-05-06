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
import com.google.api.services.calendar.model.Events;

import java.io.IOException;

public class CalendarService {
    private static final String DEFAULT_ORDER_BY = "startTime";
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
    public Optional<CalendarList> getCalendars() throws IOException {
        CalendarList calendarList = calendarService.calendarList().list().execute();
        return Optional.of(calendarList);
    }

    /**
     * Fetch a list of the next events from the specific calendar.
     */
    public Optional<Events> getEvents(String calendarId, int maxResults) throws IOException {
        DateTime now = new DateTime(System.currentTimeMillis());
        Events events = calendarService.events().list(calendarId)
                .setMaxResults(maxResults)
                .setTimeMin(now)
                .setOrderBy(DEFAULT_ORDER_BY)
                .setSingleEvents(true)
                .execute();
        return Optional.of(events);
    }
}
