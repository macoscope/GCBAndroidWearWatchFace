package com.macoscope.gcbwatchface;


import android.content.Context;

import com.eccyan.optional.Optional;

import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Func0;

public class CalendarUseCase {

    private EventsRepository eventsRepository;
    private Context context;

    public CalendarUseCase(Context context) {
        this.context = context;
        this.eventsRepository = new EventsRepository(context.getContentResolver());
    }

    public Observable<Optional<String>> getCalendarDisplayName(final int calendarId) {
        return Observable.defer(new Func0<Observable<Optional<String>>>() {
            @Override
            public Observable<Optional<String>> call() {
                return Observable.just(eventsRepository.getCalendarDisplayName(calendarId));
            }
        });
    }

    public Observable<Optional<List<EventModel>>> getEvents(final int calendarId, long timeInterval, TimeUnit timeUnit) {
        return Observable.just(eventsRepository.getEvents(calendarId, timeInterval, timeUnit));
    }

    public void logCalendars(){
        eventsRepository.logCalendarsDisplayName(context);
    }
}
