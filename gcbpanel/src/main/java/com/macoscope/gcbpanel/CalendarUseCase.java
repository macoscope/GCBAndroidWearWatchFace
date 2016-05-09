package com.macoscope.gcbpanel;

import android.util.Pair;

import com.eccyan.optional.Optional;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import java.util.List;

import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;

public class CalendarUseCase {

    private CalendarApiService calendarApiService;

    private Func1 calendarsListToPreferenceArraysMapFunction =
            new Func1<Optional<List<CalendarListEntry>>, Optional<Pair<CharSequence[], CharSequence[]>>>() {
                @Override
                public Optional<Pair<CharSequence[], CharSequence[]>> call(Optional<List<CalendarListEntry>> listOptional) {
                    if (listOptional.isPresent() && !listOptional.get().isEmpty()) {
                        return getPreferenceListEntryValueArraysPair(listOptional.get());
                    } else {
                        return Optional.empty();
                    }
                }
            };

    public CalendarUseCase(GoogleAccountCredential googleAccountCredential) {
        setCredentials(googleAccountCredential);
    }

    public void setCredentials(GoogleAccountCredential googleAccountCredential) {
        calendarApiService = new CalendarApiService(googleAccountCredential);
    }

    public Observable<Optional<List<CalendarListEntry>>> getCalendars() {
        return Observable.defer(new Func0<Observable<Optional<List<CalendarListEntry>>>>() {
            @Override
            public Observable<Optional<List<CalendarListEntry>>> call() {
                try {
                    Optional<CalendarList> calendarList = calendarApiService.getCalendars();
                    if (calendarList.isPresent()) {
                        return Observable.just(Optional.of(calendarList.get().getItems()));
                    }
                } catch (Exception error) {
                    return Observable.error(error);
                }
                return Observable.just(Optional.<List<CalendarListEntry>>empty());
            }
        });
    }

    public Observable<Optional<Pair<CharSequence[], CharSequence[]>>> getPreferenceListCalendarsArrays() {
        return getCalendars().map(calendarsListToPreferenceArraysMapFunction);
    }

    private Optional<Pair<CharSequence[], CharSequence[]>> getPreferenceListEntryValueArraysPair(List<CalendarListEntry> calendarEntries) {
        int size = calendarEntries.size();
        CharSequence[] entries = new CharSequence[size];
        CharSequence[] values = new CharSequence[size];
        for (int i = 0; i < calendarEntries.size(); i++) {
            entries[i] = calendarEntries.get(i).getSummary();
            values[i] = calendarEntries.get(i).getId();
        }
        return Optional.of(new Pair<>(entries, values));
    }

    public Observable<Optional<List<Event>>> getEvents(final String calendarId, final int maxResults) {
        return Observable.defer(new Func0<Observable<Optional<List<Event>>>>() {
            @Override
            public Observable<Optional<List<Event>>> call() {
                try {
                    Optional<Events> calendarList = calendarApiService.getEvents(calendarId, maxResults);
                    if (calendarList.isPresent()) {
                        return Observable.just(Optional.of(calendarList.get().getItems()));
                    }
                } catch (Exception error) {
                    return Observable.error(error);
                }
                return Observable.just(Optional.<List<Event>>empty());
            }
        });

    }

}
