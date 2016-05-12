package com.macoscope.gcbwatchface;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Instances;
import android.support.v4.app.ActivityCompat;
import android.support.wearable.provider.WearableCalendarContract;
import android.util.Log;

import com.eccyan.optional.Optional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class EventsRepository {

    // Projection array. Creating indices for this array instead of doing  dynamic lookups improves performance.
    private static final String[] CALENDAR_PROJECTION = new String[]{
            Calendars._ID,                           // 0
            Calendars.CALENDAR_DISPLAY_NAME          // 1
    };

    private static final String CALENDAR_SELECTION = Calendars._ID + " = ?";


    // The indices for the projection array above.
    private static final int PROJECTION_CALENDAR_ID_INDEX = 0;
    private static final int PROJECTION_CALENDAR_DISPLAY_NAME_INDEX = 1;

    private static final String[] INSTANCE_PROJECTION = new String[]{
            Instances.EVENT_ID,      // 0
            Instances.BEGIN,         // 1
            Instances.TITLE          // 2
    };

    // The indices for the projection array above.
    private static final int PROJECTION_INSTANCE_ID_INDEX = 0;
    private static final int PROJECTION_INSTANCE_BEGIN_INDEX = 1;
    private static final int PROJECTION_INSTANCE_TITLE_INDEX = 2;

    private static final String INSTANCE_SELECTION = Instances.CALENDAR_ID + " = ?" +
            " AND " + Instances.ALL_DAY + " =0";

    private ContentResolver contentResolver;

    public EventsRepository(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    public Optional<String> getCalendarDisplayName(int calendarId) {
        Uri uri = WearableCalendarContract.Instances.CONTENT_URI.buildUpon().build();
        Cursor cursor = contentResolver.query(uri, CALENDAR_PROJECTION, CALENDAR_SELECTION,
                new String[]{String.format("%d", calendarId)}, null);
        Optional<String> calendarName = Optional.empty();
        if (cursor.moveToFirst()) {
            calendarName = Optional.ofNullable(cursor.getString(PROJECTION_CALENDAR_DISPLAY_NAME_INDEX));
        }
        cursor.close();
        return calendarName;
    }

    public void logCalendarsDisplayName(Context context) {
        Uri uri = Calendars.CONTENT_URI;
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager
                .PERMISSION_GRANTED) {
            return;
        }
        Cursor cursor = contentResolver.query(uri, CALENDAR_PROJECTION, null, null, null);
        String calendarName;
        int calendarId;
        while (cursor.moveToNext()) {
            calendarName = cursor.getString(PROJECTION_CALENDAR_DISPLAY_NAME_INDEX);
            calendarId = cursor.getInt(PROJECTION_CALENDAR_ID_INDEX);
            Log.d("CALENDAR", calendarId + " : " + calendarName);
        }
        cursor.close();
    }

    public Optional<List<EventModel>> getEvents(int calendarId, long timeInterval, TimeUnit timeUnit) {
        Uri.Builder eventsUriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        long timeFrom = System.currentTimeMillis();
        long timeTo = timeFrom + timeUnit.toMillis(timeInterval);
        ContentUris.appendId(eventsUriBuilder, timeFrom);
        ContentUris.appendId(eventsUriBuilder, timeTo);
        Uri uri = eventsUriBuilder.build();
        Cursor cursor = contentResolver.query(uri, INSTANCE_PROJECTION, INSTANCE_SELECTION,
                new String[]{Integer.toString(calendarId)}, null);
        int cursorCount = cursor.getCount();
        if (cursorCount == 0) {
            cursor.close();
            return Optional.empty();
        }
        List<EventModel> events = new ArrayList<>(cursorCount);

        while (cursor.moveToNext()) {
            long startDate = cursor.getLong(PROJECTION_INSTANCE_BEGIN_INDEX);
            if (startDate > timeFrom) {
                EventModel event = new EventModel(
                        cursor.getLong(PROJECTION_INSTANCE_ID_INDEX),
                        cursor.getString(PROJECTION_INSTANCE_TITLE_INDEX),
                        cursor.getLong(PROJECTION_INSTANCE_BEGIN_INDEX));
                events.add(event);
            }

        }
//        Collections.sort(events, new Comparator<EventModel>() {
//            @Override
//            public int compare(EventModel lhs, EventModel rhs) {
//                if (lhs.isAfter(rhs.getStartDate())) {
//                    return 1;
//                } else if (rhs.isAfter(lhs.getStartDate())) {
//                    return -1;
//                } else {
//                    return 0;
//                }
//            }
//        });

        cursor.close();
        return Optional.of(events);
    }

}
