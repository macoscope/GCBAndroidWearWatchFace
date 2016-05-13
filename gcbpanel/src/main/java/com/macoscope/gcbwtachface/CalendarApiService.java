package com.macoscope.gcbwtachface;

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

public class CalendarApiService {
    private static final String APPLICATION_NAME = "GCB watch face";
    private static final String DEFAULT_ORDER_BY = "startTime";
    private Calendar calendarService;

    public CalendarApiService(GoogleAccountCredential credential) {
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        calendarService = new Calendar.Builder(
                transport, jsonFactory, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Fetch a list of calendars for given user.
     */
    public CalendarList getCalendars() throws IOException {
        CalendarList calendarList = calendarService.calendarList().list().execute();
        return calendarList;
    }

    /**
     * Fetch a list of the next events from the specific calendar.
     */
    public Events getEvents(String calendarId, int maxResults) throws IOException {
        DateTime now = new DateTime(System.currentTimeMillis());
        Events events = calendarService.events().list(calendarId)
                .setMaxResults(maxResults)
                .setTimeMin(now)
                .setOrderBy(DEFAULT_ORDER_BY)
                .setSingleEvents(true)
                .execute();
        return events;
    }
}
