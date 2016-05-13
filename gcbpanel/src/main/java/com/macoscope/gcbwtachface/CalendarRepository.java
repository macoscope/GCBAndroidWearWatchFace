package com.macoscope.gcbwtachface;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Instances;

import com.eccyan.optional.Optional;
import com.macoscpoe.gcbmodel.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CalendarRepository {

    // Projection array. Creating indices for this array instead of doing  dynamic lookups improves performance.
    private static final String[] CALENDAR_PROJECTION = new String[]{
            Calendars._ID,
            Calendars.CALENDAR_DISPLAY_NAME
    };

    private static final String CALENDAR_SELECTION = Calendars._ID + " = ?";

    private static final String CALENDARS_SELECTION = Calendars.ACCOUNT_NAME + " = ?";

    // The indices for the projection array above.
    private static final int PROJECTION_CALENDAR_ID_INDEX = 0;
    private static final int PROJECTION_CALENDAR_DISPLAY_NAME_INDEX = 1;

    private static final String[] INSTANCE_PROJECTION = new String[]{
            Instances.EVENT_ID,
            Instances.BEGIN,
            Instances.TITLE
    };

    // The indices for the projection array above.
    private static final int PROJECTION_INSTANCE_ID_INDEX = 0;
    private static final int PROJECTION_INSTANCE_BEGIN_INDEX = 1;
    private static final int PROJECTION_INSTANCE_TITLE_INDEX = 2;

    private static final String INSTANCE_SELECTION = Instances.CALENDAR_ID + " = ?" +
            " AND " + Instances.ALL_DAY + " =0";

    private ContentResolver contentResolver;

    public CalendarRepository(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    public List<CalendarModel> getCalendars(String account) {
        Uri uri = Calendars.CONTENT_URI;
        Cursor cursor = contentResolver.query(uri, CALENDAR_PROJECTION, CALENDARS_SELECTION, new String[]{account}, null);
        List<CalendarModel> calendarModels = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            CalendarModel calendarModel = new CalendarModel(cursor.getLong(PROJECTION_CALENDAR_ID_INDEX),
                    cursor.getString(PROJECTION_CALENDAR_DISPLAY_NAME_INDEX));
            calendarModels.add(calendarModel);
        }
        cursor.close();
        return calendarModels;
    }


    private Optional<String> getCalendarDisplayName(long calendarId) {
        Uri uri = Calendars.CONTENT_URI;
        Cursor cursor = contentResolver.query(uri, CALENDAR_PROJECTION, CALENDAR_SELECTION,
                new String[]{Long.toString(calendarId)}, null);
        Optional<String> calendarName = Optional.empty();
        if (cursor.moveToFirst()) {
            calendarName = Optional.of(cursor.getString(PROJECTION_CALENDAR_DISPLAY_NAME_INDEX));
        }
        cursor.close();
        return calendarName;
    }

    public List<Event> getEvents(long calendarId, long timeInterval, TimeUnit timeUnit) {
        Optional<String> calendarNameOptional = getCalendarDisplayName(calendarId);
        String calendarName = calendarNameOptional.isPresent() ? calendarNameOptional.get() : "";
        Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
        long now = System.currentTimeMillis();
        ContentUris.appendId(builder, now);
        ContentUris.appendId(builder, now + timeUnit.toMillis(timeInterval));

        Cursor cursor = contentResolver.query(builder.build(), INSTANCE_PROJECTION,
                INSTANCE_SELECTION, new String[]{Long.toString(calendarId)}, null);

        List<Event> events = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            Event event = new Event(cursor.getLong(PROJECTION_INSTANCE_ID_INDEX),
                    cursor.getLong(PROJECTION_INSTANCE_BEGIN_INDEX),
                    cursor.getString(PROJECTION_INSTANCE_TITLE_INDEX), calendarName);
            events.add(event);
        }
        cursor.close();
        return events;
    }

}
