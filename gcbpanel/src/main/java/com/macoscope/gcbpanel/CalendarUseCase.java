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
import rx.Subscriber;
import rx.functions.Func1;

public class CalendarUseCase {

    private CalendarService calendarService;

    public CalendarUseCase(GoogleAccountCredential googleAccountCredential) {
        setCredentials(googleAccountCredential);
    }

    public void setCredentials(GoogleAccountCredential googleAccountCredential) {
        calendarService = new CalendarService(googleAccountCredential);
    }

    public Observable<Optional<List<CalendarListEntry>>> getCalendars() {
        return Observable.create(new Observable.OnSubscribe<Optional<List<CalendarListEntry>>>() {
            @Override
            public void call(Subscriber<? super Optional<List<CalendarListEntry>>> subscriber) {
                try {
                    Optional<CalendarList> calendarList = calendarService.getCalendars();
                    if (calendarList.isPresent()) {
                        subscriber.onNext(Optional.of(calendarList.get().getItems()));
                    }
                } catch (Exception error) {
                    subscriber.onError(error);
                }
            }
        });
    }

    public Observable<Optional<Pair<CharSequence[], CharSequence[]>>> getPreferenceListCalendarsArrays() {
        return getCalendars().map(new Func1<Optional<List<CalendarListEntry>>, Optional<Pair<CharSequence[], CharSequence[]>>>() {
            @Override
            public Optional<Pair<CharSequence[], CharSequence[]>> call(Optional<List<CalendarListEntry>> listOptional) {
                if (listOptional.isPresent() && !listOptional.get().isEmpty()) {
                    List<CalendarListEntry> calendarEntries = listOptional.get();
                    int size = calendarEntries.size();
                    CharSequence[] entries = new CharSequence[size];
                    CharSequence[] values = new CharSequence[size];
                    for (int i = 0; i < calendarEntries.size(); i++) {
                        entries[i] = calendarEntries.get(i).getSummary();
                        values[i] = calendarEntries.get(i).getId();
                    }
                    return Optional.of(new Pair<>(entries, values));
                } else {
                    return Optional.empty();
                }
            }
        });
    }

    public Observable<Optional<List<Event>>> getEvents(final String calendarId, final int maxResults) {
        return Observable.create(new Observable.OnSubscribe<Optional<List<Event>>>() {
            @Override
            public void call(Subscriber<? super Optional<List<Event>>> subscriber) {
                try {
                    Optional<Events> calendarList = calendarService.getEvents(calendarId, maxResults);
                    if (calendarList.isPresent()) {
                        subscriber.onNext(Optional.of(calendarList.get().getItems()));
                    }
                } catch (Exception error) {
                    subscriber.onError(error);
                }
            }
        });
    }

}
