package com.macoscope.gcbpanel;

import com.eccyan.optional.Optional;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import java.util.List;

import rx.Observable;
import rx.Subscriber;

public class CalendarUseCase {

    private CalendarService calendarService;

    public CalendarUseCase(GoogleAccountCredential googleAccountCredential) {
        setCredentials(googleAccountCredential);
    }

    public void setCredentials(GoogleAccountCredential googleAccountCredential) {
        calendarService = new CalendarService(googleAccountCredential);
    }

    public Observable<Optional<List<CalendarListEntry>>> getCalendars(){
        return Observable.create(new Observable.OnSubscribe<Optional<List<CalendarListEntry>>>() {
            @Override
            public void call(Subscriber<? super Optional<List<CalendarListEntry>>> subscriber) {
                try {
                    Optional<CalendarList> calendarList = calendarService.getCalendars();
                    if(calendarList.isPresent()){
                        subscriber.onNext(Optional.of(calendarList.get().getItems()));
                    }
                } catch (Exception error) {
                    subscriber.onError(error);
                }
            }
        });
    }

    public Observable<Optional<List<Event>>> getEvents(final String calendarId, final int maxResults){
        return Observable.create(new Observable.OnSubscribe<Optional<List<Event>>>() {
            @Override
            public void call(Subscriber<? super Optional<List<Event>>> subscriber) {
                try {
                    Optional<Events> calendarList = calendarService.getEvents(calendarId, maxResults);
                    if(calendarList.isPresent()){
                        subscriber.onNext(Optional.of(calendarList.get().getItems()));
                    }
                } catch (Exception error) {
                    subscriber.onError(error);
                }
            }
        });
    }

}
